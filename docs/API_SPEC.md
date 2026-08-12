# ConnecThing 프론트엔드 API 명세

> 기준: 2026-08-12 현재 Controller, DTO, SecurityConfig, 전역 예외 처리 및 서비스 구현

프론트엔드와 코드 생성 AI가 타입과 API 클라이언트를 바로 구현할 수 있도록 작성한 명세다.
백엔드 코드가 변경되면 이 문서도 함께 갱신해야 한다.

## 1. 공통 연동 규칙

| 항목 | 값 |
|---|---|
| API prefix | `/api` |
| 인증 | 서버 세션 쿠키 |
| 세션 쿠키 | `SESSION`, 기본 만료 30분 |
| 날짜 | `YYYY-MM-DD` |
| 날짜·시간 | ISO-8601 문자열 |
| 기본 응답 | `application/json` |

프론트와 백엔드 origin이 다르면 모든 요청에 `credentials: "include"`를 적용한다.

```ts
await fetch(`${API_BASE_URL}/api/users/me`, { credentials: "include" });
```

### 1.1 역할과 접근 제어

| API 응답 값 | Spring Security 내부 authority | 의미 |
|---|---|---|
| `STUDENT` | `ROLE_STUDENT` | 학생 |
| `LOST_ITEM_STAFF` | `ROLE_LOST_ITEM_STAFF` | 분실물 보관소 담당자 |
| `FACILITY_STAFF` | `ROLE_FACILITY_STAFF` | 시설 담당자(현재 전용 API 없음) |
| `ADMIN` | `ROLE_ADMIN` | 관리자 |

인증이 없으면 `401`, 인증은 됐지만 역할이 부족하면 `403`이다. 담당자 API는 역할 검사
후에도 해당 보관소 담당자인지 서비스 계층에서 추가 검사한다.

### 1.2 CSRF

`GET`, `HEAD`, `OPTIONS` 이외의 요청에는 CSRF 토큰이 필요하다. 로그인도 `POST`이므로
먼저 토큰을 조회한다.

```ts
type CsrfResponse = { headerName: string; token: string };

const csrf = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
  credentials: "include",
}).then(r => r.json() as Promise<CsrfResponse>);

await fetch(`${API_BASE_URL}/api/auth/login`, {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    [csrf.headerName]: csrf.token,
  },
  body: JSON.stringify({ email, password }),
});
```

기본 헤더는 `X-CSRF-TOKEN`이지만 응답의 `headerName`을 사용한다. 로그인 성공 시 세션과
토큰이 교체될 수 있으므로 로그인 직후 토큰을 다시 조회한다.

### 1.3 multipart/form-data

`request`는 JSON 파트, `files`는 이미지 파일 목록이다. boundary가 자동 생성되도록
`Content-Type`을 직접 지정하지 않는다.

```ts
const body = new FormData();
body.append("request", new Blob([JSON.stringify(request)], {
  type: "application/json",
}));
files.forEach(file => body.append("files", file));

await fetch(url, {
  method: "POST",
  credentials: "include",
  headers: { [csrf.headerName]: csrf.token },
  body,
});
```

- 파일당 최대 `10MB`, 요청 전체 최대 `50MB`
- 각 도메인 첨부 최대 5개
- 분실물·소유권 요청 허용 형식: JPEG, PNG, GIF, WebP

### 1.4 성공 및 오류 응답

대부분의 응답은 `{ "data": ... }`지만 CSRF, 로그인, 인증 확인, 회원가입 응답은 wrapper가
없다. `204 No Content`에서는 `response.json()`을 호출하지 않는다.

```ts
type ApiError = {
  timestamp: string; // Instant
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: { field: string; message: string }[];
};
```

```json
{
  "timestamp": "2026-08-12T03:15:30.123Z",
  "status": 400,
  "code": "COMMON_VALIDATION_FAILED",
  "message": "요청 값 검증에 실패했습니다.",
  "path": "/api/auth/login",
  "fieldErrors": [{ "field": "email", "message": "올바른 이메일 형식이어야 합니다." }]
}
```

분기는 `message`가 아니라 안정적인 `code`를 기준으로 한다.

### 1.5 페이지네이션

```ts
type Page<T> = {
  content: T[]; page: number; size: number;
  totalElements: number; totalPages: number; hasNext: boolean;
};
type CursorSlice<T> = {
  content: T[]; nextCursor: string | null; hasNext: boolean;
};
```

페이지 번호는 0부터 시작한다. 커서는 해석·수정하지 않고 `hasNext`가 true일 때 받은
`nextCursor`를 다음 요청에 그대로 전달한다.

## 2. 전체 API 목록

| 영역 | Method | Path | 권한 | 성공 |
|---|---|---|---|---:|
| 인증 | GET | `/api/auth/csrf` | 공개 | 200 |
| 인증 | POST | `/api/auth/login` | 공개 | 200 |
| 인증 | POST | `/api/auth/email-verifications` | 공개 | 204 |
| 인증 | POST | `/api/auth/email-verifications/confirm` | 공개 | 200 |
| 인증 | POST | `/api/auth/signup` | 공개 | 201 |
| 인증 | POST | `/api/auth/logout` | 로그인 | 204 |
| 인증 | DELETE | `/api/auth/me` | 로그인 | 204 |
| 사용자 | GET | `/api/users/me` | 로그인 | 200 |
| 기준정보 | GET | `/api/locations` | 공개 | 200 |
| 기준정보 | GET | `/api/facility-categories` | 공개 | 200 |
| 기준정보 | GET | `/api/item-categories` | 공개 | 200 |
| 시설문의 | POST | `/api/facility-requests` | STUDENT | 201 |
| 시설문의 | GET | `/api/facility-requests` | 공개 | 200 |
| 시설문의 | GET | `/api/facility-requests/{id}` | 공개 | 200 |
| 시설문의 | PATCH | `/api/facility-requests/{id}` | STUDENT | 200 |
| 시설문의 | DELETE | `/api/facility-requests/{id}` | STUDENT | 204 |
| 관리자 문의 | GET | `/api/admin/facility-requests` | ADMIN | 200 |
| 관리자 문의 | GET | `/api/admin/facility-requests/{id}` | ADMIN | 200 |
| 관리자 문의 | PATCH | `/api/admin/facility-requests/{id}` | ADMIN | 200 |
| 분실물 | GET | `/api/stored-items` | 공개 | 200 |
| 분실물 | GET | `/api/stored-items/{id}` | 공개 | 200 |
| 분실물 | POST | `/api/lost-item` | LOST_ITEM_STAFF, ADMIN | 201 |
| 분실물 | PATCH | `/api/stored-items/{id}` | LOST_ITEM_STAFF, ADMIN | 200 |
| 분실물 | PATCH | `/api/stored-items/{id}/status` | LOST_ITEM_STAFF, ADMIN | 200 |
| 분실물 | DELETE | `/api/stored-items/{id}` | LOST_ITEM_STAFF, ADMIN | 204 |
| 소유권 | POST | `/api/stored-items/{id}/claims` | STUDENT | 201 |
| 소유권 | GET | `/api/stored-items/{id}/claims` | LOST_ITEM_STAFF, ADMIN | 200 |
| 소유권 | GET | `/api/item-claims/{id}` | LOST_ITEM_STAFF, ADMIN | 200 |
| 검색 | GET | `/api/search/suggestions` | 공개 | 200 |
| 검색 | GET | `/api/search/summary` | 공개 | 200 |
| 검색 | GET | `/api/search/lost-items` | 공개 | 200 |
| 검색 | GET | `/api/search/facility-requests` | 공개 | 200 |
| 최근 검색 | GET | `/api/recent-searches` | 로그인 | 200 |
| 최근 검색 | POST | `/api/recent-searches` | 로그인 | 200 |
| 최근 검색 | DELETE | `/api/recent-searches/{id}` | 로그인 | 204 |
| 최근 검색 | DELETE | `/api/recent-searches` | 로그인 | 204 |
| 파일 | GET | `/api/files/{id}` | 공개 | 200 |

로그아웃은 Controller가 아니라 Spring Security logout filter가 제공한다.

## 3. 인증 및 사용자 API

### 3.1 `GET /api/auth/csrf`

공개 API다. 응답 예시:

```json
{ "headerName": "X-CSRF-TOKEN", "token": "d24d8b37-..." }
```

### 3.2 `POST /api/auth/login`

| 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| `email` | string | O | 이메일 형식, 최대 255자 |
| `password` | string | O | 최대 100자 |

```json
{ "email": "student@mju.ac.kr", "password": "password123" }
```

```json
{ "userId": 1, "email": "student@mju.ac.kr", "roles": ["STUDENT"] }
```

오류: `AUTH_INVALID_CREDENTIALS`(401).

### 3.3 `POST /api/auth/email-verifications`

요청은 `{ "email": "student@mju.ac.kr" }`, 성공은 `204`다. `mju.ac.kr`만 허용한다.
오류: `AUTH_EMAIL_ALREADY_REGISTERED`(409),
`AUTH_EMAIL_VERIFICATION_RESEND_TOO_SOON`(429), `AUTH_EMAIL_SEND_FAILED`(502).

### 3.4 `POST /api/auth/email-verifications/confirm`

```json
{ "email": "student@mju.ac.kr", "code": "123456" }
```

```json
{
  "emailVerificationToken": "43-character-url-safe-token-value",
  "expiresAt": "2026-08-12T12:30:00"
}
```

코드는 숫자 6자리이며 토큰 기본 유효 시간은 30분이다. 오류:
`AUTH_EMAIL_VERIFICATION_NOT_FOUND`, `AUTH_EMAIL_VERIFICATION_EXPIRED`,
`AUTH_EMAIL_VERIFICATION_CODE_MISMATCH`, `AUTH_EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED`,
`AUTH_EMAIL_VERIFICATION_ALREADY_COMPLETED`.

### 3.5 `POST /api/auth/signup`

성공은 `201 Created`다.

| 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| `name` | string | O | 2~100자 |
| `studentNumber` | string | O | 숫자 8자리 |
| `email` | string | O | `mju.ac.kr`, 최대 255자 |
| `password` | string | O | 8~64자, 영문·숫자 포함 |
| `passwordConfirm` | string | O | password와 일치 |
| `emailVerificationToken` | string | O | URL-safe 43자 |

```json
{
  "name": "홍길동", "studentNumber": "60231234",
  "email": "student@mju.ac.kr", "password": "password123",
  "passwordConfirm": "password123",
  "emailVerificationToken": "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde"
}
```

```json
{
  "userId": 1, "email": "student@mju.ac.kr", "name": "홍길동",
  "studentNumber": "60231234", "roles": ["STUDENT"]
}
```

오류: `AUTH_EMAIL_ALREADY_REGISTERED`, `AUTH_STUDENT_NUMBER_ALREADY_REGISTERED`,
`AUTH_INVALID_EMAIL_VERIFICATION_TOKEN`, `AUTH_EMAIL_VERIFICATION_TOKEN_EXPIRED`,
`AUTH_EMAIL_VERIFICATION_TOKEN_CONSUMED`, `AUTH_EMAIL_VERIFICATION_EMAIL_MISMATCH`,
`AUTH_SIGNUP_CONFLICT`.

### 3.6 `POST /api/auth/logout`

로그인 및 CSRF가 필요하다. 성공은 `204`이며 세션 무효화, 인증 제거, 쿠키 삭제가 수행된다.

### 3.7 `DELETE /api/auth/me`

회원 탈퇴 후 전체 세션을 종료한다. 성공 `204`, 오류 `AUTH_ACCOUNT_NOT_FOUND`(404).

### 3.8 `GET /api/users/me`

```json
{
  "data": {
    "userId": 1, "name": "홍길동", "email": "student@mju.ac.kr",
    "studentNumber": "60231234",
    "department": { "departmentId": 10, "departmentName": "융합소프트웨어학부" },
    "roles": ["STUDENT"]
  }
}
```

`department`는 null일 수 있다. 오류: `USER_NOT_FOUND`(404).

## 4. 기준정보 API

### 4.1 `GET /api/locations`

```json
{ "data": [{ "locationId": 1, "locationCode": "S1350", "locationName": "명진당" }] }
```

### 4.2 `GET /api/facility-categories`

```json
{ "data": [{ "categoryId": 1, "categoryName": "시설 고장" }] }
```

### 4.3 `GET /api/item-categories`

```json
{ "data": [{ "categoryId": 1, "categoryName": "전자기기" }] }
```

## 5. 시설문의 API

상태: `WAITING`(대기), `IN_PROGRESS`(진행중), `COMPLETED`(완료),
`REJECTED`(반려), `CANCELED`(취소).

### 5.1 `POST /api/facility-requests`

STUDENT 전용 multipart API, 성공 `201`.

| request 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| `categoryId` | number | O | 양수, 활성 카테고리 |
| `locationId` | number | O | 양수, 활성 장소 |
| `title` | string | O | 1~200자 |
| `description` | string | O | 1~500자 |

```json
{
  "categoryId": 1, "locationId": 2,
  "title": "강의실 의자가 파손됐습니다",
  "description": "S1350 앞줄 의자 하나가 흔들립니다."
}
```

```json
{
  "data": {
    "facilityRequestId": 101, "requestStatus": "WAITING",
    "attachmentCount": 2, "createdAt": "2026-08-12T12:00:00"
  }
}
```

오류: `FACILITY_CATEGORY_NOT_FOUND`, `LOCATION_NOT_FOUND`, `FILE_LIMIT_EXCEEDED`,
`INVALID_FILE_TYPE`, `FILE_STORAGE_ERROR`.

### 5.2 `GET /api/facility-requests`

| Query | 타입 | 필수 | 기본값/제약 |
|---|---|---:|---|
| `categoryId`, `locationId` | number | X | 양수 |
| `status` | enum | X | 시설문의 상태 |
| `keyword` | string | X | 최대 100자 |
| `from`, `to` | date | X | `YYYY-MM-DD`, from <= to |
| `page` | number | X | 기본 0, 최소 0 |
| `size` | number | X | 기본 20, 1~100 |

```json
{
  "data": {
    "content": [{
      "facilityRequestId": 101, "title": "강의실 의자 파손",
      "categoryName": "시설 고장", "locationName": "명진당",
      "requestStatus": "WAITING", "requestStatusName": "대기",
      "thumbnailUrl": "/api/files/11", "createdAt": "2026-08-12T12:00:00"
    }],
    "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false
  }
}
```

`thumbnailUrl`은 null일 수 있다.

### 5.3 `GET /api/facility-requests/{facilityRequestId}`

공개 API이며 ID는 양수다. 로그인한 작성자에게만 수정/삭제 가능 값이 true다.

```json
{
  "data": {
    "facilityRequestId": 101, "title": "강의실 의자 파손",
    "description": "S1350 앞줄 의자 하나가 흔들립니다.",
    "category": { "categoryId": 1, "categoryName": "시설 고장" },
    "location": { "locationId": 2, "locationName": "명진당" },
    "requestStatus": "WAITING", "requestStatusName": "대기",
    "attachments": [{
      "fileId": 11, "originalFilename": "chair.jpg", "fileUrl": "/api/files/11"
    }],
    "editable": true, "deletable": true,
    "createdAt": "2026-08-12T12:00:00", "updatedAt": "2026-08-12T12:00:00"
  }
}
```

오류: `FACILITY_REQUEST_NOT_FOUND`(404).

### 5.4 `PATCH /api/facility-requests/{facilityRequestId}`

STUDENT 작성자 전용 multipart API. 대기 상태만 수정 가능하다.

| request 필드 | 타입 | 필수 | 의미 |
|---|---|---:|---|
| `categoryId`, `locationId` | number | X | 양수 |
| `title` | string | X | 1~200자 |
| `description` | string | X | 1~500자 |
| `keepFileIds` | number[] | X | 유지할 기존 첨부 ID |

`keepFileIds` 생략은 기존 파일 유지, 빈 배열은 기존 파일 전체 제거다. `files`는 유지 목록에
추가할 새 파일이다. 실제 변경은 하나 이상 필요하다.

```json
{
  "data": {
    "facilityRequestId": 101, "requestStatus": "WAITING",
    "attachmentCount": 1, "updatedAt": "2026-08-12T12:10:00"
  }
}
```

오류: `FACILITY_REQUEST_NOT_FOUND`, `FACILITY_REQUEST_ACCESS_DENIED`,
`FACILITY_REQUEST_NOT_EDITABLE`, `INVALID_REQUEST`, `INVALID_ATTACHMENT`, 기준정보·파일 오류.

### 5.5 `DELETE /api/facility-requests/{facilityRequestId}`

STUDENT 작성자 전용이며 대기 상태만 가능하다. 성공 `204`. 오류:
`FACILITY_REQUEST_NOT_FOUND`, `FACILITY_REQUEST_ACCESS_DENIED`,
`FACILITY_REQUEST_NOT_DELETABLE`.

## 6. 관리자 시설문의 API

### 6.1 `GET /api/admin/facility-requests`

ADMIN 전용 페이지 조회다.

| Query | 타입 | 필수 | 기본값/제약 |
|---|---|---:|---|
| `keyword` | string | X | 최대 100자 |
| `status` | enum | X | 시설문의 상태 |
| `categoryId`, `locationId` | number | X | 양수 |
| `from`, `to` | date | X | `YYYY-MM-DD`, from <= to |
| `page` | number | X | 기본 0 |
| `size` | number | X | 기본 20, 1~100 |

```json
{
  "data": {
    "content": [{
      "facilityRequestId": 101,
      "title": "강의실 의자 파손",
      "requester": { "userId": 1, "name": "홍길동", "studentNumber": "60231234" },
      "category": { "categoryId": 1, "categoryName": "시설 고장" },
      "location": { "locationId": 2, "locationCode": "S1350", "locationName": "명진당" },
      "requestStatus": "WAITING", "requestStatusName": "대기",
      "thumbnailUrl": "/api/files/11", "createdAt": "2026-08-12T12:00:00"
    }],
    "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "hasNext": false
  }
}
```

### 6.2 `GET /api/admin/facility-requests/{facilityRequestId}`

```json
{
  "data": {
    "facilityRequestId": 101,
    "title": "강의실 의자 파손",
    "description": "S1350 앞줄 의자 하나가 흔들립니다.",
    "requester": {
      "userId": 1, "name": "홍길동", "studentNumber": "60231234",
      "email": "student@mju.ac.kr"
    },
    "category": { "categoryId": 1, "categoryName": "시설 고장" },
    "location": { "locationId": 2, "locationCode": "S1350", "locationName": "명진당" },
    "requestStatus": "IN_PROGRESS", "requestStatusName": "진행중",
    "attachments": [],
    "adminResponses": [{
      "responseId": 31, "content": "시설팀에 전달했습니다.",
      "createdAt": "2026-08-12T12:30:00"
    }],
    "createdAt": "2026-08-12T12:00:00", "updatedAt": "2026-08-12T12:30:00"
  }
}
```

오류: `FACILITY_REQUEST_NOT_FOUND`(404).

### 6.3 `PATCH /api/admin/facility-requests/{facilityRequestId}`

`status` 또는 `adminResponse` 중 하나 이상 필요하다.

| 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| `status` | enum | 조건부 | 전환할 상태 |
| `adminResponse` | string | 조건부 | 공백만 입력 불가, 최대 2000자 |

```json
{ "status": "IN_PROGRESS", "adminResponse": "시설팀에 전달했습니다." }
```

```json
{
  "data": {
    "facilityRequestId": 101, "previousStatus": "WAITING",
    "requestStatus": "IN_PROGRESS", "requestStatusName": "진행중",
    "adminResponse": {
      "responseId": 31, "content": "시설팀에 전달했습니다.",
      "createdAt": "2026-08-12T12:30:00"
    },
    "updatedAt": "2026-08-12T12:30:00"
  }
}
```

답변 미등록 시 응답의 `adminResponse`는 null이다. 오류:
`FACILITY_REQUEST_UPDATE_REQUIRED`, `FACILITY_REQUEST_INVALID_STATUS_TRANSITION`,
`FACILITY_REQUEST_ALREADY_COMPLETED`, `FACILITY_REQUEST_NOT_FOUND`, `USER_NOT_FOUND`.

## 7. 분실물 API

| 상태 | 표시명 | 허용되는 다음 상태 |
|---|---|---|
| `STORED` | 보관중 | `IN_PROGRESS`, `COMPLETED` |
| `IN_PROGRESS` | 진행중 | `COMPLETED` |
| `COMPLETED` | 해결완료 | 없음 |

### 7.1 `GET /api/stored-items`

| Query | 타입 | 필수 | 기본값/제약 |
|---|---|---:|---|
| `categoryId`, `locationId` | number | X | 양수 |
| `status` | enum | X | 분실물 상태 |
| `from`, `to` | date | X | 습득일, from <= to |
| `cursor` | string | X | 최대 512자 |
| `size` | number | X | 기본 20, 1~50 |

```json
{
  "data": {
    "content": [{
      "storedItemId": 201, "itemName": "검은색 지갑",
      "description": "학생증이 들어 있는 지갑", "categoryName": "지갑",
      "foundLocationName": "명진당 1층", "foundDate": "2026-08-11",
      "publicStatus": "STORED", "publicStatusName": "보관중",
      "thumbnailUrl": "/api/files/21", "createdAt": "2026-08-12T09:00:00"
    }],
    "nextCursor": "opaque-cursor", "hasNext": true
  }
}
```

오류: `STORED_ITEM_INVALID_CURSOR`, `COMMON_VALIDATION_FAILED`.

### 7.2 `GET /api/stored-items/{storedItemId}`

```json
{
  "data": {
    "storedItemId": 201, "itemName": "검은색 지갑",
    "description": "학생증이 들어 있는 지갑",
    "category": { "categoryId": 2, "name": "지갑" },
    "foundLocation": { "locationId": 1, "name": "명진당 1층" },
    "foundDate": "2026-08-11", "publicStatus": "STORED",
    "publicStatusName": "보관중", "office": { "officeId": 3, "name": "학생지원팀" },
    "attachments": [{
      "fileId": 21, "originalFilename": "wallet.jpg", "fileUrl": "/api/files/21"
    }],
    "createdAt": "2026-08-12T09:00:00", "updatedAt": "2026-08-12T09:00:00"
  }
}
```

직접 입력한 장소라면 `foundLocation.locationId`가 null일 수 있다. 비공개 설명은 포함되지
않는다. 오류: `STORED_ITEM_NOT_FOUND`.

### 7.3 `POST /api/lost-item`

LOST_ITEM_STAFF 또는 ADMIN 전용 multipart API, 성공 `201`.

| request 필드 | 타입 | 필수 | 검증 |
|---|---|---:|---|
| `officeId` | number | O | 양수, 활성 보관소 |
| `categoryId` | number | O | 양수 |
| `foundLocationId` | number | 조건부 | 양수 |
| `foundLocationText` | string | 조건부 | 최대 255자 |
| `itemName` | string | O | 1~150자 |
| `description` | string | O | 공개 설명, 1~500자 |
| `privateDescription` | string | X | 비공개 확인 정보, 최대 2000자 |
| `foundDate` | date | O | `YYYY-MM-DD` |

`foundLocationId`와 `foundLocationText` 중 정확히 하나만 제공한다.

```json
{
  "officeId": 3, "categoryId": 2, "foundLocationId": 1,
  "itemName": "검은색 지갑", "description": "학생증이 들어 있는 지갑",
  "privateDescription": "내부 카드 이름 확인 필요", "foundDate": "2026-08-11"
}
```

```json
{
  "data": {
    "storedItemId": 201, "publicStatus": "STORED",
    "attachmentCount": 1, "createdAt": "2026-08-12T09:00:00"
  }
}
```

오류: `LOST_ITEM_OFFICE_NOT_FOUND`, `ITEM_CATEGORY_NOT_FOUND`, `LOCATION_NOT_FOUND`,
`STORED_ITEM_ACCESS_DENIED`, `STORED_ITEM_INVALID_FOUND_LOCATION`, 파일 관련 오류.

### 7.4 `PATCH /api/stored-items/{storedItemId}`

담당자 또는 관리자 전용 multipart API다. 등록 필드는 모두 선택 사항이고
`keepFileIds: number[]`가 추가된다.

- `request` 생략: 새 파일만 추가
- `keepFileIds` 생략: 기존 파일 유지, 빈 배열: 기존 파일 모두 제거
- `privateDescription` 생략: 유지, null/공백: 제거
- 장소 변경 시 `foundLocationId`, `foundLocationText` 중 하나만 전달

```json
{
  "description": "검은색 반지갑입니다.",
  "privateDescription": null,
  "keepFileIds": [21]
}
```

```json
{
  "data": {
    "storedItemId": 201, "publicStatus": "STORED",
    "attachmentCount": 2, "updatedAt": "2026-08-12T10:00:00"
  }
}
```

오류: `STORED_ITEM_NOT_FOUND`, `STORED_ITEM_ACCESS_DENIED`,
`STORED_ITEM_INVALID_REQUEST`, `STORED_ITEM_INVALID_FOUND_LOCATION`,
`STORED_ITEM_INVALID_ATTACHMENT`, `STORED_ITEM_VERSION_CONFLICT`, 기준정보·파일 오류.

### 7.5 `PATCH /api/stored-items/{storedItemId}/status`

```json
{ "status": "IN_PROGRESS", "changeReason": "소유권 요청 확인 중" }
```

`status`는 필수, `changeReason`은 선택이며 최대 1000자다.

```json
{
  "data": {
    "storedItemId": 201, "previousStatus": "STORED",
    "publicStatus": "IN_PROGRESS", "publicStatusName": "진행중",
    "changed": true, "changedAt": "2026-08-12T10:10:00"
  }
}
```

같은 상태 요청은 `changed: false`, 불가능한 전환은
`STORED_ITEM_INVALID_STATUS_TRANSITION`(409)이다. 그 밖의 오류:
`STORED_ITEM_NOT_FOUND`, `STORED_ITEM_ACCESS_DENIED`, `STORED_ITEM_VERSION_CONFLICT`.

### 7.6 `DELETE /api/stored-items/{storedItemId}`

`STORED` 상태이고 소유권 요청이 없어야 한다. 성공 `204`. 오류:
`STORED_ITEM_NOT_FOUND`, `STORED_ITEM_ACCESS_DENIED`, `STORED_ITEM_NOT_DELETABLE`,
`STORED_ITEM_HAS_CLAIMS`, `STORED_ITEM_VERSION_CONFLICT`.

## 8. 소유권 요청 API

상태: `WAITING`(대기), `IN_PROGRESS`(진행중), `APPROVED`(승인), `REJECTED`(반려),
`CLOSED_BY_OTHER_COLLECTION`(다른 소유자 인계로 종료).

### 8.1 `POST /api/stored-items/{storedItemId}/claims`

STUDENT 전용 multipart API, 성공 `201`. `request`는
`ownershipDescription`(필수, 공백 제외, 최대 500자)을 가진다.

```json
{ "ownershipDescription": "지갑 안쪽에 제 이름이 적힌 학생증이 있습니다." }
```

```json
{
  "data": {
    "itemClaimId": 301, "storedItemId": 201, "claimantName": "홍길동",
    "studentNumber": "60231234", "claimStatus": "WAITING",
    "attachmentCount": 1, "createdAt": "2026-08-12T11:00:00"
  }
}
```

등록 시 보관 중인 분실물은 `IN_PROGRESS`로 전환될 수 있다. 오류:
`STORED_ITEM_NOT_FOUND`, `USER_NOT_FOUND`, `ITEM_CLAIM_NOT_CLAIMABLE`,
`ITEM_CLAIM_DUPLICATE_ACTIVE_CLAIM`, 소유권 요청 파일 오류.

### 8.2 `GET /api/stored-items/{storedItemId}/claims`

| Query | 타입 | 필수 | 기본값/제약 |
|---|---|---:|---|
| `status` | enum | X | 소유권 요청 상태 |
| `cursor` | string | X | 최대 512자 |
| `size` | number | X | 기본 20, 1~50 |

```json
{
  "data": {
    "content": [{
      "itemClaimId": 301, "claimantName": "홍길동", "studentNumber": "60231234",
      "requestMethod": "ONLINE", "claimStatus": "WAITING", "claimStatusName": "대기",
      "thumbnailUrl": "/api/files/31", "attachmentCount": 1,
      "createdAt": "2026-08-12T11:00:00"
    }],
    "nextCursor": null, "hasNext": false
  }
}
```

오류: `STORED_ITEM_NOT_FOUND`, `ITEM_CLAIM_ACCESS_DENIED`, `ITEM_CLAIM_INVALID_CURSOR`.

### 8.3 `GET /api/item-claims/{itemClaimId}`

```json
{
  "data": {
    "itemClaimId": 301, "storedItemId": 201, "claimantName": "홍길동",
    "studentNumber": "60231234", "requestMethod": "ONLINE",
    "ownershipDescription": "지갑 안쪽에 제 학생증이 있습니다.",
    "claimStatus": "WAITING", "claimStatusName": "대기",
    "attachments": [{
      "fileId": 31, "originalFilename": "proof.jpg", "fileUrl": "/api/files/31"
    }],
    "statusHistories": [{
      "claimStatusHistoryId": 1, "previousStatus": null, "previousStatusName": null,
      "newStatus": "WAITING", "newStatusName": "대기", "changedByName": "홍길동",
      "changeReason": null, "changedAt": "2026-08-12T11:00:00"
    }],
    "createdAt": "2026-08-12T11:00:00", "updatedAt": "2026-08-12T11:00:00"
  }
}
```

오류: `ITEM_CLAIM_NOT_FOUND`, `ITEM_CLAIM_ACCESS_DENIED`.

## 9. 통합 검색 API

### 9.1 `GET /api/search/suggestions`

| Query | 필수 | 제약 |
|---|---:|---|
| `query` | O | 공백 제외, 최대 100자 |
| `size` | X | 기본 5, 1~20 |

```json
{
  "data": {
    "lostItemSuggestions": ["검은색 지갑", "검은색 우산"],
    "facilityRequestSuggestions": ["검은색 곰팡이 제거 요청"]
  }
}
```

### 9.2 `GET /api/search/summary`

`keyword`는 공백 제외 필수이며 최대 100자다.

```json
{ "data": { "keyword": "지갑", "lostItemCount": 12, "facilityRequestCount": 1 } }
```

### 9.3 `GET /api/search/lost-items`

Query: `keyword`(필수, 최대 100자), `cursor`(선택, 최대 512자),
`size`(기본 20, 1~50). 응답은 `CursorSlice<LostItemSearchItem>`이다.

```ts
type LostItemSearchItem = {
  storedItemId: number;
  itemName: string;
  categoryName: string;
  description: string;
  foundLocationName: string;
  foundDate: string;
  publicStatus: string;
  thumbnailUrl: string | null;
  createdAt: string;
};
```

오류: `SEARCH_INVALID_CURSOR`(400).

### 9.4 `GET /api/search/facility-requests`

Query는 분실물 검색과 같다. 응답은 `CursorSlice<FacilityRequestSearchItem>`이다.

```ts
type FacilityRequestSearchItem = {
  facilityRequestId: number;
  title: string;
  description: string;
  categoryName: string;
  locationName: string;
  requestStatus: string;
  requestStatusName: string;
  thumbnailUrl: string | null;
  createdAt: string;
};
```

오류: `SEARCH_INVALID_CURSOR`(400).

## 10. 최근 검색 API

모두 로그인이 필요하다.

### 10.1 `GET /api/recent-searches`

```json
{
  "data": [{
    "recentSearchId": 1, "keyword": "지갑", "searchedAt": "2026-08-12T11:30:00"
  }]
}
```

### 10.2 `POST /api/recent-searches`

요청은 `{ "keyword": "지갑" }`이다. keyword는 공백 제외 필수, 최대 100자다. 응답은
기록 후의 전체 최근 검색 목록이며 10.1과 같은 구조다.

### 10.3 `DELETE /api/recent-searches/{recentSearchId}`

ID는 양수이며 성공은 `204`. 다른 사용자의 기록이거나 존재하지 않아도 멱등적으로 `204`다.

### 10.4 `DELETE /api/recent-searches`

현재 사용자의 전체 기록을 삭제하며 성공은 `204`다.

## 11. 파일 API

### 11.1 `GET /api/files/{fileId}`

- 공개 API, ID는 양수
- body는 JSON이 아닌 이미지 binary
- 저장된 MIME type을 `Content-Type`으로 사용
- `Content-Disposition: inline`, public cache 최대 1시간
- `X-Content-Type-Options: nosniff`
- 오류: `FILE_NOT_FOUND`(404), `FILE_STORAGE_ERROR`(500)

응답의 `fileUrl`/`thumbnailUrl`을 `<img src>`에 사용할 수 있다. 상대 경로라면 API base
URL과 결합한다.

## 12. 오류 코드 목록

### 12.1 공통 및 보안

| HTTP | code | 의미 |
|---:|---|---|
| 400 | `COMMON_VALIDATION_FAILED` | 필드 또는 도메인 요청 검증 실패 |
| 400 | `COMMON_MALFORMED_JSON` | JSON 파싱 실패·enum 불일치 |
| 400 | `COMMON_TYPE_MISMATCH` | Path/Query 타입 불일치 |
| 400 | `COMMON_MISSING_PARAMETER` | 필수 파라미터·multipart 파트 누락 |
| 401 | `SECURITY_AUTHENTICATION_REQUIRED` | 로그인 필요 |
| 403 | `SECURITY_ACCESS_DENIED` | 역할 또는 접근 권한 부족 |
| 403 | `SECURITY_INVALID_CSRF_TOKEN` | CSRF 토큰 누락·불일치 |
| 404 | `COMMON_RESOURCE_NOT_FOUND` | 매핑되지 않은 리소스 |
| 405 | `COMMON_METHOD_NOT_ALLOWED` | 지원하지 않는 method |
| 413 | `COMMON_UPLOAD_SIZE_EXCEEDED` | 업로드 크기 제한 초과 |
| 415 | `COMMON_UNSUPPORTED_MEDIA_TYPE` | Content-Type 불일치 |
| 500 | `COMMON_INTERNAL_SERVER_ERROR` | 처리되지 않은 서버 오류 |

### 12.2 인증 및 사용자

| HTTP | code |
|---:|---|
| 400 | `AUTH_EMAIL_VERIFICATION_NOT_FOUND` |
| 400 | `AUTH_EMAIL_VERIFICATION_EXPIRED` |
| 400 | `AUTH_EMAIL_VERIFICATION_CODE_MISMATCH` |
| 400 | `AUTH_INVALID_EMAIL_VERIFICATION_TOKEN` |
| 400 | `AUTH_EMAIL_VERIFICATION_TOKEN_EXPIRED` |
| 400 | `AUTH_EMAIL_VERIFICATION_EMAIL_MISMATCH` |
| 401 | `AUTH_INVALID_CREDENTIALS` |
| 404 | `AUTH_ACCOUNT_NOT_FOUND`, `USER_NOT_FOUND` |
| 409 | `AUTH_EMAIL_ALREADY_REGISTERED` |
| 409 | `AUTH_STUDENT_NUMBER_ALREADY_REGISTERED` |
| 409 | `AUTH_EMAIL_VERIFICATION_ALREADY_COMPLETED` |
| 409 | `AUTH_EMAIL_VERIFICATION_TOKEN_CONSUMED` |
| 409 | `AUTH_SIGNUP_CONFLICT` |
| 429 | `AUTH_EMAIL_VERIFICATION_RESEND_TOO_SOON` |
| 429 | `AUTH_EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED` |
| 502 | `AUTH_EMAIL_SEND_FAILED` |

### 12.3 시설문의

| HTTP | code |
|---:|---|
| 400 | `INVALID_REQUEST`, `FACILITY_REQUEST_UPDATE_REQUIRED` |
| 400 | `INVALID_ATTACHMENT`, `FILE_LIMIT_EXCEEDED`, `INVALID_FILE_TYPE` |
| 403 | `FACILITY_REQUEST_ACCESS_DENIED` |
| 404 | `FACILITY_REQUEST_NOT_FOUND`, `FACILITY_CATEGORY_NOT_FOUND`, `LOCATION_NOT_FOUND` |
| 409 | `FACILITY_REQUEST_NOT_DELETABLE`, `FACILITY_REQUEST_NOT_EDITABLE` |
| 409 | `FACILITY_REQUEST_INVALID_STATUS_TRANSITION`, `FACILITY_REQUEST_ALREADY_COMPLETED` |
| 500 | `FILE_STORAGE_ERROR` |

### 12.4 분실물 및 소유권 요청

| HTTP | code |
|---:|---|
| 400 | `STORED_ITEM_INVALID_CURSOR`, `STORED_ITEM_INVALID_FOUND_LOCATION` |
| 400 | `STORED_ITEM_FILE_LIMIT_EXCEEDED`, `STORED_ITEM_INVALID_FILE_TYPE` |
| 400 | `STORED_ITEM_INVALID_REQUEST`, `STORED_ITEM_INVALID_ATTACHMENT` |
| 400 | `ITEM_CLAIM_INVALID_CURSOR`, `ITEM_CLAIM_FILE_LIMIT_EXCEEDED` |
| 400 | `ITEM_CLAIM_INVALID_FILE_TYPE` |
| 403 | `STORED_ITEM_ACCESS_DENIED`, `ITEM_CLAIM_ACCESS_DENIED` |
| 404 | `STORED_ITEM_NOT_FOUND`, `LOST_ITEM_OFFICE_NOT_FOUND`, `ITEM_CATEGORY_NOT_FOUND` |
| 404 | `ITEM_CLAIM_NOT_FOUND` |
| 409 | `STORED_ITEM_VERSION_CONFLICT`, `STORED_ITEM_INVALID_STATUS_TRANSITION` |
| 409 | `STORED_ITEM_NOT_DELETABLE`, `STORED_ITEM_HAS_CLAIMS` |
| 409 | `ITEM_CLAIM_NOT_CLAIMABLE`, `ITEM_CLAIM_DUPLICATE_ACTIVE_CLAIM` |
| 500 | `STORED_ITEM_FILE_STORAGE_ERROR`, `ITEM_CLAIM_FILE_STORAGE_ERROR` |

### 12.5 검색 및 파일

| HTTP | code |
|---:|---|
| 400 | `SEARCH_INVALID_CURSOR` |
| 404 | `FILE_NOT_FOUND` |
| 500 | `FILE_STORAGE_ERROR`, `FILE_STORAGE_PROVIDER_NOT_SUPPORTED` |

## 13. 프론트엔드 AI 구현 체크리스트

1. 모든 요청에 `credentials: "include"`를 적용한다.
2. 앱 시작 시 CSRF 토큰을 조회하고 로그인 성공 후 다시 조회한다.
3. `POST`, `PATCH`, `DELETE`에 동적으로 받은 CSRF 헤더를 추가한다.
4. `204`와 JSON 응답을 구분해서 파싱한다.
5. multipart의 `request`를 JSON Blob으로 전송하고 `Content-Type`을 직접 지정하지 않는다.
6. 오류 UI는 HTTP status뿐 아니라 `code`와 `fieldErrors`를 사용한다.
7. 커서는 불투명 값으로 취급하고 `nextCursor`를 그대로 재사용한다.
8. nullable 필드: `thumbnailUrl`, `nextCursor`, `department`, 직접 입력 장소의 `locationId`,
   일부 상태 이력 값, 관리자 처리 응답의 `adminResponse`.
9. 수정 API에서 필드 생략, null, 빈 배열의 의미 차이를 보존한다.
10. 공개 시설문의 상세의 `editable`, `deletable`을 버튼 노출 기준으로 사용하되 서버의
    `403`과 `409`도 처리한다.

## 14. 구현상 주의할 불일치와 미구현 영역

- 분실물 등록 경로만 단수형 `/api/lost-item`이다.
- 인증 API 일부는 성공 응답의 `data` wrapper가 없다.
- 시설문의는 `requestStatus`, 분실물은 `publicStatus`, 소유권은 `claimStatus`를 쓴다.
- 시설문의 목록은 page, 분실물·검색·소유권 목록은 cursor 방식이다.
- 공개 시설문의 상세에는 관리자 답변이 없고 관리자 상세에만 `adminResponses`가 있다.
- 현재 Controller에는 소유권 요청 상태 변경 API가 없다.
- 분실물 등록에 필요한 보관소 목록 조회 API가 없다. `officeId` 공급 방식을 별도로 정해야 한다.
- 알림 엔티티는 있지만 알림 조회 Controller는 없다.

## 15. 참고 자료

- [Spring Security 공식 CSRF 문서](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html):
  SPA의 토큰 조회·재발급, 로그인/로그아웃, multipart 요청 시 CSRF 처리 기준
