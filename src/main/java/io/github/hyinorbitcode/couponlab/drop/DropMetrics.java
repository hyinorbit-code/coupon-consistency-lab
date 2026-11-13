package io.github.hyinorbitcode.couponlab.drop;

/**
 * 이벤트 관련 메트릭 이름. 응모 시도 수는 매진 응답도 포함하므로 DB가 아니라 메트릭으로 센다.
 * 애플리케이션 메모리에 있어 재시작하면 0부터 다시 센다.
 */
public final class DropMetrics {

    public static final String ENTRY_ATTEMPTS = "couponlab.drop.entry.attempts";
    public static final String DROP_ID_TAG = "dropId";

    private DropMetrics() {
    }
}
