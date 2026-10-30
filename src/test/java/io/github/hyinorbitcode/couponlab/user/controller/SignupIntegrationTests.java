package io.github.hyinorbitcode.couponlab.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.hyinorbitcode.couponlab.CapturingMailSender;
import io.github.hyinorbitcode.couponlab.CapturingMailSender.MailType;
import io.github.hyinorbitcode.couponlab.CapturingMailSender.SentMail;
import io.github.hyinorbitcode.couponlab.DatabaseCleaner;
import io.github.hyinorbitcode.couponlab.IntegrationTest;
import io.github.hyinorbitcode.couponlab.user.entity.User;
import io.github.hyinorbitcode.couponlab.user.event.SignupMailEvents.VerificationMailRequested;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class SignupIntegrationTests {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "correct-horse-battery";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    CapturingMailSender mailSender;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        DatabaseCleaner.truncateAll(jdbcTemplate);
        mailSender.clear();
    }

    @Test
    @DisplayName("가입하면 인증 대기 사용자가 생기고 비밀번호는 해시로 저장되며 인증 메일이 발송된다")
    void signUpCreatesPendingUserAndSendsVerificationMail() throws Exception {
        signUp(EMAIL, PASSWORD, "가입자")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        Map<String, Object> user = findUser(EMAIL);
        assertThat(user).containsEntry("status", "PENDING_VERIFICATION");
        assertThat(user.get("email_verified_at")).isNull();
        assertThat((String) user.get("password_hash")).startsWith("{bcrypt}").doesNotContain(PASSWORD);

        assertThat(mailSender.sentOf(MailType.VERIFICATION)).singleElement()
                .satisfies(mail -> {
                    assertThat(mail.to()).isEqualTo(EMAIL);
                    assertThat(mail.link()).startsWith("http://localhost:5173/verify-email?token=");
                });
        assertThat(countTokens()).isEqualTo(1);
    }

    @Test
    @DisplayName("메일의 토큰으로 인증하면 활성화되고, 같은 토큰은 다시 쓸 수 없다")
    void verifiesEmailOnceWithToken() throws Exception {
        signUp(EMAIL, PASSWORD, "가입자");
        String token = onlyVerificationMail().token();

        verify(token)
                .andExpect(status().isOk());
        Map<String, Object> user = findUser(EMAIL);
        assertThat(user).containsEntry("status", "ACTIVE");
        assertThat(user.get("email_verified_at")).isNotNull();

        verify(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("만료된 토큰은 400을 받고 사용자는 인증 대기 상태로 남는다")
    void rejectsExpiredToken() throws Exception {
        signUp(EMAIL, PASSWORD, "가입자");
        String token = onlyVerificationMail().token();
        jdbcTemplate.update("UPDATE email_verification_tokens SET expires_at = UTC_TIMESTAMP(6) - INTERVAL 1 MINUTE");

        verify(token).andExpect(status().isBadRequest());
        assertThat(findUser(EMAIL)).containsEntry("status", "PENDING_VERIFICATION");
    }

    @Test
    @DisplayName("존재하지 않는 토큰은 400을 받는다")
    void rejectsUnknownToken() throws Exception {
        verify("not-a-real-token").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미 인증된 이메일로 다시 가입하면 같은 202 응답을 주고 안내 메일만 보낸다")
    void signUpWithRegisteredEmailSendsNoticeOnly() throws Exception {
        signUp(EMAIL, PASSWORD, "가입자");
        verify(onlyVerificationMail().token());
        String originalHash = (String) findUser(EMAIL).get("password_hash");
        mailSender.clear();

        signUp(EMAIL, "another-password", "다른사람")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        assertThat(mailSender.sent()).singleElement()
                .extracting(SentMail::type).isEqualTo(MailType.ALREADY_REGISTERED);
        assertThat(countUsers()).isEqualTo(1);
        assertThat(findUser(EMAIL)).containsEntry("password_hash", originalHash).containsEntry("nickname", "가입자");
    }

    @Test
    @DisplayName("인증 대기 중 다시 가입하면 간격 안에서는 메일을 보내지 않고, 간격이 지나면 새 인증 메일을 보낸다")
    void signUpWhilePendingRespectsResendInterval() throws Exception {
        signUp(EMAIL, PASSWORD, "가입자");
        mailSender.clear();

        signUp(EMAIL, PASSWORD, "가입자").andExpect(status().isAccepted());
        assertThat(mailSender.sent()).isEmpty();

        jdbcTemplate.update("UPDATE email_verification_tokens SET created_at = UTC_TIMESTAMP(6) - INTERVAL 2 MINUTE");
        signUp(EMAIL, PASSWORD, "가입자").andExpect(status().isAccepted());
        assertThat(mailSender.sentOf(MailType.VERIFICATION)).hasSize(1);
        assertThat(countUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("이메일은 대소문자와 앞뒤 공백을 무시하고 같은 계정으로 본다")
    void normalizesEmail() throws Exception {
        signUp("  User@Example.COM ", PASSWORD, "가입자").andExpect(status().isAccepted());
        signUp(EMAIL, PASSWORD, "가입자").andExpect(status().isAccepted());

        assertThat(countUsers()).isEqualTo(1);
        assertThat(findUser(EMAIL)).isNotNull();
    }

    @Test
    @DisplayName("이메일 형식이 틀리거나 비밀번호가 8자보다 짧으면 400을 받는다")
    void rejectsInvalidInput() throws Exception {
        signUp("not-an-email", PASSWORD, "가입자").andExpect(status().isBadRequest());
        signUp(EMAIL, "short", "가입자").andExpect(status().isBadRequest());

        assertThat(countUsers()).isZero();
        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    @DisplayName("길이는 맞지만 공백뿐인 비밀번호는 400 INVALID_PASSWORD와 구체적인 메시지를 받는다")
    void rejectsBlankPasswordWithSpecificMessage() throws Exception {
        signUp(EMAIL, " ".repeat(10), "가입자")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.detail").value("비밀번호를 입력해 주세요."));

        assertThat(countUsers()).isZero();
    }

    @Test
    @DisplayName("없는 이메일로 재발송을 요청해도 같은 202 응답을 주고 메일은 보내지 않는다")
    void resendForUnknownEmailRevealsNothing() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@example.com"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    @DisplayName("메일 요청 이벤트는 트랜잭션이 롤백되면 메일이 되지 않는다")
    void doesNotSendMailWhenTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new VerificationMailRequested(EMAIL, "가입자", "token"));
            status.setRollbackOnly();
        });
        assertThat(mailSender.sent()).isEmpty();

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new VerificationMailRequested(EMAIL, "가입자", "token")));
        assertThat(mailSender.sent()).hasSize(1);
    }

    private ResultActions signUp(String email, String password, String nickname) throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "nickname": "%s"}
                        """.formatted(email, password, nickname)));
    }

    private ResultActions verify(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s"}
                        """.formatted(token)));
    }

    private SentMail onlyVerificationMail() {
        assertThat(mailSender.sentOf(MailType.VERIFICATION)).hasSize(1);
        return mailSender.sentOf(MailType.VERIFICATION).getFirst();
    }

    private Map<String, Object> findUser(String email) {
        return jdbcTemplate.queryForMap("SELECT * FROM users WHERE email = ?", email);
    }

    private Integer countUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }

    private Integer countTokens() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verification_tokens", Integer.class);
    }
}
