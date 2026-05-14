package CATTO.config;

import CATTO.main.Main;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigValidator {

    private ConfigValidator() {}

    /**
     * Validates the parsed config before Soot is started.
     * Throws {@link ConfigValidationException} if any required field is missing or its
     * directory does not exist on disk. All errors are collected and reported together.
     */
    public static void validate(Path projectPath, Configurator config) throws ConfigValidationException {
        List<String> errors = new ArrayList<>();

        validateTempFolder(projectPath, config, errors);
        validateOutputPaths(projectPath, config, errors);

        if (!errors.isEmpty()) {
            throw new ConfigValidationException(String.join("\n", errors));
        }
    }

    private static void validateTempFolder(Path projectPath, Configurator config, List<String> errors) {
        String temp = config == null ? null : config.getTempFolderPath();
        if (temp == null || temp.isBlank()) {
            errors.add("tempFolderPath: missing or blank in config.yaml");
            return;
        }
        Path resolved = Main.resolveConfiguredPath(projectPath, temp);
        if (!resolved.toFile().isDirectory()) {
            errors.add("tempFolderPath: directory does not exist: " + resolved);
        }
    }

    private static void validateOutputPaths(Path projectPath, Configurator config, List<String> errors) {
        List<String> outputs = config == null ? null : config.getOutputPath();
        if (outputs == null || outputs.isEmpty()) {
            errors.add("outputPath: missing or empty in config.yaml — at least one class directory is required");
            return;
        }
        for (String output : outputs) {
            Path resolved = Main.resolveConfiguredPath(projectPath, output);
            if (!resolved.toFile().isDirectory()) {
                errors.add("outputPath: directory does not exist: " + resolved);
            }
        }
    }
}
