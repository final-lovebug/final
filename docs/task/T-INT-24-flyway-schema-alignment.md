# T-INT-24 — Flyway 재도입과 스키마 ↔ 엔티티 정합

상태: **1~3단계 완료(2026-09-16)** — `local` 실기동까지 확인. `dev` DB 초기화와 `prod` 재개는 남음 | 담당자:
근거: 결정은 `docs/plan/CONFLICTS.md` 3-15절(`D-106`~`D-109`)
의존: 없음. **`prod` 재개(4단계)만 운영 DB 판단을 기다린다**

## 문제

**Flyway가 세 환경 모두에서 꺼져 있다.** 스키마 검증이 기동을 막아 급히 끈 것이고,
원인은 그대로다.

| 커밋 | 무엇을 껐나 |
| --- | --- |
| `add9940` | 배포 — `start_container.sh`에 `SPRING_FLYWAY_ENABLED=false` |
| `ec0270d` | `local`·`dev` 프로파일과 루트 `compose.yaml`의 주석 처리된 backend 서비스 |

터진 예외는 이것이다.

```
Schema validation: wrong column type encountered in column [reject_reason] in table [candidate_term];
found [text (Types#LONGVARCHAR)], but expecting [longtext (Types#CLOB)]
```

### 왜 계속 터지는가

스키마 정의가 **두 벌**인데 **둘을 대조하는 지점이 없다.**

- 정의 A — `V1__init_schema.sql`
- 정의 B — JPA 엔티티 매핑

테스트 지원 클래스 넷이 전부 `ddl-auto=create-drop` + `spring.flyway.enabled=false`다
(`RepositoryTestSupport`, `IntegrationTestSupport`, `MySqlRepositoryTestSupport`,
`MySqlIntegrationTestSupport`). **엔티티가 만든 스키마를 엔티티가 검증**하므로 드리프트는
구조적으로 검출되지 않는다. 결과적으로 `ddl-auto=validate`가 걸린 `dev`·`prod`의
**기동 순간이 첫 검증 지점**이 되고, 그래서 실행·배포에서 터진다.

H2로는 대신할 수 없다. `MODE=MySQL`은 `text`도 `longtext`도 `CLOB`으로 읽어 둘을 구분하지 못한다.

### 실제 불일치 (2026-09-16, MySQL 컨테이너로 확인)

`reject_reason` 하나가 아니다. 그것을 고치면 다음 것이 나온다.

| 컬럼 | Flyway | 엔티티 | 원인 |
| --- | --- | --- | --- |
| `candidate_term.reject_reason`, `document_version.body`, `draft_document.draft_body`, `suggestion_term.reject_reason`, `term.definition` | `text` | `longtext` 기대 | `@Lob String`이 MySQL에서 `longtext`로 매핑된다 |
| `dictionary.active_flag` | `tinyint generated always as (...)` | `integer generated always as (...)` 기대 | `columnDefinition`의 타입명과 필드 타입이 둘 다 `integer` |

## 설계 확정(2026-09-16, 사용자 결정)

| 축 | 결정 | ID |
| --- | --- | --- |
| 스키마의 주인 | **Flyway 하나.** 엔티티가 따라간다 — 불일치는 엔티티 쪽에서 고친다 | `D-106` |
| `ddl-auto` | 모든 프로파일 `validate`. 어느 환경에서도 엔티티가 스키마를 만들지 않는다 | `D-107` |
| 재발 방지 | **MySQL + Flyway + `validate`로 컨텍스트만 띄우는 테스트**를 `check`에 상주시킨다 | `D-108` |
| 매핑 규약 | `@Lob` 금지 등 MySQL 타입 대응 규칙을 `backend/CLAUDE.md`에 못박는다 | `D-109` |

## 진행 순서

**1~3단계가 이 태스크의 범위다. 4단계(`prod`)는 운영 DB 판단이 필요해 분리한다.**

### 1. 대조 테스트 + 불일치 해소

- [x] `SchemaValidationTest` 신설 — `@SpringBootTest(webEnvironment = NONE)` +
      `MySqlContainerConfiguration` + `spring.flyway.enabled=true` +
      `spring.jpa.hibernate.ddl-auto=validate`. 컨텍스트가 뜨면 통과다
- [x] **테스트 전용 엔티티를 스캔에서 뺀다** — `BaseEntityAuditingTest`의 내부 엔티티는
      마이그레이션 대상이 아니라 대응 테이블이 없다. 그대로 두면 컬럼 타입에 닿기도 전에
      `missing table`로 멈춘다. `PersistenceManagedTypes` 빈을 직접 등록해 걸러낸다
      (Boot 4.0.8에는 `ManagedClassNameFilter`가 없다)
- [x] `@Lob` 5곳 → `@JdbcTypeCode(SqlTypes.LONGVARCHAR)`. **`text`가 의도된 타입이다** —
      `V1__init_schema.sql`이 「`text`는 65,535바이트이고 본문 상한은 10,000자」라고 근거를 적어 뒀다
- [x] `Dictionary.activeFlag` — `columnDefinition`의 타입명을 `tinyint`로,
      JDBC 타입을 `@JdbcTypeCode(SqlTypes.TINYINT)`로. **둘 다 고쳐야 한다**:
      기대 타입명은 `columnDefinition`의 첫 단어에서, 타입 코드는 필드 타입에서 각각 온다
- [x] `./gradlew spotlessApply && ./gradlew check`

### 2. `local` 재개

- [x] `application-local.yml`의 `spring.flyway.enabled: false` 제거
- [x] `spring.jpa.hibernate.ddl-auto: validate` 명시 (`D-107`)
- [x] **로컬 DB를 비우고 새로 받는다** — 스쿼시(`e9f052d`) 때문에 기존 `flyway_schema_history`와
      맞지 않는다. 2026-09-16 실제로 부딪혔다: `Migration checksum mismatch for migration version 1`
      (적용본은 `V1__create_member.sql`, 로컬 해석본은 `V1__init_schema.sql`).
      **이력 테이블만 남고 실제 테이블은 0개**인 상태였어서 `flyway_schema_history`를 지우는 것으로 끝났다
- [x] **검증** — `bootRun --args='--spring.profiles.active=local'`에서
      `Successfully applied 1 migration ... now at version v1` 뒤 `Started BackendApplication`.
      테이블 34개 생성, Hibernate `validate` 통과(2026-09-16)

### 3. `dev` 재개

- [x] `application-dev.yml`의 `spring.flyway.enabled: false` 제거.
      `out-of-order: true`와 `ddl-auto: validate`는 그대로 둔다
- [x] 루트 `compose.yaml`의 주석 처리된 backend 서비스에서 `SPRING_FLYWAY_ENABLED=false` 제거
- [x] 대상 DB를 비우고 새로 받는다(2단계와 같은 이유). 2026-09-16 루트 `compose.yaml`의
      MySQL(`ubidic`)에서 같은 체크섬 실패가 났다 — **`backend/compose.yaml`의 MySQL과 별개의 DB이고
      `mysql-data` 볼륨이 있어 이력이 살아남는다.** 이쪽은 `V430`조차 적용된 적이 없어
      (`draft_document.dictionary_version_no` 부재) 스키마가 `V1__init_schema.sql`보다 뒤처져 있었고,
      스모크 데이터 5행을 버리기로 하고 `docker compose down -v`로 비웠다
- [x] **검증** — 비운 DB에 `Successfully applied 1 migration ... now at version v1`,
      `Started BackendApplication`, `/actuator/health` 200, 테이블 34개(2026-09-16)

### 4. `prod` 재개 — **이 태스크 범위 밖. 별도 판단 필요**

`flyway_schema_history`에 옛 31개 행이 남아 있으면 `V1` 체크섬 불일치와 30건 missing으로
기동이 막힌다. 선택지는 둘이고 **운영 데이터를 지켜야 하는지가 갈림길이다.**

- 버려도 되면 — 스키마와 이력 테이블을 비우고 `V1`을 새로 적용한다(가장 단순)
- 지켜야 하면 — 이력 31행을 지우고 `baseline-on-migrate=true` + `baseline-version=1`로
  한 번 띄워 `V1`을 「적용됨」으로 **표시만** 한다. 이후 그 옵션과
  `SPRING_FLYWAY_ENABLED=false`를 함께 걷어낸다
- **어느 쪽이든 선행 확인이 있다** — 운영 실제 스키마가 `V1__init_schema.sql`과 같은지
  대조한다. 1단계 테스트가 만든 스키마와 운영 덤프를 비교하는 쪽이 안전하다
- `prod`는 지금 `ddl-auto`가 없어 `none`이다. **검증조차 하지 않는다** — `D-107`에 따라
  `validate`를 켠다

## `validate`가 잡지 못하는 것

컬럼의 **존재와 타입**만 본다. 아래는 어긋나도 통과하므로 **마이그레이션 리뷰로만** 막힌다.

- `nullable`, 기본값
- 인덱스·유니크 제약·외래키
- 컬럼 순서, collation

## 이번 범위 밖

- **`prod` 재개**(위 4단계) — 운영 DB 판단 후 별도 태스크
- **`RepositoryTestSupport` 등 기존 지원 클래스의 `create-drop` 유지** — 테스트 속도 때문에
  그대로 둔다. 드리프트 감시는 `SchemaValidationTest` 하나가 맡는다
- **`UbiquitousLanguageLifecycleTest.firstDictionaryIsBornFromDocuments`** — `T-INT-23`이
  기록한 선행 실패다. 이 태스크가 만든 것이 아니며 기대값 갱신은 담당을 따로 정한다
