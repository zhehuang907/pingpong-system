package com.pingpong.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pingpong.dto.KickoffKingOverviewDTO;
import com.pingpong.entity.Equipment;
import com.pingpong.entity.KickoffKingSeason;
import com.pingpong.entity.KickoffKingStage;
import com.pingpong.entity.KickoffKingStanding;
import com.pingpong.entity.UserPreferenceProfile;
import com.pingpong.enums.LeaderboardType;
import com.pingpong.mapper.EquipmentMapper;
import com.pingpong.mapper.KickoffKingSeasonMapper;
import com.pingpong.mapper.KickoffKingStageMapper;
import com.pingpong.mapper.KickoffKingStandingMapper;
import com.pingpong.mapper.UserPreferenceProfileMapper;
import com.pingpong.service.KickoffKingService;
import com.pingpong.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KickoffKingServiceImpl implements KickoffKingService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM.dd");

    @Autowired
    private KickoffKingSeasonMapper seasonMapper;

    @Autowired
    private KickoffKingStageMapper stageMapper;

    @Autowired
    private KickoffKingStandingMapper standingMapper;

    @Autowired
    private UserPreferenceProfileMapper userPreferenceProfileMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private LeaderboardService leaderboardService;

    @Override
    public KickoffKingOverviewDTO getOverview(Long userId) {
        KickoffKingSeason season = seasonMapper.selectOne(
            new LambdaQueryWrapper<KickoffKingSeason>()
                .orderByDesc(KickoffKingSeason::getUpdateTime)
                .last("LIMIT 1")
        );
        if (season == null) {
            return new KickoffKingOverviewDTO();
        }

        List<KickoffKingStage> stages = stageMapper.selectList(
            new LambdaQueryWrapper<KickoffKingStage>()
                .eq(KickoffKingStage::getSeasonId, season.getId())
                .orderByAsc(KickoffKingStage::getStageDate)
        );
        List<KickoffKingStanding> standings = standingMapper.selectList(
            new LambdaQueryWrapper<KickoffKingStanding>()
                .eq(KickoffKingStanding::getSeasonId, season.getId())
                .orderByAsc(KickoffKingStanding::getRankPosition)
                .last("LIMIT 8")
        );

        UserPreferenceProfile profile = loadProfile(userId);
        KickoffKingOverviewDTO overview = new KickoffKingOverviewDTO();
        overview.setSeason(toSeasonCard(season));
        overview.setStages(stages.stream().map(this::toStageCard).collect(Collectors.toList()));
        overview.setStandings(standings.stream().map(this::toStandingCard).collect(Collectors.toList()));
        overview.setFeaturedBoards(leaderboardService.getHomeLeaderboards(3));
        overview.setPersonalizedTrack(buildPersonalizedTrack(profile, season));
        overview.setFeaturedEquipments(loadFeaturedEquipments(overview.getPersonalizedTrack(), stages));
        return overview;
    }

    private UserPreferenceProfile loadProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        return userPreferenceProfileMapper.selectOne(
            new LambdaQueryWrapper<UserPreferenceProfile>()
                .eq(UserPreferenceProfile::getUserId, userId)
                .last("LIMIT 1")
        );
    }

    private KickoffKingOverviewDTO.SeasonCard toSeasonCard(KickoffKingSeason season) {
        KickoffKingOverviewDTO.SeasonCard card = new KickoffKingOverviewDTO.SeasonCard();
        card.setSeasonCode(season.getSeasonCode());
        card.setSeasonName(season.getSeasonName());
        card.setHeroTitle(season.getHeroTitle());
        card.setHeroSubtitle(season.getHeroSubtitle());
        card.setAnnouncement(season.getAnnouncement());
        card.setSeasonStatus(season.getSeasonStatus());
        card.setTotalStops(defaultInteger(season.getTotalStops()));
        card.setCompletedStops(defaultInteger(season.getCompletedStops()));
        card.setCurrentStopName(season.getCurrentStopName());
        int totalStops = Math.max(defaultInteger(season.getTotalStops()), 1);
        card.setProgressPercent(Math.min(100, defaultInteger(season.getCompletedStops()) * 100 / totalStops));
        card.setHighlightTags(parseTags(season.getHighlightTags()));
        return card;
    }

    private KickoffKingOverviewDTO.StageCard toStageCard(KickoffKingStage stage) {
        KickoffKingOverviewDTO.StageCard card = new KickoffKingOverviewDTO.StageCard();
        card.setId(stage.getId());
        card.setStageName(stage.getStageName());
        card.setCityName(stage.getCityName());
        card.setStageType(stage.getStageType());
        card.setStageStatus(stage.getStageStatus());
        card.setStageDate(stage.getStageDate() == null ? "" : stage.getStageDate().format(DATE_FORMATTER));
        card.setTheme(stage.getTheme());
        card.setFocusSkill(stage.getFocusSkill());
        card.setHighlightDesc(stage.getHighlightDesc());
        return card;
    }

    private KickoffKingOverviewDTO.StandingCard toStandingCard(KickoffKingStanding standing) {
        KickoffKingOverviewDTO.StandingCard card = new KickoffKingOverviewDTO.StandingCard();
        card.setRankPosition(defaultInteger(standing.getRankPosition()));
        card.setPlayerName(standing.getPlayerName());
        card.setAssociationName(standing.getAssociationName());
        card.setPoints(defaultInteger(standing.getPoints()));
        card.setStageWins(defaultInteger(standing.getStageWins()));
        card.setPodiumCount(defaultInteger(standing.getPodiumCount()));
        card.setServeStyle(standing.getServeStyle());
        card.setHotForm(standing.getHotForm());
        return card;
    }

    private KickoffKingOverviewDTO.PersonalizedTrack buildPersonalizedTrack(UserPreferenceProfile profile, KickoffKingSeason season) {
        KickoffKingOverviewDTO.PersonalizedTrack track = new KickoffKingOverviewDTO.PersonalizedTrack();
        if (profile == null) {
            track.setTitle("开球网积分赛事观察路线");
            track.setDescription("先看积分走势和分站主题，再回到榜单频道锁定更适合上手的装备方向，把观赛和选购放在同一条决策线上。");
            track.setRecommendedBoardType(LeaderboardType.BEGINNER_PICK.getCode());
            track.setRecommendedBoardName(LeaderboardType.BEGINNER_PICK.getTitle());
            track.setEquipmentKeyword("控制");
            track.setTags(Arrays.asList("先看积分走势", "再补装备认知", "完成画像问答"));
            return track;
        }

        String source = String.format(Locale.ROOT, "%s %s %s %s",
            safe(profile.getLevel()),
            safe(profile.getPlayStyle()),
            safe(profile.getPrimaryGoal()),
            safe(profile.getPriorityFocus()));

        if (containsAny(source, "高级", "专业", "对抗", "爆发", "进攻")) {
            track.setTitle("前三板压制型赛事路线");
            track.setDescription("你的画像更偏向进攻压迫和高强度对抗，建议优先关注争冠组选手的发接发节奏，再进入专业偏好榜单深挖装备。");
            track.setRecommendedBoardType(LeaderboardType.PRO_FAVORITE.getCode());
            track.setRecommendedBoardName(LeaderboardType.PRO_FAVORITE.getTitle());
            track.setEquipmentKeyword("进攻");
            track.setTags(Arrays.asList("争冠组观察", safe(season.getCurrentStopName()), "高强度对抗"));
            return track;
        }

        if (containsAny(source, "性价比", "预算", "入门", "稳定", "控制")) {
            track.setTitle("控场稳定型赛事路线");
            track.setDescription("你的画像更看重预算效率和容错率，建议先看分站里稳定控场型选手的路线，再回到性价比和入门榜单完成筛选。");
            track.setRecommendedBoardType(LeaderboardType.VALUE_KING.getCode());
            track.setRecommendedBoardName(LeaderboardType.VALUE_KING.getTitle());
            track.setEquipmentKeyword("控制");
            track.setTags(Arrays.asList("稳控发球", "预算友好", "快速成套"));
            return track;
        }

        track.setTitle("热度追踪型赛事路线");
        track.setDescription("你当前更适合先追分站热度，再结合近期讨论度更高的榜单和装备卡片完成选择，把开球网赛事热度转成行动入口。");
        track.setRecommendedBoardType(LeaderboardType.WEEKLY_POPULAR.getCode());
        track.setRecommendedBoardName(LeaderboardType.WEEKLY_POPULAR.getTitle());
        track.setEquipmentKeyword("旋转");
        track.setTags(Arrays.asList("赛事热度", "近期口碑", safe(season.getCurrentStopName())));
        return track;
    }

    private List<Equipment> loadFeaturedEquipments(KickoffKingOverviewDTO.PersonalizedTrack track, List<KickoffKingStage> stages) {
        Set<String> keywords = new LinkedHashSet<>();
        if (track != null && StringUtils.hasText(track.getEquipmentKeyword())) {
            keywords.add(track.getEquipmentKeyword().trim());
        }
        stages.stream()
            .map(KickoffKingStage::getFocusSkill)
            .filter(StringUtils::hasText)
            .limit(2)
            .forEach(keywords::add);
        if (keywords.isEmpty()) {
            keywords.addAll(Arrays.asList("旋转", "控制", "进攻"));
        }

        List<Equipment> result = new ArrayList<>();
        Set<Long> equipmentIds = new LinkedHashSet<>();
        for (String keyword : keywords) {
            List<Equipment> matches = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                    .eq(Equipment::getStatus, 1)
                    .and(wrapper -> wrapper
                        .like(Equipment::getName, keyword)
                        .or()
                        .like(Equipment::getDescription, keyword)
                        .or()
                        .like(Equipment::getFeatures, keyword)
                        .or()
                        .like(Equipment::getSuitableFor, keyword))
                    .orderByDesc(Equipment::getRating)
                    .orderByDesc(Equipment::getFavoriteCount)
                    .last("LIMIT 2")
            );
            for (Equipment equipment : matches) {
                if (equipment != null && equipmentIds.add(equipment.getId())) {
                    result.add(equipment);
                }
                if (result.size() >= 6) {
                    return result;
                }
            }
        }

        if (result.size() < 6) {
            List<Equipment> fallbacks = equipmentMapper.selectList(
                new LambdaQueryWrapper<Equipment>()
                    .eq(Equipment::getStatus, 1)
                    .orderByDesc(Equipment::getRating)
                    .orderByDesc(Equipment::getFavoriteCount)
                    .last("LIMIT 6")
            );
            for (Equipment equipment : fallbacks) {
                if (equipment != null && equipmentIds.add(equipment.getId())) {
                    result.add(equipment);
                }
                if (result.size() >= 6) {
                    break;
                }
            }
        }

        return result;
    }

    private List<String> parseTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return Collections.emptyList();
        }
        if (JSONUtil.isTypeJSONArray(rawTags)) {
            return JSONUtil.parseArray(rawTags).stream()
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        }
        return Arrays.stream(rawTags.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .collect(Collectors.toList());
    }

    private boolean containsAny(String source, String... values) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
