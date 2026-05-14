package CATTO.cache;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CallGraphCacheIntegrationTest {

    @BeforeClass
    public static void initLogging() {
        BasicConfigurator.configure();
    }

    @Test
    public void cacheFileIsCreatedAfterAnalysisWithCacheDir() throws Exception {
        Path cacheDir = Files.createTempDirectory("cg-cache-smoke-");
        Fixture f = Fixture.build();

        AnalysisRequest req = AnalysisRequest.builder()
                .previousClassesPath(f.emptyPrev)
                .newClassesPaths(List.of(f.v1Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build();

        CattoAnalyzer.analyze(req);

        assertTrue("Cache file must be created after analysis with a cache directory",
                Files.list(cacheDir).anyMatch(p -> p.getFileName().toString().endsWith(".cache")));
    }

    @Test
    public void bodyOnlyChangeUsesCallGraphFromCache() throws Exception {
        Path cacheDir = Files.createTempDirectory("cg-cache-body-");
        Fixture f = Fixture.build();

        AnalysisRequest req1 = AnalysisRequest.builder()
                .previousClassesPath(f.emptyPrev)
                .newClassesPaths(List.of(f.v1Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build();
        CattoAnalyzer.analyze(req1);

        AnalysisRequest req2 = AnalysisRequest.builder()
                .previousClassesPath(f.v1Prod)
                .newClassesPaths(List.of(f.v2Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build();
        AnalysisResult result2 = CattoAnalyzer.analyze(req2);

        assertTrue("Body-only change: cache fingerprint matches, call graph must come from cache",
                result2.callGraphFromCache());
        assertTrue("Body-only change must still select the covering test",
                result2.selectedTests().contains("fixture.CacheProdTest#testCompute"));
    }

    @Test
    public void newMethodInvalidatesCallGraphCache() throws Exception {
        Path cacheDir = Files.createTempDirectory("cg-cache-newmethod-");
        Fixture f = Fixture.build();

        AnalysisRequest req1 = AnalysisRequest.builder()
                .previousClassesPath(f.emptyPrev)
                .newClassesPaths(List.of(f.v1Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build();
        CattoAnalyzer.analyze(req1);

        AnalysisRequest req2 = AnalysisRequest.builder()
                .previousClassesPath(f.v1Prod)
                .newClassesPaths(List.of(f.v3ProdNewMethod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build();
        AnalysisResult result2 = CattoAnalyzer.analyze(req2);

        assertFalse("New method must invalidate the call graph cache",
                result2.callGraphFromCache());
    }

    @Test
    public void cachedGraphSelectsSameTestsAsFreshGraph() throws Exception {
        Fixture f = Fixture.build();

        // Run without cache
        AnalysisResult fresh = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(f.v1Prod)
                .newClassesPaths(List.of(f.v2Prod, f.testClasses))
                .build());

        // Run with cache (first call builds cache, second call uses it)
        Path cacheDir = Files.createTempDirectory("cg-cache-equiv-");
        CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(f.emptyPrev)
                .newClassesPaths(List.of(f.v1Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build());
        AnalysisResult cached = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(f.v1Prod)
                .newClassesPaths(List.of(f.v2Prod, f.testClasses))
                .callGraphCacheDirectory(cacheDir)
                .build());

        assertEquals("Cached graph must select identical tests as fresh graph",
                fresh.selectedTests(), cached.selectedTests());
    }

    @Test
    public void noCacheDirMeansNoCache() throws Exception {
        Fixture f = Fixture.build();

        AnalysisResult result = CattoAnalyzer.analyze(AnalysisRequest.builder()
                .previousClassesPath(f.v1Prod)
                .newClassesPaths(List.of(f.v2Prod, f.testClasses))
                .build());

        assertFalse("Without cache directory, callGraphFromCache must be false",
                result.callGraphFromCache());
    }

    // ---- fixture ----

    private static final class Fixture {
        final Path emptyPrev;
        final Path v1Prod;
        final Path v2Prod;
        final Path v3ProdNewMethod;
        final Path testClasses;

        private Fixture(Path emptyPrev, Path v1Prod, Path v2Prod, Path v3ProdNewMethod, Path testClasses) {
            this.emptyPrev = emptyPrev;
            this.v1Prod = v1Prod;
            this.v2Prod = v2Prod;
            this.v3ProdNewMethod = v3ProdNewMethod;
            this.testClasses = testClasses;
        }

        static Fixture build() throws Exception {
            Path emptyPrev = Files.createTempDirectory("cg-prev-empty-");

            Path v1Prod = compile(Map.of("fixture.CacheProd",
                    "package fixture; public class CacheProd { public int compute() { return 1; } }"));

            Path v2Prod = compile(Map.of("fixture.CacheProd",
                    "package fixture; public class CacheProd { public int compute() { return 2; } }"));

            Path v3ProdNewMethod = compile(Map.of("fixture.CacheProd",
                    "package fixture; public class CacheProd {"
                            + " public int compute() { return 2; }"
                            + " public int extra() { return 3; } }"));

            Path testClasses = compile(v2Prod, Map.of("fixture.CacheProdTest",
                    "package fixture;\n"
                            + "import org.junit.Test;\n"
                            + "import static org.junit.Assert.*;\n"
                            + "public class CacheProdTest {\n"
                            + "    @Test public void testCompute() { assertEquals(2, new CacheProd().compute()); }\n"
                            + "}\n"));

            return new Fixture(emptyPrev, v1Prod, v2Prod, v3ProdNewMethod, testClasses);
        }
    }

    private static Path compile(Map<String, String> sources, Path... extraCpDirs) throws Exception {
        Path srcDir = Files.createTempDirectory("cg-src-");
        Path outDir = Files.createTempDirectory("cg-out-");

        for (Map.Entry<String, String> e : sources.entrySet()) {
            Path file = srcDir.resolve(e.getKey().replace('.', File.separatorChar) + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, e.getValue(), StandardCharsets.UTF_8);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("No JavaCompiler");

        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path", ""));
        for (Path extra : extraCpDirs) {
            cp.append(File.pathSeparator).append(extra.toString());
        }

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            List<File> files = sources.keySet().stream()
                    .map(fqn -> srcDir.resolve(fqn.replace('.', File.separatorChar) + ".java").toFile())
                    .toList();
            Boolean ok = compiler.getTask(null, fm, null,
                    Arrays.asList("-d", outDir.toString(), "-classpath", cp.toString()),
                    null,
                    fm.getJavaFileObjectsFromFiles(files)
            ).call();
            if (!Boolean.TRUE.equals(ok)) throw new AssertionError("Compile failed for " + sources.keySet());
        }
        return outDir;
    }

    private static Path compile(Path extraCp, Map<String, String> sources) throws Exception {
        return compile(sources, extraCp);
    }
}
