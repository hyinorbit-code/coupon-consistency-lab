package io.github.hyinorbitcode.couponlab;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    @DisplayName("모듈 간 의존 규칙을 위반하지 않는다")
    void verifiesModuleStructure() {
        ApplicationModules.of(CouponLabApplication.class).verify();
    }
}
