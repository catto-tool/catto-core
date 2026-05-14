package CATTO.main;

import CATTO.api.AnalysisRequest;
import CATTO.api.AnalysisResult;
import CATTO.api.CattoAnalyzer;
import CATTO.cli.ResultSerializer;
import CATTO.config.ConfigValidationException;
import CATTO.config.ConfigValidator;
import CATTO.config.ConfigWrapper;
import CATTO.config.Configurator;
import CATTO.exception.AnalysisTimeoutException;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    private static final String IDENTIFIED_TESTS_FILE = "identified_tests.txt";

    public static void main(String[] args) throws InvalidTargetPaths, NoTestFoundedException, IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, ConfigValidationException, AnalysisTimeoutException {
        System.exit(run(args));
    }

    public static int run(String[] args) throws InvalidTargetPaths, NoTestFoundedException, IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, ConfigValidationException, AnalysisTimeoutException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing project path argument");
        }

        BasicConfigurator.configure();

        Path projectPath = Paths.get(args[0]).toAbsolutePath().normalize();
        ConfigWrapper ini = new ConfigWrapper(projectPath.toString());
        Configurator configurator = ini.getCONFIG();

        ConfigValidator.validate(projectPath, configurator);

        String tempFolder = configurator.getTempFolderPath();
        List<String> dependencies = configurator.getDependencies() == null ? Collections.emptyList() : configurator.getDependencies();
        List<String> outputPaths = configurator.getOutputPath() == null ? Collections.emptyList() : configurator.getOutputPath();

        List<Path> dependencyPaths = new ArrayList<>();
        for (String path : dependencies) {
            File dependencyPath = resolveConfiguredPath(projectPath, path).toFile();
            if (dependencyPath.isDirectory()) {
                List<File> files = (List<File>) FileUtils.listFiles(dependencyPath, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
                for (File f : files) {
                    for (String jar : listf(f.getAbsolutePath())) {
                        dependencyPaths.add(Paths.get(jar));
                    }
                }
            } else {
                dependencyPaths.add(dependencyPath.toPath());
            }
        }

        List<Path> newClassesPaths = new ArrayList<>();
        for (String path : outputPaths) {
            newClassesPaths.add(resolveConfiguredPath(projectPath, path));
        }

        AnalysisRequest request = AnalysisRequest.builder()
                .previousClassesPath(resolveConfiguredPath(projectPath, tempFolder))
                .newClassesPaths(newClassesPaths)
                .dependencies(dependencyPaths)
                .build();

        AnalysisResult result = CattoAnalyzer.analyze(request);

        File identifiedTestsFile = projectPath.resolve(IDENTIFIED_TESTS_FILE).toFile();
        try (FileWriter writer = new FileWriter(identifiedTestsFile)) {
            for (String testName : result.selectedTests()) {
                writer.write(testName + "\n");
            }
        }

        File jsonFile = projectPath.resolve("identified_tests.json").toFile();
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(ResultSerializer.toJson(result));
        }

        return result.exitCode();
    }

    public static Path resolveConfiguredPath(Path projectPath, String path) {
        Path configuredPath = Paths.get(path);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }
        return projectPath.resolve(configuredPath).normalize();
    }

    public static List<String> listf(String directoryName) {
        File directory = new File(directoryName);
        List<String> files = new ArrayList<>();

        // Get all files from a directory.
        if (directory.isFile()) {
            if (directory.getName().endsWith(".jar"))
                files.add(directory.getAbsolutePath());
        } else {
            File[] fList = directory.listFiles();
            if (fList != null)
                for (File file : fList) {
                    if (file.isFile()) {
                        if (file.getName().endsWith(".jar"))
                            files.add(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        files.addAll(listf(file.getAbsolutePath()));
                    }
                }
        }
        return files;
    }
}
