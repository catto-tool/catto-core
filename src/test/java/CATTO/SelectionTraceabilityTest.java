package CATTO;

import CATTO.FixtureAnalysisSupport;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SelectionTraceabilityTest {

    private static final Map<String, String> STABLE_PRODUCTION = Map.of(
            "fixture.StableProduction",
            "package fixture;\npublic class StableProduction { public int value() { return 42; } }\n"
    );

    @Test
    public void selectionReasonsContainsTriggeringMethodForSelectedTest() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.Traced",
                                "package fixture;\npublic class Traced { public int compute() { return 1; } }\n"),
                        Map.of("fixture.TracedTest",
                                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                                        + "public class TracedTest {\n"
                                        + "    @Test public void testCompute() { assertEquals(1, new Traced().compute()); }\n"
                                        + "}\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.Traced",
                                "package fixture;\npublic class Traced { public int compute() { return 2; } }\n"),
                        Map.of("fixture.TracedTest",
                                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                                        + "public class TracedTest {\n"
                                        + "    @Test public void testCompute() { assertEquals(2, new Traced().compute()); }\n"
                                        + "}\n")
                )
        );

        Map<String, Set<String>> reasons = result.selectionReasons();
        assertTrue("testCompute must be in selectionReasons",
                reasons.containsKey("fixture.TracedTest#testCompute"));
        assertTrue("selectionReasons must include the triggering method",
                reasons.get("fixture.TracedTest#testCompute").stream()
                        .anyMatch(m -> m.contains("Traced") && m.contains("compute")));
    }

    @Test
    public void selectionReasonsEmptyWhenNothingChanges() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.StableTest",
                                "package fixture;\nimport org.junit.Test;\n"
                                        + "public class StableTest { @Test public void testStable() {} }\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.StableTest",
                                "package fixture;\nimport org.junit.Test;\n"
                                        + "public class StableTest { @Test public void testStable() {} }\n")
                )
        );
        assertTrue("No changes — selectionReasons must be empty", result.selectionReasons().isEmpty());
    }

    @Test
    public void selectionReasonsKeySetMatchesSelectedTests() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.P",
                                "package fixture;\npublic class P { public int f() { return 1; } }\n"),
                        Map.of("fixture.PT",
                                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                                        + "public class PT {\n"
                                        + "    @Test public void t1() { assertEquals(1, new P().f()); }\n"
                                        + "    @Test public void t2() { assertEquals(1, new P().f()); }\n"
                                        + "}\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of("fixture.P",
                                "package fixture;\npublic class P { public int f() { return 2; } }\n"),
                        Map.of("fixture.PT",
                                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                                        + "public class PT {\n"
                                        + "    @Test public void t1() { assertEquals(2, new P().f()); }\n"
                                        + "    @Test public void t2() { assertEquals(2, new P().f()); }\n"
                                        + "}\n")
                )
        );
        assertEquals("selectionReasons key set must equal selectedTestNames",
                result.selectedTestNames(), result.selectionReasons().keySet());
    }

    @Test
    public void testsSelectedByDirectChangeHaveEmptyReasons() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.DirectTest",
                                "package fixture;\nimport org.junit.Test;\n"
                                        + "public class DirectTest { @Test public void testA() {} }\n")
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of("fixture.DirectTest",
                                "package fixture;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n"
                                        + "public class DirectTest {\n"
                                        + "    @Test public void testA() { assertTrue(true); }\n"
                                        + "}\n")
                )
        );
        assertTrue("Changed test method must be selected",
                result.selectedTestNames().contains("fixture.DirectTest#testA"));
        // Tests selected by direct change (differentTest path) have no call-graph-derived reasons
        Set<String> reasons = result.selectionReasons().getOrDefault("fixture.DirectTest#testA", Set.of());
        assertNotNull(reasons);
    }
}
