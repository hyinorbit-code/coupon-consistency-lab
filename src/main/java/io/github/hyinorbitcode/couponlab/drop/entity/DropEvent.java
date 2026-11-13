package io.github.hyinorbitcode.couponlab.drop.entity;

import io.github.hyinorbitcode.couponlab.drop.exception.InvalidDropStatusException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "drop_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DropEvent {

    public static final int MAX_TITLE_LENGTH = 100;
    public static final int MAX_PRIZE_DESCRIPTION_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String prizeDescription;
    private Instant openAt;
    private Instant closeAt;

    @Enumerated(EnumType.STRING)
    private DropEventStatus status;

    private Instant createdAt;

    private DropEvent(String title, String prizeDescription, Instant openAt, Instant closeAt, DropEventStatus status,Instant createdAt) {
        this.title = title;
        this.prizeDescription = prizeDescription;
        this.openAt = openAt;
        this.closeAt = closeAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static DropEvent create(String title, String prizeDescription,
                                   Instant openAt, Instant closeAt, Instant now) {
        Objects.requireNonNull(openAt, "openAt");
        Objects.requireNonNull(closeAt, "closeAt");
        Objects.requireNonNull(now, "now");
        requireText(title, MAX_TITLE_LENGTH, "title");
        requireText(prizeDescription, MAX_PRIZE_DESCRIPTION_LENGTH, "prizeDescription");
        if (!openAt.isBefore(closeAt)) {
            throw new IllegalArgumentException("openAt must be before closeAt");
        }
        return new DropEvent(title, prizeDescription, openAt, closeAt, DropEventStatus.DRAFT, now);
    }

    private static void requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
    }

    public void publish(Instant now) {
        if (status != DropEventStatus.DRAFT || !now.isBefore(openAt)) {
            throw new InvalidDropStatusException();
        }
        status = DropEventStatus.PUBLISHED;
    }

    public void cancel(Instant now) {
        if (status == DropEventStatus.CANCELED || !now.isBefore(openAt)) {
            throw new InvalidDropStatusException();
        }
        status = DropEventStatus.CANCELED;
    }

    public DropPhase phase(Instant now) {
        if (now.isBefore(openAt)) return DropPhase.UPCOMING;
        if (now.isBefore(closeAt)) return DropPhase.OPEN;
        return DropPhase.CLOSED;
    }

    /** 진행 중인데 남은 코드가 없으면 매진이다. 오픈 전과 종료 후는 시각만으로 정한다. */
    public DropPhase phase(Instant now, long remainingCodes) {
        DropPhase byTime = phase(now);
        if (byTime == DropPhase.OPEN && remainingCodes <= 0) {
            return DropPhase.SOLD_OUT;
        }
        return byTime;
    }

    public boolean canRegisterCodes(Instant now) {
        return status != DropEventStatus.CANCELED && now.isBefore(openAt);
    }

}
