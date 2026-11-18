# GPT 연동(MCP) 작업 안내

SRS 4.11(UC-19, FR-MCP-01~12)과 발표 자료 슬라이드 11·13 기준. 백엔드 담당: 홍승택.

## 1. 시연에서 보여 줄 것 (약 1분)

1. ChatGPT: "데이터 분야 공모전 팀원 모집 중인 공고 찾아줘" → `search_postings` → 101번 "2026 공공데이터 활용 공모전 데이터 분석 팀원 모집"을 포함한 결과
2. "101번 공고 추가 질문 보여 주고 지원 동기 초안 써 줘" → `get_posting` → GPT가 김민준 프로필(`get_my_profile`)을 보고 초안 작성
3. "그걸로 저장해 줘" → `save_application_draft` (쓰기 도구라 ChatGPT가 실행 확인을 한 번 묻는다) → "초안만 저장했어요. SKKU Crew에서 확인 후 제출하세요"
4. "지원서 제출까지 해 줘" → 제출 도구는 없으므로 웹 링크만 안내 (BR-14, FR-MCP-06) ← 개인정보 보호 설계를 보여 주는 장면
5. 이후 웹 화면에서 "초안 불러오기"로 이어짐 (프론트 시연)

## 2. 구조

발표 자료 슬라이드 13처럼 **이 Spring Boot 앱 안에 MCP 서버를 붙이는 방식**을 권장한다. 서비스 클래스를 그대로 호출하면 돼서 REST를 다시 감쌀 필요가 없다.

```
ChatGPT ──HTTPS(ngrok)──▶ :8080/mcp  (Spring AI MCP 서버, 같은 앱)
                                │
                                └─▶ PostingService / ApplicationService / BookmarkService ...
PWA ────HTTP──────────▶ :8080/api/**
```

인증: SRS는 OAuth 2.1(FR-MCP-02)이지만 데모는 **인증 없이** 김민준(회원 1) 고정으로 호출한다. ChatGPT의 커스텀 MCP 연결은 인증 방식으로 OAuth, 인증 없음, 혼합을 지원한다(OpenAI Developer mode 문서). 발표에서는 "데모라 OAuth는 생략, 실제 설계는 SCR-21 권한 동의"라고 말하면 된다.

## 3. 설정

`build.gradle` (이미 Spring Boot 3.5.7. Spring AI 1.1은 Boot 3.4·3.5 지원, 2.x는 쓰지 말 것):

```gradle
dependencies {
    implementation platform('org.springframework.ai:spring-ai-bom:1.1.x')   // Maven Central에서 1.1 최신 패치 확인해서 x 채우기
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
}
```

`src/main/resources/application.yml`에 추가:

```yaml
spring:
  ai:
    mcp:
      server:
        name: skku-crew
        version: 0.1.0
        type: SYNC
        protocol: STREAMABLE     # 엔드포인트 기본값 /mcp
        annotation-scanner:
          enabled: true
```

## 4. 도구 목록 (SRS 표 4-40 이름 그대로)

| 도구 | 입력 | 호출할 서비스 메서드 | readOnlyHint |
|---|---|---|---|
| `search_postings` | `keyword?`, `categories?[]`, `fields?[]` | `postingService.search(keyword, categories, fields, null, null, false, SortType.DEADLINE, 0, 10, demo)` | true |
| `get_posting` | `postingId` | `postingService.detail(postingId, demo)` (추가 질문 `questions` 포함) | true |
| `search_external_activities` | `keyword?`, `type?`, `fields?[]` | `externalActivityService.search(keyword, type, fields, null, SortType.DEADLINE, 0, 10, demo)` | true |
| `get_my_profile` | 없음 | `profileService.getMyProfile(demo)` | true |
| `list_my_applications` | 없음 | `applicationService.myApplications(demo)` | true |
| `list_my_postings` | 없음 | `postingService.mine(demo, null)` (지원자 수·미결정 수만, 개인정보 없음) | true |
| `save_bookmark` | `targetType`, `targetId`, `bookmarked` | `bookmarkService.add(...)` / `remove(...)` | false |
| `save_application_draft` | `postingId`, `answers[{questionId, value}]` | `applicationService.saveDraft(demo, postingId, new DraftRequest(answers, DraftSource.MCP))` | false |

지켜야 할 것:
- 지원서 제출, 공고 작성·마감, 선발 결정·발표, 프로필 수정은 **도구로 만들지 않는다** (BR-14, FR-MCP-06)
- 다른 사람의 지원서·연락처·메모를 돌려주는 도구 금지 (FR-MCP-05). 모집자용 지원서 조회 도구도 만들지 않는다
- 결과에 사람이 읽을 요약 + 데이터 + 웹 링크를 같이 (FR-MCP-12). 링크 경로(예: `/postings/101`, `/postings/101/apply`)는 PWA 담당과 맞출 것
- `save_application_draft`는 지원 가능한 모집중 공고에만 된다. 이미 지원했거나 마감이면 백엔드가 409를 던진다
- 김민준은 공고 101에 아직 지원 안 한 상태로 시작한다. 시연 리허설 후에는 `POST /api/dev/reset`

## 5. 코드 뼈대

빌드해 보지 않은 초안이다. `@McpTool`·`@McpToolParam`은 Spring AI 1.1 MCP 어노테이션이고, import 경로는 의존성 추가 후 IDE 자동 import로 맞출 것.

```java
package com.skkucrew.mcp;

import com.skkucrew.common.ApiException;
import com.skkucrew.domain.Codes.*;
import com.skkucrew.domain.Member;
import com.skkucrew.service.*;
import com.skkucrew.service.Dtos.*;
import com.skkucrew.store.DataStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SkkuCrewMcpTools {

    private final DataStore store;
    private final PostingService postingService;
    private final ApplicationService applicationService;
    private final ProfileService profileService;
    private final BookmarkService bookmarkService;
    private final ExternalActivityService externalActivityService;
    private final Long demoMemberId;
    private final String webBaseUrl;

    public SkkuCrewMcpTools(DataStore store, PostingService postingService, ApplicationService applicationService,
                            ProfileService profileService, BookmarkService bookmarkService,
                            ExternalActivityService externalActivityService,
                            @Value("${app.mcp-demo-member-id:1}") Long demoMemberId,
                            @Value("${app.web-base-url:http://localhost:5000}") String webBaseUrl) {
        this.store = store;
        this.postingService = postingService;
        this.applicationService = applicationService;
        this.profileService = profileService;
        this.bookmarkService = bookmarkService;
        this.externalActivityService = externalActivityService;
        this.demoMemberId = demoMemberId;
        this.webBaseUrl = webBaseUrl;
    }

    private Member demo() {
        return store.member(demoMemberId).orElseThrow();
    }

    @McpTool(name = "search_postings",
            description = "SKKU Crew에서 모집중인 교내 공고(프로젝트·연구·공모전·스터디·동아리)를 키워드·카테고리·분야로 검색한다.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true))
    public Map<String, Object> searchPostings(
            @McpToolParam(description = "검색어 (2자 이상)", required = false) String keyword,
            @McpToolParam(description = "카테고리: PROJECT, RESEARCH, CONTEST, STUDY, CLUB", required = false) List<Category> categories,
            @McpToolParam(description = "분야: DEV, AI, DATA, FINANCE, DESIGN, PLANNING_MARKETING, ETC", required = false) List<Field> fields) {
        var page = postingService.search(keyword, categories, fields, null, null, false, SortType.DEADLINE, 0, 10, demo());
        var items = page.items().stream().map(c -> new PostingHit(
                c.postingId(), c.title(), c.recruiter().name(), c.dDay(),
                webBaseUrl + "/postings/" + c.postingId())).toList();
        return result("모집중 공고 " + page.totalElements() + "건을 찾았어요.", items);
    }

    @McpTool(name = "save_application_draft",
            description = "공고의 추가 질문에 대한 답변을 초안으로 저장한다. 지원서를 제출하지 않는다. 사용자에게 내용을 확인받은 뒤 호출할 것.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = false, idempotentHint = true))
    public Map<String, Object> saveApplicationDraft(
            @McpToolParam(description = "공고 ID", required = true) Long postingId,
            @McpToolParam(description = "답변 목록. questionId는 get_posting의 questions[].id, 복수 선택은 value에 문자열 배열", required = true)
            List<AnswerInput> answers) {
        try {
            DraftResponse d = applicationService.saveDraft(demo(), postingId, new DraftRequest(answers, DraftSource.MCP));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("draft", d);
            data.put("link", webBaseUrl + "/postings/" + postingId + "/apply");
            return result(d.notice(), data);
        } catch (ApiException e) {
            return error(e);
        }
    }

    // get_posting, search_external_activities, get_my_profile, list_my_applications,
    // list_my_postings, save_bookmark 도 같은 방식으로 (4장 표 참고)

    /** 값이 null일 수 있어서 Map.of 대신 record 사용 */
    public record PostingHit(Long postingId, String title, String recruiter, Long dDay, String link) {}

    private Map<String, Object> result(String summary, Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", summary);
        m.put("data", data);
        return m;
    }

    private Map<String, Object> error(ApiException e) {
        // MCP 오류도 REST와 같은 오류 코드 체계 (SRS 3.4)
        return Map.of("error", Map.of("code", e.getCode(), "message", e.getMessage()));
    }
}
```

## 6. 연결·테스트 순서

1. 서버 실행 후 로컬에서 먼저 확인: `npx @modelcontextprotocol/inspector` → Transport `Streamable HTTP`, URL `http://localhost:8080/mcp` → 도구 목록·호출 확인
2. `ngrok http 8080` → `https://xxxx.ngrok-free.app`
3. ChatGPT 웹 → 설정에서 개발자 모드를 켜고 커스텀 커넥터(앱) 추가 → URL `https://xxxx.ngrok-free.app/mcp`, 인증 "없음". 메뉴 이름은 OpenAI 문서 참고: https://developers.openai.com/api/docs/guides/developer-mode
4. 새 대화에서 커넥터를 켜고 1장의 질문으로 리허설
5. 녹화 직전 `POST /api/dev/reset` (ngrok 주소로 불러도 됨)

주의:
- ngrok 무료 주소는 켤 때마다 바뀔 수 있다. 바뀌면 ChatGPT 커넥터 URL도 다시 등록
- ngrok 주소는 API 전체를 인터넷에 여는 것이다. 데모 토큰은 누구나 쓸 수 있으니 시연·리허설 때만 켤 것
- 개발자 모드를 쓸 수 있는 ChatGPT 요금제·계정 조건은 친구 계정에서 먼저 확인
