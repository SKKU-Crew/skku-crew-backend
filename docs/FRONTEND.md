# 프론트(PWA) 작업 안내

백엔드 담당: 홍승택. API 상세는 [`API.md`](API.md), 실제 요청·응답은 서버 실행 후 http://localhost:8080/swagger-ui.html 에서 확인.

## 1. 백엔드 띄우기

1. JDK 17 이상 설치 (`java -version`으로 확인)
2. 레포 받아서 폴더에서 `.\gradlew.bat bootRun` (macOS·Linux: `./gradlew bootRun`)
3. 로그에 `Seed loaded: members=29, ... follows=4` → http://localhost:8080
4. 데이터는 메모리에만 있다. 서버를 다시 켜거나 `POST /api/dev/reset` 하면 처음 상태로 돌아간다

`JAVA_HOME is set to an invalid directory` 오류가 나면 PowerShell에서:
```powershell
$env:JAVA_HOME = Split-Path (Split-Path (Get-Command java).Source)
```

## 2. 공통 규칙

| 항목 | 내용 |
|---|---|
| 로그인 | 로그인 화면에서 `GET /api/auth/demo-accounts`로 계정 목록을 보여 주고, 고르면 `POST /api/auth/demo-login {memberId}` → `accessToken` 저장 |
| 토큰 | 이후 모든 요청 헤더에 `Authorization: Bearer demo-1` |
| 시간 | `2026-11-23T15:57:06+09:00` (오프셋 포함). Dart `DateTime.parse` 그대로 사용. D-day는 응답의 `dDay` 사용 |
| 오류 | `{ code, message, fieldErrors[{field, message}], timestamp }`. `message`는 사용자에게 그대로 보여 줘도 되는 한국어 문장. 입력 칸 옆 오류는 `fieldErrors` |
| 목록 | `{ items, page, size, totalElements, hasNext }`. 무한 스크롤은 `hasNext`가 true면 `page+1` |
| CORS | 모든 origin 허용, GET·POST·PUT·PATCH·DELETE 가능 |
| 클라이언트 생성 | `/v3/api-docs`는 OpenAPI 3.1. OpenAPI Generator로 Dart 클라이언트를 만들어도 되지만, 3.1 지원이 완전하지 않을 수 있으니 직접 모델을 만드는 게 빠를 수 있다 |

### 코드값 → 화면 표시

| 코드 | 표시 |
|---|---|
| Category | `PROJECT` 프로젝트, `RESEARCH` 연구, `CONTEST` 공모전, `STUDY` 스터디, `CLUB` 동아리·학회 |
| Field | `DEV` 개발, `AI` AI, `DATA` 데이터, `FINANCE` 금융, `DESIGN` 디자인, `PLANNING_MARKETING` 기획·마케팅, `ETC` 기타 |
| ActivityMode | `ONLINE` 온라인, `OFFLINE` 오프라인, `HYBRID` 온·오프라인 병행 |
| AcademicStatus | `GRADE_1`~`GRADE_4` 1~4학년, `GRADE_5_PLUS` 5학년 이상, `LEAVE` 휴학, `DEFERRED` 졸업유예, `MASTER` 석사, `PHD` 박사, `OTHER` 기타 |
| SkillLevel | `HIGH` 상, `MID` 중, `LOW` 하 |
| ExperienceType | `PROJECT` 프로젝트, `EXTRACURRICULAR` 대외활동, `AWARD` 수상, `CAREER` 경력, `ETC` 기타 |
| ExternalType | `CONTEST` 공모전·대회, `INTERN_JOB` 인턴·채용, `EDUCATION` 교육, `EXTRACURRICULAR` 대외활동, `RESEARCH_PROGRAM` 연구 프로그램, `ETC` 기타 |
| external `source` | `SKKU_NOTICE` 성대 공지, `PUBLIC_API` 공공기관 |
| DisplayStatus (지원자) | `SUBMITTED` 제출 완료, `UNDER_REVIEW` 검토 중, `ACCEPTED` 합격, `REJECTED` 불합격, `WITHDRAWN` 취소됨 |
| Decision (모집자) | `PENDING` 미정, `ACCEPTED` 합격, `REJECTED` 불합격 |
| ContactType | `SCHOOL_EMAIL` 학교 이메일, `PHONE` 전화번호, `KAKAO_ID` 카카오톡 ID, `PERSONAL_EMAIL` 개인 이메일 |

## 3. 화면별 API (발표 자료 화면 기준)

| 화면 | 호출 | 화면에서 쓸 값 |
|---|---|---|
| 로그인 | `GET /api/auth/demo-accounts`, `POST /api/auth/demo-login` | `description`을 계정 아래 설명으로 |
| 홈 | `GET /api/me`, `GET /api/postings/home`, `GET /api/notifications/unread-count` | 완성도 바: `me.profileCompleteness` / 맞춤 공고: `recommended` / 마감 임박: `closingSoon` / 종 배지: `unreadCount` |
| 홈 카테고리 아이콘 | `GET /api/postings?category=PROJECT` 등 | |
| 탐색 | `GET /api/postings?keyword=&category=&field=&mode=&recruiterType=&sort=` | 같은 항목 여러 값은 파라미터 반복(`category=CONTEST&category=STUDY`). 결과 건수: `totalElements` |
| 공고 카드 | (목록 응답의 PostingCard) | 모집자: 개인이면 `recruiter.name (department)`, 단체면 `name` + `verified`일 때 ✓ / `requestsContact`면 "연락처 요청" 표시 / 북마크 아이콘: `bookmarked` |
| 북마크 아이콘 | `POST /api/bookmarks {targetType:"POSTING", targetId}` / `DELETE /api/bookmarks/POSTING/{id}` | 개인회원만. 비로그인이면 로그인 유도 |
| 공고 상세 | `GET /api/postings/{id}` | 하단 버튼: `viewerAction` → `APPLY` 지원하기, `APPLIED` 지원 완료(비활성), `CLOSED` 모집 마감, `OWNER` 내 공고 관리, `NOT_ALLOWED` 지원 불가(단체·관리자), `LOGIN_REQUIRED` 로그인 |
| 지원서 작성 ① 공유 항목 | `GET /api/postings/{id}/application-form` | `fixedItems` = "항상 공유되는 정보"(잠금) / `selectableItems` 체크박스 초기값 = `defaultShare` / `contacts` 중 `requested=true`는 체크 고정 + "합격 시 공개", `hasValue=false`면 입력 칸 표시 |
| 지원서 작성 ② 추가 질문 | (같은 응답의 `questions`, `draft`) | `draft.exists`면 "초안 불러오기" 버튼 → `draft.answers[].value`로 채움. "임시저장"은 `PUT .../application-draft {answers, source:"WEB"}` |
| 지원서 작성 ③ 확인·제출 | `POST /api/postings/{id}/applications` | 헤더 `Idempotency-Key: <uuid>`(버튼 연타 방지). `agreeToProvide: true` 필수 |
| 모집자에게 보이는 화면 | 프론트에서 만든다 | ①에서 체크한 항목만 모아서 보여 주면 됨 (별도 API 없음) |
| 내 활동 > 지원 현황 | `GET /api/applications/me` | 상태 배지: `displayStatus` |
| 내 지원서·결과 | `GET /api/applications/{id}` | 발표 후 `result`: 합격이면 `message` + `recruiterContact` + `disclosedContacts` |
| 지원 취소 | `POST /api/applications/{id}/withdraw` | 마감 후면 409 `WITHDRAW_NOT_ALLOWED` |
| 내 활동 > 관심 | `GET /api/bookmarks` | `postings`, `externalActivities` 두 탭 |
| 내 프로필 | `GET /api/profiles/me` | 완성도: `completeness` / 활동 이력: `activityHistory` |
| 활동 이력 공개 토글 | `PATCH /api/profiles/me/activity-history/{applicationId} {visible}` | |
| 프로필 편집 | `PUT /api/profiles/me` (전체 교체) | 기존 항목은 `id` 그대로, 새 항목은 `id` 없이 보내면 서버가 붙인다. 기술 자동완성: `GET /api/skills?keyword=fl` |
| 공고 작성 | `POST /api/postings` | `publish:false`면 임시저장. "미리보기"는 프론트에서 |
| 내 공고 | `GET /api/postings/mine?status=` | 공고별 `counts`(총·미정·합격·불합격·취소) |
| 지원자 목록 | `GET /api/postings/{id}/applications?decision=&sort=NAME&keyword=` | "지원서 검토 (1/24)"의 이전·다음은 이 목록 순서로 |
| 지원서 검토 | `GET /api/applications/{id}/review` | `snapshot`(제출 당시 프로필), `answers`, `memo`, `decision`. 발표 전 `contacts`는 항상 null |
| 결정·메모 저장 | `PATCH /api/applications/{id}/decision {decision, memo}` | |
| 조기 마감 | `POST /api/postings/{id}/close` | 발표 전에 반드시 마감해야 함 |
| 결과 발표 | `GET /api/postings/mine`의 `counts`로 합격·불합격 수 표시 → `POST /api/postings/{id}/announce {acceptMessage, rejectMessage}` | 409 `UNDECIDED_APPLICATIONS`면 `message`("결정하지 않은 지원서가 N건 있습니다.") 그대로 / 응답 `warning`은 합격 인원이 모집 인원을 **넘을 때만** 온다 |
| 합격자 연락처 | `GET /api/postings/{id}/accepted-contacts` | 발표 후에만 |
| 외부 활동 | `GET /api/external-activities?type=&field=&source=&keyword=` | 탭 = `type`, "성대 공지" 필터 = `source=SKKU_NOTICE`, 카드 누르면 `url`을 새 탭으로 |
| 알림 | `GET /api/notifications`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all` | 누르면 `targetScreen`으로 이동: `SCR-06` 공고 상세(`postingId`), `SCR-08` 내 지원서(`applicationId`), `SCR-12` 지원자 목록(`postingId`) |
| 조직 페이지 | `GET /api/organizations/{orgId}` | 관심 조직 등록: `POST`/`DELETE /api/organizations/{orgId}/follow` |

## 4. 시연 녹화 흐름 (README와 같음)

녹화 직전 `POST /api/dev/reset`.

1. 김민준(1) 로그인 → 홈(종 배지 2, 맞춤 공고)
2. (GPT 파트) GPT로 공고 101 찾고 지원 동기 초안 저장
3. 웹에서 공고 101 → 지원하기 → **초안 불러오기** → 카카오톡 ID(요청 연락처) 확인 → 제출
4. 박지호(4)로 전환 → 종 배지 → 지원자 목록 3명 → 지원서 검토(연락처 숨김) → 합격 2·불합격 1 결정 → 조기 마감 → 결과 발표
5. 김민준으로 전환 → 종 배지 3 → 결과 알림 → 합격 + 모집자 연락처

계정 전환은 데모 로그인 화면으로 돌아가서 다른 계정을 고르면 된다. 모바일 화면 두 개를 나란히 띄워 녹화하면 전환 없이도 된다.

## 5. 화면에 넣지 말 것 (백엔드·SRS와 어긋남)

- 결과 발표 화면의 "공유한 연락처**(없으면 학교 이메일)**가 공개" 문구 → SRS는 학교 이메일을 대신 공개하지 않는다(BR-09). "합격자가 제공에 동의한 연락처만 공개됩니다"로
- 프로필 완성도는 화면에 숫자를 박지 말고 API 값 사용 (김민준은 90%. SRS 공식상 10% 단위만 나옴)
- 알림 "지원서 제출 완료"는 SRS 알림 목록에 없는 이벤트라 서버가 만들지 않는다
- "합격 인원이 모집 인원 이내입니다" 경고는 없다. 넘을 때만 경고
- 질문 템플릿 불러오기, 공고 복제, 공고 수정·삭제, 일괄 결정, 웹 푸시, 실제 Google 로그인, AI 연결 권한 동의(OAuth)는 백엔드에 없다. 화면이 필요하면 정적 UI로만

## 6. 배포·네트워크 주의

- 시연 녹화는 PWA와 백엔드를 **둘 다 로컬**에서 띄우는 게 가장 안전하다
- PWA를 https 주소에 배포하고 백엔드를 http://localhost로 부르면 브라우저가 막을 수 있다. 그때는 백엔드도 `ngrok http 8080`으로 https 주소를 만든다
- ngrok 무료 주소를 부를 때는 요청 헤더에 `ngrok-skip-browser-warning: true`를 넣어야 JSON이 온다
