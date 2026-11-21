package com.school.hotel.service;

public final class AiAnswerMode {
    private AiAnswerMode() {}
    public static String resolve(String requested, String message) {
        if ("quick".equals(requested) || "deep".equals(requested)) return requested;
        return message.length() > 100 || message.matches("(?s).*(对比|比较|权衡|方案|规划|代码|实现|分析|多步|优缺点|为什么|制定).*" ) ? "deep" : "quick";
    }
    public static String instruction(String mode) {
        return "deep".equals(mode)
            ? "复杂问题先拆解需求，给出可核验的步骤、方案对比与结论；代码问题给可落地代码和风险。不要输出内部思维链。需要房源数据时用 city 等过滤条件调用平台检索；需要比较多个区域时可返回 searches 数组（最多3组过滤条件）。"
            : "简洁直接作答，不展示推理过程，不主动扩展或调用检索。只有用户明确要求查房源时才返回过滤条件。";
    }
}
