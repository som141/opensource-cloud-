# 이슈 12. User/Auth skeleton

## 목적

Google OAuth 로그인과 사용자 관리를 위한 도메인 기본 구조를 생성합니다.

## 작업 범위

1. `User` entity
2. `SocialAccount` entity
3. 사용자 role/status enum
4. repository interface
5. auth/user service skeleton
6. refresh token 저장 구조

## 완료 기준

1. Google 계정과 내부 사용자를 연결할 수 있습니다.
2. Kakao 로그인은 현재 범위에서 제외합니다.
3. 사용자 삭제는 soft delete를 고려합니다.
