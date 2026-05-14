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
 * Verifies CATTO's behavior when tests reach production code via DI-style injection patterns.
 * Uses plain bytecode patterns (no Spring/CDI dependency) to probe what Soot's RTA resolves.
 */
public class DependencyInjectionCoverageTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    /**
     * DI with a concrete-type field (not assigned in constructor — simulates field injection).
     * The virtual call site references the concrete type directly, so Soot CAN resolve it.
     */
    @Test
    public void testViaConcreteTypeFieldInjectionIsSelected() throws Exception {
        Path prevProd = compile(Map.of("dicov1/ConcreteBean.java",
                "package dicov1;\npublic class ConcreteBean { public int work() { return 1; } }\n"));
        Path newProd = compile(Map.of("dicov1/ConcreteBean.java",
                "package dicov1;\npublic class ConcreteBean { public int work() { return 2; } }\n"));

        // Field not initialized in constructor — DI framework sets it at runtime
        String testSrc =
                "package dicov1;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class ConcreteFieldTest {\n" +
                "    ConcreteBean bean;\n" +  // DI-style: no new ConcreteBean() in constructor
                "    @Test public void testWork() {\n" +
                "        assertEquals(2, bean.work());\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("dicov1/ConcreteFieldTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("dicov1/ConcreteFieldTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test accessing changed bean via concrete-type field must be selected " +
                "(Soot resolves the virtual call site from the field's declared type). " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("dicov1.ConcreteFieldTest#testWork"));
    }

    /**
     * DI with an interface-type field — typical Spring @Autowired pattern.
     * The allocation stub in NewProject adds `new EmailNotifier()` to the entry-point set,
     * so RTA marks EmailNotifier as instantiated and resolves the interface call site.
     */
    @Test
    public void testViaInterfaceFieldInjectionIsSelected() throws Exception {
        // Interface + implementation
        String ifaceSrc = "package dicov2;\npublic interface Notifier { void notify(String msg); }\n";
        String implV1   = "package dicov2;\npublic class EmailNotifier implements Notifier { public void notify(String msg) { /* v1 */ } }\n";
        String implV2   = "package dicov2;\npublic class EmailNotifier implements Notifier { public void notify(String msg) { System.out.println(msg); } }\n";

        Path prevProd = compile(Map.of("dicov2/Notifier.java", ifaceSrc, "dicov2/EmailNotifier.java", implV1));
        Path newProd  = compile(Map.of("dicov2/Notifier.java", ifaceSrc, "dicov2/EmailNotifier.java", implV2));

        // Test uses interface-type field — concrete type EmailNotifier never new-ed in test
        String testSrc =
                "package dicov2;\n" +
                "import org.junit.Test;\n" +
                "public class NotifierTest {\n" +
                "    Notifier notifier;\n" +
                "    @Test public void testNotify() {\n" +
                "        notifier.notify(\"hello\");\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("dicov2/NotifierTest.java", testSrc), prevProd);
        Path newTest  = compile(Map.of("dicov2/NotifierTest.java", testSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test using interface-type DI field must be selected when implementation changes " +
                "(allocation stub ensures RTA sees EmailNotifier as instantiated). " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("dicov2.NotifierTest#testNotify"));
    }

    /**
     * When a DI bean also has direct callers (non-DI path), those tests ARE selected.
     * DI limitation only affects tests that reach the implementation exclusively via DI.
     */
    @Test
    public void testWithDirectConstructionIsSelectedEvenWhenOtherTestsUseDI() throws Exception {
        String ifaceSrc = "package dicov3;\npublic interface Calc { int compute(); }\n";
        String implV1   = "package dicov3;\npublic class CalcImpl implements Calc { public int compute() { return 1; } }\n";
        String implV2   = "package dicov3;\npublic class CalcImpl implements Calc { public int compute() { return 2; } }\n";

        Path prevProd = compile(Map.of("dicov3/Calc.java", ifaceSrc, "dicov3/CalcImpl.java", implV1));
        Path newProd  = compile(Map.of("dicov3/Calc.java", ifaceSrc, "dicov3/CalcImpl.java", implV2));

        // DirectTest uses new CalcImpl() — CATTO selects this
        // DiTest uses interface field injection — CATTO misses this (known limitation)
        String directTestSrc =
                "package dicov3;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class DirectTest {\n" +
                "    @Test public void testDirect() {\n" +
                "        assertEquals(2, new CalcImpl().compute());\n" +
                "    }\n" +
                "}\n";

        Path prevTest = compile(Map.of("dicov3/DirectTest.java", directTestSrc), prevProd);
        Path newTest  = compile(Map.of("dicov3/DirectTest.java", directTestSrc), newProd);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(prevProd)
                .addPreviousClassesPath(prevTest)
                .addNewClassesPath(newProd)
                .addNewClassesPath(newTest)
                .build());

        assertTrue(
                "Test using direct construction must be selected even if there are other DI-based tests. " +
                "Selected=" + result.selectedTests(),
                result.selectedTests().contains("dicov3.DirectTest#testDirect"));
    }

    // ---- compilation helper ----

    private static Path compile(Map<String, String> sources, Path... extraCp) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-dicov-src-");
        Path outDir = Files.createTempDirectory("catto-dicov-out-");
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
