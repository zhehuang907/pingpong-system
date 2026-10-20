package com.pingpong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingpong.config.DeepSeekProperties;
import com.pingpong.dto.*;
import com.pingpong.entity.*;
import com.pingpong.exception.BusinessException;
import com.pingpong.mapper.*;
import com.pingpong.service.AiChatService;
import com.pingpong.service.EquipmentService;
import com.pingpong.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final String DEFAULT_SCENE = "EQUIPMENT_ADVISOR";
    private static final String DEFAULT_TITLE = "新会话";
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Autowired
    private AiChatSessionMapper aiChatSessionMapper;

    @Autowired
    private AiChatMessageMapper aiChatMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserPreferenceProfileMapper userPreferenceProfileMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private DeepSeekProperties deepSeekProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<AiChatSessionSummaryDTO> listSessions(Long userId) {
        List<AiChatSession> sessions = aiChatSessionMapper.selectList(
            new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getLastMessageAt)
                .orderByDesc(AiChatSession::getUpdateTime)
                .orderByDesc(AiChatSession::getId)
        );
        return sessions.stream()
            .map(this::toSessionSummary)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AiChatSessionSummaryDTO createSession(Long userId, AiChatCreateSessionRequestDTO request) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(resolveSessionTitle(request == null ? null : request.getTitle(), null));
        session.setScene(DEFAULT_SCENE);
        session.setLastMessagePreview("");
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionMapper.insert(session);
        return toSessionSummary(session);
    }

    @Override
    public AiChatSessionDetailDTO getSessionDetail(Long userId, Long sessionId) {
        AiChatSession session = requireOwnedSession(userId, sessionId);
        List<AiChatMessageDTO> messages = aiChatMessageMapper.selectSessionMessages(sessionId).stream()
            .map(this::toMessageDTO)
            .collect(Collectors.toList());

        AiChatSessionDetailDTO detail = new AiChatSessionDetailDTO();
        detail.setSession(toSessionSummary(session));
        detail.setMessages(messages);
        return detail;
    }

    @Override
    @Transactional
    public AiChatSessionSummaryDTO updateSession(Long userId, Long sessionId, AiChatUpdateSessionRequestDTO request) {
        AiChatSession session = requireOwnedSession(userId, sessionId);
        String title = request == null ? null : request.getTitle();
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(400, "会话标题不能为空");
        }
        session.setTitle(resolveSessionTitle(title, null));
        aiChatSessionMapper.updateById(session);
        return toSessionSummary(session);
    }

    @Override
    @Transactional
    public boolean deleteSession(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        aiChatSessionMapper.deleteById(sessionId);
        aiChatMessageMapper.delete(new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getSessionId, sessionId));
        return true;
    }

    @Override
    @Transactional
    public AiChatSendMessageResponseDTO sendMessage(Long userId, AiChatSendMessageRequestDTO request) {
        String question = request == null ? null : normalizeContent(request.getContent());
        if (!StringUtils.hasText(question)) {
            throw new BusinessException(400, "请输入聊天内容");
        }

        AiChatSession session = request != null && request.getSessionId() != null
            ? requireOwnedSession(userId, request.getSessionId())
            : createSessionEntity(userId, null);

        AiChatMessage userMessage = buildUserMessage(session.getId(), userId, question, request == null ? null : request.getEquipmentId());
        aiChatMessageMapper.insert(userMessage);

        DeepSeekAnswer answer;
        try {
            List<Map<String, Object>> modelMessages = buildModelMessages(userId, session.getId(), request == null ? null : request.getEquipmentId(), question);
            answer = invokeDeepSeek(modelMessages);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("AI chat unexpected error for user {} session {}", userId, session.getId(), ex);
            answer = DeepSeekAnswer.failure("这一轮回复暂时中断了，请稍后再试。", "系统内部异常: " + ex.getMessage(), deepSeekProperties.getModel());
        }

        AiChatMessage assistantMessage = buildAssistantMessage(
            session.getId(),
            userId,
            answer,
            request == null ? null : request.getEquipmentId()
        );
        aiChatMessageMapper.insert(assistantMessage);

        refreshSessionAfterReply(session, question, assistantMessage);

        AiChatSendMessageResponseDTO response = new AiChatSendMessageResponseDTO();
        response.setSession(toSessionSummary(session));
        response.setUserMessage(toMessageDTO(userMessage));
        response.setAssistantMessage(toMessageDTO(assistantMessage));
        return response;
    }

    private AiChatSession createSessionEntity(Long userId, String title) {
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle(resolveSessionTitle(title, null));
        session.setScene(DEFAULT_SCENE);
        session.setLastMessagePreview("");
        session.setMessageCount(0);
        session.setLastMessageAt(LocalDateTime.now());
        aiChatSessionMapper.insert(session);
        return session;
    }

    private AiChatSession requireOwnedSession(Long userId, Long sessionId) {
        AiChatSession session = aiChatSessionMapper.selectOne(
            new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getId, sessionId)
                .eq(AiChatSession::getUserId, userId)
                .last("LIMIT 1")
        );
        if (session == null) {
            throw new BusinessException(404, "会话不存在或无权访问");
        }
        return session;
    }

    private AiChatMessage buildUserMessage(Long sessionId, Long userId, String content, Long equipmentId) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ROLE_USER);
        message.setContent(content);
        message.setStatus(STATUS_SUCCESS);
        message.setRelatedEquipmentIds(toEquipmentIdPayload(equipmentId));
        return message;
    }

    private AiChatMessage buildAssistantMessage(Long sessionId, Long userId, DeepSeekAnswer answer, Long equipmentId) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ROLE_ASSISTANT);
        message.setContent(answer.content);
        message.setStatus(answer.success ? STATUS_SUCCESS : STATUS_FAILED);
        message.setModel(answer.model);
        message.setFinishReason(answer.finishReason);
        message.setPromptTokens(answer.promptTokens);
        message.setCompletionTokens(answer.completionTokens);
        message.setTotalTokens(answer.totalTokens);
        message.setRelatedEquipmentIds(toEquipmentIdPayload(equipmentId));
        message.setErrorMessage(answer.errorMessage);
        return message;
    }

    private void refreshSessionAfterReply(AiChatSession session, String question, AiChatMessage assistantMessage) {
        int currentCount = session.getMessageCount() == null ? 0 : session.getMessageCount();
        String nextTitle = session.getTitle();
        if (!StringUtils.hasText(nextTitle) || DEFAULT_TITLE.equals(nextTitle)) {
            nextTitle = resolveSessionTitle(null, question);
        }
        session.setTitle(nextTitle);
        session.setMessageCount(currentCount + 2);
        session.setLastMessageAt(assistantMessage.getCreateTime() == null ? LocalDateTime.now() : assistantMessage.getCreateTime());
        session.setLastMessagePreview(abbreviate(assistantMessage.getContent(), 80));
        aiChatSessionMapper.updateById(session);
    }

    private List<Map<String, Object>> buildModelMessages(Long userId, Long sessionId, Long equipmentId, String question) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildMessage(ROLE_SYSTEM, buildSystemPrompt(userId, equipmentId, question)));

        int limit = defaultInt(deepSeekProperties.getContextMessageLimit(), 12);
        List<AiChatMessage> contextMessages = aiChatMessageMapper.selectRecentContextMessages(sessionId, limit);
        for (AiChatMessage message : contextMessages) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            if (!ROLE_USER.equals(message.getRole()) && !ROLE_ASSISTANT.equals(message.getRole())) {
                continue;
            }
            messages.add(buildMessage(message.getRole(), message.getContent()));
        }
        return messages;
    }

    private Map<String, Object> buildMessage(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildSystemPrompt(Long userId, Long equipmentId, String question) {
        User user = getUserSafely(userId);
        UserPreferenceProfile profile = getUserPreferenceProfileSafely(userId);
        UserProfileInsightDTO insight = getInsightSafely(userId);
        RecommendationWorkbenchDTO workbench = getWorkbenchSafely(userId);
        Equipment focusEquipment = getEquipmentSafely(equipmentId);
        int catalogLimit = Math.max(3, defaultInt(deepSeekProperties.getCatalogLimit(), 4));
        int historyLimit = Math.min(3, catalogLimit);
        List<Equipment> favorites = getFavoriteEquipments(userId, historyLimit);
        List<Equipment> recentHistory = getRecentHistoryEquipments(userId, historyLimit);
        List<Equipment> personalRecommendations = getPersonalRecommendations(userId, catalogLimit);
        List<Equipment> hotRecommendations = getHotRecommendations(catalogLimit);

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是“乒乓装备智能顾问”，服务于一个乒乓球装备推荐系统。");
        prompt.append("请始终使用简体中文回答，优先围绕底板、胶皮、球鞋、配件和训练场景给出建议。");
        prompt.append("如果问题涉及器材选择，请尽量结合用户画像、系统推荐和当前目录中的装备信息。");
        prompt.append("如果系统上下文里没有足够的目录数据，请明确说明“以下建议基于通用经验，不代表站内现货”。");
        prompt.append("回答时尽量包含：适合人群、推荐理由、可能取舍、预算建议和下一步行动。");
        prompt.append("不要编造不存在的站内价格、库存或型号。");

        if (user != null) {
            prompt.append("\n\n[用户基础信息]");
            prompt.append("\n昵称: ").append(safeText(user.getNickname(), user.getUsername()));
            prompt.append("\n水平: ").append(safeText(user.getLevel(), "未知"));
            prompt.append("\n打法: ").append(safeText(user.getPlayStyle(), "未知"));
        }

        if (profile != null) {
            prompt.append("\n\n[偏好画像]");
            prompt.append("\n预算区间: ").append(formatBudget(profile));
            prompt.append("\n握拍习惯: ").append(safeText(profile.getGripStyle(), "未填写"));
            prompt.append("\n主要目标: ").append(safeText(profile.getPrimaryGoal(), "未填写"));
            prompt.append("\n优先关注: ").append(safeText(profile.getPriorityFocus(), "未填写"));
            prompt.append("\n偏好分类: ").append(safeText(profile.getPreferredCategories(), "未填写"));
            prompt.append("\n偏好品牌: ").append(safeText(profile.getPreferredBrands(), "未填写"));
        }

        if (insight != null) {
            prompt.append("\n\n[画像洞察]");
            prompt.append("\n人格标题: ").append(safeText(insight.getPersonaTitle(), "未生成"));
            prompt.append("\n推荐模式: ").append(safeText(insight.getRecommendationMode(), "未生成"));
            prompt.append("\n核心标签: ").append(joinList(insight.getCoreTags()));
            prompt.append("\n改进动作: ").append(joinList(insight.getImprovementActions()));
        }

        if (workbench != null) {
            prompt.append("\n\n[推荐工作台]");
            prompt.append("\n摘要标题: ").append(safeText(workbench.getSummaryTitle(), "未生成"));
            prompt.append("\n摘要描述: ").append(safeText(workbench.getSummaryDescription(), "未生成"));
            prompt.append("\n画像关键词: ").append(joinList(workbench.getProfileKeywords()));
            prompt.append("\n教练建议: ").append(joinList(workbench.getCoachSuggestions()));
        }

        prompt.append("\n\n[最近收藏]");
        prompt.append("\n").append(formatEquipmentList(favorites));

        prompt.append("\n\n[最近浏览]");
        prompt.append("\n").append(formatEquipmentList(recentHistory));

        prompt.append("\n\n[个性化推荐候选]");
        prompt.append("\n").append(formatEquipmentList(personalRecommendations));

        prompt.append("\n\n[热门装备候选]");
        prompt.append("\n").append(formatEquipmentList(hotRecommendations));

        if (focusEquipment != null) {
            prompt.append("\n\n[当前聚焦装备]");
            prompt.append("\n").append(formatSingleEquipment(focusEquipment));
        }

        prompt.append("\n\n[当前用户问题]");
        prompt.append("\n").append(question);
        return prompt.toString();
    }

    private DeepSeekAnswer invokeDeepSeek(List<Map<String, Object>> messages) {
        if (!deepSeekProperties.isEnabled()) {
            return DeepSeekAnswer.failure(
                "智能顾问暂时休息中，请稍后再试。",
                "DeepSeek is disabled by configuration.",
                deepSeekProperties.getModel()
            );
        }
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            return DeepSeekAnswer.failure(
                "智能顾问还在准备中，请稍后再试。",
                "Missing DeepSeek API key.",
                deepSeekProperties.getModel()
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", deepSeekProperties.getModel());
        body.put("messages", messages);
        body.put("temperature", deepSeekProperties.getTemperature());
        body.put("stream", false);
        body.put("max_tokens", deepSeekProperties.getMaxOutputTokens());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(deepSeekProperties.getApiKey().trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = buildRestTemplate().exchange(
                buildDeepSeekUrl(),
                HttpMethod.POST,
                entity,
                String.class
            );
            return parseDeepSeekResponse(response.getBody());
        } catch (HttpStatusCodeException ex) {
            log.warn("DeepSeek HTTP error: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return DeepSeekAnswer.failure(
                "这一轮回复没有顺利完成，请稍后再试。",
                "HTTP " + ex.getStatusCode().value() + ": " + abbreviate(ex.getResponseBodyAsString(), 240),
                deepSeekProperties.getModel()
            );
        } catch (RestClientException ex) {
            log.warn("DeepSeek client error", ex);
            return DeepSeekAnswer.failure(
                "这一轮回复暂时没有发回来，请稍后再试。",
                ex.getMessage(),
                deepSeekProperties.getModel()
            );
        } catch (Exception ex) {
            log.warn("DeepSeek parse error", ex);
            return DeepSeekAnswer.failure(
                "这一轮回复暂时无法展示，请稍后再试。",
                ex.getMessage(),
                deepSeekProperties.getModel()
            );
        }
    }

    private DeepSeekAnswer parseDeepSeekResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) {
            return DeepSeekAnswer.failure(
                "这次对话没有生成有效回复，请换个问法继续。",
                abbreviate(body, 240),
                deepSeekProperties.getModel()
            );
        }

        JsonNode choice = choices.get(0);
        JsonNode messageNode = choice.path("message");
        String content = messageNode.path("content").asText("");
        if (!StringUtils.hasText(content)) {
            content = messageNode.path("reasoning_content").asText("");
        }
        if (!StringUtils.hasText(content)) {
            return DeepSeekAnswer.failure(
                "这次对话没有生成正文回复，请换个问法继续。",
                abbreviate(body, 240),
                deepSeekProperties.getModel()
            );
        }

        JsonNode usage = root.path("usage");
        DeepSeekAnswer answer = new DeepSeekAnswer();
        answer.success = true;
        answer.content = content.trim();
        answer.model = root.path("model").asText(deepSeekProperties.getModel());
        answer.finishReason = choice.path("finish_reason").asText("");
        answer.promptTokens = usage.path("prompt_tokens").isMissingNode() ? null : usage.path("prompt_tokens").asInt();
        answer.completionTokens = usage.path("completion_tokens").isMissingNode() ? null : usage.path("completion_tokens").asInt();
        answer.totalTokens = usage.path("total_tokens").isMissingNode() ? null : usage.path("total_tokens").asInt();
        return answer;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = defaultInt(deepSeekProperties.getTimeoutMillis(), 30000);
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private String buildDeepSeekUrl() {
        String baseUrl = deepSeekProperties.getBaseUrl();
        String chatPath = deepSeekProperties.getChatPath();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = chatPath.startsWith("/") ? chatPath : "/" + chatPath;
        return normalizedBase + normalizedPath;
    }

    private User getUserSafely(Long userId) {
        try {
            return userMapper.selectById(userId);
        } catch (Exception ex) {
            log.warn("Failed to load user {} for AI prompt", userId, ex);
            return null;
        }
    }

    private UserPreferenceProfile getUserPreferenceProfileSafely(Long userId) {
        try {
            return userPreferenceProfileMapper.selectOne(
                new LambdaQueryWrapper<UserPreferenceProfile>()
                    .eq(UserPreferenceProfile::getUserId, userId)
                    .last("LIMIT 1")
            );
        } catch (Exception ex) {
            log.warn("Failed to load preference profile for AI prompt user={}", userId, ex);
            return null;
        }
    }

    private UserProfileInsightDTO getInsightSafely(Long userId) {
        try {
            return recommendService.getUserProfileInsight(userId);
        } catch (Exception ex) {
            log.warn("Failed to build profile insight for AI prompt user={}", userId, ex);
            return null;
        }
    }

    private RecommendationWorkbenchDTO getWorkbenchSafely(Long userId) {
        try {
            return recommendService.getRecommendationWorkbench(userId);
        } catch (Exception ex) {
            log.warn("Failed to build recommendation workbench for AI prompt user={}", userId, ex);
            return null;
        }
    }

    private Equipment getEquipmentSafely(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        try {
            return equipmentService.getEquipmentDetail(equipmentId, false);
        } catch (Exception ex) {
            log.warn("Failed to load focus equipment {} for AI prompt", equipmentId, ex);
            return null;
        }
    }

    private List<Equipment> getFavoriteEquipments(Long userId, int limit) {
        try {
            List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, userId)
                    .orderByDesc(Favorite::getUpdateTime)
                    .last("LIMIT " + limit)
            );
            return loadEquipmentsByIds(favorites.stream().map(Favorite::getEquipmentId).collect(Collectors.toList()));
        } catch (Exception ex) {
            log.warn("Failed to load favorites for AI prompt user={}", userId, ex);
            return Collections.emptyList();
        }
    }

    private List<Equipment> getRecentHistoryEquipments(Long userId, int limit) {
        try {
            List<BrowseHistory> histories = browseHistoryMapper.selectRecentHistory(userId, limit);
            return loadEquipmentsByIds(histories.stream().map(BrowseHistory::getEquipmentId).collect(Collectors.toList()));
        } catch (Exception ex) {
            log.warn("Failed to load browse history for AI prompt user={}", userId, ex);
            return Collections.emptyList();
        }
    }

    private List<Equipment> getPersonalRecommendations(Long userId, int limit) {
        try {
            List<Equipment> list = recommendService.getPersonalRecommend(userId, limit);
            return list == null ? Collections.emptyList() : list.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Failed to load personal recommendations for AI prompt user={}", userId, ex);
            return Collections.emptyList();
        }
    }

    private List<Equipment> getHotRecommendations(int limit) {
        try {
            List<Equipment> list = recommendService.getHotRecommend(limit);
            return list == null ? Collections.emptyList() : list.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Failed to load hot recommendations for AI prompt", ex);
            return Collections.emptyList();
        }
    }

    private List<Equipment> loadEquipmentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> orderedIds = ids.stream()
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (orderedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Equipment> equipmentMap = equipmentService.listByIds(orderedIds).stream()
            .filter(item -> item.getStatus() != null && item.getStatus() == 1)
            .collect(Collectors.toMap(Equipment::getId, item -> item, (left, right) -> left));

        List<Equipment> result = new ArrayList<>();
        for (Long id : orderedIds) {
            Equipment equipment = equipmentMap.get(id);
            if (equipment != null) {
                result.add(equipment);
            }
        }
        return result;
    }

    private String formatBudget(UserPreferenceProfile profile) {
        if (profile == null) {
            return "未填写";
        }
        String min = profile.getBudgetMin() == null ? "" : profile.getBudgetMin().stripTrailingZeros().toPlainString();
        String max = profile.getBudgetMax() == null ? "" : profile.getBudgetMax().stripTrailingZeros().toPlainString();
        if (StringUtils.hasText(min) && StringUtils.hasText(max)) {
            return min + " - " + max + " 元";
        }
        if (StringUtils.hasText(max)) {
            return max + " 元以内";
        }
        if (StringUtils.hasText(min)) {
            return min + " 元以上";
        }
        return "未填写";
    }

    private String formatEquipmentList(List<Equipment> equipments) {
        if (equipments == null || equipments.isEmpty()) {
            return "暂无";
        }
        return equipments.stream()
            .limit(defaultInt(deepSeekProperties.getCatalogLimit(), 6))
            .map(this::formatSingleEquipment)
            .collect(Collectors.joining("\n"));
    }

    private String formatSingleEquipment(Equipment equipment) {
        List<String> parts = new ArrayList<>();
        parts.add("- " + safeText(equipment.getName(), "未命名装备"));
        parts.add("品牌=" + safeText(equipment.getBrand(), "未知"));
        parts.add("型号=" + safeText(equipment.getModel(), "未知"));
        parts.add("价格=" + (equipment.getPrice() == null ? "未知" : equipment.getPrice().stripTrailingZeros().toPlainString() + " 元"));
        parts.add("适合=" + safeText(equipment.getSuitableFor(), "未标注"));
        parts.add("特点=" + safeText(abbreviate(equipment.getFeatures(), 42), "未标注"));
        if (StringUtils.hasText(equipment.getRecommendReason())) {
            parts.add("推荐理由=" + abbreviate(equipment.getRecommendReason(), 56));
        }
        return String.join(" | ", parts);
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "暂无";
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .collect(Collectors.joining(" / "));
    }

    private String safeText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        return content.replace("\r\n", "\n").trim();
    }

    private String resolveSessionTitle(String preferredTitle, String firstQuestion) {
        if (StringUtils.hasText(preferredTitle)) {
            return abbreviate(preferredTitle.trim(), 24);
        }
        if (StringUtils.hasText(firstQuestion)) {
            return abbreviate(firstQuestion.trim(), 24);
        }
        return DEFAULT_TITLE;
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String toEquipmentIdPayload(Long equipmentId) {
        return equipmentId == null ? null : String.valueOf(equipmentId);
    }

    private int defaultInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private AiChatSessionSummaryDTO toSessionSummary(AiChatSession session) {
        AiChatSessionSummaryDTO dto = new AiChatSessionSummaryDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setScene(session.getScene());
        dto.setLastMessagePreview(session.getLastMessagePreview());
        dto.setMessageCount(session.getMessageCount());
        dto.setLastMessageAt(session.getLastMessageAt());
        dto.setCreateTime(session.getCreateTime());
        dto.setUpdateTime(session.getUpdateTime());
        return dto;
    }

    private AiChatMessageDTO toMessageDTO(AiChatMessage message) {
        AiChatMessageDTO dto = new AiChatMessageDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getSessionId());
        dto.setRole(message.getRole());
        dto.setContent(message.getContent());
        dto.setStatus(message.getStatus());
        dto.setModel(message.getModel());
        dto.setFinishReason(message.getFinishReason());
        dto.setPromptTokens(message.getPromptTokens());
        dto.setCompletionTokens(message.getCompletionTokens());
        dto.setTotalTokens(message.getTotalTokens());
        dto.setRelatedEquipmentIds(message.getRelatedEquipmentIds());
        dto.setErrorMessage(message.getErrorMessage());
        dto.setCreateTime(message.getCreateTime());
        return dto;
    }

    private static class DeepSeekAnswer {
        private boolean success;
        private String content;
        private String model;
        private String finishReason;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private String errorMessage;

        private static DeepSeekAnswer failure(String content, String errorMessage, String model) {
            DeepSeekAnswer answer = new DeepSeekAnswer();
            answer.success = false;
            answer.content = content;
            answer.errorMessage = errorMessage;
            answer.model = model;
            answer.finishReason = "error";
            return answer;
        }
    }
}
