package io.github.hyinorbitcode.couponlab.prizecode.service;

import io.github.hyinorbitcode.couponlab.drop.DropEventApi;
import io.github.hyinorbitcode.couponlab.prizecode.entity.PrizeCode;
import io.github.hyinorbitcode.couponlab.prizecode.repository.PrizeCodeRepository;
import io.github.hyinorbitcode.couponlab.prizecode.security.PrizeCodeCipher;
import io.github.hyinorbitcode.couponlab.prizecode.security.PrizeCodeHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PrizeCodeService {

    private final DropEventApi dropEventApi;
    private final PrizeCodeRepository prizeCodeRepository;
    private final PrizeCodeCipher cipher;
    private final PrizeCodeHasher hasher;
    private final Clock clock;

    @Transactional
    public CodeRegistrationResult register(long dropEventId, List<String> codes) {
        dropEventApi.checkCodesRegistrable(dropEventId);
        Instant now = clock.instant();

        Set<String> seenInRequest = new HashSet<>();
        int registered = 0;
        int duplicates = 0;
        for (String raw : codes) {
            String code = raw.strip();
            byte[] hash = hasher.hash(code);
            boolean firstInRequest = seenInRequest.add(HexFormat.of().formatHex(hash));
            if (!firstInRequest || prizeCodeRepository.existsByCodeHash(hash)) {
                duplicates++;
                continue;
            }
            prizeCodeRepository.save(PrizeCode.register(
                    dropEventId, cipher.encrypt(code), hash, PrizeCode.hintOf(code), now));
            registered++;
        }
        return new CodeRegistrationResult(registered, duplicates);
    }
}