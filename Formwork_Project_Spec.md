# Microform (폼워크/마이크로 폼) — 과제 명세서

## 0. 한 줄 요약
**Microform**은 “폼(Form) 기반 워크플로우”를 초소형으로 구현하는 프로젝트입니다.  
사용자는 **HTML Form / JSON**으로 입력을 제출하고, 서버는 **검증 → 저장 → 승인/상태 전이 → 조회/내보내기**를 제공합니다.

---

## 1. 목표
- 폼 제출(POST) 중심의 데이터 수집 시스템을 **프레임워크처럼** 재사용 가능한 구조로 구현
- 입력 스키마/검증/상태머신/감사로그를 포함한 “실서비스형” 기본기 확보
- 확장 포인트(필드 타입, 검증 규칙, 워크플로우 단계, 웹훅)를 명확히 설계

---

## 2. 핵심 개념(용어)
- **Form**: 입력 템플릿(필드 정의, 기본값, 제약조건 포함)
- **Submission**: 실제 제출된 한 건의 데이터(폼 + 값 + 상태)
- **Workflow**: Submission의 상태 전이 규칙(예: DRAFT → SUBMITTED → APPROVED/REJECTED)
- **Schema**: 폼 필드 정의(타입, required, min/max, regex, enum 등)
- **Audit Log**: 누가 언제 무엇을 바꿨는지(불변 이벤트 로그)

---

## 3. 기능 요구사항

### 3.1 폼 정의 관리(Form Registry)
- 폼을 생성/수정/버전업할 수 있어야 함
- 폼은 “버전”을 가진다 (v1, v2 …)
- 폼 정의는 아래 정보를 포함:
    - `form_id` (슬러그)
    - `version`
    - `fields[]` (name, type, required, constraints, label, description)
    - `workflow` (states, transitions)
    - `created_at`, `updated_at`

### 3.2 제출(Submission)
- 제출 생성: DRAFT 또는 SUBMITTED로 시작 가능
- 제출 데이터는 “해당 폼 버전”에 종속
- 제출 시 서버가 수행:
    1) 스키마 검증(타입/required/제약조건)
    2) 서버측 정규화(normalization) (trim, 날짜 파싱 등)
    3) 저장 + Audit Log 이벤트 기록

### 3.3 상태 전이(Workflow / State Machine)
- 상태 예시:
    - `DRAFT`
    - `SUBMITTED`
    - `UNDER_REVIEW`
    - `APPROVED`
    - `REJECTED`
- 전이 조건:
    - 정의된 transition 외에는 전이 불가
    - 특정 전이는 role 권한 필요(예: APPROVE는 reviewer만)
- 모든 전이는 이벤트로 기록(Audit Log)

### 3.4 조회/필터/검색
- 제출 목록 조회에서 지원:
    - 폼 ID, 버전, 상태, 기간(created_at), 제출자, 특정 필드 키워드(간단 검색)
- 제출 단건 조회:
    - 폼 스키마 + 제출 값 + 상태 + 이벤트 로그 포함(옵션)

### 3.5 내보내기(Export)
- CSV Export:
    - 특정 form_id + version + 기간 조건으로 CSV 다운로드
    - 필드가 많은 경우에도 안정적으로 스트리밍 처리(메모리 폭발 방지)

### 3.6 웹훅(Webhook) / 이벤트 푸시(선택)
- 특정 이벤트(예: SUBMITTED, APPROVED)에 대해 외부 URL로 POST
- 재시도 정책(최소 3회, 지수백오프)
- 실패 로그 남김

---

## 4. 비기능 요구사항(중요)

### 4.1 데이터 무결성
- Submission은 **폼 버전 고정**: 폼이 v2로 바뀌어도 v1 제출은 v1 스키마 기준으로 해석
- Audit Log는 append-only (수정/삭제 금지)

### 4.2 성능/확장
- 목록 조회는 pagination 필수(cursor 또는 offset)
- CSV Export는 스트리밍으로 구현(대량 데이터 대비)

### 4.3 보안
- 인증/인가:
    - 최소 2개 역할: `submitter`, `reviewer` (추가 가능)
- 입력 검증:
    - 서버에서만 신뢰(클라이언트 검증은 참고용)
- 기본 방어:
    - Rate limit(폼 제출/로그인 등), 요청 크기 제한
    - 민감 필드 마스킹 옵션(예: 주민번호 같은 것 가정)

### 4.4 관측 가능성(Observability)
- 구조화 로그(JSON 권장)
- 요청 단위 trace_id/req_id
- 핵심 지표:
    - 제출 성공/실패 수
    - 상태 전이 수
    - Export 수행 시간, 레코드 수

---

## 5. API 설계(예시)

### 5.1 Form
- `POST /forms` 폼 생성
- `POST /forms/{form_id}/versions` 폼 버전 추가
- `GET /forms/{form_id}` 최신 버전 조회
- `GET /forms/{form_id}/versions/{version}` 특정 버전 조회

### 5.2 Submission
- `POST /forms/{form_id}/versions/{version}/submissions`
- `GET /submissions?form_id=&version=&state=&from=&to=&q=&page=`
- `GET /submissions/{submission_id}`
- `POST /submissions/{submission_id}/transition` (예: to=APPROVED)

### 5.3 Export
- `GET /exports/forms/{form_id}/versions/{version}.csv?from=&to=&state=`

### 5.4 Webhook(선택)
- `POST /forms/{form_id}/webhooks`
- `GET /forms/{form_id}/webhooks`
- `DELETE /forms/{form_id}/webhooks/{webhook_id}`

---

## 6. 데이터 모델(권장)

### 6.1 Forms
- `forms(form_id, latest_version, created_at, updated_at)`
- `form_versions(form_id, version, schema_json, workflow_json, created_at)`

### 6.2 Submissions
- `submissions(id, form_id, version, state, submitter_id, data_json, created_at, updated_at)`
- `submission_events(id, submission_id, type, payload_json, actor_id, created_at)`
    - type 예: `CREATED`, `VALIDATED`, `STATE_CHANGED`, `UPDATED_FIELDS`

### 6.3 Webhooks(선택)
- `webhooks(id, form_id, event_type, url, secret, created_at)`
- `webhook_deliveries(id, webhook_id, event_id, status, attempts, last_error, created_at, updated_at)`

---

## 7. 마일스톤(권장 진행)
1) **Form 버전 관리 + 스키마 저장**
2) **Submission 생성 + 검증 + 조회**
3) **Workflow 상태머신 + 역할 기반 전이**
4) **Audit Log 이벤트화**
5) **필터/검색 + pagination**
6) **CSV Export 스트리밍**
7) (선택) Webhook + 재시도

---

## 8. 테스트 요구사항
- 단위 테스트:
    - 스키마 검증(타입/required/regex/enum)
    - 상태 전이(허용/불가 케이스)
- 통합 테스트:
    - 폼 v1/v2 공존에서 제출/조회 일관성
    - Export 대량 데이터 스트리밍(메모리 상한 체크)
- 보안 테스트:
    - 권한 없는 approve 시도 차단
    - rate limit 동작 확인

---

## 9. 제출물(Deliverables)
- `README.md`
    - 아키텍처(모듈 분리)
    - API 문서(엔드포인트/요청/응답 예시)
    - 스키마 정의 예시(최소 2개 폼)
    - 실행 방법(개발/테스트)
- 소스코드 전체
- 테스트 코드
- (가능하면) Docker compose 또는 단일 실행 스크립트

---

## 10. 스키마 예시(폼 정의 JSON)
아래는 “간단한 회원가입 폼” 예시입니다.

```json
{
  "form_id": "signup",
  "version": 1,
  "fields": [
    {"name": "email", "type": "string", "required": true, "constraints": {"regex": "^[^@]+@[^@]+\\.[^@]+$"}},
    {"name": "age", "type": "int", "required": false, "constraints": {"min": 0, "max": 150}},
    {"name": "tos_agree", "type": "bool", "required": true}
  ],
  "workflow": {
    "states": ["DRAFT", "SUBMITTED", "APPROVED", "REJECTED"],
    "transitions": [
      {"from": "DRAFT", "to": "SUBMITTED", "role": "submitter"},
      {"from": "SUBMITTED", "to": "APPROVED", "role": "reviewer"},
      {"from": "SUBMITTED", "to": "REJECTED", "role": "reviewer"}
    ]
  }
}
```

---

## 11. 합격 기준(체크리스트)
- [ ] 폼 버전이 존재하고, 제출은 특정 버전에 고정된다
- [ ] 서버 검증이 강제되며, invalid submission이 저장되지 않거나 상태가 invalid로 분리된다
- [ ] 상태 전이가 정의 기반으로만 일어난다(권한 포함)
- [ ] 모든 변경/전이가 Audit Log로 남는다
- [ ] 목록 조회 pagination + 필터가 동작한다
- [ ] CSV Export가 스트리밍으로 동작한다(대량에서도 안정)
- [ ] 테스트가 최소 핵심 영역을 커버한다
