# final

edit
hello
hello

## Git 훅 설정

브랜치명에서 Jira 이슈 키를 추출해 커밋 메시지 앞에 자동으로 붙여주는
`prepare-commit-msg` 훅이 `.githooks/`에 있습니다.

`core.hooksPath`는 로컬 git 설정이라 저장소에 포함되지 않으므로,
clone 후 아래 명령을 한 번 실행해 주세요.

```bash
git config core.hooksPath .githooks
```

예를 들어 `WLSH-14-login-fix` 브랜치에서 커밋하면:

```
fix: 로그인 오류 수정  →  WLSH-14 fix: 로그인 오류 수정
```

이미 메시지에 이슈 키가 있거나 브랜치명에 키가 없으면 아무것도 하지 않습니다.
