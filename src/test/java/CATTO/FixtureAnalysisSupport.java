package CATTO;

import CATTO.code.analyzer.CodeAnalyzer;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoNameException;
import CATTO.exception.NoPathException;
import CATTO.exception.NoTestFoundedException;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.Test;
import org.apache.log4j.BasicConfigurator;
import soot.SootClass;
import soot.SootMethod;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class FixtureAnalysisSupport {
    private static final String FIXTURE_ROOT = "whatTestProjectForTesting";

    private FixtureAnalysisSupport() {
    }

    static AnalysisResult analyzeFixture() throws NoPathException, IOException, NoTestFoundedException,
            NoNameException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            InvalidTargetPaths {
        BasicConfigurator.configure();

        PreviousProject previousProject = new PreviousProject(
                classPath(),
                FIXTURE_ROOT + File.separator + "out" + File.separator + "production" + File.separator + "p",
                FIXTURE_ROOT + File.separator + "out" + File.separator + "test" + File.separator + "p"
        );
        NewProject newProject = new NewProject(
                classPath(),
                FIXTURE_ROOT + File.separator + "out" + File.separator + "production" + File.separator + "p1",
                FIXTURE_ROOT + File.separator + "out" + File.separator + "test" + File.separator + "p1"
        );

        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(newProject, previousProject);
        codeAnalyzer.analyze();
        CATTO.test.selector.TestSelector selector = new CATTO.test.selector.TestSelector(
                newProject,
                codeAnalyzer.getDifferentMethods(),
                codeAnalyzer.getDifferentTest(),
                codeAnalyzer.getNewMethods(),
                codeAnalyzer.getDifferentObject()
        );

        return new AnalysisResult(previousProject, newProject, codeAnalyzer, selector.selectTest());
    }

    static AnalysisResult analyzeSources(SourceSet previousSources, SourceSet newSources) throws Exception {
        BasicConfigurator.configure();

        Path fixture = Files.createTempDirectory("catto-selection-contract-");
        Path previousProduction = Files.createDirectories(fixture.resolve("previous/production"));
        Path previousTest = Files.createDirectories(fixture.resolve("previous/test"));
        Path newProduction = Files.createDirectories(fixture.resolve("new/production"));
        Path newTest = Files.createDirectories(fixture.resolve("new/test"));

        compile(previousProduction, previousSources.production);
        compile(previousTest, previousSources.tests, previousProduction);
        compile(newProduction, newSources.production);
        compile(newTest, newSources.tests, newProduction);

        PreviousProject previousProject = new PreviousProject(
                classPath(),
                previousProduction.toString(),
                previousTest.toString()
        );
        NewProject newProject = new NewProject(
                classPath(),
                newProduction.toString(),
                newTest.toString()
        );

        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(newProject, previousProject);
        codeAnalyzer.analyze();
        CATTO.test.selector.TestSelector selector = new CATTO.test.selector.TestSelector(
                newProject,
                codeAnalyzer.getDifferentMethods(),
                codeAnalyzer.getDifferentTest(),
                codeAnalyzer.getNewMethods(),
                codeAnalyzer.getDifferentObject()
        );

        return new AnalysisResult(previousProject, newProject, codeAnalyzer, selector.selectTest());
    }

    private static String[] classPath() {
        return new String[0];
    }

    private static void compile(Path outputDirectory, Map<String, String> sources, Path... classPathEntries)
            throws IOException {
        if (sources.isEmpty()) {
            return;
        }

        Path sourceDirectory = Files.createTempDirectory("catto-selection-contract-src-");
        List<File> sourceFiles = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path sourceFile = sourceDirectory.resolve(source.getKey().replace('.', File.separatorChar) + ".java");
            Files.createDirectories(sourceFile.getParent());
            Files.writeString(sourceFile, source.getValue(), StandardCharsets.UTF_8);
            sourceFiles.add(sourceFile.toFile());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Java compiler not available. Run tests with a JDK, not a JRE.");
        }

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            String compilerClassPath = compilerClassPath(classPathEntries);
            List<String> options = Arrays.asList(
                    "-d", outputDirectory.toString(),
                    "-classpath", compilerClassPath
            );
            Boolean success = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    fileManager.getJavaFileObjectsFromFiles(sourceFiles)
            ).call();
            if (!Boolean.TRUE.equals(success)) {
                throw new AssertionError("Failed to compile dynamic CATTO fixture sources into " + outputDirectory);
            }
        }
    }

    private static String compilerClassPath(Path... classPathEntries) {
        List<String> entries = new ArrayList<>();
        entries.add(System.getProperty("java.class.path", ""));
        for (Path classPathEntry : classPathEntries) {
            entries.add(classPathEntry.toString());
        }
        return String.join(File.pathSeparator, entries);
    }

    static final class SourceSet {
        final Map<String, String> production;
        final Map<String, String> tests;

        SourceSet(Map<String, String> production, Map<String, String> tests) {
            this.production = production;
            this.tests = tests;
        }
    }

    static final class AnalysisResult {
        final PreviousProject previousProject;
        final NewProject newProject;
        final CodeAnalyzer codeAnalyzer;
        final Set<Test> selectedTests;

        private AnalysisResult(PreviousProject previousProject, NewProject newProject,
                               CodeAnalyzer codeAnalyzer, Set<Test> selectedTests) {
            this.previousProject = previousProject;
            this.newProject = newProject;
            this.codeAnalyzer = codeAnalyzer;
            this.selectedTests = selectedTests;
        }

        Set<String> changedMethodNames() {
            return new TreeSet<>(codeAnalyzer.getChangedMethods());
        }

        Set<String> newMethodNames() {
            return new TreeSet<>(codeAnalyzer.getStringNewMethods());
        }

        Set<String> differentTestNames() {
            Set<String> names = new TreeSet<>();
            for (Test test : codeAnalyzer.getDifferentTest()) {
                names.add(testName(test.getTestMethod()));
            }
            return names;
        }

        Set<String> differentObjectNames() {
            Set<String> names = new TreeSet<>();
            for (SootClass sootClass : codeAnalyzer.getDifferentObject()) {
                names.add(sootClass.getName());
            }
            return names;
        }

        Set<String> selectedTestNames() {
            Set<String> names = new TreeSet<>();
            for (Test test : selectedTests) {
                names.add(testName(test.getTestMethod()));
            }
            return names;
        }

        Set<String> removedTestNames() {
            Set<String> names = new TreeSet<>();
            for (SootMethod m : codeAnalyzer.getRemovedTests()) {
                names.add(m.getDeclaringClass().getName() + "#" + m.getName());
            }
            return names;
        }

        Map<String, Set<String>> selectedTestingMethods() {
            return selectionReasons();
        }

        Map<String, Set<String>> selectionReasons() {
            Map<String, Set<String>> reasons = new HashMap<>();
            for (Test test : selectedTests) {
                reasons.put(testName(test.getTestMethod()), new HashSet<>(test.getTestingMethods()));
            }
            return reasons;
        }

        private static String testName(SootMethod sootMethod) {
            return sootMethod.getDeclaringClass().getName() + "#" + sootMethod.getName();
        }
    }
}
