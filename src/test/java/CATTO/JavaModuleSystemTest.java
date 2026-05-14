package CATTO;

import CATTO.api.AnalysisRequest;
import CATTO.api.AnalysisResult;
import CATTO.api.CattoAnalyzer;
import org.apache.log4j.BasicConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class JavaModuleSystemTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test
    public void analysisSucceedsWhenNewProjectContainsModuleInfoClass() throws Exception {
        Path prevProd = compile(Map.of("fixture/ModuleProd.java",
                "package fixture;\npublic class ModuleProd { public int compute() { return 1; } }\n"));

        Path newProd = compileWithModule(
                Map.of("fixture/ModuleProd.java",
                        "package fixture;\npublic class ModuleProd { public int compute() { return 2; } }\n"),
                "module fixture.module { }");

        Path tests = compile(Map.of("fixture/ModuleTest.java",
                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                        + "public class ModuleTest {\n"
                        + "    @Test public void testCompute() { assertEquals(2, new ModuleProd().compute()); }\n"
                        + "}\n"), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(prevProd)
                .addNewClassesPath(newProd)
                .addNewClassesPath(tests)
                .build());

        assertTrue("Analysis must succeed even with module-info.class present",
                result.selectedTests().contains("fixture.ModuleTest#testCompute"));
    }

    @Test
    public void moduleInfoNeverAppearsInSelectedTests() throws Exception {
        Path prevProd = compile(Map.of("fixture/LeakProd.java",
                "package fixture;\npublic class LeakProd { public int v() { return 1; } }\n"));

        Path newProd = compileWithModule(
                Map.of("fixture/LeakProd.java",
                        "package fixture;\npublic class LeakProd { public int v() { return 2; } }\n"),
                "module fixture.module { }");

        Path tests = compile(Map.of("fixture/LeakTest.java",
                "package fixture;\nimport org.junit.Test;\n"
                        + "public class LeakTest { @Test public void t() { new LeakProd().v(); } }\n"), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(prevProd)
                .addNewClassesPath(newProd)
                .addNewClassesPath(tests)
                .build());

        assertFalse("module-info must never appear as a selected test",
                result.selectedTests().stream().anyMatch(n -> n.contains("module-info")));
    }

    @Test
    public void moduleInfoNeverAppearsInChangedMethods() throws Exception {
        Path prevProd = compile(Map.of("fixture/ChangedProd.java",
                "package fixture;\npublic class ChangedProd { public int v() { return 1; } }\n"));

        Path newProd = compileWithModule(
                Map.of("fixture/ChangedProd.java",
                        "package fixture;\npublic class ChangedProd { public int v() { return 2; } }\n"),
                "module fixture.module { }");

        Path tests = compile(Map.of("fixture/ChangedTest.java",
                "package fixture;\nimport org.junit.Test;\n"
                        + "public class ChangedTest { @Test public void t() { new ChangedProd().v(); } }\n"), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(prevProd)
                .addNewClassesPath(newProd)
                .addNewClassesPath(tests)
                .build());

        assertFalse("module-info must never appear in changed methods",
                result.changedMethods().stream().anyMatch(n -> n.contains("module-info")));
    }

    // ---- compilation helpers ----

    private static Path compile(Map<String, String> sources, Path... extraCp) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-modsys-src-");
        Path outDir = Files.createTempDirectory("catto-modsys-out-");
        List<File> files = new ArrayList<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path f = srcDir.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue(), StandardCharsets.UTF_8);
            files.add(f.toFile());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));
        for (Path p : extraCp) cp.append(File.pathSeparator).append(p);
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Boolean ok = compiler.getTask(null, fm, null,
                    Arrays.asList("-d", outDir.toString(), "-classpath", cp.toString()),
                    null, fm.getJavaFileObjectsFromFiles(files)).call();
            if (!Boolean.TRUE.equals(ok)) throw new AssertionError("Compile failed for " + sources.keySet());
        }
        return outDir;
    }

    private static Path compileWithModule(Map<String, String> sources, String moduleSource) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-modsys-mod-src-");
        Path outDir = Files.createTempDirectory("catto-modsys-mod-out-");
        List<File> files = new ArrayList<>();
        Path moduleFile = srcDir.resolve("module-info.java");
        Files.writeString(moduleFile, moduleSource, StandardCharsets.UTF_8);
        files.add(moduleFile.toFile());
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path f = srcDir.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue(), StandardCharsets.UTF_8);
            files.add(f.toFile());
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Boolean ok = compiler.getTask(null, fm, null,
                    Arrays.asList("-d", outDir.toString()),
                    null, fm.getJavaFileObjectsFromFiles(files)).call();
            if (!Boolean.TRUE.equals(ok)) throw new AssertionError("Compile with module-info failed");
        }
        return outDir;
    }
}
