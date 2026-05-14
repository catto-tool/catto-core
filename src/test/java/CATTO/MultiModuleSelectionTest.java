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
 * Verifies that CATTO correctly selects tests across Maven/Gradle module boundaries.
 * Multi-module means: separate class directories per module, cross-module calls in call graph.
 */
public class MultiModuleSelectionTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    /**
     * Downstream test covers upstream production via call chain.
     * When upstream production changes, downstream test must be selected.
     */
    @Test
    public void testInDownstreamModuleSelectedWhenUpstreamProductionChanges() throws Exception {
        // Upstream module "core": CoreService.compute() changes 1→2
        Path coreV1 = compile(Map.of("mmtest1/core/CoreService.java",
                "package mmtest1.core;\n" +
                "public class CoreService { public int compute() { return 1; } }\n"));
        Path coreV2 = compile(Map.of("mmtest1/core/CoreService.java",
                "package mmtest1.core;\n" +
                "public class CoreService { public int compute() { return 2; } }\n"));

        // Downstream module "app": production calls CoreService, test calls production
        String appProdSrc =
                "package mmtest1.app;\n" +
                "import mmtest1.core.CoreService;\n" +
                "public class AppService { public int run() { return new CoreService().compute(); } }\n";
        String appTestSrc =
                "package mmtest1.app;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class AppTest {\n" +
                "    @Test public void testRun() { assertEquals(2, new AppService().run()); }\n" +
                "}\n";

        Path appProdV1 = compile(Map.of("mmtest1/app/AppService.java", appProdSrc), coreV1);
        Path appTestV1 = compile(Map.of("mmtest1/app/AppTest.java", appTestSrc), coreV1, appProdV1);
        Path appProdV2 = compile(Map.of("mmtest1/app/AppService.java", appProdSrc), coreV2);
        Path appTestV2 = compile(Map.of("mmtest1/app/AppTest.java", appTestSrc), coreV2, appProdV2);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(coreV1)
                .addPreviousClassesPath(appProdV1)
                .addPreviousClassesPath(appTestV1)
                .addNewClassesPath(coreV2)
                .addNewClassesPath(appProdV2)
                .addNewClassesPath(appTestV2)
                .build());

        assertTrue(
                "Downstream test must be selected when upstream production changes. " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("mmtest1.app.AppTest#testRun"));
    }

    /**
     * Both upstream and downstream tests must be selected when upstream production changes.
     */
    @Test
    public void bothModuleTestsSelectedWhenUpstreamProductionChanges() throws Exception {
        Path coreV1 = compile(Map.of("mmtest2/core/CoreSvc.java",
                "package mmtest2.core;\n" +
                "public class CoreSvc { public int val() { return 1; } }\n"));
        Path coreV2 = compile(Map.of("mmtest2/core/CoreSvc.java",
                "package mmtest2.core;\n" +
                "public class CoreSvc { public int val() { return 2; } }\n"));

        String coreTestSrc =
                "package mmtest2.core;\n" +
                "import org.junit.Test;\n" +
                "import static org.junit.Assert.*;\n" +
                "public class CoreTest {\n" +
                "    @Test public void testVal() { assertEquals(2, new CoreSvc().val()); }\n" +
                "}\n";
        String appProdSrc =
                "package mmtest2.app;\n" +
                "import mmtest2.core.CoreSvc;\n" +
                "public class AppSvc { public int run() { return new CoreSvc().val(); } }\n";
        String appTestSrc =
                "package mmtest2.app;\n" +
                "import org.junit.Test;\n" +
                "public class AppTest {\n" +
                "    @Test public void testRun() { new AppSvc().run(); }\n" +
                "}\n";

        Path coreTestV1 = compile(Map.of("mmtest2/core/CoreTest.java", coreTestSrc), coreV1);
        Path coreTestV2 = compile(Map.of("mmtest2/core/CoreTest.java", coreTestSrc), coreV2);
        Path appProdV1  = compile(Map.of("mmtest2/app/AppSvc.java", appProdSrc), coreV1);
        Path appTestV1  = compile(Map.of("mmtest2/app/AppTest.java", appTestSrc), coreV1, appProdV1);
        Path appProdV2  = compile(Map.of("mmtest2/app/AppSvc.java", appProdSrc), coreV2);
        Path appTestV2  = compile(Map.of("mmtest2/app/AppTest.java", appTestSrc), coreV2, appProdV2);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(coreV1)
                .addPreviousClassesPath(coreTestV1)
                .addPreviousClassesPath(appProdV1)
                .addPreviousClassesPath(appTestV1)
                .addNewClassesPath(coreV2)
                .addNewClassesPath(coreTestV2)
                .addNewClassesPath(appProdV2)
                .addNewClassesPath(appTestV2)
                .build());

        assertTrue("CoreTest must be selected when CoreSvc changes. Selected=" + result.selectedTests(),
                result.selectedTests().contains("mmtest2.core.CoreTest#testVal"));
        assertTrue("AppTest must be selected transitively. Selected=" + result.selectedTests(),
                result.selectedTests().contains("mmtest2.app.AppTest#testRun"));
    }

    /**
     * Downstream test must NOT be selected when the changed upstream method is not on its call path.
     */
    @Test
    public void downstreamTestNotSelectedWhenUnrelatedUpstreamMethodChanges() throws Exception {
        // CoreSvc has used() and unused(); AppTest only calls used()
        Path coreV1 = compile(Map.of("mmtest3/core/CoreSvc.java",
                "package mmtest3.core;\n" +
                "public class CoreSvc {\n" +
                "    public int used()   { return 1; }\n" +
                "    public int unused() { return 1; }\n" +
                "}\n"));
        Path coreV2 = compile(Map.of("mmtest3/core/CoreSvc.java",
                "package mmtest3.core;\n" +
                "public class CoreSvc {\n" +
                "    public int used()   { return 1; }\n" +
                "    public int unused() { return 99; }\n" +  // only this changes
                "}\n"));

        String appTestSrc =
                "package mmtest3.app;\n" +
                "import mmtest3.core.CoreSvc;\n" +
                "import org.junit.Test;\n" +
                "public class AppTest {\n" +
                "    @Test public void testUsed() { new CoreSvc().used(); }\n" +
                "}\n";

        Path appTestV1 = compile(Map.of("mmtest3/app/AppTest.java", appTestSrc), coreV1);
        Path appTestV2 = compile(Map.of("mmtest3/app/AppTest.java", appTestSrc), coreV2);

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .addPreviousClassesPath(coreV1)
                .addPreviousClassesPath(appTestV1)
                .addNewClassesPath(coreV2)
                .addNewClassesPath(appTestV2)
                .build());

        assertFalse(
                "AppTest must NOT be selected when only unrelated upstream method changes. " +
                "Selected=" + result.selectedTests() + " Changed=" + result.changedMethods(),
                result.selectedTests().contains("mmtest3.app.AppTest#testUsed"));
    }

    // ---- compilation helper ----

    private static Path compile(Map<String, String> sources, Path... extraCp) throws Exception {
        Path srcDir = Files.createTempDirectory("catto-multimod-src-");
        Path outDir = Files.createTempDirectory("catto-multimod-out-");
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
