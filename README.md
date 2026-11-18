# SKKU Crew — 데모 백엔드

SWE3002-42 Team 7 최종 발표 시연용 백엔드. SRS의 핵심 흐름(공고 탐색 → 지원 → 검토·선발 → 결과 발표)만 남긴 "빈 깡통" 서버다.
DB 없이 `src/main/resources/seed/*.json` 더미 데이터를 메모리에 올려서 동작한다.

- Spring Boot 3.3.5 / Java 17 / Gradle
- API 명세: [`docs/API.md`](docs/API.md)
- Swagger UI: http://localhost:8080/swagger-ui.html (오른쪽 위 **Authorize**에 `demo-1` 입력 후 호출)

## 실행

**IntelliJ**
1. `File > Open` → 이 폴더(`build.gradle` 있는 곳) 열기
2. `Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM` 을 **JDK 17 이상**으로
3. `SkkuCrewApplication` 실행 → 로그에 `Seed loaded: members=10, profiles=7, postings=18, ...` 나오면 성공

**터미널**
```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

## 데모 계정

토큰은 `demo-{회원ID}`. 요청 헤더에 `Authorization: Bearer demo-1` 처럼 보낸다.

| ID | 이름 | 유형 | 시연 역할 |
|---|---|---|---|
| 1 | 김민준 | 개인 | **시연용 지원자** (소프트웨어학과 3학년, 복수 통계학과, 카카오톡 ID `minjun_k`, 전화번호 없음) |
| 4 | 박지호 | 개인 | **시연용 모집자** (통계학과, 공고 101 작성자) |
| 2 | 이서연 | 개인 | 공고 101 지원자 (소프트웨어학과 2학년), 공고 106 모집자 |
| 3 | 최유진 | 개인 | 공고 101 지원자 (디자인학과) |
| 5 | 정하늘 | 개인 | 모집자 (경영학과, 공고 118) |
| 6 | 데이터 분석 학회 D:LAB | 단체 | 공고 103(27기, 발표 완료, 지원자 24명)·104 등 |
| 7 | 로봇 학회 R:BOT | 단체 | 공고 105·111 |
| 8 | SKKU Crew 운영자 | 관리자 | |
| 9, 10, 11 | 한도윤, 윤서아, 강도윤 | 개인 | 기타 지원자·모집자 |
| 12~29 | | 개인 | D:LAB 27기 지원자 채우기용 |

## 시연 시나리오 (3~4분)

녹화 직전에 `POST /api/dev/reset` 한 번 호출해서 처음 상태로 돌린다.

| 순서 | 누가 | 화면 | API |
|---|---|---|---|
| 1 | 김민준(1) | 로그인 → 홈 맞춤 공고 | `POST /api/auth/demo-login`, `GET /api/postings/home` |
| 2 | 김민준 @ GPT | "데이터 분야 공모전 팀원 모집 공고 찾아줘" → 101번 추천 | MCP `search_postings` → J1 |
| 3 | 김민준 @ GPT | "101번 지원 동기 초안 써서 저장해 줘" → "SKKU Crew에서 확인 후 제출하세요" | MCP `get_posting` → J4, `save_application_draft` → S7 (`source: MCP`) |
| 4 | 김민준 | 웹에서 공고 101 → 지원하기 → **초안 불러오기**로 GPT 답변 채움 | `GET .../101/application-form` (`draft.exists=true`) |
| 5 | 김민준 | 공유 항목 선택(요청 연락처 카카오톡 ID는 고정·동의, 전화번호는 프로필에 없음) → 확인 → 제출 | `POST .../101/applications` |
| 6 | 박지호(4) | 종 아이콘 배지 → 새 지원서 알림 → 지원자 3명, 지원서 열람(연락처는 아직 안 보임) | `GET /api/notifications`, `GET .../101/applications`, `GET /api/applications/{id}/review` |
| 7 | 박지호 | 합격/불합격 결정 → 조기 마감 → 결과 발표 (모집 인원 2명) | `PATCH .../decision` ×3, `POST .../101/close`, `POST .../101/announce` |
| 8 | 김민준 | 종 아이콘 → 결과 발표 알림 → 합격 + 모집자 연락처 확인 | `GET /api/notifications`, `GET /api/applications/{id}` |

GPT 단계를 빼고 웹만 시연하려면 2·3을 건너뛰고 4에서 답변을 직접 입력하면 된다.

이미 만들어진 상태 보여주기용 데이터:
- 공고 102: 마감(CLOSED), 미정 지원서 1건 → "미정이 남으면 발표 불가"(409 `UNDECIDED_APPLICATIONS`) 확인용
- 공고 103: D:LAB 27기, 발표 완료(지원 24명, 합격 16·불합격 8, 모집 인원 20). 김민준 합격 → 프로필 활동 이력 "데이터 분석 학회 D:LAB"
- 공고 105·110·120: 마감 3일 이내라 홈 "마감 임박"에 노출 (105 R:BOT D-2)
- 공고 117: 임시저장(DRAFT), 목록에 안 나옴
- 공고 106: 김민준의 답변 초안(GPT 저장)이 이미 있음 → "초안 불러오기" 버튼 확인용
- 관심: 김민준이 공고 106·119(AI 해커톤)·120(정보처리기사 스터디), 외부 활동 2·5를 저장 → 카드에 북마크 표시
- 관심 조직: 김민준 → D:LAB (D:LAB이 104를 올려서 N-05 알림 받음)
- 알림: 김민준 안 읽음 2개(D:LAB 27기 결과 발표, 정보처리기사 스터디 마감 임박) → 발표 시연 후 3개. 박지호 안 읽음 2개

## 더미 데이터 수정

`src/main/resources/seed/` 의 JSON을 고치고 서버 재시작(또는 `POST /api/dev/reset`).
날짜는 서버 시작 기준 상대값이다: `deadlineDaysFromNow: 5` = 5일 뒤 마감, `createdDaysAgo`, `submittedDaysAgo`, `announcedDaysAgo` 도 같은 방식.

## 구조

```
src/main/java/com/skkucrew/
├── common/    오류 형식(SRS 표 3-29), 페이지 응답, CORS
├── domain/    Member(조직 정보 포함), Profile, Posting, Application, ApplicationDraft, Bookmark, Notification, ExternalActivity, 코드값(Codes)
├── store/     DataStore — 인메모리 저장소 + seed 로딩 (DB로 바꾸려면 여기만 교체)
├── service/   비즈니스 규칙 + 요청/응답 DTO(Dtos)
└── web/       REST 컨트롤러
```

SRS에서 살린 기능: 조직 페이지·관심 조직(FR-ORG-01, N-05), 지원자 목록 정렬·검색(FR-SEL-02), 활동 이력 공개 토글(FR-PROF-09), 기술 스택 자동완성(FR-PROF-04), 외부 활동 정보원 필터(FR-EXT-07), 관심 공고 마감 임박 알림(N-04).
SRS에서 살린 규칙: BR-01(개인회원 공고 카테고리 제한), BR-06(발표 전 결정 비공개), BR-08(제출 시 프로필 스냅샷), BR-09(요청 연락처 동의 필수), 마감 후에만 발표·미정 0건일 때만 발표, 발표 후 합격자 연락처 공개, 지원 중복 제출 방지(Idempotency-Key), 답변 초안(FR-APPL-14, 저장만으로는 미제출·제출/마감 시 삭제), 관심 저장(FR-SRCH-11, FR-EXT-11), 서비스 내 알림 N-01~N-03(FR-NOTI-01·06).
뺀 것: 실제 학교 Google 로그인·OAuth, 단체 인증 심사, 조직 정보 수정, 신고·관리자 기능, 웹 푸시(FCM), 공고 수정·삭제·복제, 질문 템플릿, 일괄 결정, 파일 업로드, 외부 활동 수집기, MySQL·Redis·S3(전부 메모리).

## GPT 연동 (MCP) 메모

ChatGPT는 로컬 서버(`localhost`)에 직접 못 붙는다. 공개 HTTPS 주소가 필요하다.

1. 이 서버 실행 후 터널: `ngrok http 8080` → `https://xxxx.ngrok-free.app`
2. MCP 서버를 붙이는 방법 둘 중 하나
   - **같은 Spring 앱에 추가**: Spring AI MCP Server(WebMVC) 스타터 + `@Tool` 메서드에서 `PostingService`, `ApplicationService` 를 그대로 호출. Boot 3.3 과 맞는 Spring AI 버전은 직접 확인할 것(2.x 는 Boot 4 기준일 수 있음)
   - **별도 MCP 서버**(Python FastMCP 등)에서 이 REST API를 호출 — 백엔드 코드를 안 건드려도 됨
3. ChatGPT 설정에서 Developer mode 켜고 커넥터에 MCP 주소(`https://.../mcp`) 등록
4. 시연은 인증 없이: MCP 쪽에서 고정 회원으로 호출 (`application.yml` 의 `app.mcp-demo-member-id: 1`, 토큰 `Bearer demo-1`)
5. 도구는 SRS 표 4-40 이름 그대로 만들면 된다. 백엔드 API 대응:

| MCP 도구 (표 4-40) | 호출할 API | 구분 |
|---|---|---|
| `search_postings` | `GET /api/postings?keyword=&category=&field=` | 읽기 |
| `get_posting` | `GET /api/postings/{id}` (추가 질문 `questions` 포함) | 읽기 |
| `search_external_activities` | `GET /api/external-activities` | 읽기 |
| `get_my_profile` | `GET /api/profiles/me` | 읽기 |
| `list_my_applications` | `GET /api/applications/me` | 읽기 |
| `list_my_postings` | `GET /api/postings/mine` (지원자 수·미결정 수만, 개인정보 없음) | 읽기 |
| `save_bookmark` | `POST /api/bookmarks` / `DELETE /api/bookmarks/{type}/{id}` | 쓰기 |
| `save_application_draft` | `PUT /api/postings/{id}/application-draft` (`source: "MCP"`) | 쓰기 |

   - 읽기 도구는 `readOnlyHint: true`, 쓰기 도구는 `readOnlyHint: false` (FR-MCP-12)
   - 지원서 제출·공고 마감·결과 발표는 도구로 만들지 말 것 (BR-14, FR-MCP-06). 요청받으면 웹 링크만 돌려준다
   - 결과에 웹 링크를 붙일 때는 PWA 주소 기준으로 `/postings/{id}` 같은 경로를 프론트 담당이랑 맞출 것

## 주의

- 데이터는 메모리에만 있어서 서버를 끄면 사라진다(시작할 때 seed로 다시 채워짐).
- 인증은 데모용이다. 토큰만 알면 누구로든 로그인된다. 공개 URL(ngrok)은 시연 때만 열어 둘 것.
