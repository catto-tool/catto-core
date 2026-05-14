package CATTO.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ConfigValidatorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // ---- valid config ----

    @Test
    public void validConfigDoesNotThrow() throws Exception {
        File prevDir   = tmp.newFolder("prev");
        File outputDir = tmp.newFolder("output");
        Configurator cfg = configurator(prevDir.getAbsolutePath(), List.of(outputDir.getAbsolutePath()), null);

        ConfigValidator.validate(tmp.getRoot().toPath(), cfg);
        // no exception → pass
    }

    // ---- outputPath validation ----

    @Test
    public void throwsWhenOutputPathIsNull() {
        Configurator cfg = configurator(".tmp", null, null);
        ConfigValidationException ex = assertThrows(
                "null outputPath must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("outputPath"));
    }

    @Test
    public void throwsWhenOutputPathIsEmptyList() {
        Configurator cfg = configurator(".tmp", Collections.emptyList(), null);
        ConfigValidationException ex = assertThrows(
                "empty outputPath must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("outputPath"));
    }

    @Test
    public void throwsWhenOutputPathDirectoryDoesNotExist() {
        Configurator cfg = configurator(".tmp", List.of("/nonexistent/output/dir"), null);
        ConfigValidationException ex = assertThrows(
                "non-existent outputPath directory must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("outputPath") || ex.getMessage().contains("nonexistent"));
    }

    // ---- tempFolderPath validation ----

    @Test
    public void throwsWhenTempFolderPathIsNull() {
        Configurator cfg = configurator(null, List.of(tmp.getRoot().getAbsolutePath()), null);
        ConfigValidationException ex = assertThrows(
                "null tempFolderPath must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("tempFolderPath"));
    }

    @Test
    public void throwsWhenTempFolderPathIsBlank() {
        Configurator cfg = configurator("  ", List.of(tmp.getRoot().getAbsolutePath()), null);
        ConfigValidationException ex = assertThrows(
                "blank tempFolderPath must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("tempFolderPath"));
    }

    @Test
    public void throwsWhenTempFolderDirectoryDoesNotExist() throws Exception {
        File outputDir = tmp.newFolder("output");
        Configurator cfg = configurator("/nonexistent/.tmp", List.of(outputDir.getAbsolutePath()), null);
        ConfigValidationException ex = assertThrows(
                "non-existent tempFolderPath directory must be rejected",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue(ex.getMessage().contains("tempFolderPath") || ex.getMessage().contains("nonexistent"));
    }

    // ---- accumulates multiple errors ----

    @Test
    public void messageContainsAllErrors() {
        Configurator cfg = configurator(null, null, null);
        ConfigValidationException ex = assertThrows(
                "both null outputPath and null tempFolderPath must appear in message",
                ConfigValidationException.class,
                () -> ConfigValidator.validate(tmp.getRoot().toPath(), cfg));
        assertTrue("message must mention outputPath: " + ex.getMessage(),
                ex.getMessage().contains("outputPath"));
        assertTrue("message must mention tempFolderPath: " + ex.getMessage(),
                ex.getMessage().contains("tempFolderPath"));
    }

    // ---- helpers ----

    private static <T extends Throwable> T assertThrows(String msg, Class<T> type, ThrowingRunnable r) {
        try {
            r.run();
            fail(msg + " — expected " + type.getSimpleName() + " but nothing was thrown");
            return null;
        } catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            throw new AssertionError(msg + " — expected " + type.getSimpleName() + " but got " + t, t);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }

    private static Configurator configurator(String tempFolderPath, List<String> outputPath, List<String> deps) {
        Configurator cfg = new Configurator();
        cfg.setTempFolderPath(tempFolderPath);
        cfg.setOutputPath(outputPath);
        cfg.setDependencies(deps);
        return cfg;
    }
}
