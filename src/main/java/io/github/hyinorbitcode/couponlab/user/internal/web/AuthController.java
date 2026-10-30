package io.github.hyinorbitcode.couponlab.user.internal.web;

import io.github.hyinorbitcode.couponlab.user.internal.application.EmailVerificationService;
import io.github.hyinorbitcode.couponlab.user.internal.application.SignupService;
import io.github.hyinorbitcode.couponlab.user.internal.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 전 사용자가 쓰는 인증 절차 API(/api/auth/**, 인증 불필요). 컨트롤러는 URL 단위로 묶고
 * 로직은 유스케이스별 서비스에 둔다(ADR-0008). 가입·재발송은 계정 존재 여부와 관계없이 같은 202 응답을 준다(ADR-0012).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private static final MessageResponse CHECK_YOUR_MAIL =
            new MessageResponse("입력한 이메일로 안내 메일을 보냈습니다. 메일함을 확인해 주세요.");

    private final SignupService signupService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse signUp(@Valid @RequestBody SignupRequest request) {
        signupService.signUp(request.email(), request.password(), request.nickname());
        return CHECK_YOUR_MAIL;
    }

    @PostMapping("/verify-email")
    MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return new MessageResponse("이메일 인증이 완료되었습니다.");
    }

    @PostMapping("/verify-email/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    MessageResponse resend(@Valid @RequestBody ResendRequest request) {
        emailVerificationService.resend(request.email());
        return CHECK_YOUR_MAIL;
    }

    record SignupRequest(
            @NotBlank @Email @Size(max = User.MAX_EMAIL_LENGTH) String email,
            @NotNull @Size(min = SignupService.MIN_PASSWORD_LENGTH, max = SignupService.MAX_PASSWORD_LENGTH)
            String password,
            @NotBlank @Size(max = User.MAX_NICKNAME_LENGTH) String nickname) {

        /** 앞뒤 공백은 검증 전에 지운다. 대소문자 정규화는 서비스에서 한다. */
        SignupRequest {
            email = strip(email);
        }
    }

    record VerifyEmailRequest(@NotBlank String token) {
    }

    record ResendRequest(@NotBlank @Email @Size(max = User.MAX_EMAIL_LENGTH) String email) {

        ResendRequest {
            email = strip(email);
        }
    }

    record MessageResponse(String message) {
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
