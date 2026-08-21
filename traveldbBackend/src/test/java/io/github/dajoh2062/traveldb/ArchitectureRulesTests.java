package io.github.dajoh2062.traveldb;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTests {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("io.github.dajoh2062.traveldb");

    @Test
    void controllersDoNotAccessRepositoriesDirectly() {
        noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void controllersDoNotAccessJdbcDirectly() {
        noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.jdbc..", "javax.sql..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void repositoriesDoNotDependOnApiOrServices() {
        noClasses().that().areAnnotatedWith(Repository.class)
                .should().dependOnClassesThat()
                .resideInAnyPackage("..api..", "..service..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void servicesDoNotAccessJdbcDirectly() {
        noClasses().that().areAnnotatedWith(Service.class)
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.jdbc..", "javax.sql..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void startupAdaptersDoNotAccessRepositoriesDirectly() {
        noClasses().that().implement(ApplicationRunner.class)
                .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
                .check(PRODUCTION_CLASSES);
    }
}
