package io.github.hyinorbitcode.couponlab.user.event;

/**
 * 가입 트랜잭션 안에서 발행하고, 커밋된 뒤에만 메일로 바뀌는 모듈 내부 이벤트.
 */
public final class SignupMailEvents {

    private SignupMailEvents() {
    }

    public record VerificationMailRequested(String email, String nickname, String rawToken) {
    }

    public record AlreadyRegisteredMailRequested(String email) {
    }
}
