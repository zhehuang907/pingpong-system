package com.pingpong.enums;

import java.util.Arrays;
import java.util.Optional;

public enum LeaderboardType {

    WEEKLY_POPULAR("weekly_popular", "本周人气王", "基于近 7 天真实互动热度生成"),
    VALUE_KING("value_king", "性价比之王", "综合价格优势与真实口碑表现"),
    BEGINNER_PICK("beginner_pick", "新手入门首选", "面向入门阶段用户的稳定推荐"),
    PRO_FAVORITE("pro_favorite", "专业选手最爱", "偏向进阶对抗与专业场景的高分装备");

    private final String code;
    private final String title;
    private final String subtitle;

    LeaderboardType(String code, String title, String subtitle) {
        this.code = code;
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public static Optional<LeaderboardType> fromCode(String code) {
        return Arrays.stream(values())
            .filter(type -> type.code.equalsIgnoreCase(code))
            .findFirst();
    }
}
