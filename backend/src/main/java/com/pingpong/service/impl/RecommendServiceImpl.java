package com.pingpong.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.dto.RecommendationWorkbenchDTO;
import com.pingpong.dto.UserProfileActionDTO;
import com.pingpong.dto.UserProfileInsightDTO;
import com.pingpong.dto.UserProfileInsightSignalDTO;
import com.pingpong.entity.BrowseHistory;
import com.pingpong.entity.Category;
import com.pingpong.entity.Comment;
import com.pingpong.entity.EquipmentCombination;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.Favorite;
import com.pingpong.entity.PlayerEquipmentRelation;
import com.pingpong.entity.StarPlayer;
import com.pingpong.entity.User;
import com.pingpong.entity.UserPreferenceProfile;
import com.pingpong.mapper.BrowseHistoryMapper;
import com.pingpong.mapper.CategoryMapper;
import com.pingpong.mapper.CommentMapper;
import com.pingpong.mapper.EquipmentCombinationMapper;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.FavoriteMapper;
import com.pingpong.mapper.PlayerEquipmentRelationMapper;
import com.pingpong.mapper.StarPlayerMapper;
import com.pingpong.mapper.UserMapper;
import com.pingpong.mapper.UserPreferenceProfileMapper;
import com.pingpong.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPreferenceProfileMapper userPreferenceProfileMapper;

    @Autowired
    private EquipmentCombinationMapper equipmentCombinationMapper;

    @Autowired
    private StarPlayerMapper starPlayerMapper;

    @Autowired
    private PlayerEquipmentRelationMapper playerEquipmentRelationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${recommend.hot.count}")
    private Integer hotCount;

    @Value("${recommend.personal.count}")
    private Integer personalCount;

    @Value("${recommend.similar.count}")
    private Integer similarCount;

    @Value("${recommend.personal.behavior-weight.browse}")
    private Double browseWeight;

    @Value("${recommend.personal.behavior-weight.favorite}")
    private Double favoriteWeight;

    @Value("${recommend.personal.behavior-weight.comment}")
    private Double commentWeight;

    @Value("${recommend.similar.tag-weight}")
    private Double tagWeight;

    @Value("${recommend.similar.category-weight}")
    private Double categoryWeight;

    @Value("${recommend.similar.price-weight}")
    private Double priceWeight;

    @Override
    public List<Equipment> getPersonalRecommend(Long userId, Integer count) {
        if (count == null) {
            count = personalCount;
        }

        String cacheKey = "recommend:personal:" + userId;
        List<Equipment> cached = getCachedList(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        Map<Long, Double> equipmentScores = buildUserEquipmentScores(userId);
        User user = getUser(userId);
        UserPreferenceProfile preferenceProfile = getUserPreferenceProfile(userId);

        if (equipmentScores.isEmpty() && isPreferenceProfileEmpty(preferenceProfile)) {
            List<Equipment> hotEquipments = getHotRecommend(count);
            cacheList(cacheKey, hotEquipments, 30, TimeUnit.MINUTES);
            return hotEquipments;
        }

        List<Long> userEquipmentIds = new ArrayList<>(equipmentScores.keySet());
        List<Equipment> interactedEquipments = userEquipmentIds.isEmpty()
            ? Collections.emptyList()
            : equipmentMapper.selectBatchIds(
                equipmentScores.entrySet().stream()
                    .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                    .limit(20)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())
            );

        List<Long> excludeIds = new ArrayList<>(userEquipmentIds);
        List<Equipment> candidates = equipmentMapper.selectRecommendCandidates(excludeIds, 300);

        Map<Long, Double> candidateScores = new LinkedHashMap<>();
        for (Equipment equipment : candidates) {
            double score = calculatePersonalScore(equipment, interactedEquipments, equipmentScores, user, preferenceProfile);
            candidateScores.put(equipment.getId(), score);
        }

        List<Equipment> result = candidateScores.entrySet().stream()
            .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
            .limit(count)
            .map(entry -> candidates.stream().filter(item -> Objects.equals(item.getId(), entry.getKey())).findFirst().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (result.isEmpty()) {
            result = getHotRecommend(count);
        }

        decoratePersonalRecommendItems(result, user, preferenceProfile);

        cacheList(cacheKey, result, 30, TimeUnit.MINUTES);
        for (Equipment equipment : result) {
            recordRecommend(
                userId,
                equipment.getId(),
                "PERSONAL",
                candidateScores.getOrDefault(equipment.getId(), 0.0),
                StringUtils.hasText(equipment.getRecommendReason())
                    ? equipment.getRecommendReason()
                    : "根据浏览行为与偏好问答进行个性化推荐"
            );
        }

        return result;
    }

    @Override
    public List<Equipment> getSimilarRecommend(Long equipmentId, Integer count) {
        if (count == null) {
            count = similarCount;
        }

        String cacheKey = "recommend:similar:" + equipmentId;
        List<Equipment> cached = getCachedList(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        Equipment targetEquipment = equipmentMapper.selectById(equipmentId);
        if (targetEquipment == null) {
            return new ArrayList<>();
        }

        List<Equipment> allEquipments = equipmentMapper.selectList(
            new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getStatus, 1)
                .ne(Equipment::getId, equipmentId)
        );

        Map<Long, Double> scoreMap = new LinkedHashMap<>();
        for (Equipment equipment : allEquipments) {
            double similarity = calculateSimilarity(targetEquipment, equipment);
            if (similarity > 0.3) {
                scoreMap.put(equipment.getId(), similarity * 10);
            }
        }

        List<Equipment> result = scoreMap.entrySet().stream()
            .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
            .limit(count)
            .map(entry -> allEquipments.stream().filter(item -> Objects.equals(item.getId(), entry.getKey())).findFirst().orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        decorateSimilarRecommendItems(result, targetEquipment);
        cacheList(cacheKey, result, 1, TimeUnit.HOURS);
        return result;
    }

    @Override
    public List<Equipment> getHotRecommend(Integer count) {
        if (count == null) {
            count = hotCount;
        }

        String cacheKey = "recommend:hot";
        List<Equipment> cached = getCachedList(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Equipment> hotEquipments = equipmentMapper.selectList(
            new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getStatus, 1)
                .orderByDesc(Equipment::getViewCount)
                .orderByDesc(Equipment::getFavoriteCount)
                .orderByDesc(Equipment::getSalesCount)
                .last("LIMIT " + count)
        );

        decorateHotRecommendItems(hotEquipments);
        cacheList(cacheKey, hotEquipments, 1, TimeUnit.HOURS);
        return hotEquipments;
    }

    @Override
    public Map<String, Object> analyzeUserBehavior(Long userId) {
        Map<String, Object> analysis = new HashMap<>();

        List<BrowseHistory> browseHistories = browseHistoryMapper.selectList(
            new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getUserId, userId)
        );
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
        );
        List<Comment> comments = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId)
        );

        Map<Long, Double> equipmentScores = buildUserEquipmentScores(browseHistories, favorites, comments);
        List<Long> equipmentIds = new ArrayList<>(equipmentScores.keySet());
        List<Equipment> browsedEquipments = equipmentIds.isEmpty()
            ? Collections.emptyList()
            : equipmentMapper.selectBatchIds(equipmentIds);

        Map<String, Double> categoryCount = new HashMap<>();
        Map<String, Double> brandCount = new HashMap<>();
        Map<String, Double> priceRangeCount = new HashMap<>();

        for (Equipment equipment : browsedEquipments) {
            double weight = equipmentScores.getOrDefault(equipment.getId(), 0.0);
            if (equipment.getCategoryId() != null) {
                categoryCount.merge(equipment.getCategoryId().toString(), weight, Double::sum);
            }
            if (StringUtils.hasText(equipment.getBrand())) {
                brandCount.merge(equipment.getBrand(), weight, Double::sum);
            }
            priceRangeCount.merge(getPriceRange(equipment.getPrice()), weight, Double::sum);
        }

        UserPreferenceProfile preferenceProfile = getUserPreferenceProfile(userId);

        analysis.put("browseCount", browseHistories.size());
        analysis.put("favoriteCount", favorites.size());
        analysis.put("commentCount", comments.size());
        analysis.put("categoryPreference", categoryCount);
        analysis.put("brandPreference", brandCount);
        analysis.put("pricePreference", priceRangeCount);
        analysis.put("preferenceProfile", preferenceProfile);

        return analysis;
    }

    @Override
    public UserProfileInsightDTO getUserProfileInsight(Long userId) {
        List<BrowseHistory> browseHistories = browseHistoryMapper.selectList(
            new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getUpdateTime)
                .orderByDesc(BrowseHistory::getCreateTime)
                .last("LIMIT 50")
        );
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
        );
        List<Comment> comments = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime)
                .last("LIMIT 20")
        );

        Map<Long, Double> equipmentScores = buildUserEquipmentScores(browseHistories, favorites, comments);
        List<Long> equipmentIds = new ArrayList<>(equipmentScores.keySet());
        List<Equipment> engagedEquipments = equipmentIds.isEmpty()
            ? Collections.emptyList()
            : equipmentMapper.selectBatchIds(equipmentIds);

        Map<String, Double> categoryPreference = new HashMap<>();
        Map<String, Double> brandPreference = new HashMap<>();
        Map<String, Double> pricePreference = new HashMap<>();

        for (Equipment equipment : engagedEquipments) {
            double weight = equipmentScores.getOrDefault(equipment.getId(), 0.0);
            if (equipment.getCategoryId() != null) {
                categoryPreference.merge(String.valueOf(equipment.getCategoryId()), weight, Double::sum);
            }
            if (StringUtils.hasText(equipment.getBrand())) {
                brandPreference.merge(equipment.getBrand(), weight, Double::sum);
            }
            pricePreference.merge(getPriceRange(equipment.getPrice()), weight, Double::sum);
        }

        User user = getUser(userId);
        UserPreferenceProfile preferenceProfile = getUserPreferenceProfile(userId);
        int completionScore = calculateProfileCompletion(preferenceProfile);
        String recommendationMode = resolveRecommendationMode(preferenceProfile, browseHistories.size(), favorites.size(), comments.size());

        List<String> topCategoryIds = getTopKeys(categoryPreference, 2);
        Map<String, String> categoryNameMap = loadCategoryNameMap(topCategoryIds);
        String dominantCategoryId = topCategoryIds.isEmpty() ? "" : topCategoryIds.get(0);
        String dominantCategoryName = categoryNameMap.getOrDefault(dominantCategoryId, "");
        String dominantBrand = getTopKey(brandPreference);
        String dominantPriceRange = getTopKey(pricePreference);

        UserProfileInsightDTO insight = new UserProfileInsightDTO();
        insight.setCompletionScore(completionScore);
        insight.setRecommendationMode(recommendationMode);
        insight.setPersonaTitle(buildPersonaTitle(preferenceProfile, user, browseHistories.size(), favorites.size(), comments.size()));
        insight.setPersonaDescription(buildPersonaDescription(
            preferenceProfile,
            user,
            recommendationMode,
            dominantCategoryName,
            dominantBrand,
            dominantPriceRange
        ));
        insight.setCoreTags(buildCoreTags(preferenceProfile, user, dominantCategoryName, dominantBrand, dominantPriceRange));
        insight.setImprovementActions(buildImprovementActions(
            preferenceProfile,
            completionScore,
            browseHistories.size(),
            favorites.size(),
            comments.size(),
            categoryPreference.isEmpty()
        ));
        insight.setSignals(buildInsightSignals(
            completionScore,
            browseHistories.size(),
            favorites.size(),
            dominantCategoryName,
            dominantBrand,
            preferenceProfile,
            dominantPriceRange
        ));
        insight.setActionCards(buildActionCards(
            preferenceProfile,
            completionScore,
            dominantCategoryId,
            dominantCategoryName,
            dominantBrand,
            dominantPriceRange,
            favorites.size()
        ));
        return insight;
    }

    @Override
    public RecommendationWorkbenchDTO getRecommendationWorkbench(Long userId) {
        User user = getUser(userId);
        UserPreferenceProfile profile = getUserPreferenceProfile(userId);
        Map<String, Object> behavior = analyzeUserBehavior(userId);
        Map<String, Object> snapshot = parseAnswerSnapshot(profile);

        List<Equipment> personalRecommendations = getPersonalRecommend(userId, 8);
        List<Equipment> hotRecommendations = getHotRecommend(8);
        List<Equipment> recentArrivals = equipmentMapper.selectList(
            new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getStatus, 1)
                .orderByDesc(Equipment::getCreateTime)
                .last("LIMIT 6")
        );

        RecommendationWorkbenchDTO dto = new RecommendationWorkbenchDTO();
        dto.setSummaryTitle(buildWorkbenchTitle(profile, user, snapshot));
        dto.setSummaryDescription(buildWorkbenchDescription(profile, behavior, snapshot));
        dto.setProfileKeywords(buildWorkbenchKeywords(profile, user, snapshot));
        dto.setCoachSuggestions(buildCoachSuggestions(profile, snapshot));
        dto.setCombinationPlans(buildCombinationPlans(profile, snapshot, personalRecommendations, hotRecommendations));
        dto.setUpgradePlans(buildUpgradePlans(profile, snapshot));
        dto.setTrendSignals(buildTrendSignals(behavior, hotRecommendations, recentArrivals));
        dto.setNewArrivals(recentArrivals);
        dto.setTrialCards(buildTrialCards(profile, personalRecommendations));
        dto.setPlayerReferences(buildPlayerReferences());
        dto.setCompareOptions(buildCompareOptions(personalRecommendations, hotRecommendations));
        dto.setFeedbackLoop(buildFeedbackLoop(userId, profile, behavior));
        return dto;
    }

    @Override
    public void recordRecommend(Long userId, Long equipmentId, String recommendType, Double score, String reason) {
        String key = "recommend:record:" + userId + ":" + equipmentId;
        Map<String, Object> record = new HashMap<>();
        record.put("userId", userId);
        record.put("equipmentId", equipmentId);
        record.put("recommendType", recommendType);
        record.put("recommendScore", score);
        record.put("recommendReason", reason);
        record.put("createTime", LocalDateTime.now());
        try {
            redisTemplate.opsForHash().putAll(key, record);
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void recordRecommendClick(Long userId, Long equipmentId) {
        String key = "recommend:click:" + userId + ":" + equipmentId;
        try {
            redisTemplate.opsForValue().set(key, "1", 30, TimeUnit.DAYS);
        } catch (Exception ignored) {
        }
    }

    private double calculatePersonalScore(Equipment equipment,
                                          List<Equipment> interactedEquipments,
                                          Map<Long, Double> userScores,
                                          User user,
                                          UserPreferenceProfile preferenceProfile) {
        double baseScore = 1.0;

        if (equipment.getIsRecommend() != null && equipment.getIsRecommend() == 1) {
            baseScore += 2.0;
        }
        if (equipment.getRating() != null) {
            baseScore += equipment.getRating().doubleValue() * 0.5;
        }

        baseScore += Math.log1p(equipment.getViewCount() == null ? 0 : equipment.getViewCount()) * 0.3;
        baseScore += Math.log1p(equipment.getFavoriteCount() == null ? 0 : equipment.getFavoriteCount()) * 0.5;

        double personalizedScore = 0.0;
        double totalWeight = 0.0;
        for (Equipment interactedEquipment : interactedEquipments) {
            Double weight = userScores.get(interactedEquipment.getId());
            if (weight == null || weight <= 0) {
                continue;
            }
            personalizedScore += calculateSimilarity(interactedEquipment, equipment) * weight;
            totalWeight += weight;
        }
        if (totalWeight > 0) {
            baseScore += (personalizedScore / totalWeight) * 5.0;
        }

        baseScore += calculateUserProfileBonus(user, equipment);
        baseScore += calculatePreferenceProfileScore(preferenceProfile, equipment);
        return baseScore;
    }

    @SuppressWarnings("unused")
    private double calculatePersonalScore(Equipment equipment,
                                          List<Equipment> interactedEquipments,
                                          Map<Long, Double> userScores) {
        return calculatePersonalScore(equipment, interactedEquipments, userScores, null, null);
    }

    private double calculateUserProfileBonus(User user, Equipment equipment) {
        if (user == null) {
            return 0.0;
        }

        double score = 0.0;
        String suitableFor = defaultText(equipment.getSuitableFor());
        String features = defaultText(equipment.getFeatures());

        if (StringUtils.hasText(user.getLevel()) && suitableFor.contains(user.getLevel())) {
            score += 1.5;
        }

        if (StringUtils.hasText(user.getPlayStyle())) {
            if (suitableFor.contains(user.getPlayStyle()) || features.contains(user.getPlayStyle())) {
                score += 2.0;
            } else if ("全能".equals(user.getPlayStyle())) {
                score += 0.5;
            }
        }

        return score;
    }

    private double calculatePreferenceProfileScore(UserPreferenceProfile profile, Equipment equipment) {
        if (isPreferenceProfileEmpty(profile)) {
            return 0.0;
        }

        double score = 0.0;
        String searchableText = String.join(" ",
            defaultText(equipment.getName()),
            defaultText(equipment.getBrand()),
            defaultText(equipment.getDescription()),
            defaultText(equipment.getFeatures()),
            defaultText(equipment.getSuitableFor()),
            defaultText(equipment.getParameters())
        );

        if (isPriceInBudget(equipment.getPrice(), profile.getBudgetMin(), profile.getBudgetMax())) {
            score += 1.4;
        } else if (isPriceNearBudget(equipment.getPrice(), profile.getBudgetMin(), profile.getBudgetMax())) {
            score += 0.4;
        } else if (equipment.getPrice() != null && (profile.getBudgetMin() != null || profile.getBudgetMax() != null)) {
            score -= 0.8;
        }

        if (StringUtils.hasText(profile.getPlayStyle()) && searchableText.contains(profile.getPlayStyle())) {
            score += 1.6;
        }
        if (StringUtils.hasText(profile.getGripStyle()) && searchableText.contains(profile.getGripStyle())) {
            score += 0.9;
        }
        if (StringUtils.hasText(profile.getPrimaryGoal()) && searchableText.contains(profile.getPrimaryGoal())) {
            score += 1.0;
        }
        if (StringUtils.hasText(profile.getPriorityFocus()) && searchableText.contains(profile.getPriorityFocus())) {
            score += 1.1;
        }

        Set<String> preferredCategories = parseJsonStringSet(profile.getPreferredCategories());
        if (equipment.getCategoryId() != null && preferredCategories.contains(String.valueOf(equipment.getCategoryId()))) {
            score += 1.2;
        }

        Set<String> preferredBrands = parseJsonStringSet(profile.getPreferredBrands());
        if (StringUtils.hasText(equipment.getBrand()) && preferredBrands.contains(equipment.getBrand())) {
            score += 1.2;
        }

        Set<String> preferenceTags = parseJsonStringSet(profile.getPreferenceTags());
        if (!preferenceTags.isEmpty()) {
            int matchCount = 0;
            for (String tag : preferenceTags) {
                if (StringUtils.hasText(tag) && searchableText.contains(tag)) {
                    matchCount++;
                }
            }
            score += Math.min(matchCount, 3) * 0.5;
        }

        if (StringUtils.hasText(profile.getWeeklyFrequency())) {
            if ("高频".equals(profile.getWeeklyFrequency()) && equipment.getRating() != null && equipment.getRating().doubleValue() >= 4.5) {
                score += 0.6;
            }
            if ("休闲".equals(profile.getWeeklyFrequency()) && equipment.getPrice() != null && equipment.getPrice().doubleValue() <= 800) {
                score += 0.4;
            }
        }

        score += calculateSnapshotPreferenceBonus(parseAnswerSnapshot(profile), searchableText, equipment);
        return score;
    }

    private double calculateSimilarity(Equipment left, Equipment right) {
        double similarity = 0.0;

        if (left.getCategoryId() != null && Objects.equals(left.getCategoryId(), right.getCategoryId())) {
            similarity += categoryWeight;
        }

        similarity += calculateTagSimilarity(left.getTags(), right.getTags()) * tagWeight;
        similarity += calculatePriceSimilarity(left.getPrice(), right.getPrice()) * priceWeight;

        if (StringUtils.hasText(left.getBrand()) && Objects.equals(left.getBrand(), right.getBrand())) {
            similarity += 0.2;
        }

        return Math.min(similarity, 1.0);
    }

    private double calculateTagSimilarity(String tags1, String tags2) {
        if (!StringUtils.hasText(tags1) || !StringUtils.hasText(tags2)) {
            return 0.0;
        }

        try {
            JSONArray array1 = JSONUtil.parseArray(tags1);
            JSONArray array2 = JSONUtil.parseArray(tags2);
            Set<Long> set1 = new HashSet<>();
            Set<Long> set2 = new HashSet<>();
            for (int i = 0; i < array1.size(); i++) {
                set1.add(array1.getLong(i));
            }
            for (int i = 0; i < array2.size(); i++) {
                set2.add(array2.getLong(i));
            }
            set1.retainAll(set2);
            if (set1.isEmpty()) {
                return 0.0;
            }
            return (double) set1.size() / Math.max(array1.size(), array2.size());
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private double calculatePriceSimilarity(BigDecimal price1, BigDecimal price2) {
        if (price1 == null || price2 == null) {
            return 0.5;
        }

        double diff = Math.abs(price1.doubleValue() - price2.doubleValue());
        double avg = (price1.doubleValue() + price2.doubleValue()) / 2;
        if (avg == 0) {
            return 0.0;
        }

        double similarity = 1.0 - (diff / avg);
        return Math.max(0.0, similarity);
    }

    private String getPriceRange(BigDecimal price) {
        if (price == null) {
            return "unknown";
        }
        double value = price.doubleValue();
        if (value < 100) {
            return "0-100";
        }
        if (value < 300) {
            return "100-300";
        }
        if (value < 500) {
            return "300-500";
        }
        if (value < 1000) {
            return "500-1000";
        }
        return "1000+";
    }

    private Map<Long, Double> buildUserEquipmentScores(Long userId) {
        List<BrowseHistory> browseHistories = browseHistoryMapper.selectList(
            new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getUpdateTime)
                .orderByDesc(BrowseHistory::getCreateTime)
                .last("LIMIT 50")
        );
        List<Favorite> favorites = favoriteMapper.selectList(
            new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
        );
        List<Comment> comments = commentMapper.selectList(
            new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .orderByDesc(Comment::getCreateTime)
                .last("LIMIT 20")
        );
        return buildUserEquipmentScores(browseHistories, favorites, comments);
    }

    private Map<Long, Double> buildUserEquipmentScores(List<BrowseHistory> browseHistories,
                                                       List<Favorite> favorites,
                                                       List<Comment> comments) {
        Map<Long, Double> equipmentScores = new HashMap<>();

        for (BrowseHistory history : browseHistories) {
            if (history.getEquipmentId() == null) {
                continue;
            }
            double score = equipmentScores.getOrDefault(history.getEquipmentId(), 0.0) + calculateBrowseHistoryWeight(history);
            equipmentScores.put(history.getEquipmentId(), score);
        }

        for (Favorite favorite : favorites) {
            double score = equipmentScores.getOrDefault(favorite.getEquipmentId(), 0.0) + favoriteWeight;
            equipmentScores.put(favorite.getEquipmentId(), score);
        }

        for (Comment comment : comments) {
            double score = equipmentScores.getOrDefault(comment.getEquipmentId(), 0.0) + commentWeight;
            equipmentScores.put(comment.getEquipmentId(), score);
        }

        return equipmentScores;
    }

    private double calculateBrowseHistoryWeight(BrowseHistory history) {
        double score = browseWeight == null ? 1.0 : browseWeight;
        int durationSeconds = history.getDurationSeconds() == null ? 0 : history.getDurationSeconds();
        int interactionCount = history.getInteractionCount() == null ? 0 : history.getInteractionCount();

        score += Math.min(durationSeconds, 600) / 120.0 * 0.35;
        score += Math.min(interactionCount, 6) * 0.4;

        if (StringUtils.hasText(history.getContentType())) {
            String contentType = history.getContentType().toLowerCase();
            if (contentType.contains("detail")) {
                score += 0.8;
            } else if (contentType.contains("compare")) {
                score += 0.6;
            } else if (contentType.contains("list")) {
                score += 0.3;
            }
        }

        LocalDateTime referenceTime = history.getLeaveTime() != null
            ? history.getLeaveTime()
            : (history.getUpdateTime() != null ? history.getUpdateTime() : history.getCreateTime());
        if (referenceTime != null) {
            LocalDateTime now = LocalDateTime.now();
            if (referenceTime.isAfter(now.minusDays(3))) {
                score += 0.5;
            } else if (referenceTime.isAfter(now.minusDays(7))) {
                score += 0.25;
            }
        }

        return score;
    }

    private User getUser(Long userId) {
        try {
            return userMapper.selectById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private UserPreferenceProfile getUserPreferenceProfile(Long userId) {
        try {
            return userPreferenceProfileMapper.selectOne(
                new LambdaQueryWrapper<UserPreferenceProfile>()
                    .eq(UserPreferenceProfile::getUserId, userId)
                    .last("LIMIT 1")
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isPreferenceProfileEmpty(UserPreferenceProfile profile) {
        return profile == null
            || (!StringUtils.hasText(profile.getLevel())
            && !StringUtils.hasText(profile.getPlayStyle())
            && !StringUtils.hasText(profile.getGripStyle())
            && profile.getBudgetMin() == null
            && profile.getBudgetMax() == null
            && !StringUtils.hasText(profile.getWeeklyFrequency())
            && !StringUtils.hasText(profile.getPrimaryGoal())
            && !StringUtils.hasText(profile.getPriorityFocus())
            && !StringUtils.hasText(profile.getPreferredCategories())
            && !StringUtils.hasText(profile.getPreferredBrands())
            && !StringUtils.hasText(profile.getPreferenceTags()));
    }

    private boolean isPriceInBudget(BigDecimal price, BigDecimal min, BigDecimal max) {
        if (price == null) {
            return false;
        }
        if (min != null && price.compareTo(min) < 0) {
            return false;
        }
        if (max != null && price.compareTo(max) > 0) {
            return false;
        }
        return min != null || max != null;
    }

    private boolean isPriceNearBudget(BigDecimal price, BigDecimal min, BigDecimal max) {
        if (price == null) {
            return false;
        }
        if (max != null) {
            BigDecimal threshold = max.multiply(BigDecimal.valueOf(1.15));
            if (price.compareTo(max) > 0 && price.compareTo(threshold) <= 0) {
                return true;
            }
        }
        if (min != null) {
            BigDecimal threshold = min.multiply(BigDecimal.valueOf(0.85));
            return price.compareTo(min) < 0 && price.compareTo(threshold) >= 0;
        }
        return false;
    }

    private Set<String> parseJsonStringSet(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptySet();
        }
        try {
            List<String> values = JSONUtil.toList(JSONUtil.parseArray(json), String.class);
            return values.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(HashSet::new));
        } catch (Exception ignored) {
            return Collections.emptySet();
        }
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private void decoratePersonalRecommendItems(List<Equipment> items, User user, UserPreferenceProfile profile) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (Equipment item : items) {
            if (item == null) {
                continue;
            }
            item.setRecommendChannel(isPreferenceProfileEmpty(profile) ? "行为推荐" : "画像推荐");
            item.setRecommendReason(buildPersonalRecommendReason(item, user, profile));
            item.setRecommendTags(buildPersonalRecommendTags(item, user, profile));
        }
    }

    private void decorateHotRecommendItems(List<Equipment> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (Equipment item : items) {
            if (item == null) {
                continue;
            }
            item.setRecommendChannel("热门精选");
            item.setRecommendReason(buildHotRecommendReason(item));
            item.setRecommendTags(buildHotRecommendTags(item));
        }
    }

    private void decorateSimilarRecommendItems(List<Equipment> items, Equipment targetEquipment) {
        if (items == null || items.isEmpty() || targetEquipment == null) {
            return;
        }

        for (Equipment item : items) {
            if (item == null) {
                continue;
            }
            item.setRecommendChannel("相似推荐");
            item.setRecommendReason(buildSimilarRecommendReason(item, targetEquipment));
            item.setRecommendTags(buildSimilarRecommendTags(item, targetEquipment));
        }
    }

    private String buildPersonalRecommendReason(Equipment equipment, User user, UserPreferenceProfile profile) {
        List<String> reasons = new ArrayList<>();
        String searchableText = String.join(" ",
            defaultText(equipment.getName()),
            defaultText(equipment.getBrand()),
            defaultText(equipment.getDescription()),
            defaultText(equipment.getFeatures()),
            defaultText(equipment.getSuitableFor()),
            defaultText(equipment.getParameters())
        );

        if (!isPreferenceProfileEmpty(profile)) {
            if (isPriceInBudget(equipment.getPrice(), profile.getBudgetMin(), profile.getBudgetMax())) {
                reasons.add("价格落在你的预算范围内");
            }
            if (StringUtils.hasText(profile.getPlayStyle()) && searchableText.contains(profile.getPlayStyle())) {
                reasons.add("更贴近你的" + profile.getPlayStyle() + "打法");
            }
            if (StringUtils.hasText(profile.getPriorityFocus()) && searchableText.contains(profile.getPriorityFocus())) {
                reasons.add("命中了你最看重的" + profile.getPriorityFocus());
            }
            if (StringUtils.hasText(profile.getPrimaryGoal()) && searchableText.contains(profile.getPrimaryGoal())) {
                reasons.add("更适合你当前的" + profile.getPrimaryGoal() + "目标");
            }
            Set<String> preferredCategories = parseJsonStringSet(profile.getPreferredCategories());
            if (equipment.getCategoryId() != null && preferredCategories.contains(String.valueOf(equipment.getCategoryId()))) {
                reasons.add("属于你最近常选的偏好分类");
            }
        }

        if (reasons.isEmpty() && user != null && StringUtils.hasText(user.getPlayStyle()) && searchableText.contains(user.getPlayStyle())) {
            reasons.add("和你当前的" + user.getPlayStyle() + "风格更契合");
        }
        if (reasons.isEmpty()) {
            reasons.add("结合你最近的浏览与收藏方向，这件装备在相似候选里排序更靠前");
        }

        return "推荐理由：" + reasons.stream().limit(2).collect(Collectors.joining("，"));
    }

    private List<String> buildPersonalRecommendTags(Equipment equipment, User user, UserPreferenceProfile profile) {
        List<String> tags = new ArrayList<>();
        String searchableText = String.join(" ",
            defaultText(equipment.getName()),
            defaultText(equipment.getBrand()),
            defaultText(equipment.getDescription()),
            defaultText(equipment.getFeatures()),
            defaultText(equipment.getSuitableFor()),
            defaultText(equipment.getParameters())
        );

        if (!isPreferenceProfileEmpty(profile)) {
            if (isPriceInBudget(equipment.getPrice(), profile.getBudgetMin(), profile.getBudgetMax())) {
                addUniqueTag(tags, "预算匹配");
            }
            if (StringUtils.hasText(profile.getPlayStyle()) && searchableText.contains(profile.getPlayStyle())) {
                addUniqueTag(tags, profile.getPlayStyle());
            }
            if (StringUtils.hasText(profile.getPriorityFocus()) && searchableText.contains(profile.getPriorityFocus())) {
                addUniqueTag(tags, profile.getPriorityFocus());
            }
            if (StringUtils.hasText(profile.getPrimaryGoal()) && searchableText.contains(profile.getPrimaryGoal())) {
                addUniqueTag(tags, profile.getPrimaryGoal());
            }
        }

        if (tags.isEmpty() && user != null) {
            addUniqueTag(tags, user.getLevel());
            addUniqueTag(tags, user.getPlayStyle());
        }

        if (equipment.getRating() != null && equipment.getRating().doubleValue() >= 4.5) {
            addUniqueTag(tags, "高评分");
        }
        if (defaultInteger(equipment.getFavoriteCount()) >= 20) {
            addUniqueTag(tags, "高收藏");
        }

        return tags.stream().limit(3).collect(Collectors.toList());
    }

    private String buildHotRecommendReason(Equipment equipment) {
        List<String> reasons = new ArrayList<>();
        if (equipment.getRating() != null && equipment.getRating().doubleValue() >= 4.5) {
            reasons.add("评分表现稳定");
        }
        if (defaultInteger(equipment.getFavoriteCount()) >= 20) {
            reasons.add("收藏热度持续靠前");
        }
        if (defaultInteger(equipment.getViewCount()) >= 100) {
            reasons.add("近期浏览热度较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合热度与口碑表现更稳定");
        }
        return "推荐理由：" + reasons.stream().limit(2).collect(Collectors.joining("，"));
    }

    private List<String> buildHotRecommendTags(Equipment equipment) {
        List<String> tags = new ArrayList<>();
        addUniqueTag(tags, "热门趋势");
        if (equipment.getRating() != null && equipment.getRating().doubleValue() >= 4.5) {
            addUniqueTag(tags, "高评分");
        }
        if (defaultInteger(equipment.getFavoriteCount()) >= 20) {
            addUniqueTag(tags, "高收藏");
        }
        if (defaultInteger(equipment.getSalesCount()) >= 20) {
            addUniqueTag(tags, "热销");
        }
        return tags.stream().limit(3).collect(Collectors.toList());
    }

    private String buildSimilarRecommendReason(Equipment equipment, Equipment targetEquipment) {
        List<String> reasons = new ArrayList<>();
        if (targetEquipment.getCategoryId() != null && Objects.equals(targetEquipment.getCategoryId(), equipment.getCategoryId())) {
            reasons.add("同属一个器材分类");
        }
        if (StringUtils.hasText(targetEquipment.getBrand()) && Objects.equals(targetEquipment.getBrand(), equipment.getBrand())) {
            reasons.add("品牌路线相近");
        }
        if (calculatePriceSimilarity(targetEquipment.getPrice(), equipment.getPrice()) >= 0.8) {
            reasons.add("价格带更接近");
        }
        if (reasons.isEmpty()) {
            reasons.add("和你刚浏览的装备在定位和使用场景上更接近");
        }
        return "推荐理由：" + reasons.stream().limit(2).collect(Collectors.joining("，"));
    }

    private List<String> buildSimilarRecommendTags(Equipment equipment, Equipment targetEquipment) {
        List<String> tags = new ArrayList<>();
        addUniqueTag(tags, "同类替代");
        if (targetEquipment.getCategoryId() != null && Objects.equals(targetEquipment.getCategoryId(), equipment.getCategoryId())) {
            addUniqueTag(tags, "同分类");
        }
        if (StringUtils.hasText(targetEquipment.getBrand()) && Objects.equals(targetEquipment.getBrand(), equipment.getBrand())) {
            addUniqueTag(tags, equipment.getBrand());
        }
        if (calculatePriceSimilarity(targetEquipment.getPrice(), equipment.getPrice()) >= 0.8) {
            addUniqueTag(tags, "同价位");
        }
        return tags.stream().limit(3).collect(Collectors.toList());
    }

    private int calculateProfileCompletion(UserPreferenceProfile profile) {
        if (profile == null) {
            return 0;
        }

        int total = 8;
        int filled = 0;
        if (StringUtils.hasText(profile.getLevel())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getPlayStyle())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getGripStyle())) {
            filled++;
        }
        if (profile.getBudgetMin() != null || profile.getBudgetMax() != null) {
            filled++;
        }
        if (StringUtils.hasText(profile.getWeeklyFrequency())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getPrimaryGoal())) {
            filled++;
        }
        if (StringUtils.hasText(profile.getPriorityFocus())) {
            filled++;
        }
        if (!parseJsonStringSet(profile.getPreferredCategories()).isEmpty()
            || !parseJsonStringSet(profile.getPreferredBrands()).isEmpty()
            || !parseJsonStringSet(profile.getPreferenceTags()).isEmpty()) {
            filled++;
        }

        return (int) Math.round((filled * 100.0) / total);
    }

    private String resolveRecommendationMode(UserPreferenceProfile profile, int browseCount, int favoriteCount, int commentCount) {
        if (!isPreferenceProfileEmpty(profile)) {
            return "画像 + 行为混合推荐";
        }
        if (browseCount > 0 || favoriteCount > 0 || commentCount > 0) {
            return "行为驱动推荐";
        }
        return "热门趋势推荐";
    }

    private String buildPersonaTitle(UserPreferenceProfile profile, User user, int browseCount, int favoriteCount, int commentCount) {
        String level = firstText(profile == null ? null : profile.getLevel(), user == null ? null : user.getLevel());
        String playStyle = firstText(profile == null ? null : profile.getPlayStyle(), user == null ? null : user.getPlayStyle());
        String priorityLabel = mapPriorityFocusLabel(profile == null ? null : profile.getPriorityFocus());

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(level)) {
            parts.add(level);
        }
        if (StringUtils.hasText(playStyle)) {
            parts.add(playStyle);
        }
        if (StringUtils.hasText(priorityLabel)) {
            parts.add(priorityLabel);
        }

        if (!parts.isEmpty()) {
            return String.join(" · ", parts) + "器材画像";
        }

        if (browseCount > 0 || favoriteCount > 0 || commentCount > 0) {
            return "行为画像待完善";
        }

        return "新用户画像待建立";
    }

    private String buildPersonaDescription(UserPreferenceProfile profile,
                                           User user,
                                           String recommendationMode,
                                           String dominantCategoryName,
                                           String dominantBrand,
                                           String dominantPriceRange) {
        String level = firstText(profile == null ? null : profile.getLevel(), user == null ? null : user.getLevel());
        String playStyle = firstText(profile == null ? null : profile.getPlayStyle(), user == null ? null : user.getPlayStyle());
        String gripStyle = profile == null ? null : profile.getGripStyle();
        String budgetLabel = formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax());

        List<String> profileParts = new ArrayList<>();
        if (StringUtils.hasText(level)) {
            profileParts.add(level);
        }
        if (StringUtils.hasText(playStyle)) {
            profileParts.add(playStyle);
        }
        if (StringUtils.hasText(gripStyle)) {
            profileParts.add(gripStyle);
        }

        if (!profileParts.isEmpty()) {
            String profileSummary = String.join(" / ", profileParts);
            List<String> focus = new ArrayList<>();
            if (StringUtils.hasText(budgetLabel)) {
                focus.add("预算聚焦 " + budgetLabel);
            }
            if (StringUtils.hasText(dominantCategoryName)) {
                focus.add("近期重点关注 " + dominantCategoryName);
            }
            if (StringUtils.hasText(dominantBrand)) {
                focus.add("对 " + dominantBrand + " 的关注更高");
            }
            String focusText = focus.isEmpty() ? "系统会优先把更贴近你当前需求的器材排在前面。" : String.join("，", focus) + "。";
            return "当前画像更偏向 " + profileSummary + "，" + focusText;
        }

        if ("行为驱动推荐".equals(recommendationMode)) {
            String categoryText = StringUtils.hasText(dominantCategoryName) ? dominantCategoryName : "近期浏览内容";
            String priceText = formatPriceRangeLabel(dominantPriceRange);
            if (StringUtils.hasText(priceText)) {
                return "系统正在根据你最近的浏览和收藏行为建立临时画像，目前更偏向 " + categoryText + "，并优先参考 " + priceText + " 这个价位带。";
            }
            return "系统正在根据你最近的浏览和收藏行为建立临时画像，目前更偏向 " + categoryText + "，继续完善问答后推荐会更稳定。";
        }

        return "当前画像信息还比较少，先从问答和浏览行为里积累样本，系统才能更准确地理解你的打法与预算。";
    }

    private List<String> buildCoreTags(UserPreferenceProfile profile,
                                       User user,
                                       String dominantCategoryName,
                                       String dominantBrand,
                                       String dominantPriceRange) {
        List<String> tags = new ArrayList<>();
        addUniqueTag(tags, firstText(profile == null ? null : profile.getLevel(), user == null ? null : user.getLevel()));
        addUniqueTag(tags, firstText(profile == null ? null : profile.getPlayStyle(), user == null ? null : user.getPlayStyle()));
        addUniqueTag(tags, profile == null ? null : profile.getGripStyle());
        addUniqueTag(tags, profile == null ? null : profile.getPrimaryGoal());
        addUniqueTag(tags, profile == null ? null : profile.getPriorityFocus());
        addUniqueTag(tags, formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax()));
        addUniqueTag(tags, dominantCategoryName);
        addUniqueTag(tags, dominantBrand);
        addUniqueTag(tags, formatPriceRangeLabel(dominantPriceRange));
        return tags.stream().limit(8).collect(Collectors.toList());
    }

    private List<String> buildImprovementActions(UserPreferenceProfile profile,
                                                 int completionScore,
                                                 int browseCount,
                                                 int favoriteCount,
                                                 int commentCount,
                                                 boolean categoryMissing) {
        List<String> actions = new ArrayList<>();

        if (completionScore < 75) {
            actions.add("补齐问答画像，系统才能更稳定地区分你的打法、预算和使用目标。");
        }
        if (profile == null || (profile.getBudgetMin() == null && profile.getBudgetMax() == null)) {
            actions.add("补充预算区间后，推荐结果会更容易收敛到真正可选的范围。");
        }
        if (browseCount < 6) {
            actions.add("再多浏览几类器材，系统需要更多行为样本来识别真实偏好。");
        }
        if (favoriteCount == 0) {
            actions.add("收藏几件候选装备，能显著增强推荐确定性。");
        }
        if (commentCount == 0) {
            actions.add("写 1 条使用评价，系统会把真实反馈纳入画像。");
        }
        if (categoryMissing) {
            actions.add("先从底板、胶皮或球鞋中挑一类深入浏览，画像会更快形成稳定方向。");
        }

        return actions.stream().distinct().limit(4).collect(Collectors.toList());
    }

    private List<UserProfileInsightSignalDTO> buildInsightSignals(int completionScore,
                                                                  int browseCount,
                                                                  int favoriteCount,
                                                                  String dominantCategoryName,
                                                                  String dominantBrand,
                                                                  UserPreferenceProfile profile,
                                                                  String dominantPriceRange) {
        List<UserProfileInsightSignalDTO> signals = new ArrayList<>();
        signals.add(buildSignal("画像完成度", completionScore + "%", completionScore >= 75 ? "画像信息已经比较完整" : "继续补齐后推荐会更稳"));
        signals.add(buildSignal("浏览样本", browseCount + " 次", browseCount >= 8 ? "行为样本足够参与排序" : "样本偏少，建议继续浏览"));
        signals.add(buildSignal("收藏线索", favoriteCount + " 件", favoriteCount > 0 ? "收藏会直接增强推荐权重" : "还没有形成明确收藏偏好"));
        signals.add(buildSignal(
            "主看方向",
            StringUtils.hasText(dominantCategoryName) ? dominantCategoryName : "待识别",
            StringUtils.hasText(dominantBrand)
                ? "近期也更关注 " + dominantBrand
                : (StringUtils.hasText(formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax()))
                    ? "预算锚点 " + formatBudgetLabel(profile.getBudgetMin(), profile.getBudgetMax())
                    : defaultSignalDetailForPrice(dominantPriceRange))
        ));
        return signals;
    }

    private List<UserProfileActionDTO> buildActionCards(UserPreferenceProfile profile,
                                                        int completionScore,
                                                        String dominantCategoryId,
                                                        String dominantCategoryName,
                                                        String dominantBrand,
                                                        String dominantPriceRange,
                                                        int favoriteCount) {
        List<UserProfileActionDTO> actions = new ArrayList<>();

        if (completionScore < 75) {
            actions.add(buildAction(
                "补全画像，让推荐更稳",
                "再完善一次问答，首页推荐会更聚焦，解释也会更清晰。",
                "继续问答",
                "/quiz",
                null,
                Collections.singletonList(completionScore + "% 完成度")
            ));
        }

        Map<String, Object> categoryQuery = new LinkedHashMap<>();
        if (StringUtils.hasText(dominantCategoryId)) {
            categoryQuery.put("categoryId", dominantCategoryId);
        } else {
            String keyword = inferKeyword(profile, dominantCategoryName, dominantBrand);
            if (StringUtils.hasText(keyword)) {
                categoryQuery.put("keyword", keyword);
            }
        }
        categoryQuery.put("sortBy", "rating");
        actions.add(buildAction(
            StringUtils.hasText(dominantCategoryName) ? "继续深挖 " + dominantCategoryName : "按当前打法继续筛装备",
            StringUtils.hasText(dominantCategoryName)
                ? "这是你最近行为里权重最高的方向，先按评分和口碑看一轮。"
                : "把当前画像最相关的方向单独拉出来，更容易缩小候选范围。",
            "查看精选",
            "/equipment-list",
            categoryQuery,
            buildTags(dominantCategoryName, profile == null ? null : profile.getPlayStyle())
        ));

        Map<String, Object> budgetQuery = buildBudgetQuery(profile, dominantPriceRange);
        if (!budgetQuery.isEmpty()) {
            budgetQuery.put("sortBy", "rating");
            String budgetLabel = formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax());
            if (!StringUtils.hasText(budgetLabel)) {
                budgetLabel = formatPriceRangeLabel(dominantPriceRange);
            }
            actions.add(buildAction(
                budgetLabel + " 预算内优先看这些",
                "先锁定价格带，再结合评分与热度筛选，会更接近真实可购买方案。",
                "按预算浏览",
                "/equipment-list",
                budgetQuery,
                buildTags(budgetLabel, profile == null ? null : profile.getPriorityFocus())
            ));
        }

        if (favoriteCount == 0) {
            actions.add(buildAction(
                "先看看本周口碑榜单",
                "当收藏样本还不多时，先用榜单把高热度候选快速过一遍。",
                "查看榜单",
                "/leaderboards",
                null,
                Collections.singletonList("趋势参考")
            ));
        } else if (StringUtils.hasText(dominantBrand)) {
            Map<String, Object> brandQuery = new LinkedHashMap<>();
            brandQuery.put("keyword", dominantBrand);
            brandQuery.put("sortBy", "rating");
            actions.add(buildAction(
                dominantBrand + " 品牌线继续看",
                "你近期对这个品牌关注更高，可以顺着品牌线继续筛选。",
                "查看品牌线",
                "/equipment-list",
                brandQuery,
                buildTags(dominantBrand)
            ));
        }

        return actions.stream().limit(3).collect(Collectors.toList());
    }

    private UserProfileInsightSignalDTO buildSignal(String label, String value, String detail) {
        UserProfileInsightSignalDTO signal = new UserProfileInsightSignalDTO();
        signal.setLabel(label);
        signal.setValue(value);
        signal.setDetail(detail);
        return signal;
    }

    private UserProfileActionDTO buildAction(String title,
                                             String description,
                                             String buttonText,
                                             String targetPath,
                                             Map<String, Object> query,
                                             List<String> tags) {
        UserProfileActionDTO action = new UserProfileActionDTO();
        action.setTitle(title);
        action.setDescription(description);
        action.setButtonText(buttonText);
        action.setTargetPath(targetPath);
        action.setQuery(query);
        action.setTags(tags == null ? Collections.emptyList() : tags);
        return action;
    }

    private List<String> buildTags(String... values) {
        List<String> tags = new ArrayList<>();
        for (String value : values) {
            addUniqueTag(tags, value);
        }
        return tags;
    }

    private void addUniqueTag(List<String> tags, String value) {
        if (StringUtils.hasText(value) && !tags.contains(value)) {
            tags.add(value);
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String mapPriorityFocusLabel(String priorityFocus) {
        if (!StringUtils.hasText(priorityFocus)) {
            return "";
        }
        if ("控制".equals(priorityFocus)) {
            return "稳控型";
        }
        if ("速度".equals(priorityFocus)) {
            return "爆发型";
        }
        if ("旋转".equals(priorityFocus)) {
            return "旋转型";
        }
        if ("性价比".equals(priorityFocus)) {
            return "实用型";
        }
        return priorityFocus;
    }

    private String formatBudgetLabel(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "";
        }
        if (max != null && max.doubleValue() <= 300) {
            return "300 元以内";
        }
        if (min != null && min.doubleValue() >= 1500 && max != null && max.doubleValue() >= 1500) {
            return "1500 元以上";
        }
        if (min != null && max != null) {
            return min.stripTrailingZeros().toPlainString() + " - " + max.stripTrailingZeros().toPlainString() + " 元";
        }
        if (max != null) {
            return max.stripTrailingZeros().toPlainString() + " 元以内";
        }
        return min.stripTrailingZeros().toPlainString() + " 元以上";
    }

    private String formatPriceRangeLabel(String priceRange) {
        if (!StringUtils.hasText(priceRange) || "unknown".equals(priceRange)) {
            return "";
        }
        if ("0-100".equals(priceRange)) {
            return "100 元以内";
        }
        if ("100-300".equals(priceRange)) {
            return "100 - 300 元";
        }
        if ("300-500".equals(priceRange)) {
            return "300 - 500 元";
        }
        if ("500-1000".equals(priceRange)) {
            return "500 - 1000 元";
        }
        if ("1000+".equals(priceRange)) {
            return "1000 元以上";
        }
        return priceRange;
    }

    private String defaultSignalDetailForPrice(String priceRange) {
        String label = formatPriceRangeLabel(priceRange);
        return StringUtils.hasText(label) ? "近期行为更集中在 " + label : "继续浏览后会出现更清晰的价格偏好";
    }

    private String getTopKey(Map<String, Double> map) {
        return map.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
    }

    private List<String> getTopKeys(Map<String, Double> map, int limit) {
        return map.entrySet().stream()
            .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private Map<String, String> loadCategoryNameMap(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<Long> ids = categoryIds.stream()
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
            List<Category> categories = categoryMapper.selectBatchIds(ids);
            return categories.stream().collect(Collectors.toMap(
                item -> String.valueOf(item.getId()),
                Category::getName,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private String inferKeyword(UserPreferenceProfile profile, String dominantCategoryName, String dominantBrand) {
        if (StringUtils.hasText(dominantBrand)) {
            return dominantBrand;
        }
        if (StringUtils.hasText(dominantCategoryName)) {
            return dominantCategoryName;
        }
        if (profile != null && StringUtils.hasText(profile.getPlayStyle())) {
            if ("弧圈".equals(profile.getPlayStyle())) {
                return "胶皮";
            }
            if ("快攻".equals(profile.getPlayStyle())) {
                return "底板";
            }
            if ("削球".equals(profile.getPlayStyle())) {
                return "胶皮";
            }
        }
        return "底板";
    }

    private Map<String, Object> buildBudgetQuery(UserPreferenceProfile profile, String dominantPriceRange) {
        Map<String, Object> query = new LinkedHashMap<>();
        if (profile != null && (profile.getBudgetMin() != null || profile.getBudgetMax() != null)) {
            if (profile.getBudgetMin() != null) {
                query.put("minPrice", profile.getBudgetMin().stripTrailingZeros().toPlainString());
            }
            if (profile.getBudgetMax() != null) {
                query.put("maxPrice", profile.getBudgetMax().stripTrailingZeros().toPlainString());
            }
            return query;
        }

        if (!StringUtils.hasText(dominantPriceRange)) {
            return query;
        }

        switch (dominantPriceRange) {
            case "0-100":
                query.put("maxPrice", "100");
                break;
            case "100-300":
                query.put("minPrice", "100");
                query.put("maxPrice", "300");
                break;
            case "300-500":
                query.put("minPrice", "300");
                query.put("maxPrice", "500");
                break;
            case "500-1000":
                query.put("minPrice", "500");
                query.put("maxPrice", "1000");
                break;
            case "1000+":
                query.put("minPrice", "1000");
                break;
            default:
                break;
        }

        return query;
    }

    private String buildWorkbenchTitle(UserPreferenceProfile profile, User user, Map<String, Object> snapshot) {
        String level = firstText(profile == null ? null : profile.getLevel(), user == null ? null : user.getLevel(), "成长中球友");
        String style = firstText(profile == null ? null : profile.getPlayStyle(), user == null ? null : user.getPlayStyle(), "全面");
        String goal = firstText(profile == null ? null : profile.getPrimaryGoal(), getSnapshotString(snapshot, "equipmentGoal"), "稳定升级");
        return level + "·" + style + "·" + goal + " 推荐工作台";
    }

    private String buildWorkbenchDescription(UserPreferenceProfile profile,
                                             Map<String, Object> behavior,
                                             Map<String, Object> snapshot) {
        String budgetLabel = formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax());
        String specialNeed = String.join("、", getSnapshotList(snapshot, "specialNeeds").stream().limit(2).collect(Collectors.toList()));
        String favoriteDirection = valueAsString(behavior.get("favoriteCategory"));
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(budgetLabel)) {
            parts.add("预算主要落在 " + budgetLabel);
        }
        if (StringUtils.hasText(favoriteDirection)) {
            parts.add("近期更关注 " + favoriteDirection + " 方向");
        }
        if (StringUtils.hasText(specialNeed)) {
            parts.add("同时需要兼顾 " + specialNeed);
        }
        if (parts.isEmpty()) {
            parts.add("系统会结合你的问答、浏览、收藏和评价表现，持续修正装备推荐策略");
        }
        return String.join("，", parts) + "。";
    }

    private List<String> buildWorkbenchKeywords(UserPreferenceProfile profile, User user, Map<String, Object> snapshot) {
        List<String> tags = new ArrayList<>();
        addUniqueTag(tags, profile == null ? null : profile.getLevel());
        addUniqueTag(tags, profile == null ? null : profile.getPlayStyle());
        addUniqueTag(tags, profile == null ? null : profile.getGripStyle());
        addUniqueTag(tags, getSnapshotString(snapshot, "heightRange"));
        addUniqueTag(tags, getSnapshotString(snapshot, "dominantHand"));
        addUniqueTag(tags, getSnapshotString(snapshot, "swingForceHabit"));
        addUniqueTag(tags, profile == null ? null : profile.getPriorityFocus());
        addUniqueTag(tags, profile == null ? null : profile.getPrimaryGoal());
        addUniqueTag(tags, formatBudgetLabel(profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax()));
        for (String item : getSnapshotList(snapshot, "specialNeeds")) {
            addUniqueTag(tags, item);
        }
        if (tags.isEmpty()) {
            addUniqueTag(tags, user == null ? null : user.getPlayStyle());
        }
        return tags.stream().limit(8).collect(Collectors.toList());
    }

    private List<String> buildCoachSuggestions(UserPreferenceProfile profile, Map<String, Object> snapshot) {
        List<String> suggestions = new ArrayList<>();
        String level = profile == null ? "" : defaultText(profile.getLevel());
        String playStyle = profile == null ? "" : defaultText(profile.getPlayStyle());
        String swingHabit = getSnapshotString(snapshot, "swingForceHabit");
        List<String> specialNeeds = getSnapshotList(snapshot, "specialNeeds");

        if ("初级".equals(level)) {
            suggestions.add("先优先保证容错、重量和手感稳定，不急着一步上高硬高弹配置。");
        } else if ("中级".equals(level)) {
            suggestions.add("建议围绕当前主打法做单点强化，优先升级最影响出球质量的那一环。");
        } else if ("高级".equals(level)) {
            suggestions.add("可以开始微调重量、平衡感和胶皮搭配，把训练状态和比赛状态区分开来。");
        }

        if ("快攻".equals(playStyle)) {
            suggestions.add("快攻型用户优先关注出球干脆、重心清晰的底板，再用胶皮补充弧线。");
        } else if ("弧圈".equals(playStyle)) {
            suggestions.add("弧圈型更适合优先看持球感、摩擦质量和连续对拉稳定性。");
        } else if ("削球".equals(playStyle)) {
            suggestions.add("削球或防守路线建议把控制、减震和长时间使用的舒适度放在前面。");
        }

        if (StringUtils.hasText(swingHabit)) {
            suggestions.add("你的发力习惯偏向“" + swingHabit + "”，推荐时会同步校正速度和容错的配比。");
        }
        if (!specialNeeds.isEmpty()) {
            suggestions.add("特殊需求已纳入推荐权重：" + String.join("、", specialNeeds) + "。");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("继续补充画像问答和使用反馈后，系统会给出更细的专业建议。");
        }
        return suggestions.stream().limit(4).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCombinationPlans(UserPreferenceProfile profile,
                                                            Map<String, Object> snapshot,
                                                            List<Equipment> personalRecommendations,
                                                            List<Equipment> hotRecommendations) {
        List<Equipment> pool = new ArrayList<>();
        pool.addAll(personalRecommendations);
        pool.addAll(hotRecommendations);

        Equipment blade = findFirstByRole(pool, "blade");
        Equipment rubber = findFirstByRole(pool, "rubber");
        Equipment accessory = findFirstByRole(pool, "accessory");
        Equipment shoe = findFirstByRole(pool, "shoe");

        List<Map<String, Object>> plans = new ArrayList<>();
        plans.add(buildCombinationPlan(
            "稳控起步方案",
            "适合先把上手难度降下来，优先兼顾稳定性、重量控制和容错表现。",
            profile,
            blade,
            rubber,
            accessory != null ? accessory : shoe,
            buildTags(profile == null ? null : profile.getLevel(), "稳控", firstText(profile == null ? null : profile.getGripStyle(), getSnapshotString(snapshot, "dominantHand")))
        ));
        plans.add(buildCombinationPlan(
            "均衡进阶方案",
            "围绕你当前主打法做一套训练和实战都能衔接的组合，兼顾速度、旋转与控制。",
            profile,
            blade != null ? blade : findAny(pool),
            rubber != null ? rubber : findAny(pool),
            shoe != null ? shoe : accessory,
            buildTags(profile == null ? null : profile.getPlayStyle(), profile == null ? null : profile.getPriorityFocus(), "训练升级")
        ));
        plans.add(buildCombinationPlan(
            "比赛冲刺方案",
            "把近期高热度和高匹配候选组合在一起，适合准备比赛或集中升级阶段参考。",
            profile,
            findPreferredCandidate(pool, "blade", profile),
            findPreferredCandidate(pool, "rubber", profile),
            findPreferredCandidate(pool, "shoe", profile),
            buildTags(profile == null ? null : profile.getPrimaryGoal(), "比赛场景", getSnapshotString(snapshot, "coachNeed"))
        ));
        return plans;
    }

    private Map<String, Object> buildCombinationPlan(String title,
                                                     String description,
                                                     UserPreferenceProfile profile,
                                                     Equipment first,
                                                     Equipment second,
                                                     Equipment third,
                                                     List<String> tags) {
        List<Equipment> items = new ArrayList<>();
        if (first != null) {
            items.add(first);
        }
        if (second != null && !items.contains(second)) {
            items.add(second);
        }
        if (third != null && !items.contains(third)) {
            items.add(third);
        }

        BigDecimal total = items.stream()
            .map(Equipment::getPrice)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("title", title);
        plan.put("description", description);
        plan.put("estimatedBudget", total.compareTo(BigDecimal.ZERO) > 0 ? total.stripTrailingZeros().toPlainString() : "");
        plan.put("budgetFit", total.compareTo(BigDecimal.ZERO) > 0 && isPriceInBudget(total, profile == null ? null : profile.getBudgetMin(), profile == null ? null : profile.getBudgetMax()));
        plan.put("tags", tags);
        plan.put("items", items.stream().map(item -> {
            Map<String, Object> equipmentMap = new LinkedHashMap<>();
            equipmentMap.put("id", item.getId());
            equipmentMap.put("name", item.getName());
            equipmentMap.put("brand", item.getBrand());
            equipmentMap.put("price", item.getPrice());
            equipmentMap.put("mainImage", item.getMainImage());
            equipmentMap.put("role", inferEquipmentRole(item));
            return equipmentMap;
        }).collect(Collectors.toList()));
        return plan;
    }

    private List<Map<String, Object>> buildUpgradePlans(UserPreferenceProfile profile, Map<String, Object> snapshot) {
        List<Map<String, Object>> plans = new ArrayList<>();
        String level = profile == null ? "" : defaultText(profile.getLevel());
        String playStyle = profile == null ? "" : defaultText(profile.getPlayStyle());
        String goal = firstText(profile == null ? null : profile.getPrimaryGoal(), getSnapshotString(snapshot, "equipmentGoal"));

        plans.add(buildUpgradePlan(
            "当前阶段优先升级",
            firstText(goal, "把最短板补齐"),
            "先从最影响上手感受的一项开始，避免一次性全套更换导致适应成本过高。",
            buildTags(level, playStyle, profile == null ? null : profile.getPriorityFocus())
        ));
        plans.add(buildUpgradePlan(
            "未来 4-8 周训练迭代",
            "按训练节奏逐步升级",
            "每一轮训练后记录控制、吃球、出球稳定性和疲劳感，系统会据此继续迭代建议。",
            buildTags(profile == null ? null : profile.getWeeklyFrequency(), getSnapshotString(snapshot, "swingForceHabit"), "阶段复盘")
        ));
        plans.add(buildUpgradePlan(
            "比赛/专项场景配置",
            "为专项目标准备第二套思路",
            "如果你已经进入进阶训练期，可以为比赛、日常训练和恢复期保留不同的配置参考。",
            buildTags(getSnapshotString(snapshot, "coachNeed"), String.join("、", getSnapshotList(snapshot, "specialNeeds")), "场景化")
        ));
        return plans;
    }

    private Map<String, Object> buildUpgradePlan(String title, String stage, String description, List<String> tags) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("title", title);
        plan.put("stage", stage);
        plan.put("description", description);
        plan.put("tags", tags);
        return plan;
    }

    private List<Map<String, Object>> buildTrendSignals(Map<String, Object> behavior,
                                                        List<Equipment> hotRecommendations,
                                                        List<Equipment> recentArrivals) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildTrendSignal(
            "热门趋势",
            "当前高热装备更多集中在“" + firstText(valueAsString(behavior.get("favoriteCategory")), "综合榜") + "”方向。",
            buildTags(valueAsString(behavior.get("favoriteBrand")), valueAsString(behavior.get("favoritePriceRange")))
        ));
        if (!hotRecommendations.isEmpty()) {
            items.add(buildTrendSignal(
                "高热候选",
                "近期热度和互动都较高的装备是 “" + hotRecommendations.get(0).getName() + "”，适合作为比较基准。",
                buildTags("热度高", hotRecommendations.get(0).getBrand(), hotRecommendations.get(0).getRecommendChannel())
            ));
        }
        if (!recentArrivals.isEmpty()) {
            items.add(buildTrendSignal(
                "新品提醒",
                "最近入库的新装备会优先进入你的候选池，帮助你同步跟进新款方案。",
                buildTags("新品", recentArrivals.get(0).getBrand(), inferEquipmentRole(recentArrivals.get(0)))
            ));
        }
        return items;
    }

    private Map<String, Object> buildTrendSignal(String title, String description, List<String> tags) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("tags", tags);
        return map;
    }

    private List<Map<String, Object>> buildTrialCards(UserPreferenceProfile profile, List<Equipment> personalRecommendations) {
        List<Map<String, Object>> cards = new ArrayList<>();
        for (Equipment item : personalRecommendations.stream().limit(3).collect(Collectors.toList())) {
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("equipmentId", item.getId());
            card.put("title", item.getName() + " 试打预约建议");
            card.put("description", "建议先在线加入对比或收藏，再预约到店试打，重点验证重量、出球和连续性表现。");
            card.put("bookingHint", "优先预约 " + firstText(profile == null ? null : profile.getPlayStyle(), "当前主打法") + " 训练场景试用");
            card.put("tags", buildTags(item.getBrand(), inferEquipmentRole(item), item.getRecommendChannel()));
            cards.add(card);
        }
        return cards;
    }

    private List<Map<String, Object>> buildPlayerReferences() {
        List<StarPlayer> players = starPlayerMapper.selectList(
            new LambdaQueryWrapper<StarPlayer>().orderByAsc(StarPlayer::getId).last("LIMIT 6")
        );
        if (players.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> playerIds = players.stream().map(StarPlayer::getId).collect(Collectors.toList());
        List<PlayerEquipmentRelation> relations = playerEquipmentRelationMapper.selectList(
            new LambdaQueryWrapper<PlayerEquipmentRelation>()
                .in(PlayerEquipmentRelation::getPlayerId, playerIds)
                .eq(PlayerEquipmentRelation::getIsCurrent, 1)
        );
        List<Long> equipmentIds = relations.stream()
            .map(PlayerEquipmentRelation::getEquipmentId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, Equipment> equipmentMap = equipmentIds.isEmpty()
            ? Collections.emptyMap()
            : equipmentMapper.selectBatchIds(equipmentIds).stream().collect(Collectors.toMap(Equipment::getId, item -> item));

        return players.stream().map(player -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("playerId", player.getId());
            map.put("name", player.getName());
            map.put("country", player.getCountry());
            map.put("avatarUrl", player.getAvatar());
            map.put("description", player.getIntro());
            List<Map<String, Object>> equipmentList = relations.stream()
                .filter(item -> Objects.equals(item.getPlayerId(), player.getId()))
                .map(item -> equipmentMap.get(item.getEquipmentId()))
                .filter(Objects::nonNull)
                .map(item -> {
                    Map<String, Object> equipmentItem = new LinkedHashMap<>();
                    equipmentItem.put("id", item.getId());
                    equipmentItem.put("name", item.getName());
                    equipmentItem.put("brand", item.getBrand());
                    equipmentItem.put("mainImage", item.getMainImage());
                    return equipmentItem;
                })
                .collect(Collectors.toList());
            map.put("equipments", equipmentList);
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCompareOptions(List<Equipment> personalRecommendations, List<Equipment> hotRecommendations) {
        List<Map<String, Object>> options = new ArrayList<>();
        List<Equipment> source = new ArrayList<>();
        source.addAll(personalRecommendations);
        source.addAll(hotRecommendations);
        Map<String, List<Equipment>> roleGroups = source.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(this::inferEquipmentRole, LinkedHashMap::new, Collectors.toList()));

        roleGroups.forEach((role, items) -> {
            List<Equipment> uniqueItems = items.stream()
                .collect(Collectors.toMap(Equipment::getId, item -> item, (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .limit(3)
                .collect(Collectors.toList());
            if (uniqueItems.size() < 2) {
                return;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("title", roleLabel(role) + " 对比组");
            option.put("description", "把同类高匹配候选放在一起比参数、价格和适用场景。");
            option.put("ids", uniqueItems.stream().map(Equipment::getId).collect(Collectors.toList()));
            option.put("tags", uniqueItems.stream().map(Equipment::getBrand).filter(StringUtils::hasText).distinct().limit(3).collect(Collectors.toList()));
            options.add(option);
        });
        return options;
    }

    private Map<String, Object> buildFeedbackLoop(Long userId, UserPreferenceProfile profile, Map<String, Object> behavior) {
        long combinationCount = equipmentCombinationMapper.selectCount(
            new LambdaQueryWrapper<EquipmentCombination>().eq(EquipmentCombination::getUserId, userId)
        );
        int completionScore = calculateProfileCompletion(profile);

        Map<String, Object> loop = new LinkedHashMap<>();
        loop.put("headline", "数据采集 - 智能推荐 - 用户反馈 - 模型迭代");
        loop.put("description", "系统会把你的浏览、收藏、评论、问答和组合使用情况持续转成新的推荐信号。");
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(buildMetric("画像完成度", completionScore + "%"));
        metrics.add(buildMetric("浏览记录", valueAsString(behavior.getOrDefault("browseCount", 0))));
        metrics.add(buildMetric("收藏反馈", valueAsString(behavior.getOrDefault("favoriteCount", 0))));
        metrics.add(buildMetric("组合沉淀", String.valueOf(combinationCount)));
        loop.put("metrics", metrics);

        List<String> nextActions = new ArrayList<>();
        nextActions.add("继续补充画像问答，尤其是身体特征、发力习惯和特殊需求。");
        nextActions.add("对推荐结果做收藏、评论和对比，系统会据此更新排序权重。");
        nextActions.add("在训练后记录使用体验，下一轮推荐会更贴近真实手感。");
        loop.put("nextActions", nextActions);
        return loop;
    }

    private Map<String, Object> buildMetric(String label, String value) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("label", label);
        metric.put("value", value);
        return metric;
    }

    private Map<String, Object> parseAnswerSnapshot(UserPreferenceProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getAnswerSnapshot())) {
            return Collections.emptyMap();
        }
        try {
            return new LinkedHashMap<>(JSONUtil.parseObj(profile.getAnswerSnapshot()));
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private double calculateSnapshotPreferenceBonus(Map<String, Object> snapshot, String searchableText, Equipment equipment) {
        if (snapshot == null || snapshot.isEmpty()) {
            return 0.0;
        }
        double score = 0.0;
        String swingForceHabit = getSnapshotString(snapshot, "swingForceHabit");
        String heightRange = getSnapshotString(snapshot, "heightRange");
        List<String> specialNeeds = getSnapshotList(snapshot, "specialNeeds");

        if (StringUtils.hasText(swingForceHabit)) {
            if (("爆发型".equals(swingForceHabit) || "主动发力".equals(swingForceHabit))
                && containsAny(searchableText, "速度", "爆发", "进攻", "外置", "力量")) {
                score += 0.7;
            }
            if (("连贯借力".equals(swingForceHabit) || "中等均衡".equals(swingForceHabit))
                && containsAny(searchableText, "稳定", "容错", "持球", "均衡", "控制")) {
                score += 0.7;
            }
        }

        if (StringUtils.hasText(heightRange)) {
            if (containsAny(heightRange, "185", "高个") && containsAny(searchableText, "中远台", "支撑", "底劲")) {
                score += 0.4;
            }
            if (containsAny(heightRange, "170", "灵活") && containsAny(searchableText, "轻量", "快攻", "灵活")) {
                score += 0.4;
            }
        }

        if (!specialNeeds.isEmpty()) {
            if (specialNeeds.stream().anyMatch(item -> containsAny(item, "手腕", "轻量", "减负"))
                && containsAny(searchableText, "轻", "轻量", "减震", "柔和", "易控")) {
                score += 0.9;
            }
            if (specialNeeds.stream().anyMatch(item -> containsAny(item, "容错", "稳定"))
                && containsAny(searchableText, "稳定", "容错", "控制")) {
                score += 0.6;
            }
        }

        if ("shoe".equals(inferEquipmentRole(equipment)) && specialNeeds.stream().anyMatch(item -> containsAny(item, "膝", "脚", "移动"))) {
            score += 0.5;
        }
        return score;
    }

    private List<String> getSnapshotList(Map<String, Object> snapshot, String key) {
        if (snapshot == null || !snapshot.containsKey(key) || snapshot.get(key) == null) {
            return Collections.emptyList();
        }
        Object raw = snapshot.get(key);
        try {
            if (raw instanceof JSONArray) {
                return JSONUtil.toList((JSONArray) raw, String.class);
            }
            if (raw instanceof List) {
                return ((List<?>) raw).stream().map(this::valueAsString).filter(StringUtils::hasText).collect(Collectors.toList());
            }
            if (raw instanceof String) {
                return Collections.singletonList((String) raw);
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private String getSnapshotString(Map<String, Object> snapshot, String key) {
        if (snapshot == null || !snapshot.containsKey(key) || snapshot.get(key) == null) {
            return "";
        }
        return valueAsString(snapshot.get(key));
    }

    private String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Equipment findFirstByRole(List<Equipment> items, String role) {
        return items.stream()
            .filter(Objects::nonNull)
            .filter(item -> role.equals(inferEquipmentRole(item)))
            .findFirst()
            .orElse(null);
    }

    private Equipment findPreferredCandidate(List<Equipment> items, String role, UserPreferenceProfile profile) {
        return items.stream()
            .filter(Objects::nonNull)
            .filter(item -> role.equals(inferEquipmentRole(item)))
            .sorted((left, right) -> Double.compare(
                calculatePreferenceProfileScore(profile, right),
                calculatePreferenceProfileScore(profile, left)
            ))
            .findFirst()
            .orElse(findFirstByRole(items, role));
    }

    private Equipment findAny(List<Equipment> items) {
        return items.stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String inferEquipmentRole(Equipment equipment) {
        String text = String.join(" ",
            defaultText(equipment.getName()),
            defaultText(equipment.getDescription()),
            defaultText(equipment.getFeatures()),
            defaultText(equipment.getSuitableFor())
        );
        if (containsAny(text, "底板", "ALC", "viscaria", "carbonado")) {
            return "blade";
        }
        if (containsAny(text, "胶皮", "套胶", "反胶", "正胶", "长胶")) {
            return "rubber";
        }
        if (containsAny(text, "鞋", "shoe", "wave drive", "attack")) {
            return "shoe";
        }
        if (containsAny(text, "包", "护具", "服", "拍包", "配件", "袜")) {
            return "accessory";
        }
        return "equipment";
    }

    private String roleLabel(String role) {
        switch (role) {
            case "blade":
                return "底板";
            case "rubber":
                return "胶皮";
            case "shoe":
                return "球鞋";
            case "accessory":
                return "配件";
            default:
                return "装备";
        }
    }

    private boolean containsAny(String text, String... values) {
        if (!StringUtils.hasText(text) || values == null) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String value : values) {
            if (StringUtils.hasText(value) && lower.contains(value.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Equipment> getCachedList(String cacheKey) {
        try {
            return (List<Equipment>) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void cacheList(String cacheKey, List<Equipment> value, long ttl, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl, unit);
        } catch (Exception ignored) {
        }
    }
}
