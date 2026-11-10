package io.github.hyinorbitcode.couponlab.prizecode.dto.response;

import io.github.hyinorbitcode.couponlab.prizecode.service.CodeRegistrationResult;

public record RegisterCodesResponse(int registered, int duplicates) {

    public static RegisterCodesResponse from(CodeRegistrationResult result) {
        return new RegisterCodesResponse(result.registered(), result.duplicates());
    }
}