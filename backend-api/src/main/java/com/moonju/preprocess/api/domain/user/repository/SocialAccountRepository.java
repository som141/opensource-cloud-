package com.moonju.preprocess.api.domain.user.repository;

import com.moonju.preprocess.api.domain.user.entity.SocialAccount;
import com.moonju.preprocess.api.domain.user.entity.SocialProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    boolean existsByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    boolean existsByUser_IdAndProvider(Long userId, SocialProvider provider);

    List<SocialAccount> findAllByUser_Id(Long userId);
}
