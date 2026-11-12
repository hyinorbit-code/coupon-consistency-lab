package io.github.hyinorbitcode.couponlab.participation.service;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.drop.service.DropService;
import io.github.hyinorbitcode.couponlab.prizecode.exception.SoldOutException;
import io.github.hyinorbitcode.couponlab.prizecode.service.PrizeCodeService;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.IdentityProvider;
import io.github.hyinorbitcode.couponlab.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("concurrency")
@IntegrationTest
class EntryConcurrencyTests {

    private static final int USERS = 50;
    private static final int CODES = 10;
    private static final int SAME_USER_REQUESTS = 10;

    @Autowired
    ParticipationService participationService;

    @Autowired
    DropService dropService;

    @Autowired
    PrizeCodeService prizeCodeService;

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
    }

    @Test
    @DisplayName("서로 다른 사용자가 동시에 응모해도 당첨자는 코드 수만큼이고, 각 당첨자는 자기에게 할당된 서로 다른 코드를 받는다")
    void concurrentEntriesNeverShareCodes() throws Exception {
        long dropId = openDropWithCodes(CODES);
        List<Long> userIds = IntStream.range(0, USERS)
                .mapToObj(i -> createUser("user-" + i))
                .toList();

        List<Attempt> attempts = enterConcurrently(dropId, userIds);

        List<Attempt> winners = attempts.stream().filter(Attempt::won).toList();
        List<Attempt> failures = attempts.stream().filter(a -> !a.won()).toList();

        assertThat(failures)
                .as("매진 말고 다른 이유로 실패한 요청")
                .allSatisfy(a -> assertThat(a.error()).isInstanceOf(SoldOutException.class));
        assertThat(winners).as("당첨 응답 수").hasSize(CODES);
        assertThat(winners.stream().map(a -> a.result().prize().prizeCodeId()).distinct())
                .as("당첨자에게 나간 서로 다른 코드 수")
                .hasSize(CODES);
        assertThat(winners)
                .as("받은 코드가 DB에서도 본인에게 할당되어 있어야 한다")
                .allSatisfy(a -> assertThat(ownerOf(a.result().prize().prizeCodeId())).isEqualTo(a.userId()));
        assertThat(assignedCount(dropId)).as("DB에 할당된 코드 수").isEqualTo(CODES);
    }

    @Test
    @DisplayName("같은 사용자가 동시에 여러 번 응모해도 코드 하나만 할당되고, 모든 요청이 같은 코드를 받으며, 새 당첨은 한 번뿐이다")
    void concurrentEntriesBySameUserAssignOnce() throws Exception {
        long dropId = openDropWithCodes(CODES);
        long userId = createUser("same-user");

        List<Attempt> attempts = enterConcurrently(dropId, Collections.nCopies(SAME_USER_REQUESTS, userId));

        assertThat(attempts).as("실패한 요청").allSatisfy(a -> assertThat(a.error()).isNull());
        assertThat(attempts.stream().map(a -> a.result().prize().prizeCodeId()).distinct())
                .as("받은 코드 종류")
                .hasSize(1);
        assertThat(attempts.stream().filter(a -> a.result().newlyWon()))
                .as("새 당첨(201) 응답 수")
                .hasSize(1);
        assertThat(assignedCount(dropId)).as("DB에 할당된 코드 수").isEqualTo(1);
    }

    private List<Attempt> enterConcurrently(long dropId, List<Long> userIds) throws Exception {
        CountDownLatch ready = new CountDownLatch(userIds.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<EntryResult>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(userIds.size())) {
            for (Long userId : userIds) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return participationService.enter(dropId, userId);
                }));
            }
            ready.await();
            start.countDown();
        }

        List<Attempt> attempts = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                attempts.add(new Attempt(userIds.get(i), futures.get(i).get(), null));
            } catch (ExecutionException e) {
                attempts.add(new Attempt(userIds.get(i), null, e.getCause()));
            }
        }
        return attempts;
    }

    /** 미래 시각으로 만들고 코드를 등록해 공개한 뒤, 오픈 시각을 과거로 옮겨 진행 중으로 만든다. */
    private long openDropWithCodes(int codes) {
        long dropId = dropService.create("동시 응모", "아메리카노 쿠폰",
                Instant.parse("2099-01-01T00:00:00Z"), Instant.parse("2099-01-01T01:00:00Z")).getId();
        prizeCodeService.register(dropId,
                IntStream.range(0, codes).mapToObj(i -> "COFFEE-" + dropId + "-" + i).toList());
        dropService.publish(dropId);
        jdbcTemplate.update("UPDATE drop_events SET open_at = '2000-01-01 00:00:00' WHERE id = ?", dropId);
        return dropId;
    }

    private long createUser(String providerUserId) {
        return userService.findOrCreate(IdentityProvider.EXPERIMENT, providerUserId, providerUserId, Role.USER).getId();
    }

    private Long ownerOf(long prizeCodeId) {
        return jdbcTemplate.queryForObject(
                "SELECT assigned_user_id FROM prize_codes WHERE id = ?", Long.class, prizeCodeId);
    }

    private int assignedCount(long dropId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prize_codes WHERE drop_event_id = ? AND assigned_user_id IS NOT NULL",
                Integer.class, dropId);
        return count == null ? 0 : count;
    }

    private record Attempt(long userId, EntryResult result, Throwable error) {
        boolean won() {
            return error == null;
        }
    }
}
