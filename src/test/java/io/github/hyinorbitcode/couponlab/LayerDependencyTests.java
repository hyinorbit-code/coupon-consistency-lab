package io.github.hyinorbitcode.couponlab;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class LayerDependencyTests {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.hyinorbitcode.couponlab");

    @Test
    @DisplayName("웹 계층 타입(controller, dto)은 웹 계층 안에서만 사용한다")
    void webLayerTypesStayInWebLayer(){
        noClasses().that()
                .resideOutsideOfPackages("..controller..", "..dto..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..dto..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    @DisplayName("영속성 계층 타입(entity,repository)은 service를 참조하지 않는다")
    void persistenceDoesNotDependOnServices(){
        noClasses().that()
                .resideInAnyPackage("..entity..","..repository..")
                .should().dependOnClassesThat().resideInAPackage("..service..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    @DisplayName("entity는 repository를 참조하지 않는다")
    void entitiesDoNotDependOnRepositories(){
        noClasses().that()
                .resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    @DisplayName("global은 도메인 모듈을 참조하지 않는다")
    void globalDoesNotDependOnDomainModules(){
        noClasses().that()
                .resideInAPackage("..couponlab.global..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..couponlab.user..","..couponlab.drop..","..couponlab.prizecode..", "..couponlab.participation..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }



}
