package io.github.hyinorbitcode.couponlab;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 도메인 모듈 안의 계층 의존 방향(ADR-0014). 모듈 간 경계는 Spring Modulith가, 모듈 안의 방향은 이 테스트가 검사한다.
 * 방향: controller → service → repository·entity. dto는 controller에서만, global은 어디서나 쓸 수 있다.
 */
class LayerDependencyTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.hyinorbitcode.couponlab");

    @Test
    @DisplayName("controller와 dto 밖에서는 controller·dto를 참조하지 않는다")
    void onlyControllersUseControllersAndDtos() {
        noClasses().that().resideOutsideOfPackages("..controller..", "..dto..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..dto..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("entity·repository는 service를 참조하지 않는다")
    void persistenceDoesNotDependOnServices() {
        noClasses().that().resideInAnyPackage("..entity..", "..repository..")
                .should().dependOnClassesThat().resideInAPackage("..service..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("entity는 repository를 참조하지 않는다")
    void entitiesDoNotDependOnRepositories() {
        noClasses().that().resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(CLASSES);
    }

    @Test
    @DisplayName("global은 도메인 모듈을 참조하지 않는다")
    void globalDoesNotDependOnDomainModules() {
        noClasses().that().resideInAPackage("..couponlab.global..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..couponlab.user..", "..couponlab.drop..", "..couponlab.prizecode..",
                        "..couponlab.participation..")
                .check(CLASSES);
    }
}
