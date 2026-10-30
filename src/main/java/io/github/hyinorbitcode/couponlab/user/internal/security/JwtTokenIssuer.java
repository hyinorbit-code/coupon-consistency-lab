package io.github.hyinorbitcode.couponlab.user.internal.security;

import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;
    private final Clock clock;

    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(authProperties.tokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(authProperties.issuer())
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(ROLE_CLAIM, user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String accessToken, Instant expiresAt) {
    }
}
