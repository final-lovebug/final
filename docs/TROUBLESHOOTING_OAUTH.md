# OAuth 로그인 트러블슈팅

## `authorization_request_not_found`

### 증상

Google 로그인 후 callback에서 다음 예외가 발생한다.

```text
OAuth2AuthenticationException: [authorization_request_not_found]
```

### 원인

Spring Security는 Google로 보내기 직전에 authorization request를 저장하고, callback의 `state`로 같은 요청을 찾는다.

기본 구현은 `HttpSessionOAuth2AuthorizationRequestRepository`라서 세션에 저장한다. 배포 환경에서 다음 조건이면 callback에서 찾지 못한다.

- OAuth 시작 요청과 Google callback이 서로 다른 EC2 인스턴스로 전달됨
- 인스턴스들이 세션을 공유하지 않음
- callback 요청에 세션 쿠키가 전달되지 않음
- 저장 후 TTL이 지나 authorization request가 만료됨

이 프로젝트는 JWT 인증을 stateless로 사용하므로 세션 고정(sticky session)에 의존하지 않고 Redis에 authorization request를 저장한다.

## 현재 저장 방식

`RedisOAuth2AuthorizationRequestRepository`가 다음 키로 요청을 저장한다.

```text
auth:oauth-authorization:{state}
```

- TTL: 5분
- Google callback의 `state`로 조회
- callback 처리 시 `GETDEL`로 조회와 삭제를 원자적으로 수행
- Redis에는 client id, redirect URI, scope, state 등이 저장된다

## 배포 확인 순서

1. 애플리케이션의 `prod` 프로파일이 활성화되어 있는지 확인한다.
2. `/lovebug/redis/host`, `/lovebug/redis/port`가 올바른 Redis를 가리키는지 확인한다.
3. 애플리케이션 EC2 보안 그룹에서 Redis 포트 접근이 허용되는지 확인한다.
4. 로그인 시작 직후 Redis에 키가 생성되는지 확인한다.

```bash
redis-cli --tls -h "$REDIS_HOST" -p "$REDIS_PORT" \
  --scan --pattern 'auth:oauth-authorization:*'
```

Redis CLI를 사용할 수 없다면 애플리케이션 로그와 Redis 모니터링 지표로 확인한다. authorization request에는 OAuth secret이 들어가지 않지만, client id와 redirect URI가 포함될 수 있으므로 값을 애플리케이션 로그에 출력하지 않는다.

## 계속 실패할 때

- 키가 생성되지 않으면 `/oauth2/authorization/google` 요청이 새 코드로 배포됐는지 확인한다.
- 키는 생성되지만 callback에서 사라지면 Redis 연결, TTL, Redis eviction 정책을 확인한다.
- callback의 `state`가 시작 요청의 state와 다르면 브라우저·프록시가 callback URL을 변경하지 않는지 확인한다.
- `redirect_uri_mismatch`가 나오면 Google Cloud Console의 callback URI를 확인한다. 이는 `authorization_request_not_found`와 다른 문제다.
- Redis 장애 시 OAuth 로그인은 실패해야 하며, JWT로 이미 로그인한 API 요청까지 영향을 받지 않도록 Redis 용도를 분리한다.

## 롤백

Redis 저장소를 제거하고 세션 방식으로 되돌리면 인스턴스 간 세션 공유 또는 ALB sticky session이 필요하다. 코드 롤백 후에는 기존 OAuth callback 흐름의 세션 쿠키 전달 여부를 함께 확인한다.
