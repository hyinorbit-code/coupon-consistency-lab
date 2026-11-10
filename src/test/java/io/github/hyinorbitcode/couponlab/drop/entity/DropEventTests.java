package io.github.hyinorbitcode.couponlab.drop.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DropEventTests {

    private static final Instant NOW = Instant.parse("2026-11-10T00:00:00Z");
    private static final Instant OPEN_AT = NOW.plus(Duration.ofDays(1));
    private static final Instant CLOSE_AT = OPEN_AT.plus(Duration.ofHours(1));

    @Test
    @DisplayName("이벤트는 초안 상태로 만들어지고 전달한 값과 시각을 가진다")
    void createDraft() {
        DropEvent event = DropEvent.create("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT, NOW);

        assertThat(event.getId()).isNull();
        assertThat(event.getTitle()).isEqualTo("커피 드롭");
        assertThat(event.getPrizeDescription()).isEqualTo("아메리카노 쿠폰");
        assertThat(event.getOpenAt()).isEqualTo(OPEN_AT);
        assertThat(event.getCloseAt()).isEqualTo(CLOSE_AT);
        assertThat(event.getStatus()).isEqualTo(DropEventStatus.DRAFT);
        assertThat(event.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("제목과 경품 설명은 최대 길이까지 허용한다")
    void acceptsTextAtMaxLength() {
        String title = "가".repeat(DropEvent.MAX_TITLE_LENGTH);
        String prizeDescription = "가".repeat(DropEvent.MAX_PRIZE_DESCRIPTION_LENGTH);

        DropEvent event = DropEvent.create(title, prizeDescription, OPEN_AT, CLOSE_AT, NOW);

        assertThat(event.getTitle()).hasSize(DropEvent.MAX_TITLE_LENGTH);
        assertThat(event.getPrizeDescription()).hasSize(DropEvent.MAX_PRIZE_DESCRIPTION_LENGTH);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("제목이 비어 있으면 만들 수 없다")
    void rejectsBlankTitle(String title) {
        assertThatThrownBy(() -> DropEvent.create(title, "아메리카노 쿠폰", OPEN_AT, CLOSE_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("경품 설명이 비어 있으면 만들 수 없다")
    void rejectsBlankPrizeDescription(String prizeDescription) {
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", prizeDescription, OPEN_AT, CLOSE_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("제목이 최대 길이를 넘으면 만들 수 없다")
    void rejectsTooLongTitle() {
        String title = "가".repeat(DropEvent.MAX_TITLE_LENGTH + 1);

        assertThatThrownBy(() -> DropEvent.create(title, "아메리카노 쿠폰", OPEN_AT, CLOSE_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("경품 설명이 최대 길이를 넘으면 만들 수 없다")
    void rejectsTooLongPrizeDescription() {
        String prizeDescription = "가".repeat(DropEvent.MAX_PRIZE_DESCRIPTION_LENGTH + 1);

        assertThatThrownBy(() -> DropEvent.create("커피 드롭", prizeDescription, OPEN_AT, CLOSE_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("오픈 시각과 종료 시각이 같으면 만들 수 없다")
    void rejectsEmptyPeriod() {
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", "아메리카노 쿠폰", OPEN_AT, OPEN_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("종료 시각이 오픈 시각보다 앞서면 만들 수 없다")
    void rejectsReversedPeriod() {
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", "아메리카노 쿠폰", CLOSE_AT, OPEN_AT, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("오픈 시각, 종료 시각, 생성 시각이 없으면 만들 수 없다")
    void rejectsMissingInstants() {
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", "아메리카노 쿠폰", null, CLOSE_AT, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", "아메리카노 쿠폰", OPEN_AT, null, NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> DropEvent.create("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("초안 이벤트는 공개할 수 있다")
    void publishDraft() {
        DropEvent event = draft();

        event.publish();

        assertThat(event.getStatus()).isEqualTo(DropEventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("이미 공개한 이벤트는 다시 공개할 수 없다")
    void rejectsPublishingTwice() {
        DropEvent event = draft();
        event.publish();

        assertThatThrownBy(event::publish)
                .isInstanceOf(IllegalStateException.class);
        assertThat(event.getStatus()).isEqualTo(DropEventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("취소한 이벤트는 공개할 수 없다")
    void rejectsPublishingCanceled() {
        DropEvent event = draft();
        event.cancel();

        assertThatThrownBy(event::publish)
                .isInstanceOf(IllegalStateException.class);
        assertThat(event.getStatus()).isEqualTo(DropEventStatus.CANCELED);
    }

    @Test
    @DisplayName("초안 이벤트는 취소할 수 있다")
    void cancelDraft() {
        DropEvent event = draft();

        event.cancel();

        assertThat(event.getStatus()).isEqualTo(DropEventStatus.CANCELED);
    }

    @Test
    @DisplayName("공개한 이벤트는 취소할 수 있다")
    void cancelPublished() {
        DropEvent event = draft();
        event.publish();

        event.cancel();

        assertThat(event.getStatus()).isEqualTo(DropEventStatus.CANCELED);
    }

    @Test
    @DisplayName("이미 취소한 이벤트는 다시 취소할 수 없다")
    void rejectsCancelingTwice() {
        DropEvent event = draft();
        event.cancel();

        assertThatThrownBy(event::cancel)
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("phaseBoundaries")
    @DisplayName("단계는 [오픈, 종료) 반열린 구간으로 계산한다")
    void phaseFollowsHalfOpenPeriod(String description, Instant now, DropPhase expected) {
        DropEvent event = draft();

        assertThat(event.phase(now)).isEqualTo(expected);
    }

    static Stream<Arguments> phaseBoundaries() {
        return Stream.of(
                Arguments.of("오픈 1나노초 전이면 오픈 전", OPEN_AT.minusNanos(1), DropPhase.UPCOMING),
                Arguments.of("정확히 오픈 시각이면 진행 중", OPEN_AT, DropPhase.OPEN),
                Arguments.of("종료 1나노초 전이면 진행 중", CLOSE_AT.minusNanos(1), DropPhase.OPEN),
                Arguments.of("정확히 종료 시각이면 종료", CLOSE_AT, DropPhase.CLOSED)
        );
    }

    private static DropEvent draft() {
        return DropEvent.create("커피 드롭", "아메리카노 쿠폰", OPEN_AT, CLOSE_AT, NOW);
    }
}
