package io.github.hyinorbitcode.couponlab.user.repository;

import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.entity.UserIdentity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<UserIdentity> findByProviderAndProviderUserId(IdentityProvider provider, String providerUserId);
}
