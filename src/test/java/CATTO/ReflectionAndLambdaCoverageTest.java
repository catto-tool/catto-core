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

/**
 * Verifies CATTO's behavior when tests reach production code via lambdas, method references,
 * or reflection. Documents the known limitation for pure reflection paths.
 */
public class ReflectionAndLambdaCoverageTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    /**
     * Test calls changed production code inside a lambda body.
     * Soot handles lambda synthetic methods via invokedynamic — the call graph should include
     * the edge from the lambda body to the production method.
     */
    @Test
    public void testViaLambdaBodyIsSelectedWhenProductionChanges() throws Exception {
        Path prevProd = compile(Map.of("rlcov1/Svc.java",
                "package rlcov1;\npublic class Svc { public int compute() { return 1; } }\n"));
        Path newProd = compile(Map.of("rlcov1/Svc.java",
                "package rlcov1;\npublic class Svc { public int compute() { return 2; } }\n"));

        String testSrc =
                "package rlcov1;\n" +
                "import java.util.function.Supplier;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class SvcLambdaTest {\n" +
                "    @Test public void testViaLambda() {\n" +
                "        Supplier<Integer> s = () -> new Svc().compute();\n" +
                "        assertEquals(2, (int) s.get());\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("rlcov1/SvcLambdaTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("rlcov1/SvcLambdaTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test calling changed production code via lambda must be selected. " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("rlcov1.SvcLambdaTest#testViaLambda"));
    }

    /**
     * Test calls changed production code via an instance method reference.
     */
    @Test
    public void testViaMethodReferenceIsSelectedWhenProductionChanges() throws Exception {
        Path prevProd = compile(Map.of("rlcov2/Producer.java",
                "package rlcov2;\npublic class Producer { public int value() { return 1; } }\n"));
        Path newProd = compile(Map.of("rlcov2/Producer.java",
                "package rlcov2;\npublic class Producer { public int value() { return 2; } }\n"));

        String testSrc =
                "package rlcov2;\n" +
                "import java.util.function.IntSupplier;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class ProducerRefTest {\n" +
                "    @Test public void testViaRef() {\n" +
                "        Producer p = new Producer();\n" +
                "        IntSupplier s = p::value;\n" +
                "        assertEquals(2, s.getAsInt());\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("rlcov2/ProducerRefTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("rlcov2/ProducerRefTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test calling changed production code via method reference must be selected. " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("rlcov2.ProducerRefTest#testViaRef"));
    }

    /**
     * Test calls changed production code inside a lambda passed to a stream pipeline.
     */
    @Test
    public void testViaStreamLambdaIsSelectedWhenProductionChanges() throws Exception {
        Path prevProd = compile(Map.of("rlcov3/Item.java",
                "package rlcov3;\npublic class Item { public int score() { return 1; } }\n"));
        Path newProd = compile(Map.of("rlcov3/Item.java",
                "package rlcov3;\npublic class Item { public int score() { return 2; } }\n"));

        String testSrc =
                "package rlcov3;\n" +
                "import java.util.List;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class StreamLambdaTest {\n" +
                "    @Test public void testViaStream() {\n" +
                "        int sum = List.of(new Item()).stream()\n" +
                "                      .mapToInt(Item::score)\n" +
                "                      .sum();\n" +
                "        assertEquals(2, sum);\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("rlcov3/StreamLambdaTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("rlcov3/StreamLambdaTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test calling changed production code via stream + method reference must be selected. " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("rlcov3.StreamLambdaTest#testViaStream"));
    }

    /**
     * Known limitation: when the only path from the test to the changed production method
     * goes through java.lang.reflect.Method.invoke(), CATTO cannot select the test.
     * The static call graph has no edge from the reflective call site to the target method.
     * This is documented behavior — not a bug.
     */
    @Test
    public void reflectionPathIsKnownBlindSpot() throws Exception {
        Path prevProd = compile(Map.of("rlcov4/ReflTarget.java",
                "package rlcov4;\npublic class ReflTarget { public int val() { return 1; } }\n"));
        Path newProd = compile(Map.of("rlcov4/ReflTarget.java",
                "package rlcov4;\npublic class ReflTarget { public int val() { return 2; } }\n"));

        String testSrc =
                "package rlcov4;\n" +
                "import java.lang.reflect.Method;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class ReflectionTest {\n" +
                "    @Test public void testViaReflection() throws Exception {\n" +
                "        Method m = ReflTarget.class.getDeclaredMethod(\"val\");\n" +
                "        assertEquals(2, m.invoke(new ReflTarget()));\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("rlcov4/ReflectionTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("rlcov4/ReflectionTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        // Known limitation: static call graph has no edge through Method.invoke() to the target.
        // This test documents the false-negative — CATTO misses tests reached only via reflection.
        assertFalse(
                "KNOWN LIMITATION: CATTO cannot select tests whose only path to changed code is " +
                "via java.lang.reflect.Method.invoke(). Static call graph has no reflective edge. " +
                "Selected=" + result.selectedTests(),
                result.selectedTests().contains("rlcov4.ReflectionTest#testViaReflection"));
    }

    // ---- compilation helper ----

    private static Path compile(Map<String, String> sources, Path... extraCp) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-rlcov-src-");
        Path outDir = Files.createTempDirectory("catto-rlcov-out-");
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
