# Formwork
### 버전 관리되는 동적 폼 시스템

---

## 1. 프로젝트 개요

Formwork는 동적 폼 정의, 제출, 승인 워크플로우를 처리하는 백엔드 시스템이다.

이 과제의 핵심은 폼 입력을 받는 것이 아니라
**"변하는 폼 구조 위에서 데이터의 정합성과 추적 가능성을 유지하는 것"** 이다.

> 폼은 한 번 만들고 끝나는 것이 아니다.
> 구조가 바뀌어도 이미 제출된 데이터는 깨져서는 안 된다.

---

## 2. 배경 및 문제 정의

일반적인 폼 처리:
- HTML form → POST → DB insert

하지만 실제 운영 환경에서는:
- 폼 구조가 변경됨 (필드 추가, 삭제, 타입 변경)
- 이전 제출 데이터는 당시 구조 기준으로 해석되어야 함
- 제출은 단순 저장이 아니라 상태를 가짐 (심사, 승인, 반려)
- 누가, 언제, 무엇을 바꿨는지 추적 가능해야 함

---

## 3. 핵심 개념

### 3.1 Form

폼의 논리적 단위. 입력 구조를 정의하는 상위 개체.

- id, name, description, created_at, updated_at

### 3.2 Form Version

폼의 불변 버전. 제출은 항상 특정 버전에 고정된다.

- id, form_id, version_number, schema (JSON), created_at
- 생성 후 수정 불가
- 제출은 반드시 특정 버전을 참조

### 3.3 Submission

특정 폼 버전에 대한 사용자 제출 데이터.

- id, form_version_id, data (JSON), status, created_at, updated_at
- 상태: DRAFT → SUBMITTED → APPROVED / REJECTED

### 3.4 Submission Event (Audit Log)

모든 상태 변경 이력.

- id, submission_id, event_type, payload, created_at
- Append-only (수정/삭제 금지)

---

## 4. 워크플로우

상태 전이:

```
DRAFT → SUBMITTED → APPROVED
                   → REJECTED
```

규칙:
- SUBMITTED 상태만 심사 가능
- APPROVED / REJECTED는 최종 상태

---

## 5. API 요구사항

### Form
- POST /forms
- GET /forms
- POST /forms/{id}/versions
- GET /forms/{id}/versions

### Submission
- POST /submissions
- GET /submissions
- GET /submissions/{id}
- PATCH /submissions/{id}/submit
- PATCH /submissions/{id}/approve
- PATCH /submissions/{id}/reject

### Export
- GET /submissions/export (CSV 스트리밍)

---

## 6. 필수 구현 요구사항

### 검증
- Schema 기반 서버 측 검증 필수
- 유효하지 않은 제출은 거부

### 인가
- submitter: 본인 제출 생성/수정
- reviewer: 제출 승인/반려

### 비기능
- Audit log는 불변
- 민감 필드 마스킹
- Rate limiting
- trace_id 포함 구조화 로깅
- 목록 API 페이지네이션
- CSV export 스트리밍 처리

---

## 7. 제한 사항

- Form schema 덮어쓰기 금지 (버전 분리 필수)
- Submission에 form_version 참조 없이 저장 금지
- 상태 전이를 단순 UPDATE로 처리 금지
- Audit log 없이 current_state만 갱신 금지
- 서버 검증 없이 클라이언트 입력 신뢰 금지

---

## 8. 평가 기준

- 데이터 모델링의 정확성
- 워크플로우 무결성
- API 설계 품질
- 에러 처리의 일관성
- 코드 구조와 유지보수성

---

## 9. 보너스 과제

- Webhook 시스템 (제출 이벤트 트리거 + 실패 시 재시도)
- Cursor 기반 페이지네이션
- 제출 필드 값 기반 검색

---

## 10. 결과물

- 소스 코드
- README (설계 의도, 트레이드오프, 한계점 포함)
- API 문서
- 테스트 코드
