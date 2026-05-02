# Issue 15. Google/Kakao OAuth2 skeleton

## 이슈

- Issue: `#15`
- Title: `✨ [Feat] Google/Kakao OAuth2 설정 skeleton 추가`
- Branch: `feat/som/15`
- Base: `feat/som/12`

## 목표

Google/Kakao 소셜 로그인 구현을 위한 provider 설정과 user info adapter skeleton을 추가한다.

## 작업 범위

1. Google OAuth2 registration placeholder
2. Kakao OAuth2 registration/provider placeholder
3. `OAuth2UserInfo`
4. `GoogleOAuth2UserInfo`
5. `KakaoOAuth2UserInfo`
6. `OAuth2UserInfoFactory`
7. `OAuth2LoginFailedException`
8. `OAuth2LoginSuccessHandler`
9. `SecurityConfig`
10. OAuth provider setup 문서

## 범위 제외

1. 사용자 자동 가입
2. 기존 소셜 계정 로그인 처리
3. JWT access token 발급
4. Refresh token 발급/저장
5. 인증 controller API
6. 프론트엔드 OAuth callback 화면

## 사용자가 준비해야 하는 값

실제 로그인 테스트 전 아래 값을 발급받아야 한다.

1. Google OAuth Client ID
2. Google OAuth Client Secret
3. Kakao REST API Key
4. Kakao Client Secret

로컬 redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/kakao
```

## 검증 기준

1. provider별 user info parsing 테스트
2. unsupported provider 예외 테스트
3. Docker 기반 `gradle test`
4. secret 값 미커밋 확인
