package io.github.hyinorbitcode.couponlab.participation.service;

import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.drop.service.DropService;
import io.github.hyinorbitcode.couponlab.prizecode.exception.SoldOutException;
import io.github.hyinorbitcode.couponlab.prizecode.service.PrizeCodeService;
import io.github.hyinorbitcode.couponlab.user.Role;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.repository.UserRepository;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * 응모 스트레스 측정. 결과를 출력만 하고 단정하지 않는다.
 * STRESS=true 환경변수가 있을 때만 실행한다: STRESS=true ./gradlew concurrencyTest --tests '*EntryStressTests'
 */
@Tag("concurrency")
@IntegrationTest
@EnabledIfEnvironmentVariable(named = "STRESS", matches = "true")
class EntryStressTests {

    @Autowired ParticipationService participationService;
    @Autowired DropService dropService;
    @Autowired PrizeCodeService prizeCodeService;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired DataSource dataSource;

    @Test
    void stress() throws Exception {
        int pool = dataSource instanceof HikariDataSource h ? h.getMaximumPoolSize() : -1;
        System.out.println("STRESS env | hikariMaxPool=" + pool + " cpus=" + Runtime.getRuntime().availableProcessors());

        scenario("S1", 1_000, 100, 50, false);
        scenario("S2", 3_000, 300, 100, false);
        scenario("S3", 5_000, 500, 100, false);
        scenario("S4", 5_000, 50, 100, false);
        scenario("S5", 500, 100, 100, true);
        scenario("S3-repeat", 5_000, 500, 100, false);
    }

    private void scenario(String name, int requests, int codes, int threads, boolean sameUser) throws Exception {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        long dropId = openDrop(codes);
        List<Long> userIds = sameUser ? Collections.nCopies(requests, createUsers(1).getFirst()) : createUsers(requests);

        List<Result> results = new ArrayList<>(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result>> futures = new ArrayList<>(requests);
        long wallStart;
        long wallEnd;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            for (Long userId : userIds) {
                futures.add(executor.submit(() -> {
                    start.await();
                    long t0 = System.nanoTime();
                    try {
                        EntryResult r = participationService.enter(dropId, userId);
                        return new Result(userId, r.prize().prizeCodeId(), r.newlyWon(), null, System.nanoTime() - t0);
                    } catch (Exception e) {
                        return new Result(userId, null, false, e, System.nanoTime() - t0);
                    }
                }));
            }
            wallStart = System.nanoTime();
            start.countDown();
            for (Future<Result> f : futures) results.add(f.get());
            wallEnd = System.nanoTime();
        }

        long newWins = results.stream().filter(r -> r.error == null && r.newlyWon).count();
        long repeats = results.stream().filter(r -> r.error == null && !r.newlyWon).count();
        long soldOut = results.stream().filter(r -> r.error instanceof SoldOutException).count();
        Map<String, Long> otherErrors = new TreeMap<>();
        results.stream().filter(r -> r.error != null && !(r.error instanceof SoldOutException))
                .forEach(r -> otherErrors.merge(classify(r.error), 1L, Long::sum));

        long distinctCodes = results.stream().filter(r -> r.error == null).map(r -> r.codeId).distinct().count();
        long mismatched = results.stream().filter(r -> r.error == null).filter(r -> {
            Long owner = jdbcTemplate.queryForObject("SELECT assigned_user_id FROM prize_codes WHERE id = ?", Long.class, r.codeId);
            return !Objects.equals(owner, r.userId);
        }).count();
        int dbAssigned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM prize_codes WHERE drop_event_id = ? AND assigned_user_id IS NOT NULL", Integer.class, dropId);

        long[] lat = results.stream().mapToLong(r -> r.nanos).sorted().toArray();
        double wallMs = (wallEnd - wallStart) / 1e6;
        System.out.printf(Locale.ROOT,
                "STRESS %s | requests=%d codes=%d threads=%d sameUser=%s | wallMs=%.0f throughput=%.0f/s | p50=%.1fms p95=%.1fms p99=%.1fms max=%.1fms | newWins=%d repeats=%d soldOut=%d otherErrors=%s | distinctCodes=%d mismatched=%d dbAssigned=%d%n",
                name, requests, codes, threads, sameUser, wallMs, requests / (wallMs / 1000.0),
                ms(lat, 0.50), ms(lat, 0.95), ms(lat, 0.99), lat[lat.length - 1] / 1e6,
                newWins, repeats, soldOut, otherErrors, distinctCodes, mismatched, dbAssigned);
    }

    private static String classify(Throwable e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("Deadlock")) return "Deadlock";
        if (msg.contains("Lock wait timeout")) return "LockWaitTimeout";
        return e.getClass().getSimpleName();
    }

    private static double ms(long[] sorted, double q) {
        int idx = (int) Math.ceil(q * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))] / 1e6;
    }

    private long openDrop(int codes) {
        long dropId = dropService.create("스트레스", "쿠폰",
                Instant.parse("2099-01-01T00:00:00Z"), Instant.parse("2099-01-01T01:00:00Z")).getId();
        List<String> all = IntStream.range(0, codes).mapToObj(i -> "S-" + dropId + "-" + i).toList();
        for (int i = 0; i < all.size(); i += 1000) {
            prizeCodeService.register(dropId, all.subList(i, Math.min(i + 1000, all.size())));
        }
        dropService.publish(dropId);
        jdbcTemplate.update("UPDATE drop_events SET open_at = '2000-01-01 00:00:00' WHERE id = ?", dropId);
        return dropId;
    }

    private List<Long> createUsers(int count) {
        Instant now = Instant.now();
        List<User> users = IntStream.range(0, count)
                .mapToObj(i -> User.createWithoutCredentials("s" + i, Role.USER, now)).toList();
        return userRepository.saveAll(users).stream().map(User::getId).toList();
    }

    private record Result(Long userId, Long codeId, boolean newlyWon, Throwable error, long nanos) {}
}
