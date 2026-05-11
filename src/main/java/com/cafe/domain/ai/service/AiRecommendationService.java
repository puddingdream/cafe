package com.cafe.domain.ai.service;

import com.cafe.common.error.AiErrorCode;
import com.cafe.common.error.AiException;
import com.cafe.domain.ai.dto.AiRecommendedDrink;
import com.cafe.domain.ai.dto.AiRecommendationResult;
import com.cafe.domain.ai.dto.CoffeeRecommendationRequest;
import com.cafe.domain.ai.dto.CoffeeRecommendationResponse;
import com.cafe.domain.menu.entity.Menu;
import com.cafe.domain.menu.enums.MenuCategory;
import com.cafe.domain.menu.enums.MenuStatus;
import com.cafe.domain.menu.repository.MenuRepository;
import com.cafe.domain.menu.support.PopularMenuRankingItem;
import com.cafe.domain.menu.support.PopularMenuRankingService;
import com.cafe.infrastructure.security.dto.LoginUserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {
    // AI 추천의 핵심 흐름은 "후보 축소 -> AI 선택 -> 서버 검증 -> 최종 응답 조립"이다.
    // AI가 최종 메뉴 정보를 직접 만들게 하지 않고 menuId만 고르게 해서, 없는 메뉴 추천을 서버에서 차단한다.

    // LLM에 보내는 후보 메뉴 수를 제한한다. 메뉴가 많아져도 토큰 비용과 응답 지연이 폭증하지 않게 하기 위한 상한이다.
    private static final int MAX_PROMPT_MENU_COUNT = 30;

    // 사용자에게 보여줄 추천 결과는 최대 3개로 제한한다.
    private static final int MAX_RECOMMENDATION_COUNT = 3;

    // AI 후보가 인기 카테고리 하나로만 채워지면 비슷한 메뉴를 반복 추천하게 된다.
    // 그래서 후보 목록 안에서 한 카테고리가 차지하는 수를 제한하고, 부족할 때만 제한을 풀어 채운다.
    private static final int MAX_MENU_COUNT_PER_CATEGORY_IN_PROMPT = 6;

    // AI가 JSON 응답에 넣어야 하는 answerType 값이다. 문자열 상수로 관리해 오타를 줄인다.
    private static final String ANSWER_RECOMMENDATION = "RECOMMENDATION";
    private static final String ANSWER_OUT_OF_SCOPE = "OUT_OF_SCOPE";

    // SPRING_AI_MODEL_CHAT=none이면 ChatClient.Builder 빈이 없을 수 있으므로 ObjectProvider로 선택 주입한다.
    // 이렇게 두면 OpenAI 키가 없는 로컬/테스트 환경에서도 앱 부팅이 깨지지 않는다.
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final MenuRepository menuRepository;
    private final PopularMenuRankingService popularMenuRankingService;
    private final JsonMapper objectMapper;

    public CoffeeRecommendationResponse recommendCoffee(
            CoffeeRecommendationRequest request,
            LoginUserInfoDto loginUser
    ) {
        // OpenAI 설정이 없으면 여기서 명확한 서비스 미설정 예외를 던진다.
        ChatClient chatClient = getChatClient();

        // Redis 인기 메뉴 read model을 우선 사용해 AI에 보낼 후보를 줄인다.
        // 주문 데이터가 아직 부족하면 최신 판매중 메뉴로 보강해 초기 서비스 상태에서도 추천이 가능하게 한다.
        List<Menu> menus = filterMenusByRequest(request, findPromptMenus());

        if (menus.isEmpty()) {
            throw new AiException(AiErrorCode.NO_RECOMMENDABLE_MENU);
        }

        // AI는 자유 문장 대신 JSON을 반환한다. 서버는 이 JSON을 검증한 뒤 실제 DB 메뉴 정보와 합친다.
        AiRecommendationResult result = callOpenAi(chatClient, request, loginUser, menus);
        if (ANSWER_OUT_OF_SCOPE.equalsIgnoreCase(result.answerType())) {
            return CoffeeRecommendationResponse.outOfScope(menus.size());
        }

        return buildVerifiedResponse(result, menus);
    }

    private ChatClient getChatClient() {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new AiException(AiErrorCode.AI_RECOMMENDATION_NOT_CONFIGURED);
        }
        return builder.build();
    }

    private List<Menu> findPromptMenus() {
        // 1차 후보: Redis ZSET에 쌓인 최근 7일 인기 메뉴 top 30.
        // 이 값은 주문 완료/취소 Kafka 이벤트로 갱신되는 read model이라 RDB 집계보다 가볍게 읽을 수 있다.
        List<Long> popularMenuIds = popularMenuRankingService.findTopMenus(MAX_PROMPT_MENU_COUNT)
                .stream()
                .map(PopularMenuRankingItem::menuId)
                .toList();

        // Redis에는 menuId와 점수만 있으므로, 실제 메뉴명/가격/설명은 DB에서 다시 조회한다.
        // 동시에 판매중지/삭제된 메뉴가 Redis에 남아 있을 수 있어 ACTIVE 상태만 남긴다.
        List<Menu> popularMenus = findActiveMenusByPopularOrder(popularMenuIds);

        // 인기 메뉴가 특정 카테고리로만 몰릴 수 있으므로 최신 판매중 메뉴도 함께 가져와 후보 다양성을 보강한다.
        // 카페 메뉴 수는 일반적으로 작고, 최종 프롬프트에는 최대 30개만 넣는다.
        List<Menu> recentMenus = menuRepository.findAllByStatusOrderByCreatedAtDesc(MenuStatus.ACTIVE);

        return selectPromptMenus(popularMenus, recentMenus);
    }

    private List<Menu> findActiveMenusByPopularOrder(List<Long> popularMenuIds) {
        if (popularMenuIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Menu> popularMenuById = menuRepository.findAllById(popularMenuIds)
                .stream()
                .filter(menu -> menu.getStatus() == MenuStatus.ACTIVE)
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        // findAllById는 입력 순서를 보장하지 않으므로, Redis ranking 순서대로 다시 정렬한다.
        return popularMenuIds.stream()
                .map(popularMenuById::get)
                .filter(Objects::nonNull)
                .limit(MAX_PROMPT_MENU_COUNT)
                .toList();
    }

    private List<Menu> selectPromptMenus(List<Menu> popularMenus, List<Menu> recentMenus) {
        // 인기 메뉴와 최신 메뉴를 합치되 순서는 유지한다. 인기 메뉴를 먼저 두어 주문 데이터 기반 추천 성격은 유지한다.
        List<Menu> candidateMenus = mergeUniqueMenus(popularMenus, recentMenus);

        List<Menu> selectedMenus = new ArrayList<>();
        Set<Long> selectedMenuIds = new LinkedHashSet<>();
        Map<MenuCategory, Integer> categoryCounts = new EnumMap<>(MenuCategory.class);

        // 1차로 카테고리별 대표 메뉴를 하나씩 먼저 넣는다.
        // 이렇게 해야 인기 메뉴가 전부 LATTE여도 COFFEE, TEA, DESSERT 같은 선택지가 AI 후보에 들어간다.
        for (Menu menu : candidateMenus) {
            if (!categoryCounts.containsKey(menu.getCategory())) {
                addPromptMenu(menu, selectedMenus, selectedMenuIds, categoryCounts);
            }
            if (selectedMenus.size() >= MAX_PROMPT_MENU_COUNT) {
                return List.copyOf(selectedMenus);
            }
        }

        // 2차로 카테고리별 상한을 지키며 채운다.
        // 한 카테고리가 후보 대부분을 차지하는 상황을 줄여 비슷한 추천 문장이 반복되는 문제를 완화한다.
        for (Menu menu : candidateMenus) {
            int currentCategoryCount = categoryCounts.getOrDefault(menu.getCategory(), 0);
            if (currentCategoryCount < MAX_MENU_COUNT_PER_CATEGORY_IN_PROMPT) {
                addPromptMenu(menu, selectedMenus, selectedMenuIds, categoryCounts);
            }
            if (selectedMenus.size() >= MAX_PROMPT_MENU_COUNT) {
                return List.copyOf(selectedMenus);
            }
        }

        // 메뉴 종류가 실제로 적은 매장이라면 카테고리 상한을 지키면 30개를 못 채울 수 있다.
        // 그때는 제한을 풀고 남은 후보를 채워 모델이 고를 수 있는 메뉴 수를 확보한다.
        for (Menu menu : candidateMenus) {
            addPromptMenu(menu, selectedMenus, selectedMenuIds, categoryCounts);
            if (selectedMenus.size() >= MAX_PROMPT_MENU_COUNT) {
                return List.copyOf(selectedMenus);
            }
        }

        return List.copyOf(selectedMenus);
    }

    private List<Menu> filterMenusByRequest(CoffeeRecommendationRequest request, List<Menu> menus) {
        // 프롬프트만으로 "시원한 메뉴인데 치즈케이크 추천" 같은 오류를 완전히 막기 어렵다.
        // 그래서 사용자의 명시 조건은 AI 호출 전에 서버에서 후보 메뉴 자체를 줄여 모델이 엉뚱한 선택을 하지 못하게 한다.
        return menus.stream()
                .filter(menu -> isWithinBudget(menu, request.maxPrice()))
                .filter(menu -> matchesTemperatureIntent(menu, request))
                .toList();
    }

    private boolean isWithinBudget(Menu menu, Integer maxPrice) {
        return maxPrice == null || menu.getPrice() <= maxPrice;
    }

    private boolean matchesTemperatureIntent(Menu menu, CoffeeRecommendationRequest request) {
        String requestText = getRequestText(request);
        boolean wantsDessert = containsAny(requestText, "디저트", "케이크", "쿠키", "빵", "마카롱", "간식");

        if (containsAny(requestText, "시원", "차가", "아이스", "ice", "iced", "cold", "더운", "더위", "여름")) {
            // 사용자가 시원한 메뉴를 원하지만 디저트를 직접 요청하지 않았다면 음료 계열만 남긴다.
            // 케이크 같은 상온/디저트 메뉴가 "달다"는 이유로 끼어드는 문제를 여기서 차단한다.
            if (menu.getCategory() == MenuCategory.DESSERT && !wantsDessert) {
                return false;
            }
            return isColdMenu(menu);
        }

        if (containsAny(requestText, "따뜻", "뜨거", "핫", "hot", "warm")) {
            if (menu.getCategory() == MenuCategory.DESSERT && !wantsDessert) {
                return false;
            }
            return isHotMenu(menu);
        }

        return true;
    }

    private boolean isColdMenu(Menu menu) {
        if (menu.getCategory() == MenuCategory.ADE || menu.getCategory() == MenuCategory.SMOOTHIE) {
            return true;
        }

        String menuText = getMenuText(menu);
        return containsAny(menuText, "아이스", "ice", "iced", "cold", "에이드", "ade", "스무디", "smoothie", "쉐이크", "shake", "프라페", "frappe", "빙수", "아이스크림", "ice cream");
    }

    private boolean isHotMenu(Menu menu) {
        if (menu.getCategory() == MenuCategory.COFFEE
                || menu.getCategory() == MenuCategory.LATTE
                || menu.getCategory() == MenuCategory.TEA
                || menu.getCategory() == MenuCategory.DECAFFEINATED) {
            return !isClearlyColdOnly(menu);
        }

        String menuText = getMenuText(menu);
        return containsAny(menuText, "따뜻", "hot", "warm");
    }

    private boolean isClearlyColdOnly(Menu menu) {
        return menu.getCategory() == MenuCategory.ADE
                || menu.getCategory() == MenuCategory.SMOOTHIE
                || containsAny(getMenuText(menu), "아이스", "ice", "iced", "cold", "에이드", "ade", "스무디", "smoothie");
    }

    private String getRequestText(CoffeeRecommendationRequest request) {
        return String.join(" ",
                defaultText(request.preference()),
                defaultText(request.temperaturePreference()),
                defaultText(request.caffeinePreference())
        );
    }

    private String getMenuText(Menu menu) {
        return String.join(" ",
                defaultText(menu.getName()),
                defaultText(menu.getDescription()),
                menu.getCategory().name(),
                menu.getCategory().getLabel()
        );
    }

    private boolean containsAny(String text, String... keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private List<Menu> mergeUniqueMenus(List<Menu> firstMenus, List<Menu> secondMenus) {
        List<Menu> mergedMenus = new ArrayList<>();
        Set<Long> mergedMenuIds = new LinkedHashSet<>();

        appendUniqueMenus(firstMenus, mergedMenus, mergedMenuIds);
        appendUniqueMenus(secondMenus, mergedMenus, mergedMenuIds);

        return mergedMenus;
    }

    private void appendUniqueMenus(List<Menu> sourceMenus, List<Menu> targetMenus, Set<Long> targetMenuIds) {
        for (Menu menu : sourceMenus) {
            if (targetMenuIds.add(menu.getId())) {
                targetMenus.add(menu);
            }
        }
    }

    private void addPromptMenu(
            Menu menu,
            List<Menu> selectedMenus,
            Set<Long> selectedMenuIds,
            Map<MenuCategory, Integer> categoryCounts
    ) {
        if (selectedMenus.size() >= MAX_PROMPT_MENU_COUNT || !selectedMenuIds.add(menu.getId())) {
            return;
        }

        selectedMenus.add(menu);
        categoryCounts.merge(menu.getCategory(), 1, Integer::sum);
    }

    private AiRecommendationResult callOpenAi(
            ChatClient chatClient,
            CoffeeRecommendationRequest request,
            LoginUserInfoDto loginUser,
            List<Menu> menus
    ) {
        try {
            // system prompt에는 역할/금지 범위/응답 형식 같은 변하지 않는 규칙을 둔다.
            // user prompt에는 매 요청마다 달라지는 사용자 취향과 후보 메뉴 목록을 넣는다.
            String content = chatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(buildUserPrompt(request, loginUser, menus))
                    .call()
                    .content();

            if (!StringUtils.hasText(content)) {
                throw new AiException(AiErrorCode.AI_RECOMMENDATION_FAILED);
            }
            return parseAiResponse(content);
        } catch (AiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorCode.AI_RECOMMENDATION_FAILED, exception);
        }
    }

    private String buildSystemPrompt() {
        // 모델이 카페 메뉴 추천 외 질문에 답하지 않도록 역할 범위를 좁힌다.
        // 그래도 모델 응답은 신뢰하지 않고, 아래 buildVerifiedResponse에서 menuId를 다시 검증한다.
        return """
                너는 카페 메뉴 추천 전용 도우미다.
                역할 범위는 "제공된 판매중 메뉴 추천"과 "제공된 메뉴 정보 설명"뿐이다.
                음료와 디저트를 포함해 후보 목록에 있는 실제 판매중 메뉴만 추천할 수 있다.
                RAG 문서, 개발, 인프라, 보안, 일반 지식, 글쓰기, 번역, 상담 등 카페 메뉴 추천과 무관한 요청에는 답하지 않는다.
                범위 밖 요청이면 answerType을 OUT_OF_SCOPE로 반환하고 recommendations는 빈 배열로 둔다.
                추천은 반드시 후보 목록의 menuId만 사용한다. 후보에 없는 메뉴명이나 menuId를 만들지 않는다.
                후보는 실제 DB에 있는 판매중 메뉴만 제공된다.
                최대 3개 메뉴만 추천한다.
                사용자가 특정 카테고리만 요구하지 않았다면 가능한 서로 다른 카테고리에서 추천한다.
                같은 카테고리 메뉴를 2개 이상 추천해야 한다면 맛, 상황, 가격, 페어링, 카페인 부담 등 서로 다른 차별점을 설명한다.
                각 추천의 reason, caffeineInfo, calorieInfo, servingTip은 서로 다른 문장과 관점으로 작성한다.
                "달콤한 맛", "얼음과 함께 즐기세요"처럼 같은 표현을 여러 메뉴에 반복하지 않는다.
                카페인/칼로리 정보는 DB에 정확한 수치가 없으므로 일반적인 메뉴 기준의 "추정"이라고 명시한다.
                디저트처럼 카페인이 거의 없는 메뉴는 카페인 정보에 "일반적으로 거의 없음 또는 낮음으로 추정"처럼 설명한다.
                반드시 JSON 객체만 반환한다. 마크다운, 코드블록, 추가 설명은 금지한다.
                """;
    }

    private String buildUserPrompt(
            CoffeeRecommendationRequest request,
            LoginUserInfoDto loginUser,
            List<Menu> menus
    ) {
        // 후보 메뉴 목록에는 menuId를 반드시 포함한다.
        // AI가 메뉴명을 직접 생성하는 것이 아니라 이 menuId 중에서만 선택하게 만들기 위해서다.
        return """
                사용자 ID: %d
                사용자 취향/상황: %s
                온도 취향: %s
                카페인 취향: %s
                최대 예산: %s

                판매중 메뉴 목록:
                %s

                응답 JSON 형식:
                {
                  "answerType": "RECOMMENDATION",
                  "message": "한 문장 추천 요약",
                  "recommendations": [
                    {
                      "menuId": 1,
                      "reason": "추천 이유",
                      "caffeineInfo": "카페인 정보. 정확 수치가 아니면 추정이라고 표시",
                      "calorieInfo": "칼로리 정보. 정확 수치가 아니면 추정이라고 표시",
                      "servingTip": "마시는 방식이나 페어링 팁"
                    }
                  ]
                }

                범위 밖 요청이면 아래 형식만 반환:
                {
                  "answerType": "OUT_OF_SCOPE",
                  "message": "카페 메뉴 추천과 메뉴 정보 안내만 도와드릴 수 있습니다.",
                  "recommendations": []
                }

                추천 작성 규칙:
                - 후보 목록은 서버가 사용자의 온도/예산 조건으로 이미 걸러서 제공한 메뉴다.
                - 조건을 만족하는 메뉴가 여러 카테고리에 있으면 서로 다른 카테고리를 우선 선택한다.
                - 3개 추천은 각각 다른 선택 이유를 가져야 한다. 예: 청량감, 고소함, 디저트 페어링, 카페인 부담, 가격, 포만감.
                - reason, caffeineInfo, calorieInfo, servingTip 문장을 복붙처럼 반복하지 않는다.
                - 같은 카테고리에서 여러 개를 고를 때는 메뉴 설명상 차이가 분명한 경우만 고른다.

                위 후보 menuId 안에서만 추천해줘.
                """.formatted(
                loginUser.id(),
                request.preference(),
                defaultText(request.temperaturePreference()),
                defaultText(request.caffeinePreference()),
                request.maxPrice() == null ? "상관없음" : request.maxPrice() + "원",
                formatMenus(menus)
        );
    }

    private String formatMenus(List<Menu> menus) {
        // 프롬프트에 필요한 최소 메뉴 정보만 넣는다.
        // 이미지 URL 같은 추천 판단에 필요 없는 값은 토큰 비용을 줄이기 위해 제외한다.
        StringBuilder builder = new StringBuilder();
        for (Menu menu : menus) {
            builder.append("- id=")
                    .append(menu.getId())
                    .append(" / 이름: ")
                    .append(menu.getName())
                    .append(" / 카테고리: ")
                    .append(menu.getCategory().getLabel())
                    .append(" / 가격: ")
                    .append(menu.getPrice())
                    .append("원 / 설명: ")
                    .append(menu.getDescription())
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String defaultText(String value) {
        // 선택 입력값이 비어 있으면 AI가 조건을 과도하게 해석하지 않도록 "상관없음"으로 명시한다.
        return StringUtils.hasText(value) ? value : "상관없음";
    }

    private AiRecommendationResult parseAiResponse(String content) {
        try {
            // 모델이 실수로 코드블록이나 앞뒤 설명을 붙일 수 있어 JSON 객체 부분만 잘라 파싱한다.
            // 파싱 실패는 외부 AI 응답이 계약을 지키지 못한 것으로 보고 도메인 예외로 변환한다.
            return objectMapper.readValue(extractJsonObject(content), AiRecommendationResult.class);
        } catch (Exception exception) {
            throw new AiException(AiErrorCode.AI_RECOMMENDATION_FAILED, exception);
        }
    }

    private String extractJsonObject(String content) {
        // "```json ... ```"처럼 감싸진 응답에서도 첫 '{'부터 마지막 '}'까지만 추출한다.
        // 완전한 JSON 객체가 없으면 추천 결과를 신뢰할 수 없으므로 실패 처리한다.
        String trimmed = content.trim();
        int startIndex = trimmed.indexOf('{');
        int endIndex = trimmed.lastIndexOf('}');
        if (startIndex < 0 || endIndex <= startIndex) {
            throw new AiException(AiErrorCode.AI_RECOMMENDATION_FAILED);
        }
        return trimmed.substring(startIndex, endIndex + 1);
    }

    private CoffeeRecommendationResponse buildVerifiedResponse(
            AiRecommendationResult result,
            List<Menu> menus
    ) {
        // OUT_OF_SCOPE가 아닌데 RECOMMENDATION도 아니면 계약 위반이다.
        if (!ANSWER_RECOMMENDATION.equalsIgnoreCase(result.answerType())) {
            throw new AiException(AiErrorCode.INVALID_AI_RECOMMENDATION);
        }

        // 검증 기준이 되는 후보 메뉴 map이다. 이 map에 없는 menuId는 최종 응답에서 제외된다.
        Map<Long, Menu> menuById = menus.stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));

        // AI 응답은 신뢰하지 않고 다음 조건을 모두 통과해야 한다.
        // 1. menuId가 null이 아님
        // 2. 후보 메뉴 목록에 실제 존재함
        // 3. 추천 이유/카페인/칼로리 설명이 비어 있지 않음
        List<CoffeeRecommendationResponse.RecommendedDrink> recommendations = safeRecommendations(result).stream()
                .filter(recommendation -> recommendation.menuId() != null)
                .filter(recommendation -> menuById.containsKey(recommendation.menuId()))
                .filter(this::hasDrinkInformation)
                .limit(MAX_RECOMMENDATION_COUNT)
                .map(recommendation -> CoffeeRecommendationResponse.RecommendedDrink.from(
                        menuById.get(recommendation.menuId()),
                        recommendation
                ))
                .toList();

        if (recommendations.isEmpty()) {
            // 모든 추천이 후보 검증에서 탈락하면 없는 메뉴 추천을 내보내지 않고 실패시킨다.
            throw new AiException(AiErrorCode.INVALID_AI_RECOMMENDATION);
        }

        String message = StringUtils.hasText(result.message())
                ? result.message().trim()
                : "취향에 맞는 음료 메뉴를 추천했습니다.";
        return CoffeeRecommendationResponse.of(message, recommendations, menus.size());
    }

    private List<AiRecommendedDrink> safeRecommendations(AiRecommendationResult result) {
        // recommendations 자체가 null이어도 스트림 처리에서 NPE가 나지 않게 빈 목록으로 다룬다.
        if (result.recommendations() == null) {
            return List.of();
        }
        return result.recommendations()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean hasDrinkInformation(AiRecommendedDrink recommendation) {
        // 메뉴 기본 정보는 DB에서 가져오지만, 추천 설명 필드는 AI가 채워야 할 최소 응답 품질로 본다.
        return StringUtils.hasText(recommendation.reason())
                && StringUtils.hasText(recommendation.caffeineInfo())
                && StringUtils.hasText(recommendation.calorieInfo());
    }
}
