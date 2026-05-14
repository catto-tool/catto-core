package CATTO.api;

import CATTO.exception.AnalysisTimeoutException;
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

public class AnalysisTimeoutTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test
    public void analyzeWithGenerousTimeoutCompletesNormally() throws Exception {
        Path prevProd = compile(Map.of("tmout1/Svc.java",
                "package tmout1;\npublic class Svc { public int v() { return 1; } }\n"));
        Path newProd = compile(Map.of("tmout1/Svc.java",
                "package tmout1;\npublic class Svc { public int v() { return 2; } }\n"));
        String testSrc = "package tmout1;\nimport org.junit.Test;\n" +
                "public class SvcTest { @Test public void t() { new Svc().v(); } }\n";
        Path prevTest = compile(Map.of("tmout1/SvcTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("tmout1/SvcTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .analysisTimeoutSeconds(120)
                .build());

        assertTrue("Analysis must complete and select the test within generous timeout",
                result.selectedTests().contains("tmout1.SvcTest#t"));
    }

    @Test
    public void analyzeWithZeroTimeoutThrowsAnalysisTimeoutException() throws Exception {
        Path prevProd = compile(Map.of("tmout2/Svc.java",
                "package tmout2;\npublic class Svc { public int v() { return 1; } }\n"));
        Path newProd = compile(Map.of("tmout2/Svc.java",
                "package tmout2;\npublic class Svc { public int v() { return 2; } }\n"));
        String testSrc = "package tmout2;\nimport org.junit.Test;\n" +
                "public class SvcTest { @Test public void t() { new Svc().v(); } }\n";
        Path prevTest = compile(Map.of("tmout2/SvcTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("tmout2/SvcTest.java", testSrc), newProd);

        try {
            CattoAnalyzer.analyze(AnalysisRequest.builder()
                    .addPreviousClassesPath(prevProd)
                    .addPreviousClassesPath(prevTest)
                    .addNewClassesPath(newProd)
                    .addNewClassesPath(newTest)
                    .analysisTimeoutSeconds(0)
                    .build());
            fail("Expected AnalysisTimeoutException with 0-second timeout");
        } catch (AnalysisTimeoutException e) {
            assertTrue("Exception message must mention timeout duration",
                    e.getMessage().contains("0"));
        }
    }

    @Test
    public void analyzeWithoutTimeoutSetCompletesNormally() throws Exception {
        Path prevProd = compile(Map.of("tmout3/Svc.java",
                "package tmout3;\npublic class Svc { public int v() { return 1; } }\n"));
        Path newProd = compile(Map.of("tmout3/Svc.java",
                "package tmout3;\npublic class Svc { public int v() { return 2; } }\n"));
        String testSrc = "package tmout3;\nimport org.junit.Test;\n" +
                "public class SvcTest { @Test public void t() { new Svc().v(); } }\n";
        Path prevTest = compile(Map.of("tmout3/SvcTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("tmout3/SvcTest.java", testSrc), newProd);

        // No timeout set — should complete without exception
        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue("Analysis without timeout must complete normally",
                result.selectedTests().contains("tmout3.SvcTest#t"));
    }

    // ---- compilation helper ----

    private static Path compile(Map<String, String> sources, Path... extraCp) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-tmout-src-");
        Path outDir = Files.createTempDirectory("catto-tmout-out-");
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
}
