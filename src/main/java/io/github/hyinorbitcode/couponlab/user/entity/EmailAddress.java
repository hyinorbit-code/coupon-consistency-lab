package io.github.hyinorbitcode.couponlab.user.entity;

import io.github.hyinorbitcode.couponlab.user.exception.InvalidEmailException;
import java.util.Locale;

/**
 * 이메일 주소 값 객체. 생성될 때 스스로 앞뒤 공백을 지우고 소문자로 정규화하며, 그 뒤로는 바뀌지 않는다.
 * 그래서 이 타입이 있으면 항상 정규화·검증이 끝난 값이다. User@Example.com과 user@example.com은 같은 주소다.
 *
 * <p>{@code +} 별칭이나 {@code .} 제거는 하지 않는다. 이메일은 응모 자격의 근거가 아니기 때문이다(ADR-0010, ADR-0012).
 */
public record EmailAddress(String value) {

    public static final int MAX_LENGTH = 254;

    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidEmailException("이메일을 입력해 주세요.");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !looksLikeAddress(value)) {
            throw new InvalidEmailException("올바른 이메일 주소를 입력해 주세요.");
        }
    }

    /** 형식 검사의 최종 방어선. 자세한 형식 검증은 요청 DTO의 @Email이 먼저 한다. */
    private static boolean looksLikeAddress(String value) {
        int at = value.indexOf('@');
        return at > 0 && at == value.lastIndexOf('@') && at < value.length() - 1;
    }

    @Override
    public String toString() {
        return value;
    }
}
