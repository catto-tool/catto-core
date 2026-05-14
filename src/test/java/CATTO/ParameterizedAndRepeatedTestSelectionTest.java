package CATTO;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Policy: @ParameterizedTest and @RepeatedTest are treated as single selectable units.
 * CATTO selects them; the JUnit runner decides how to execute (parameter sets, repetitions).
 */
public class ParameterizedAndRepeatedTestSelectionTest {

    private static final Map<String, String> STABLE_PRODUCTION = Map.of(
            "fixture.StableProd",
            "package fixture;\npublic class StableProd { public int value() { return 42; } }\n"
    );

    private static final String CHANGING_PROD_V1 =
            "package fixture;\npublic class ChangingProd { public int compute() { return 1; } }\n";
    private static final String CHANGING_PROD_V2 =
            "package fixture;\npublic class ChangingProd { public int compute() { return 2; } }\n";

    // ---- @ParameterizedTest ----

    @Test
    public void newParameterizedTestMethodIsSelected() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.ParamTest",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.params.ParameterizedTest;\n"
                                        + "import org.junit.jupiter.params.provider.ValueSource;\n"
                                        + "public class ParamTest {\n"
                                        + "    @ParameterizedTest @ValueSource(ints = {1})\n"
                                        + "    public void testValue(int v) {}\n"
                                        + "}\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.ParamTest",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.params.ParameterizedTest;\n"
                                        + "import org.junit.jupiter.params.provider.ValueSource;\n"
                                        + "public class ParamTest {\n"
                                        + "    @ParameterizedTest @ValueSource(ints = {1})\n"
                                        + "    public void testValue(int v) {}\n"
                                        + "    @ParameterizedTest @ValueSource(ints = {1, 2})\n"
                                        + "    public void newParamTest(int v) {}\n"
                                        + "}\n")
                )
        );
        assertTrue(
                "New @ParameterizedTest method must be selected as a single unit. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.ParamTest#newParamTest")
        );
    }

    @Test
    public void parameterizedTestCoveringChangedProductionIsSelected() throws Exception {
        String testSrc =
                "package fixture;\n"
                        + "import org.junit.jupiter.params.ParameterizedTest;\n"
                        + "import org.junit.jupiter.params.provider.ValueSource;\n"
                        + "import static org.junit.jupiter.api.Assertions.*;\n"
                        + "public class ParamCoverTest {\n"
                        + "    @ParameterizedTest @ValueSource(ints = {1})\n"
                        + "    public void testCompute(int expected) { assertEquals(expected, new ChangingProd().compute()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.ChangingProd", CHANGING_PROD_V1),
                        Map.of("fixture.ParamCoverTest", testSrc)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.ChangingProd", CHANGING_PROD_V2),
                        Map.of("fixture.ParamCoverTest", testSrc)
                )
        );
        assertTrue(
                "@ParameterizedTest covering changed production method must be selected. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.ParamCoverTest#testCompute")
        );
    }

    @Test
    public void plainMethodNamedTestWithoutParameterizedAnnotationIsNotSelected() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.FalseParamTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class FalseParamTest {\n"
                                        + "    @Test public void realTest() {}\n"
                                        + "    public void testSomething(int v) {}\n"
                                        + "}\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.FalseParamTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class FalseParamTest {\n"
                                        + "    @Test public void realTest() {}\n"
                                        + "    public void testSomething(int v) {}\n"
                                        + "    public void testNewMethod(int v) {}\n"
                                        + "}\n")
                )
        );
        assertFalse(
                "Method named test* with int parameter but no @ParameterizedTest must not be selected. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.FalseParamTest#testNewMethod")
        );
    }

    // ---- @RepeatedTest ----

    @Test
    public void newRepeatedTestMethodIsSelected() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.RepeatedTestClass",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.RepeatedTest;\n"
                                        + "public class RepeatedTestClass {\n"
                                        + "    @RepeatedTest(3) public void testRepeat() {}\n"
                                        + "}\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.RepeatedTestClass",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.RepeatedTest;\n"
                                        + "public class RepeatedTestClass {\n"
                                        + "    @RepeatedTest(3) public void testRepeat() {}\n"
                                        + "    @RepeatedTest(5) public void newRepeated() {}\n"
                                        + "}\n")
                )
        );
        assertTrue(
                "New @RepeatedTest method must be selected as a single unit. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.RepeatedTestClass#newRepeated")
        );
    }

    @Test
    public void repeatedTestCoveringChangedProductionIsSelected() throws Exception {
        String testSrc =
                "package fixture;\n"
                        + "import org.junit.jupiter.api.RepeatedTest;\n"
                        + "import static org.junit.jupiter.api.Assertions.*;\n"
                        + "public class RepeatedCoverTest {\n"
                        + "    @RepeatedTest(3)\n"
                        + "    public void testCompute() { assertEquals(2, new ChangingProd().compute()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.ChangingProd", CHANGING_PROD_V1),
                        Map.of("fixture.RepeatedCoverTest", testSrc)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.ChangingProd", CHANGING_PROD_V2),
                        Map.of("fixture.RepeatedCoverTest", testSrc)
                )
        );
        assertTrue(
                "@RepeatedTest covering changed production method must be selected. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.RepeatedCoverTest#testCompute")
        );
    }

    private static String diagnostic(FixtureAnalysisSupport.AnalysisResult result) {
        return "Selected=" + result.selectedTestNames()
                + " Changed=" + result.changedMethodNames()
                + " DiffTests=" + result.differentTestNames();
    }
}
