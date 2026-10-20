package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.dto.EquipmentLeaderboardPositionDTO;
import com.pingpong.dto.LeaderboardBoardDTO;
import com.pingpong.dto.LeaderboardItemDTO;
import com.pingpong.dto.LeaderboardShareRequestDTO;
import com.pingpong.dto.LeaderboardShareResponseDTO;
import com.pingpong.entity.Comment;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.entity.LeaderboardShareStat;
import com.pingpong.entity.LeaderboardSnapshot;
import com.pingpong.entity.LeaderboardSubscription;
import com.pingpong.entity.ScheduleJobLock;
import com.pingpong.entity.User;
import com.pingpong.enums.LeaderboardType;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import com.pingpong.mapper.LeaderboardShareStatMapper;
import com.pingpong.mapper.LeaderboardSnapshotMapper;
import com.pingpong.mapper.LeaderboardSubscriptionMapper;
import com.pingpong.mapper.ScheduleJobLockMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.realtime.RealtimeEventPublisher;
import com.pingpong.realtime.RealtimeEventType;
import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final BigDecimal FAVORITE_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal COMMENT_WEIGHT = new BigDecimal("0.2");
    private static final BigDecimal RATING_WEIGHT = new BigDecimal("0.5");
    private static final String FULL_REFRESH = "FULL";
    private static final String INCREMENTAL_REFRESH = "INCREMENTAL";
    private static final int MAX_BOARD_ITEMS = 50;
    private static final int SEGMENT_BEGINNER = 1;
    private static final int SEGMENT_PRO = 2;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderboardSnapshotMapper leaderboardSnapshotMapper;

    @Autowired
    private LeaderboardSubscriptionMapper leaderboardSubscriptionMapper;

    @Autowired
    private LeaderboardShareStatMapper leaderboardShareStatMapper;

    @Autowired
    private ScheduleJobLockMapper scheduleJobLockMapper;

    @Autowired
    private RealtimeEventPublisher realtimeEventPublisher;

    @Value("${leaderboard.refresh.stale-hours:6}")
    private Integer staleHours;

    @Value("${leaderboard.home.limit:10}")
    private Integer defaultHomeLimit;

    @Value("${leaderboard.share.base-url:http://127.0.0.1:3000}")
    private String shareBaseUrl;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"leaderboard:home", "leaderboard:detail", "leaderboard:positions"}, allEntries = true)
    public void recalculateAllBoards(boolean incremental) {
        String lockKey = incremental ? "leaderboard:incremental" : "leaderboard:full";
        String owner = UUID.randomUUID().toString();
        if (!tryAcquireLock(lockKey, owner, 20)) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Equipment> activeEquipments = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                    .eq(Equipment::getStatus, 1)
                    .orderByDesc(Equipment::getViewCount)
                    .orderByDesc(Equipment::getRating)
            );
            if (activeEquipments.isEmpty()) {
                leaderboardSnapshotMapper.delete(new LambdaQueryWrapper<>());
                return;
            }

            LocalDateTime since = now.minusDays(30);
            List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                    .ge(Favorite::getCreateTime, since)
                    .orderByDesc(Favorite::getCreateTime)
            );
            List<Comment> comments = commentMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getParentId, 0)
                    .eq(Comment::getStatus, 1)
                    .eq(Comment::getReviewStatus, 1)
                    .ge(Comment::getCreateTime, since)
                    .orderByDesc(Comment::getCreateTime)
            );

            Map<Long, User> userMap = loadReferencedUsers(favorites, comments);
            ComputationContext context = new ComputationContext(activeEquipments, favorites, comments, userMap, now);

            for (LeaderboardType type : LeaderboardType.values()) {
                List<LeaderboardSnapshot> snapshots = buildBoardSnapshots(type, context, incremental);
                leaderboardSnapshotMapper.delete(
                    new LambdaQueryWrapper<LeaderboardSnapshot>()
                        .eq(LeaderboardSnapshot::getBoardType, type.getCode())
                );
                for (LeaderboardSnapshot snapshot : snapshots) {
                    leaderboardSnapshotMapper.insert(snapshot);
                }
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("incremental", incremental);
            payload.put("scope", "leaderboard");
            payload.put("boardTypes", Arrays.stream(LeaderboardType.values())
                .map(LeaderboardType::getCode)
                .collect(Collectors.toList()));
            realtimeEventPublisher.publish(RealtimeEventType.LEADERBOARD_REFRESHED, payload);
        } finally {
            releaseLock(lockKey, owner);
        }
    }

    @Override
    @Cacheable(cacheNames = "leaderboard:home", key = "T(java.util.Arrays).toString(#root.args)")
    public List<LeaderboardBoardDTO> getHomeLeaderboards(Integer limit) {
        ensureFreshSnapshots();
        int safeLimit = normalizeLimit(limit);
        List<LeaderboardBoardDTO> boards = new ArrayList<>();
        for (LeaderboardType type : LeaderboardType.values()) {
            boards.add(loadBoard(type, "score", safeLimit));
        }
        return boards;
    }

    @Override
    @Cacheable(cacheNames = "leaderboard:detail", key = "T(java.util.Arrays).toString(#root.args)")
    public LeaderboardBoardDTO getLeaderboardDetail(String boardType, String sortBy, Integer limit) {
        ensureFreshSnapshots();
        LeaderboardType type = resolveBoardType(boardType);
        return loadBoard(type, sortBy, normalizeLimit(limit));
    }

    @Override
    @Cacheable(cacheNames = "leaderboard:positions", key = "T(java.util.Arrays).toString(#root.args)")
    public List<EquipmentLeaderboardPositionDTO> getEquipmentRankings(Long equipmentId) {
        ensureFreshSnapshots();
        List<LeaderboardSnapshot> snapshots = leaderboardSnapshotMapper.selectList(
            new LambdaQueryWrapper<LeaderboardSnapshot>()
                .eq(LeaderboardSnapshot::getEquipmentId, equipmentId)
        );

        return snapshots.stream()
            .sorted(Comparator
                .comparingInt((LeaderboardSnapshot snapshot) -> boardOrder(snapshot.getBoardType()))
                .thenComparing(snapshot -> defaultInteger(snapshot.getRankPosition())))
            .map(snapshot -> {
                EquipmentLeaderboardPositionDTO dto = new EquipmentLeaderboardPositionDTO();
                dto.setBoardType(snapshot.getBoardType());
                dto.setBoardName(snapshot.getBoardName());
                dto.setRankPosition(snapshot.getRankPosition());
                dto.setTotalScore(snapshot.getTotalScore());
                dto.setBoardReason(snapshot.getBoardReason());
                return dto;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = "leaderboard:subscriptions", key = "T(java.util.Arrays).toString(#root.args)")
    public List<LeaderboardSubscription> getUserSubscriptions(Long userId) {
        return leaderboardSubscriptionMapper.selectList(
            new LambdaQueryWrapper<LeaderboardSubscription>()
                .eq(LeaderboardSubscription::getUserId, userId)
                .eq(LeaderboardSubscription::getActive, 1)
                .orderByAsc(LeaderboardSubscription::getBoardType)
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "leaderboard:subscriptions", allEntries = true)
    public boolean subscribe(Long userId, String boardType) {
        LeaderboardType type = resolveBoardType(boardType);
        LeaderboardSubscription subscription = leaderboardSubscriptionMapper.selectOne(
            new LambdaQueryWrapper<LeaderboardSubscription>()
                .eq(LeaderboardSubscription::getUserId, userId)
                .eq(LeaderboardSubscription::getBoardType, type.getCode())
                .last("LIMIT 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (subscription == null) {
            subscription = new LeaderboardSubscription();
            subscription.setUserId(userId);
            subscription.setBoardType(type.getCode());
            subscription.setActive(1);
            subscription.setLastViewedAt(now);
            return leaderboardSubscriptionMapper.insert(subscription) > 0;
        }

        subscription.setActive(1);
        subscription.setLastViewedAt(now);
        return leaderboardSubscriptionMapper.updateById(subscription) > 0;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "leaderboard:subscriptions", allEntries = true)
    public boolean unsubscribe(Long userId, String boardType) {
        LeaderboardType type = resolveBoardType(boardType);
        LeaderboardSubscription subscription = leaderboardSubscriptionMapper.selectOne(
            new LambdaQueryWrapper<LeaderboardSubscription>()
                .eq(LeaderboardSubscription::getUserId, userId)
                .eq(LeaderboardSubscription::getBoardType, type.getCode())
                .last("LIMIT 1")
        );
        if (subscription == null) {
            return true;
        }
        subscription.setActive(0);
        return leaderboardSubscriptionMapper.updateById(subscription) > 0;
    }

    @Override
    @Transactional
    public LeaderboardShareResponseDTO recordShare(Long userId, LeaderboardShareRequestDTO requestDTO) {
        LeaderboardType boardType = resolveBoardType(requestDTO.getBoardType());
        ensureFreshSnapshots();
        Long shareEquipmentId = requestDTO.getEquipmentId() == null ? 0L : requestDTO.getEquipmentId();

        LeaderboardShareStat shareStat = leaderboardShareStatMapper.selectOne(
            new LambdaQueryWrapper<LeaderboardShareStat>()
                .eq(LeaderboardShareStat::getBoardType, boardType.getCode())
                .eq(LeaderboardShareStat::getEquipmentId, shareEquipmentId)
                .eq(LeaderboardShareStat::getChannel, requestDTO.getChannel())
                .last("LIMIT 1")
        );

        LocalDateTime now = LocalDateTime.now();
        if (shareStat == null) {
            shareStat = new LeaderboardShareStat();
            shareStat.setBoardType(boardType.getCode());
            shareStat.setEquipmentId(shareEquipmentId);
            shareStat.setChannel(requestDTO.getChannel());
            shareStat.setShareCount(1);
            shareStat.setLastShareUserId(userId);
            shareStat.setLastSharedAt(now);
            leaderboardShareStatMapper.insert(shareStat);
        } else {
            shareStat.setShareCount(defaultInteger(shareStat.getShareCount()) + 1);
            shareStat.setLastShareUserId(userId);
            shareStat.setLastSharedAt(now);
            leaderboardShareStatMapper.updateById(shareStat);
        }

        return buildShareResponse(boardType, requestDTO.getEquipmentId(), requestDTO.getChannel());
    }

    @Override
    public Map<String, Object> getShareStatistics(String boardType) {
        LambdaQueryWrapper<LeaderboardShareStat> wrapper = new LambdaQueryWrapper<>();
        String normalizedBoardType = null;
        if (StringUtils.hasText(boardType)) {
            normalizedBoardType = resolveBoardType(boardType).getCode();
            wrapper.eq(LeaderboardShareStat::getBoardType, normalizedBoardType);
        }

        List<LeaderboardShareStat> stats = leaderboardShareStatMapper.selectList(wrapper);
        Map<String, Integer> channels = new LinkedHashMap<>();
        for (LeaderboardShareStat stat : stats) {
            channels.merge(stat.getChannel(), defaultInteger(stat.getShareCount()), Integer::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("boardType", normalizedBoardType);
        result.put("totalShares", stats.stream().mapToInt(stat -> defaultInteger(stat.getShareCount())).sum());
        result.put("channels", channels);
        return result;
    }

    private void ensureFreshSnapshots() {
        long total = leaderboardSnapshotMapper.selectCount(new LambdaQueryWrapper<>());
        if (total == 0) {
            recalculateAllBoards(false);
            return;
        }

        LeaderboardSnapshot latest = leaderboardSnapshotMapper.selectOne(
            new LambdaQueryWrapper<LeaderboardSnapshot>()
                .orderByDesc(LeaderboardSnapshot::getRefreshedAt)
                .last("LIMIT 1")
        );
        if (latest == null || latest.getRefreshedAt() == null) {
            recalculateAllBoards(false);
            return;
        }

        if (latest.getRefreshedAt().isBefore(LocalDateTime.now().minusHours(Math.max(defaultInteger(staleHours), 1)))) {
            recalculateAllBoards(true);
        }
    }

    private Map<Long, User> loadReferencedUsers(List<Favorite> favorites, List<Comment> comments) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (Favorite favorite : favorites) {
            if (favorite.getUserId() != null) {
                userIds.add(favorite.getUserId());
            }
        }
        for (Comment comment : comments) {
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));
    }

    private List<LeaderboardSnapshot> buildBoardSnapshots(
        LeaderboardType type,
        ComputationContext context,
        boolean incremental
    ) {
        List<Favorite> scopedFavorites = filterFavorites(type, context.favorites, context.userMap, context.now);
        List<Comment> scopedComments = filterComments(type, context.comments, context.userMap, context.now);

        Map<Long, List<Favorite>> favoritesByEquipment = scopedFavorites.stream()
            .filter(favorite -> favorite.getEquipmentId() != null)
            .collect(Collectors.groupingBy(Favorite::getEquipmentId));
        Map<Long, List<Comment>> commentsByEquipment = scopedComments.stream()
            .filter(comment -> comment.getEquipmentId() != null)
            .collect(Collectors.groupingBy(Comment::getEquipmentId));

        List<BoardMetric> metrics = new ArrayList<>();
        for (Equipment equipment : context.activeEquipments) {
            metrics.add(buildMetric(
                type,
                equipment,
                favoritesByEquipment.get(equipment.getId()),
                commentsByEquipment.get(equipment.getId()),
                context.now
            ));
        }

        if ((type == LeaderboardType.BEGINNER_PICK || type == LeaderboardType.PRO_FAVORITE)
            && metrics.stream().allMatch(metric -> metric.favoriteCount == 0 && metric.commentCount == 0)) {
            metrics = buildFallbackMetrics(type, context);
        }

        metrics.sort(Comparator
            .comparing(BoardMetric::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(BoardMetric::getHotScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(BoardMetric::getLatestInteractionTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(metric -> defaultDecimal(metric.equipment.getRating(), BigDecimal.ZERO), Comparator.reverseOrder())
        );

        List<LeaderboardSnapshot> snapshots = new ArrayList<>();
        int rank = 1;
        for (BoardMetric metric : metrics.stream().limit(MAX_BOARD_ITEMS).collect(Collectors.toList())) {
            LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
            snapshot.setBoardType(type.getCode());
            snapshot.setBoardName(type.getTitle());
            snapshot.setEquipmentId(metric.equipment.getId());
            snapshot.setRankPosition(rank++);
            snapshot.setTotalScore(metric.totalScore);
            snapshot.setHotScore(metric.hotScore);
            snapshot.setValueScore(metric.valueScore);
            snapshot.setTimeDecay(metric.timeDecay);
            snapshot.setFavoriteCount(metric.favoriteCount);
            snapshot.setCommentCount(metric.commentCount);
            snapshot.setAvgRating(metric.avgRating);
            snapshot.setSatisfactionRate(metric.satisfactionRate);
            snapshot.setLatestInteractionTime(metric.latestInteractionTime);
            snapshot.setBoardReason(metric.reason);
            snapshot.setRefreshMode(incremental ? INCREMENTAL_REFRESH : FULL_REFRESH);
            snapshot.setRefreshedAt(context.now);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    private List<BoardMetric> buildFallbackMetrics(LeaderboardType type, ComputationContext context) {
        Map<Long, List<Favorite>> favoritesByEquipment = context.favorites.stream()
            .filter(favorite -> favorite.getEquipmentId() != null)
            .collect(Collectors.groupingBy(Favorite::getEquipmentId));
        Map<Long, List<Comment>> commentsByEquipment = context.comments.stream()
            .filter(comment -> comment.getEquipmentId() != null)
            .collect(Collectors.groupingBy(Comment::getEquipmentId));

        List<BoardMetric> metrics = new ArrayList<>();
        for (Equipment equipment : context.activeEquipments) {
            metrics.add(buildMetric(
                type,
                equipment,
                favoritesByEquipment.get(equipment.getId()),
                commentsByEquipment.get(equipment.getId()),
                context.now
            ));
        }
        return metrics;
    }

    private BoardMetric buildMetric(
        LeaderboardType type,
        Equipment equipment,
        List<Favorite> favorites,
        List<Comment> comments,
        LocalDateTime now
    ) {
        List<Favorite> safeFavorites = favorites == null ? Collections.emptyList() : favorites;
        List<Comment> safeComments = comments == null ? Collections.emptyList() : comments;

        int favoriteCount = safeFavorites.size();
        int commentCount = safeComments.size();
        BigDecimal avgRating = safeComments.isEmpty()
            ? defaultDecimal(equipment.getRating(), BigDecimal.ZERO)
            : round(new BigDecimal(safeComments.stream()
                .mapToInt(comment -> defaultInteger(comment.getRating()))
                .average()
                .orElse(0D)), 2);

        BigDecimal satisfactionRate = safeComments.isEmpty()
            ? round(avgRating.multiply(new BigDecimal("20")), 2)
            : round(
                BigDecimal.valueOf(safeComments.stream()
                    .filter(comment -> defaultInteger(comment.getRating()) >= 4)
                    .count())
                    .divide(BigDecimal.valueOf(Math.max(safeComments.size(), 1L)), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")),
                2
            );

        LocalDateTime latestInteractionTime = resolveLatestInteractionTime(equipment, safeFavorites, safeComments);
        BigDecimal timeDecay = calculateTimeDecay(latestInteractionTime, now);
        BigDecimal baseScore = round(
            BigDecimal.valueOf(favoriteCount).multiply(FAVORITE_WEIGHT)
                .add(BigDecimal.valueOf(commentCount).multiply(COMMENT_WEIGHT))
                .add(avgRating.multiply(RATING_WEIGHT))
                .add(timeDecay),
            4
        );
        BigDecimal hotScore = round(
            baseScore.add(BigDecimal.valueOf(Math.log1p(defaultInteger(equipment.getViewCount()))).multiply(new BigDecimal("0.55"))),
            4
        );
        BigDecimal valueScore = round(calculateValueScore(equipment, avgRating), 4);

        BigDecimal totalScore = baseScore;
        if (type == LeaderboardType.WEEKLY_POPULAR) {
            totalScore = round(
                baseScore.add(BigDecimal.valueOf(Math.log1p(defaultInteger(equipment.getViewCount()))).multiply(new BigDecimal("0.25"))),
                4
            );
        } else if (type == LeaderboardType.VALUE_KING) {
            totalScore = round(baseScore.add(valueScore.multiply(new BigDecimal("0.8"))), 4);
        } else if (type == LeaderboardType.BEGINNER_PICK) {
            totalScore = round(baseScore.add(beginnerBoost(equipment, avgRating)), 4);
        } else if (type == LeaderboardType.PRO_FAVORITE) {
            totalScore = round(baseScore.add(proBoost(equipment, avgRating)), 4);
        }

        BoardMetric metric = new BoardMetric();
        metric.equipment = equipment;
        metric.favoriteCount = favoriteCount;
        metric.commentCount = commentCount;
        metric.avgRating = avgRating;
        metric.satisfactionRate = satisfactionRate;
        metric.latestInteractionTime = latestInteractionTime;
        metric.timeDecay = timeDecay;
        metric.hotScore = hotScore;
        metric.valueScore = valueScore;
        metric.totalScore = totalScore;
        metric.reason = buildReason(type, equipment, favoriteCount, commentCount, avgRating, satisfactionRate, valueScore);
        return metric;
    }

    private LocalDateTime resolveLatestInteractionTime(
        Equipment equipment,
        List<Favorite> favorites,
        List<Comment> comments
    ) {
        LocalDateTime latest = equipment.getCreateTime();
        for (Favorite favorite : favorites) {
            if (favorite.getCreateTime() != null && (latest == null || favorite.getCreateTime().isAfter(latest))) {
                latest = favorite.getCreateTime();
            }
        }
        for (Comment comment : comments) {
            if (comment.getCreateTime() != null && (latest == null || comment.getCreateTime().isAfter(latest))) {
                latest = comment.getCreateTime();
            }
        }
        return latest == null ? LocalDateTime.now() : latest;
    }

    private BigDecimal calculateTimeDecay(LocalDateTime latestInteractionTime, LocalDateTime now) {
        long hours = Math.max(1, Duration.between(latestInteractionTime, now).toHours());
        return round(BigDecimal.valueOf(Math.exp(-hours / 240.0D) * 1.8D), 4);
    }

    private BigDecimal calculateValueScore(Equipment equipment, BigDecimal avgRating) {
        BigDecimal price = defaultDecimal(equipment.getPrice(), BigDecimal.ONE).max(BigDecimal.ONE);
        BigDecimal priceScore = new BigDecimal("650")
            .divide(price, 4, RoundingMode.HALF_UP)
            .min(new BigDecimal("3.5"));
        return priceScore.add(avgRating.multiply(new BigDecimal("0.15")));
    }

    private BigDecimal beginnerBoost(Equipment equipment, BigDecimal avgRating) {
        BigDecimal boost = BigDecimal.ZERO;
        String suitableFor = safeLower(equipment.getSuitableFor());
        if (suitableFor.contains("入门") || suitableFor.contains("新手") || suitableFor.contains("训练")) {
            boost = boost.add(new BigDecimal("1.35"));
        }
        if (defaultDecimal(equipment.getPrice(), BigDecimal.ZERO).compareTo(new BigDecimal("400")) <= 0) {
            boost = boost.add(new BigDecimal("0.65"));
        }
        if (avgRating.compareTo(new BigDecimal("4.2")) >= 0) {
            boost = boost.add(new BigDecimal("0.35"));
        }
        return boost;
    }

    private BigDecimal proBoost(Equipment equipment, BigDecimal avgRating) {
        BigDecimal boost = BigDecimal.ZERO;
        String suitableFor = safeLower(equipment.getSuitableFor());
        if (suitableFor.contains("专业") || suitableFor.contains("竞技") || suitableFor.contains("高阶")) {
            boost = boost.add(new BigDecimal("1.45"));
        }
        if (avgRating.compareTo(new BigDecimal("4.5")) >= 0) {
            boost = boost.add(new BigDecimal("0.8"));
        }
        if (defaultDecimal(equipment.getPrice(), BigDecimal.ZERO).compareTo(new BigDecimal("450")) >= 0) {
            boost = boost.add(new BigDecimal("0.35"));
        }
        return boost;
    }

    private String buildReason(
        LeaderboardType type,
        Equipment equipment,
        int favoriteCount,
        int commentCount,
        BigDecimal avgRating,
        BigDecimal satisfactionRate,
        BigDecimal valueScore
    ) {
        if (favoriteCount >= 300) {
            return "被 300+ 用户收藏";
        }
        if (favoriteCount > 0) {
            return "被 " + favoriteCount + " 位用户收藏";
        }
        if (avgRating.compareTo(new BigDecimal("4.8")) >= 0) {
            return "评分高达 " + avgRating.setScale(1, RoundingMode.HALF_UP).toPlainString();
        }
        if (satisfactionRate.compareTo(new BigDecimal("95")) >= 0) {
            return satisfactionRate.setScale(0, RoundingMode.HALF_UP).toPlainString() + "% 用户表示满意";
        }
        if (type == LeaderboardType.VALUE_KING) {
            return "综合性价比指数 " + valueScore.setScale(1, RoundingMode.HALF_UP).toPlainString();
        }
        if (type == LeaderboardType.BEGINNER_PICK) {
            return "适合入门阶段稳步提升";
        }
        if (type == LeaderboardType.PRO_FAVORITE) {
            return "高强度对抗场景表现稳定";
        }
        if (commentCount > 0) {
            return "收到 " + commentCount + " 条真实评价";
        }
        return (StringUtils.hasText(equipment.getBrand()) ? equipment.getBrand() : "这件装备") + " 口碑表现稳定";
    }

    private List<Favorite> filterFavorites(
        LeaderboardType type,
        List<Favorite> favorites,
        Map<Long, User> userMap,
        LocalDateTime now
    ) {
        LocalDateTime since = type == LeaderboardType.WEEKLY_POPULAR ? now.minusDays(7) : now.minusDays(30);
        return favorites.stream()
            .filter(favorite -> favorite.getCreateTime() != null && !favorite.getCreateTime().isBefore(since))
            .filter(favorite -> favorite.getEquipmentId() != null)
            .filter(favorite -> matchesSegment(type, userMap.get(favorite.getUserId())))
            .collect(Collectors.toList());
    }

    private List<Comment> filterComments(
        LeaderboardType type,
        List<Comment> comments,
        Map<Long, User> userMap,
        LocalDateTime now
    ) {
        LocalDateTime since = type == LeaderboardType.WEEKLY_POPULAR ? now.minusDays(7) : now.minusDays(30);
        return comments.stream()
            .filter(comment -> comment.getCreateTime() != null && !comment.getCreateTime().isBefore(since))
            .filter(comment -> comment.getEquipmentId() != null)
            .filter(comment -> matchesSegment(type, userMap.get(comment.getUserId())))
            .collect(Collectors.toList());
    }

    private boolean matchesSegment(LeaderboardType type, User user) {
        if (type == LeaderboardType.WEEKLY_POPULAR || type == LeaderboardType.VALUE_KING) {
            return true;
        }
        int segment = resolveUserSegment(user);
        if (type == LeaderboardType.BEGINNER_PICK) {
            return segment != SEGMENT_PRO;
        }
        if (type == LeaderboardType.PRO_FAVORITE) {
            return segment == SEGMENT_PRO;
        }
        return true;
    }

    private int resolveUserSegment(User user) {
        if (user == null) {
            return SEGMENT_BEGINNER;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return SEGMENT_PRO;
        }
        String level = safeLower(user.getLevel());
        String playStyle = safeLower(user.getPlayStyle());
        if (level.contains("高级")
            || level.contains("专业")
            || level.contains("advanced")
            || level.contains("pro")
            || playStyle.contains("竞技")
            || playStyle.contains("快攻")) {
            return SEGMENT_PRO;
        }
        return SEGMENT_BEGINNER;
    }

    private LeaderboardBoardDTO loadBoard(LeaderboardType type, String sortBy, Integer limit) {
        List<LeaderboardSnapshot> snapshots = leaderboardSnapshotMapper.selectList(
            new LambdaQueryWrapper<LeaderboardSnapshot>()
                .eq(LeaderboardSnapshot::getBoardType, type.getCode())
        );
        Map<Long, Equipment> equipmentMap = loadEquipments(snapshots);
        Comparator<LeaderboardSnapshot> comparator = buildComparator(sortBy);

        LeaderboardBoardDTO dto = new LeaderboardBoardDTO();
        dto.setBoardType(type.getCode());
        dto.setBoardName(type.getTitle());
        dto.setSubtitle(type.getSubtitle());
        dto.setSortBy(normalizeSort(sortBy));
        dto.setRefreshedAt(snapshots.stream()
            .map(LeaderboardSnapshot::getRefreshedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null));
        dto.setItems(snapshots.stream()
            .sorted(comparator)
            .limit(limit)
            .map(snapshot -> toBoardItem(snapshot, equipmentMap.get(snapshot.getEquipmentId())))
            .collect(Collectors.toList()));
        return dto;
    }

    private Map<Long, Equipment> loadEquipments(List<LeaderboardSnapshot> snapshots) {
        Set<Long> equipmentIds = snapshots.stream()
            .map(LeaderboardSnapshot::getEquipmentId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return equipmentMapper.selectBatchIds(equipmentIds).stream()
            .collect(Collectors.toMap(Equipment::getId, equipment -> equipment));
    }

    private Comparator<LeaderboardSnapshot> buildComparator(String sortBy) {
        String normalized = normalizeSort(sortBy);
        if ("heat".equals(normalized)) {
            return Comparator.comparing(LeaderboardSnapshot::getHotScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LeaderboardSnapshot::getRankPosition);
        }
        if ("time".equals(normalized)) {
            return Comparator.comparing(LeaderboardSnapshot::getLatestInteractionTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LeaderboardSnapshot::getRankPosition);
        }
        return Comparator.comparing(LeaderboardSnapshot::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(LeaderboardSnapshot::getRankPosition);
    }

    private LeaderboardItemDTO toBoardItem(LeaderboardSnapshot snapshot, Equipment equipment) {
        LeaderboardItemDTO dto = new LeaderboardItemDTO();
        dto.setEquipmentId(snapshot.getEquipmentId());
        dto.setRankPosition(snapshot.getRankPosition());
        dto.setTotalScore(snapshot.getTotalScore());
        dto.setHotScore(snapshot.getHotScore());
        dto.setValueScore(snapshot.getValueScore());
        dto.setFavoriteCount(snapshot.getFavoriteCount());
        dto.setCommentCount(snapshot.getCommentCount());
        dto.setAvgRating(snapshot.getAvgRating());
        dto.setSatisfactionRate(snapshot.getSatisfactionRate());
        dto.setBoardReason(snapshot.getBoardReason());
        dto.setLatestInteractionTime(snapshot.getLatestInteractionTime());
        if (equipment != null) {
            dto.setName(equipment.getName());
            dto.setBrand(equipment.getBrand());
            dto.setModel(equipment.getModel());
            dto.setMainImage(equipment.getMainImage());
            dto.setPrice(equipment.getPrice());
            dto.setSuitableFor(equipment.getSuitableFor());
            dto.setEquipmentRating(equipment.getRating());
        }
        return dto;
    }

    private LeaderboardShareResponseDTO buildShareResponse(LeaderboardType boardType, Long equipmentId, String channel) {
        LeaderboardBoardDTO boardDTO = loadBoard(boardType, "score", 10);
        LeaderboardItemDTO item = boardDTO.getItems().stream()
            .filter(entry -> Objects.equals(entry.getEquipmentId(), equipmentId))
            .findFirst()
            .orElse(boardDTO.getItems().isEmpty() ? null : boardDTO.getItems().get(0));

        String shareUrl = equipmentId == null
            ? shareBaseUrl + "/leaderboards?boardType=" + boardType.getCode()
            : shareBaseUrl + "/equipment/" + equipmentId;
        String title = equipmentId == null
            ? boardType.getTitle() + " | 乒乓装备口碑榜"
            : boardType.getTitle() + "推荐：" + (item == null ? "精选装备" : item.getName());
        String shareText = equipmentId == null
            ? boardType.getTitle() + "已经更新，来看看本期最值得关注的乒乓装备。"
            : "在「" + boardType.getTitle() + "」里，"
            + (item == null ? "这件装备" : item.getName())
            + " 当前排名第 " + (item == null ? "-" : item.getRankPosition()) + "。"
            + (item == null ? "一起来看看。" : item.getBoardReason() + "，一起来看看。");

        LeaderboardShareResponseDTO responseDTO = new LeaderboardShareResponseDTO();
        responseDTO.setBoardType(boardType.getCode());
        responseDTO.setEquipmentId(equipmentId);
        responseDTO.setChannel(channel);
        responseDTO.setTitle(title);
        responseDTO.setShareText(shareText);
        responseDTO.setShareUrl(shareUrl);
        responseDTO.setPosterSvg(buildPosterSvg(boardType, item, shareUrl));
        return responseDTO;
    }

    private String buildPosterSvg(LeaderboardType boardType, LeaderboardItemDTO item, String shareUrl) {
        String name = item == null ? "榜单精选" : escapeXml(item.getName());
        String reason = item == null ? "看看本期上榜装备" : escapeXml(item.getBoardReason());
        String score = item == null || item.getTotalScore() == null
            ? "--"
            : item.getTotalScore().setScale(1, RoundingMode.HALF_UP).toPlainString();

        return "<svg xmlns='http://www.w3.org/2000/svg' width='720' height='960' viewBox='0 0 720 960'>"
            + "<defs><linearGradient id='bg' x1='0%' y1='0%' x2='100%' y2='100%'>"
            + "<stop offset='0%' stop-color='#0e5135'/><stop offset='55%' stop-color='#166241'/><stop offset='100%' stop-color='#f28c28'/>"
            + "</linearGradient></defs>"
            + "<rect width='720' height='960' rx='36' fill='url(#bg)'/>"
            + "<circle cx='604' cy='138' r='68' fill='rgba(255,255,255,0.14)'/>"
            + "<text x='64' y='118' fill='#fff8e8' font-size='26' font-family='Microsoft YaHei'>乒乓装备口碑榜</text>"
            + "<text x='64' y='186' fill='#ffffff' font-size='56' font-weight='700' font-family='Microsoft YaHei'>" + escapeXml(boardType.getTitle()) + "</text>"
            + "<text x='64' y='246' fill='#ffe7bf' font-size='24' font-family='Microsoft YaHei'>" + name + "</text>"
            + "<text x='64' y='364' fill='#ffffff' font-size='140' font-weight='700' font-family='Microsoft YaHei'>" + score + "</text>"
            + "<text x='64' y='414' fill='#ffe7bf' font-size='24' font-family='Microsoft YaHei'>综合得分</text>"
            + "<rect x='64' y='472' width='592' height='178' rx='28' fill='rgba(255,255,255,0.14)'/>"
            + "<text x='92' y='536' fill='#fff8e8' font-size='24' font-family='Microsoft YaHei'>推荐理由</text>"
            + "<text x='92' y='586' fill='#ffffff' font-size='34' font-weight='700' font-family='Microsoft YaHei'>" + reason + "</text>"
            + "<text x='92' y='636' fill='#ffe7bf' font-size='22' font-family='Microsoft YaHei'>分享链接：" + escapeXml(shareUrl) + "</text>"
            + "<text x='64' y='846' fill='#ffffff' font-size='30' font-weight='700' font-family='Microsoft YaHei'>基于真实收藏、评论与评分动态生成</text>"
            + "<text x='64' y='894' fill='#ffe7bf' font-size='24' font-family='Microsoft YaHei'>查看完整榜单，找到更适合你的下一件装备</text>"
            + "</svg>";
    }

    private String escapeXml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private LeaderboardType resolveBoardType(String boardType) {
        return LeaderboardType.fromCode(boardType)
            .orElseThrow(() -> new RuntimeException("未知榜单类型"));
    }

    private int normalizeLimit(Integer limit) {
        int safeLimit = defaultInteger(limit);
        if (safeLimit <= 0) {
            safeLimit = defaultInteger(defaultHomeLimit);
        }
        if (safeLimit <= 0) {
            safeLimit = 10;
        }
        return Math.min(Math.max(safeLimit, 1), 20);
    }

    private String normalizeSort(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return "score";
        }
        String normalized = sortBy.toLowerCase(Locale.ROOT);
        if (Arrays.asList("score", "heat", "time").contains(normalized)) {
            return normalized;
        }
        return "score";
    }

    private int boardOrder(String boardType) {
        for (int i = 0; i < LeaderboardType.values().length; i++) {
            if (LeaderboardType.values()[i].getCode().equalsIgnoreCase(boardType)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private boolean tryAcquireLock(String jobKey, String owner, int minutes) {
        LocalDateTime now = LocalDateTime.now();
        ScheduleJobLock lock = scheduleJobLockMapper.selectOne(
            new LambdaQueryWrapper<ScheduleJobLock>()
                .eq(ScheduleJobLock::getJobKey, jobKey)
                .last("LIMIT 1")
        );
        if (lock == null) {
            ScheduleJobLock newLock = new ScheduleJobLock();
            newLock.setJobKey(jobKey);
            newLock.setOwner(owner);
            newLock.setLockedUntil(now.plusMinutes(minutes));
            try {
                return scheduleJobLockMapper.insert(newLock) > 0;
            } catch (Exception ignored) {
                return false;
            }
        }
        if (lock.getLockedUntil() == null || lock.getLockedUntil().isBefore(now) || owner.equals(lock.getOwner())) {
            lock.setOwner(owner);
            lock.setLockedUntil(now.plusMinutes(minutes));
            return scheduleJobLockMapper.updateById(lock) > 0;
        }
        return false;
    }

    private void releaseLock(String jobKey, String owner) {
        ScheduleJobLock lock = scheduleJobLockMapper.selectOne(
            new LambdaQueryWrapper<ScheduleJobLock>()
                .eq(ScheduleJobLock::getJobKey, jobKey)
                .eq(ScheduleJobLock::getOwner, owner)
                .last("LIMIT 1")
        );
        if (lock != null) {
            lock.setLockedUntil(LocalDateTime.now().minusSeconds(1));
            scheduleJobLockMapper.updateById(lock);
        }
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static class ComputationContext {
        private final List<Equipment> activeEquipments;
        private final List<Favorite> favorites;
        private final List<Comment> comments;
        private final Map<Long, User> userMap;
        private final LocalDateTime now;

        private ComputationContext(
            List<Equipment> activeEquipments,
            List<Favorite> favorites,
            List<Comment> comments,
            Map<Long, User> userMap,
            LocalDateTime now
        ) {
            this.activeEquipments = activeEquipments;
            this.favorites = favorites;
            this.comments = comments;
            this.userMap = userMap;
            this.now = now;
        }
    }

    private static class BoardMetric {
        private Equipment equipment;
        private Integer favoriteCount;
        private Integer commentCount;
        private BigDecimal avgRating;
        private BigDecimal satisfactionRate;
        private LocalDateTime latestInteractionTime;
        private BigDecimal timeDecay;
        private BigDecimal totalScore;
        private BigDecimal hotScore;
        private BigDecimal valueScore;
        private String reason;

        private BigDecimal getTotalScore() {
            return totalScore == null ? BigDecimal.ZERO : totalScore;
        }

        private BigDecimal getHotScore() {
            return hotScore == null ? BigDecimal.ZERO : hotScore;
        }

        private LocalDateTime getLatestInteractionTime() {
            return latestInteractionTime;
        }
    }
}
