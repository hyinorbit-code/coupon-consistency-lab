package io.github.hyinorbitcode.couponlab;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 모듈 안의 역할 패키지 의존 방향(ADR-0008). 모듈 간 경계는 Spring Modulith가, 모듈 안의 방향은 이 테스트가 검사한다.
 * 웹 DTO(web.dto)는 public이므로 서비스·도메인이 가져다 쓰지 못하게 여기서 막는다.
 */
class LayerDependencyTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.hyinorbitcode.couponlab");

    @Test
    @DisplayName("application·domain은 web(웹 DTO 포함)을 참조하지 않는다")
    void applicationAndDomainDoNotDependOnWeb() {
        noClasses().that().resideInAnyPackage("..internal.application..", "..internal.domain..")
                .should().dependOnClassesThat().resideInAPackage("..internal.web..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("domain은 application을 참조하지 않는다")
    void domainDoesNotDependOnApplication() {
        noClasses().that().resideInAPackage("..internal.domain..")
                .should().dependOnClassesThat().resideInAPackage("..internal.application..")
                .check(CLASSES);
    }
}
