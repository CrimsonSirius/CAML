package org.chs.camlauncher.backend.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProjectDependencyServiceTest {

    @Test
    void listsRuntimeDependenciesWithAuthorAndLicense() {
        List<ProjectDependency> dependencies = new ProjectDependencyService().dependencies();

        assertEquals(6, dependencies.size());
        assertEquals("Gson", dependencies.get(2).name());
        assertEquals("Google", dependencies.get(2).author());
        assertEquals("Apache-2.0", dependencies.get(2).license());
        assertFalse(dependencies.stream().anyMatch(dependency ->
                dependency.name().contains("JUnit")));
    }
}
