package com.moonju.preprocess.api.domain.auth.service;

import com.moonju.preprocess.api.domain.auth.dto.AuthMeResponse;
import com.moonju.preprocess.api.domain.auth.dto.TokenRefreshResponse;
import com.moonju.preprocess.api.domain.auth.exception.InvalidTokenException;
import com.moonju.preprocess.api.domain.user.entity.SocialAccount;
import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import com.moonju.preprocess.api.domain.user.entity.User;
import com.moonju.preprocess.api.domain.user.repository.SocialAccountRepository;
import com.moonju.preprocess.api.domain.user.repository.UserRepository;
import com.moonju.preprocess.api.infra.oauth.OAuth2UserInfo;
import com.moonju.preprocess.api.infra.oauth.OAuth2UserInfoFactory;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final TokenService tokenService;

    public AuthService(
        UserRepository userRepository,
        SocialAccountRepository socialAccountRepository,
        TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public IssuedTokenPair loginWithOAuth2(String registrationId, Map<String, Object> attributes) {
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.create(registrationId, attributes);
        User user = findOrCreateUser(userInfo);
        linkSocialAccountIfNeeded(user, userInfo);
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new InvalidTokenException("Authenticated user not found."));
        List<SocialProvider> providers = socialAccountRepository.findAllByUser_Id(user.getId())
            .stream()
            .map(SocialAccount::getProvider)
            .toList();
        return AuthMeResponse.of(user, providers);
    }

    @Transactional
    public TokenRefreshResponse refresh(String refreshToken) {
        IssuedTokenPair tokenPair = tokenService.refresh(refreshToken);
        return new TokenRefreshResponse(
            tokenPair.accessToken(),
            tokenPair.accessTokenExpiresAt(),
            tokenPair.refreshToken()
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revoke(refreshToken);
    }

    private User findOrCreateUser(OAuth2UserInfo userInfo) {
        return socialAccountRepository.findByProviderAndProviderUserId(
                SocialProvider.GOOGLE,
                userInfo.getProviderUserId()
            )
            .map(SocialAccount::getUser)
            .orElseGet(() -> userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> userRepository.save(User.createUser(
                    userInfo.getEmail(),
                    userInfo.getName(),
                    userInfo.getProfileImageUrl()
                )))
            );
    }

    private void linkSocialAccountIfNeeded(User user, OAuth2UserInfo userInfo) {
        if (!socialAccountRepository.existsByUser_IdAndProvider(user.getId(), SocialProvider.GOOGLE)) {
            socialAccountRepository.save(new SocialAccount(
                user,
                SocialProvider.GOOGLE,
                userInfo.getProviderUserId(),
                userInfo.getEmail()
            ));
        }
    }
}
