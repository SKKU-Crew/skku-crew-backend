# SKKU Crew 데모 백엔드 API 명세

> 최종 발표 시연용 "빈 깡통" 백엔드. DB 대신 메모리에 더미 데이터(JSON)를 올려 두고 동작한다.
> 실제 요청·응답 스키마는 서버 실행 후 **http://localhost:8080/swagger-ui.html** 에서 직접 눌러 볼 수 있다 (OpenAPI JSON: `/v3/api-docs`).

## 0. 공통

| 항목 | 내용 |
|---|---|
| Base URL | `http://localhost:8080` (ngrok 쓰면 `https://xxxx.ngrok-free.app`) |
| 형식 | JSON, UTF-8 |
| 시간 | ISO-8601, 한국 시간 (`2026-11-23T18:00:00+09:00`) |
| 인증 | `POST /api/auth/demo-login` 으로 받은 토큰을 `Authorization: Bearer demo-1` 처럼 보냄 |
| CORS | 모든 origin 허용 (PWA 개발 서버에서 바로 호출 가능) |
| 목록 페이지 | `?page=0&size=20` → `{ items, page, size, totalElements, hasNext }` |

### 오류 응답 (SRS 표 3-29 형식)

```json
{
  "code": "REQUESTED_CONTACT_MISSING",
  "message": "요청 연락처를 입력하고 제공에 동의해 주세요.",
  "fieldErrors": [ { "field": "contacts.KAKAO_ID", "message": "연락처 값을 입력해 주세요." } ],
  "timestamp": "2026-11-23T18:00:00+09:00"
}
```

| HTTP | code | 상황 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 필수 입력 누락, 필수 질문 미답변, 정보 제공 미동의 |
| 401 | `UNAUTHORIZED` | 토큰 없음/잘못됨 |
| 403 | `FORBIDDEN` | 본인 것이 아님, 회원 유형상 불가 |
| 404 | `NOT_FOUND` | 없는 공고·지원서 |
| 409 | `ALREADY_APPLIED` | 이미 지원한 공고 |
| 409 | `OWN_POSTING` | 본인 공고에 지원 |
| 409 | `APPLICATION_CLOSED` | 마감된 공고에 지원 |
| 409 | `PROFILE_INCOMPLETE` | 프로필 필수 항목(주전공·학년·관심 분야) 미입력 |
| 409 | `REQUESTED_CONTACT_MISSING` | 모집자가 요청한 연락처의 값이 없거나 동의 안 함 |
| 409 | `WITHDRAW_NOT_ALLOWED` | 마감 후 지원 취소 |
| 409 | `INVALID_STATUS` | 모집중이 아닌 공고를 마감 |
| 409 | `POSTING_NOT_CLOSED` | 마감 전에 결과 발표 |
| 409 | `UNDECIDED_APPLICATIONS` | 미정(PENDING) 지원서가 남은 채로 발표 |
| 409 | `ALREADY_ANNOUNCED` | 발표 후 결정 변경·재발표 |
| 409 | `NOT_ANNOUNCED` | 발표 전 합격자 연락처 조회 |

## 1. 코드 값

| 이름 | 값 |
|---|---|
| MemberType | `PERSONAL` 개인, `ORGANIZATION` 단체, `ADMIN` 관리자 |
| Category | `PROJECT`, `RESEARCH`, `CONTEST`, `STUDY`, `CLUB` (개인회원은 PROJECT·CONTEST·STUDY만, BR-01) |
| Field | `DEV`, `AI`, `DATA`, `FINANCE`, `DESIGN`, `PLANNING_MARKETING`, `ETC` |
| ActivityMode | `ONLINE`, `OFFLINE`, `HYBRID` |
| AcademicStatus | `GRADE_1`~`GRADE_4`, `GRADE_5_PLUS`, `LEAVE`, `DEFERRED`, `MASTER`, `PHD`, `OTHER` |
| SkillLevel | `HIGH`, `MID`, `LOW` |
| ExperienceType | `PROJECT`, `EXTRACURRICULAR`, `AWARD`, `CAREER`, `ETC` |
| ContactType | `SCHOOL_EMAIL`, `PHONE`, `KAKAO_ID`, `PERSONAL_EMAIL` |
| PostingStatus | `DRAFT` 임시저장, `OPEN` 모집중, `CLOSED` 마감, `ANNOUNCED` 발표 완료, `HIDDEN` |
| QuestionType | `SHORT_TEXT`, `LONG_TEXT`, `SINGLE_CHOICE`, `MULTI_CHOICE` |
| Decision (모집자 내부) | `PENDING`, `ACCEPTED`, `REJECTED` |
| DisplayStatus (지원자 화면) | `SUBMITTED` 제출 완료, `UNDER_REVIEW` 검토 중(마감 후), `ACCEPTED`, `REJECTED`, `WITHDRAWN` |
| ViewerAction (공고 상세 하단 버튼) | `APPLY` 지원하기, `APPLIED` 지원 완료, `CLOSED` 모집 마감, `OWNER` 내 공고 관리, `NOT_ALLOWED`, `LOGIN_REQUIRED` |
| ExternalType | `CONTEST`, `INTERN_JOB`, `EDUCATION`, `EXTRACURRICULAR`, `RESEARCH_PROGRAM`, `ETC` |
| SortType | `LATEST` 최신순, `DEADLINE` 마감 임박순 |
| BookmarkTarget | `POSTING` 교내 공고, `EXTERNAL_ACTIVITY` 외부 활동 |
| NotificationType | `NEW_APPLICATION` N-01 새 지원서(모집자), `POSTING_CLOSED` N-02 모집 마감·발표 요청(모집자), `RESULT_ANNOUNCED` N-03 결과 발표(지원자), `BOOKMARK_CLOSING_SOON` N-04 관심 공고 마감 임박 |
| DraftSource | `WEB` 웹 임시저장, `MCP` GPT 초안 저장 도구 |

> 발표 전에는 지원자에게 결정이 보이지 않는다(BR-06). 지원자는 `displayStatus`만 받는다.

## 2. 엔드포인트 한눈에 보기

🔒 = 토큰 필요, 👤 = 개인회원만, 🏷 = 공고 작성자만

| # | Method | Path | 설명 |
|---|---|---|---|
| A1 | GET | `/api/auth/demo-accounts` | 로그인 화면용 데모 계정 목록 |
| A2 | POST | `/api/auth/demo-login` | `{ "memberId": 1 }` → `{ accessToken, member }` |
| A3 | GET 🔒 | `/api/me` | 내 계정 정보 + 프로필 완성도 |
| P1 | GET 🔒👤 | `/api/profiles/me` | 내 프로필 |
| P2 | PUT 🔒👤 | `/api/profiles/me` | 내 프로필 전체 교체 |
| J1 | GET | `/api/postings` | 공고 검색·목록 |
| J2 | GET | `/api/postings/home` | 홈: 맞춤 공고 + 마감 임박(3일 이내) |
| J3 | GET 🔒 | `/api/postings/mine?status=` | 내 공고 + 지원자 수 |
| J4 | GET | `/api/postings/{postingId}` | 공고 상세 (토큰 있으면 viewerAction 계산) |
| J5 | POST 🔒 | `/api/postings` | 공고 작성 (`publish:false`면 DRAFT) |
| J6 | POST 🔒🏷 | `/api/postings/{postingId}/close` | 조기 마감 OPEN → CLOSED |
| S1 | GET 🔒👤 | `/api/postings/{postingId}/application-form` | 지원서 작성 화면 데이터 |
| S2 | POST 🔒👤 | `/api/postings/{postingId}/applications` | 지원서 제출 (`Idempotency-Key` 헤더 권장) |
| S3 | GET 🔒👤 | `/api/applications/me` | 내 지원 현황 |
| S4 | GET 🔒👤 | `/api/applications/{applicationId}` | 내 지원서 상세·결과 |
| S5 | POST 🔒👤 | `/api/applications/{applicationId}/withdraw` | 지원 취소 (마감 전만) |
| S6 | GET 🔒👤 | `/api/postings/{postingId}/application-draft` | 답변 초안 조회 ("초안 불러오기") |
| S7 | PUT 🔒👤 | `/api/postings/{postingId}/application-draft` | 답변 초안 저장 (제출 아님) |
| S8 | DELETE 🔒👤 | `/api/postings/{postingId}/application-draft` | 답변 초안 삭제 |
| R1 | GET 🔒🏷 | `/api/postings/{postingId}/applications?decision=` | 지원자 목록 |
| R2 | GET 🔒🏷 | `/api/applications/{applicationId}/review` | 지원서 상세 (모집자용, 발표 전 연락처 null) |
| R3 | PATCH 🔒🏷 | `/api/applications/{applicationId}/decision` | `{ decision, memo }` 저장 |
| R4 | POST 🔒🏷 | `/api/postings/{postingId}/announce` | 결과 일괄 발표 |
| R5 | GET 🔒🏷 | `/api/postings/{postingId}/accepted-contacts` | 합격자 연락처 (발표 후) |
| E1 | GET | `/api/external-activities` | 외부 활동 목록 |
| B1 | GET 🔒👤 | `/api/bookmarks` | 내 관심 목록 (공고 + 외부 활동) |
| B2 | POST 🔒👤 | `/api/bookmarks` | 관심 저장 `{ targetType, targetId }` |
| B3 | DELETE 🔒👤 | `/api/bookmarks/{targetType}/{targetId}` | 관심 해제 |
| N1 | GET 🔒 | `/api/notifications?unreadOnly=` | 알림 목록 + 안 읽은 개수 |
| N2 | GET 🔒 | `/api/notifications/unread-count` | 안 읽은 알림 개수 (종 아이콘 배지) |
| N3 | POST 🔒 | `/api/notifications/{notificationId}/read` | 알림 읽음 |
| N4 | POST 🔒 | `/api/notifications/read-all` | 모두 읽음 |
| D1 | POST | `/api/dev/reset` | 더미 데이터 처음 상태로 되돌리기 |

## 3. 주요 요청·응답

### J1. 공고 검색 `GET /api/postings`

| 파라미터 | 예 | 설명 |
|---|---|---|
| keyword | `데이터` | 제목·설명·기술 스택 검색 |
| category | `category=CONTEST&category=STUDY` | 복수 = OR |
| field | `field=DATA` | 복수 = OR |
| mode | `mode=ONLINE` | 복수 = OR |
| recruiterType | `PERSONAL` / `ORGANIZATION` | |
| includeClosed | `false`(기본) | true면 마감 공고 포함 |
| sort | `LATEST`(기본) / `DEADLINE` | |
| page, size | `0`, `20` | |

서로 다른 항목끼리는 AND. 응답 `items[]`는 PostingCard:

```json
{
  "postingId": 101, "category": "CONTEST",
  "title": "2026 공공데이터 활용 공모전 데이터 분석 팀원 모집",
  "recruiter": { "type": "PERSONAL", "memberId": 4, "name": "박지훈", "department": "통계학과" },
  "fields": ["DATA", "AI"], "skills": ["Python", "SQL", "Pandas"],
  "capacity": 3, "deadline": "2026-11-28T18:00:00+09:00", "dDay": 5,
  "status": "OPEN", "requestsContact": true, "bookmarked": false
}
```

### J4. 공고 상세 `GET /api/postings/{id}`

PostingCard 필드 + `activityStart`, `activityEnd`, `activityMode`, `place`, `description`, `imageUrl`, `questions[]`, `requestedContacts[]`, `applicantCount`(작성자에게만), `viewerAction`, `createdAt`.

### J5. 공고 작성 `POST /api/postings`

```json
{
  "publish": true,
  "category": "PROJECT",
  "title": "캡스톤 프론트엔드 팀원 모집",
  "fields": ["DEV"],
  "capacity": 2,
  "deadline": "2026-12-01T23:59:00+09:00",
  "activityMode": "ONLINE",
  "skills": ["React"],
  "description": "주 1회 온라인 회의",
  "questions": [ { "type": "LONG_TEXT", "content": "지원 동기", "required": true, "maxLength": 500 } ],
  "requestedContacts": ["KAKAO_ID"]
}
```

응답은 201 + 공고 상세.

### S1. 지원서 작성 화면 `GET /api/postings/{id}/application-form`

```json
{
  "postingId": 101,
  "postingTitle": "2026 공공데이터 활용 공모전 데이터 분석 팀원 모집",
  "fixedItems": { "name": "김민준", "majorPrimary": "소프트웨어학과", "academicStatus": "GRADE_3", "fields": ["DEV", "DATA"] },
  "selectableItems": {
    "profileImage": { "available": false, "defaultShare": false },
    "bio": { "available": true, "defaultShare": true },
    "skills": { "available": true, "defaultShare": true },
    "majorsSecondary": { "available": true, "defaultShare": true },
    "experiences": [ { "id": 21, "title": "캠퍼스 중고거래 웹앱", "defaultShare": true }, { "id": 22, "title": "교내 해커톤 장려상", "defaultShare": true } ],
    "portfolioLinks": [ { "id": 31, "title": "GitHub", "defaultShare": true } ],
    "portfolioFiles": [ { "id": 41, "title": "portfolio_minjun.pdf", "defaultShare": false } ]
  },
  "contacts": [
    { "type": "SCHOOL_EMAIL", "requested": false, "hasValue": true, "maskedValue": "min***@g.skku.edu" },
    { "type": "PHONE", "requested": true, "hasValue": true, "maskedValue": "010-****-5678" },
    { "type": "KAKAO_ID", "requested": true, "hasValue": false, "maskedValue": null },
    { "type": "PERSONAL_EMAIL", "requested": false, "hasValue": false, "maskedValue": null }
  ],
  "questions": [ { "id": 1001, "order": 1, "type": "LONG_TEXT", "content": "지원 동기를 적어 주세요.", "required": true, "maxLength": 500 } ]
}
```

`requested=true && hasValue=false` 인 연락처는 지원서 화면에서 바로 입력받는다 (FR-APPL-17).

맨 끝에 `draft`(S6과 같은 형식)가 붙는다. `draft.exists=true`면 "초안 불러오기" 버튼을 보여 주고, 누르면 `draft.answers`로 답변 칸을 채운다.

### S2. 지원서 제출 `POST /api/postings/{id}/applications`

헤더: `Authorization: Bearer demo-1`, `Idempotency-Key: <uuid>` (같은 키로 다시 보내면 새로 만들지 않고 기존 결과를 돌려줌)

```json
{
  "share": {
    "bio": true, "skills": true, "majorsSecondary": true, "profileImage": false,
    "experienceIds": [21, 22], "portfolioLinkIds": [31], "portfolioFileIds": []
  },
  "contacts": [
    { "type": "PHONE", "consent": true },
    { "type": "KAKAO_ID", "consent": true, "value": "minjun_dev", "saveToProfile": true }
  ],
  "answers": [
    { "questionId": 1001, "value": "공공데이터로 백엔드 API를 만들어 본 경험이 있습니다." },
    { "questionId": 1002, "value": "가능" },
    { "questionId": 1003, "value": "https://github.com/example-minjun" }
  ],
  "agreeToProvide": true
}
```

- 요청 연락처는 `consent:true` 필수, 프로필에 값이 없으면 `value`도 필수 → 아니면 409 `REQUESTED_CONTACT_MISSING`
- 복수 선택 질문은 `value`에 문자열 배열
- 제출 시점의 프로필이 스냅샷으로 저장됨 (이후 프로필을 고쳐도 지원서는 그대로, BR-08)

응답 201: `{ "applicationId": 5001, "postingId": 101, "displayStatus": "SUBMITTED", "submittedAt": "..." }`

### S3. 내 지원 현황 `GET /api/applications/me`

```json
[ { "applicationId": 505,
    "posting": { "postingId": 103, "title": "캠퍼스 중고거래 웹앱 백엔드 개발자 모집", "category": "PROJECT",
                 "deadline": "...", "status": "ANNOUNCED", "recruiterName": "정하늘" },
    "displayStatus": "ACCEPTED", "submittedAt": "...", "announcedAt": "..." } ]
```

### S4. 내 지원서 상세 `GET /api/applications/{id}`

`snapshot`(제출 당시 공유한 프로필), `sharedItems`, `answers[]`, `result`.
`result`는 발표 후에만 채워진다:

```json
"result": {
  "result": "ACCEPTED",
  "message": "합격을 축하합니다! ...",
  "recruiterContact": { "type": "SCHOOL_EMAIL", "value": "haneul.jung@g.skku.edu" },
  "disclosedContacts": [ { "type": "PHONE", "value": "010-1234-5678", "disclosedAt": "..." } ]
}
```

### R1~R5. 모집자 검토·발표 흐름

1. `GET /api/postings/101/applications` → 지원자 목록 (`applicationId`, `name`, `department`, `topSkills`, `decision`, `hasMemo`, `withdrawn`)
2. `GET /api/applications/501/review` → 스냅샷 + 답변 (`contacts`는 발표 전 항상 `null`, `sharedContactTypes`로 어떤 연락처를 동의했는지만 보임)
3. `PATCH /api/applications/501/decision` ← `{ "decision": "ACCEPTED", "memo": "시각화 담당" }`
4. `POST /api/postings/101/close` → `{ "postingId": 101, "status": "CLOSED" }`
5. `POST /api/postings/101/announce` ← `{ "acceptMessage": "합격 축하해요! 카톡으로 연락드릴게요.", "rejectMessage": null }`
   → `{ "postingId": 101, "status": "ANNOUNCED", "accepted": 2, "rejected": 1, "announcedAt": "...", "warning": null }`
   - `rejectMessage`를 비우면 기본 불합격 문구가 들어감
   - 합격자에게는 SKKU Crew 활동 이력이 자동 추가됨
6. `GET /api/postings/101/accepted-contacts` → 합격자가 동의한 연락처만

### S6~S8. 답변 초안 `/api/postings/{id}/application-draft`

웹 임시저장과 GPT(MCP `save_application_draft`)가 같은 API를 쓴다. 회원·공고마다 1개이고 저장할 때마다 덮어쓴다.
**저장만으로는 지원이 제출되지 않는다.** 제출하면 초안은 지워지고, 공고가 마감돼도 지워진다 (FR-APPL-14).

PUT 요청:
```json
{
  "source": "MCP",
  "answers": [
    { "questionId": 1001, "value": "공공데이터로 지역 문제를 분석해 보고 싶어 지원합니다. ..." },
    { "questionId": 1002, "value": "가능" }
  ]
}
```

응답 (GET·PUT·DELETE 공통):
```json
{
  "postingId": 101,
  "postingTitle": "2026 공공데이터 활용 공모전 데이터 분석 팀원 모집",
  "exists": true,
  "answers": [
    { "questionId": 1001, "content": "지원 동기를 적어 주세요.", "value": "공공데이터로 ..." },
    { "questionId": 1002, "content": "주 1회 오프라인 회의(자연과학캠퍼스)에 참석할 수 있나요?", "value": "가능" },
    { "questionId": 1003, "content": "참고할 GitHub나 노션 주소가 있다면 적어 주세요.", "value": null }
  ],
  "source": "MCP",
  "updatedAt": "2026-11-18T15:30:00+09:00",
  "notice": "초안만 저장되었습니다. 아직 지원이 제출되지 않았으니 SKKU Crew에서 확인 후 제출하세요."
}
```

- 지원할 수 없는 공고(마감, 내 공고, 이미 지원함, 프로필 미완성)에 PUT 하면 409 (FR-MCP-07)
- 공고에 없는 `questionId`, `maxLength` 초과, 복수 선택인데 배열이 아님 → 400
- 초안이 없으면 `exists: false`, `answers: []`

### E1. 외부 활동 `GET /api/external-activities`

`keyword`, `type`, `field`(복수), `sort`(기본 `DEADLINE`), `page`, `size`. 마감 지난 항목은 빠진다.
더미 데이터는 모두 "(예시)" 표기된 가상 공고이고 `url`은 `https://example.com/...`.

```json
{ "id": 2, "type": "INTERN_JOB", "title": "겨울방학 데이터 분석 인턴 모집 (예시)", "organizer": "소프트웨어학과",
  "fields": ["DATA"], "source": "SKKU_NOTICE", "postedAt": "2026-11-15", "deadline": "2026-11-25",
  "dDay": 7, "url": "https://example.com/external/2", "bookmarked": true }
```

### B1~B3. 관심 저장

- `POST /api/bookmarks` ← `{ "targetType": "POSTING", "targetId": 101 }` → `{ "targetType": "POSTING", "targetId": 101, "bookmarked": true }` (이미 저장돼 있어도 200)
- `DELETE /api/bookmarks/POSTING/101` → `bookmarked: false`
- `GET /api/bookmarks` → `{ "postings": [PostingCard...], "externalActivities": [외부 활동...] }` 최근 저장순
- 로그인하고 J1·J2·J4·E1을 부르면 각 항목의 `bookmarked`가 채워진다 (비로그인이면 항상 false)

### N1~N4. 알림

```json
{
  "unreadCount": 2,
  "items": [
    { "id": 20005, "type": "NEW_APPLICATION", "title": "새 지원서가 도착했어요",
      "body": "'2026 공공데이터 활용 공모전 데이터 분석 팀원 모집'에 최유진님이 지원했어요.",
      "targetScreen": "SCR-12", "postingId": 101, "applicationId": 502, "read": false, "createdAt": "..." }
  ]
}
```

자동으로 생기는 알림:

| 언제 | 누구에게 | type | targetScreen |
|---|---|---|---|
| 지원서 제출 (S2) | 모집자 | `NEW_APPLICATION` | `SCR-12` 지원자 목록 |
| 조기 마감 (J6) 또는 마감 시각 도래 | 모집자 | `POSTING_CLOSED` | `SCR-12` |
| 결과 발표 (R4) | 취소하지 않은 모든 지원자 | `RESULT_ANNOUNCED` | `SCR-08` 내 지원 현황 |

웹 푸시(FCM)는 데모에서 뺐다. PWA는 `unread-count`를 화면 전환 때마다 부르면 된다.

## 4. 더미 데이터 (src/main/resources/seed/)

날짜는 **서버 시작 시각 기준 상대값**(`deadlineDaysFromNow` 등)이라 언제 녹화해도 D-day가 자연스럽다.

| 파일 | 내용 |
|---|---|
| `members.json` | 회원 10명 (개인 7, 단체 2, 관리자 1) |
| `profiles.json` | 개인회원 프로필 7개 |
| `postings.json` | 공고 18개 (101~118: OPEN 15, CLOSED 1, ANNOUNCED 1, DRAFT 1) |
| `applications.json` | 지원서 10개 (501~510) |
| `external-activities.json` | 외부 활동 10개 |
| `bookmarks.json` | 관심 저장 9개 (김민준: 공고 101·106·111, 외부 활동 2·5) |
| `notifications.json` | 알림 13개 (박지훈 안 읽음 2개, 김민준은 모두 읽음 → 발표 시연 때 새 알림이 생김) |
| `drafts.json` | 답변 초안 1개 (김민준 → 공고 106, GPT가 저장한 것으로 표시) |

수정하면 서버 재시작 또는 `POST /api/dev/reset` 으로 반영.
