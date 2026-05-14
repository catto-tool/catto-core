package CATTO;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionContractLifecycleTest {

    private static final Map<String, String> STABLE_PRODUCTION = Map.of(
            "fixture.StableProduction",
            "package fixture;\n"
                    + "public class StableProduction {\n"
                    + "    public int value() { return 42; }\n"
                    + "}\n"
    );

    @Test
    public void changedSetUpSelectsAllTestsInSameClass() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.SetUpTest",
                                "package fixture;\n"
                                        + "import org.junit.Before;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class SetUpTest {\n"
                                        + "    private int extra;\n"
                                        + "    @Before public void setUp() { extra = 1; }\n"
                                        + "    @Test public void testFirst() {}\n"
                                        + "    @Test public void testSecond() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.SetUpTest",
                                "package fixture;\n"
                                        + "import org.junit.Before;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class SetUpTest {\n"
                                        + "    private int extra;\n"
                                        + "    @Before public void setUp() { extra = 2; }\n"
                                        + "    @Test public void testFirst() {}\n"
                                        + "    @Test public void testSecond() {}\n"
                                        + "}\n"
                        )
                )
        );
        Set<String> selected = result.selectedTestNames();
        assertTrue(
                "Changed setUp must select testFirst. " + diagnostic(result),
                selected.contains("fixture.SetUpTest#testFirst")
        );
        assertTrue(
                "Changed setUp must select testSecond. " + diagnostic(result),
                selected.contains("fixture.SetUpTest#testSecond")
        );
    }

    @Test
    public void changedTearDownAloneDoesNotSelectAnyTest() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.TearDownOnlyTest",
                                "package fixture;\n"
                                        + "import org.junit.After;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class TearDownOnlyTest {\n"
                                        + "    private int value;\n"
                                        + "    @After public void tearDown() { value = 0; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.TearDownOnlyTest",
                                "package fixture;\n"
                                        + "import org.junit.After;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class TearDownOnlyTest {\n"
                                        + "    private int value;\n"
                                        + "    @After public void tearDown() { value = -1; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "tearDown change alone must not select testA. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.TearDownOnlyTest#testA")
        );
        assertFalse(
                "tearDown change alone must not select testB. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.TearDownOnlyTest#testB")
        );
    }

    @Test
    public void changedTestClassInitSelectsAllTestsInClass() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.InitTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class InitTest {\n"
                                        + "    private final int base;\n"
                                        + "    public InitTest() { base = 1; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.InitTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class InitTest {\n"
                                        + "    private final int base;\n"
                                        + "    public InitTest() { base = 2; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                )
        );
        Set<String> selected = result.selectedTestNames();
        assertTrue(
                "Changed test class <init> must select testA. " + diagnostic(result),
                selected.contains("fixture.InitTest#testA")
        );
        assertTrue(
                "Changed test class <init> must select testB. " + diagnostic(result),
                selected.contains("fixture.InitTest#testB")
        );
    }

    @Test
    public void changedTestClassClinitSelectsAllTestsInClass() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ClinitTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class ClinitTest {\n"
                                        + "    static int CONSTANT;\n"
                                        + "    static { CONSTANT = 1; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ClinitTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class ClinitTest {\n"
                                        + "    static int CONSTANT;\n"
                                        + "    static { CONSTANT = 2; }\n"
                                        + "    @Test public void testA() {}\n"
                                        + "    @Test public void testB() {}\n"
                                        + "}\n"
                        )
                )
        );
        Set<String> selected = result.selectedTestNames();
        assertTrue(
                "Changed test class <clinit> must select testA. " + diagnostic(result),
                selected.contains("fixture.ClinitTest#testA")
        );
        assertTrue(
                "Changed test class <clinit> must select testB. " + diagnostic(result),
                selected.contains("fixture.ClinitTest#testB")
        );
    }

    @Test
    public void productionVariableRenameIsNotSelected() throws Exception {
        String testClass =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "import static org.junit.Assert.assertEquals;\n"
                        + "public class RenameTest {\n"
                        + "    @Test public void testCompute() { assertEquals(1, new RenameProduction().compute()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.RenameProduction",
                                "package fixture;\n"
                                        + "public class RenameProduction {\n"
                                        + "    public int compute() { int originalName = 1; return originalName; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.RenameTest", testClass)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.RenameProduction",
                                "package fixture;\n"
                                        + "public class RenameProduction {\n"
                                        + "    public int compute() { int renamedVariable = 1; return renamedVariable; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.RenameTest", testClass)
                )
        );
        assertFalse(
                "A production local variable rename produces the same Jimple body and must not trigger test selection. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.RenameTest#testCompute")
        );
    }

    @Test
    public void productionSubstantiveChangeSelectsTest() throws Exception {
        String testClass =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "import static org.junit.Assert.assertEquals;\n"
                        + "public class SubstantiveTest {\n"
                        + "    @Test public void testCompute() { assertEquals(1, new SubstantiveProduction().compute()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.SubstantiveProduction",
                                "package fixture;\n"
                                        + "public class SubstantiveProduction {\n"
                                        + "    public int compute() { return 1; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.SubstantiveTest", testClass)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.SubstantiveProduction",
                                "package fixture;\n"
                                        + "public class SubstantiveProduction {\n"
                                        + "    public int compute() { return 2; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.SubstantiveTest", testClass)
                )
        );
        assertTrue(
                "A substantive production method change must select tests covering it. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.SubstantiveTest#testCompute")
        );
    }

    @Test
    public void productionInitChangeSelectsTestsThatInstantiateThatClass() throws Exception {
        String testClass =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "import static org.junit.Assert.assertEquals;\n"
                        + "public class InitCoverTest {\n"
                        + "    @Test public void testInstantiate() { assertEquals(1, new InitProduction().field); }\n"
                        + "    @Test public void testUnrelated() {}\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.InitProduction",
                                "package fixture;\n"
                                        + "public class InitProduction {\n"
                                        + "    public int field;\n"
                                        + "    public InitProduction() { field = 1; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.InitCoverTest", testClass)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.InitProduction",
                                "package fixture;\n"
                                        + "public class InitProduction {\n"
                                        + "    public int field;\n"
                                        + "    public InitProduction() { field = 2; }\n"
                                        + "}\n"
                        ),
                        Map.of("fixture.InitCoverTest", testClass)
                )
        );
        assertTrue(
                "Changed production <init> must select tests that instantiate that class. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.InitCoverTest#testInstantiate")
        );
    }

    private static String diagnostic(FixtureAnalysisSupport.AnalysisResult result) {
        return "Selected=" + result.selectedTestNames()
                + " Changed=" + result.changedMethodNames()
                + " DiffTests=" + result.differentTestNames();
    }
}
