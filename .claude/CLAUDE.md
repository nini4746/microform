# Microform

## 프로젝트 개요
폼(Form) 기반 워크플로우 시스템. HTML Form/JSON 제출 → 검증 → 저장 → 상태 전이 → 조회/내보내기.
Spring Boot 3.5.6 + Java 21 + PostgreSQL + Gradle Kotlin DSL.

## 핵심 도메인
- Form / FormVersion: 폼 정의 및 버전 관리 (schema_json, workflow_json)
- Submission: 제출 건 (상태: DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED)
- SubmissionEvent: append-only 감사 로그
- WorkflowEngine: workflow_json 기반 커스텀 상태머신
- SchemaValidatorService: 필드 타입/제약조건 검증 (외부 라이브러리 없음)

## 패키지 구조
com.microform 하위 도메인 중심 구조:
common/, form/, submission/, validation/, workflow/, export/, webhook/

## 주요 결정사항
- 인증: HTTP Basic + InMemoryUserDetailsService (submitter / reviewer / admin 3개 역할)
- 상태머신: Spring State Machine 미사용, 커스텀 WorkflowEngine (~80줄)
- 감사 로그: JPA 미사용, JdbcTemplate raw INSERT (불변성 보장)
- CSV export: StreamingResponseBody + RowCallbackHandler + fetchSize=500 (OOM 방지)
- Rate limit: Resilience4j + Caffeine (Redis 불필요)
- DB 마이그레이션: Flyway (ddl-auto=validate)
- JSON in DB: JSONB 컬럼 + AttributeConverter<T, String>

## 마일스톤 진행 상황
- [x] M1: 프로젝트 스캐폴딩 + Form 버전 관리
- [x] M2: Submission 생성 + 스키마 검증
- [x] M3: 워크플로우 상태머신 + 역할 기반 전이
- [x] M4: Audit Log + 단건 상세 조회
- [x] M5: 목록 필터/검색 + 페이지네이션
- [x] M6: CSV Export 스트리밍
- [ ] M7: Webhook + 재시도 (선택)

## 테스트 전략
- Unit: SchemaValidatorServiceTest, WorkflowEngineTest
- Integration: @SpringBootTest (H2), TransitionSecurityTest (@WithMockUser)
- Streaming: CsvStreamingTest (10,000행, -Xmx128m 으로 OOM 검증)

## 실행 방법
```bash
# PostgreSQL 필요 (docker-compose.yml)
./gradlew bootRun

# 테스트 (H2 in-memory)
./gradlew test
```

## 스펙 문서
Formwork_Project_Spec.md 참고
