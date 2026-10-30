package io.github.hyinorbitcode.couponlab.user;

/**
 * 인증된 호출자. 컨트롤러 메서드 파라미터로 선언하면 JWT에서 채워진다.
 * 다른 모듈은 사용자 엔티티 대신 이 타입만 참조한다.
 */
public record CurrentUser(long id, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
