package fr.traqueur.nexus.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * The compiled application, imported once for every architecture rule.
 *
 * <p>These rules live in {@code nexus-bootstrap} because it is the only module
 * that sees all the others, so a single import covers the whole application.
 *
 * <p>Tests are excluded on purpose. A test may legitimately declare a context
 * type without registering it, or reach across packages to set up a fixture;
 * holding test code to the production layering would produce noise, not safety.
 */
final class NexusClasses {

    static final String ROOT = "fr.traqueur.nexus";

    static final String DOMAIN = "fr.traqueur.nexus.domain..";
    static final String APPLICATION = "fr.traqueur.nexus.application..";
    static final String INFRASTRUCTURE = "fr.traqueur.nexus.infrastructure..";
    static final String BOOTSTRAP = "fr.traqueur.nexus.bootstrap..";

    /** The JDK, and nothing else third-party. */
    static final String JDK = "java..";

    /*
     * Jars are deliberately included: from here, every other module is a jar on
     * the test classpath, so excluding them would import almost nothing and every
     * rule would pass vacuously.
     */
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private NexusClasses() {
    }

    static JavaClasses get() {
        return CLASSES;
    }
}