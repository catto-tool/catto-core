package CATTO;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NewTestSelectionContractTest {
    private static final Map<String, String> STABLE_PRODUCTION = Map.of(
            "fixture.StableProduction",
            "package fixture;\n"
                    + "public class StableProduction {\n"
                    + "    public int value() {\n"
                    + "        return 42;\n"
                    + "    }\n"
                    + "}\n"
    );

    @Test
    public void selectsNewTestMethodInExistingClassEvenWhenItDoesNotCoverNewOrChangedProduction() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ExistingSelectionTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "import static org.junit.Assert.assertEquals;\n"
                                        + "public class ExistingSelectionTest {\n"
                                        + "    @Test public void existingCoversStableProduction() {\n"
                                        + "        assertEquals(42, new StableProduction().value());\n"
                                        + "    }\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ExistingSelectionTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "import static org.junit.Assert.assertEquals;\n"
                                        + "import static org.junit.Assert.assertTrue;\n"
                                        + "public class ExistingSelectionTest {\n"
                                        + "    @Test public void existingCoversStableProduction() {\n"
                                        + "        assertEquals(42, new StableProduction().value());\n"
                                        + "    }\n"
                                        + "    @Test public void newlyAddedPureTest() {\n"
                                        + "        assertTrue(true);\n"
                                        + "    }\n"
                                        + "}\n"
                        )
                )
        );

        assertTrue(
                "A new @Test method is itself a selection trigger, even without new/changed production coverage. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.ExistingSelectionTest#newlyAddedPureTest")
        );
    }

    @Test
    public void selectsAllRunnableTestsInANewTestClass() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ExistingSelectionTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "import static org.junit.Assert.assertEquals;\n"
                                        + "public class ExistingSelectionTest {\n"
                                        + "    @Test public void existingCoversStableProduction() {\n"
                                        + "        assertEquals(42, new StableProduction().value());\n"
                                        + "    }\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ExistingSelectionTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "import static org.junit.Assert.assertEquals;\n"
                                        + "public class ExistingSelectionTest {\n"
                                        + "    @Test public void existingCoversStableProduction() {\n"
                                        + "        assertEquals(42, new StableProduction().value());\n"
                                        + "    }\n"
                                        + "}\n",
                                "fixture.NewSelectionTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "import static org.junit.Assert.assertEquals;\n"
                                        + "import static org.junit.Assert.assertTrue;\n"
                                        + "public class NewSelectionTest {\n"
                                        + "    @Test public void coversStableProduction() {\n"
                                        + "        assertEquals(42, new StableProduction().value());\n"
                                        + "    }\n"
                                        + "    @Test public void pureRunnableTest() {\n"
                                        + "        assertTrue(true);\n"
                                        + "    }\n"
                                        + "    public void helperIsNotRunnable() {\n"
                                        + "    }\n"
                                        + "}\n"
                        )
                )
        );

        Set<String> expected = Set.of(
                "fixture.NewSelectionTest#coversStableProduction",
                "fixture.NewSelectionTest#pureRunnableTest"
        );

        assertTrue(
                "A new test class selects every runnable test in that class. "
                        + diagnostic(result),
                result.selectedTestNames().containsAll(expected)
        );
        assertFalse(
                "Helper methods in a new test class must not be selected as runnable tests.",
                result.selectedTestNames().contains("fixture.NewSelectionTest#helperIsNotRunnable")
        );
    }

    @Test
    public void removedTestMethodIsNotInSelectedSet() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ModifiableTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class ModifiableTest {\n"
                                        + "    @Test public void testExisting() {}\n"
                                        + "    @Test public void testToBeRemoved() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.ModifiableTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class ModifiableTest {\n"
                                        + "    @Test public void testExisting() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "Removed test methods must not appear in the selected runnable test set. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.ModifiableTest#testToBeRemoved")
        );
    }

    @Test
    public void removedTestClassHasNoTestsInSelectedSet() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.RemovedTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class RemovedTest {\n"
                                        + "    @Test public void testOne() {}\n"
                                        + "    @Test public void testTwo() {}\n"
                                        + "}\n",
                                "fixture.SurvivingTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class SurvivingTest {\n"
                                        + "    @Test public void testSurvives() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.SurvivingTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class SurvivingTest {\n"
                                        + "    @Test public void testSurvives() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "Removed test class: no tests from it should appear in the selected runnable set. "
                        + diagnostic(result),
                result.selectedTestNames().stream().anyMatch(n -> n.startsWith("fixture.RemovedTest#"))
        );
    }

    @Test
    public void jUnit4MethodNamedTestPrefixWithoutAnnotationIsNotATest() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.Junit4NegativeTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class Junit4NegativeTest {\n"
                                        + "    @Test public void correctAnnotatedTest() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.Junit4NegativeTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class Junit4NegativeTest {\n"
                                        + "    @Test public void correctAnnotatedTest() {}\n"
                                        + "    public void testWithoutAnnotation() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "A JUnit 4 method named test* without @Test must not be selected as a runnable test. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.Junit4NegativeTest#testWithoutAnnotation")
        );
    }

    @Test
    public void jUnit5TestFactoryIsNotASelectableTest() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.TestFactoryCheck",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.Test;\n"
                                        + "public class TestFactoryCheck {\n"
                                        + "    @Test public void normalTest() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.TestFactoryCheck",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.Test;\n"
                                        + "import org.junit.jupiter.api.TestFactory;\n"
                                        + "import org.junit.jupiter.api.DynamicTest;\n"
                                        + "import java.util.Collection;\n"
                                        + "import java.util.Collections;\n"
                                        + "public class TestFactoryCheck {\n"
                                        + "    @Test public void normalTest() {}\n"
                                        + "    @TestFactory public Collection<DynamicTest> dynamicTests() { return Collections.emptyList(); }\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "A @TestFactory method must not be selected as a regular runnable test. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.TestFactoryCheck#dynamicTests")
        );
    }

    @Test
    public void removedTestMethodIsReportedInRemovedTestMetadata() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.MetadataTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class MetadataTest {\n"
                                        + "    @Test public void testStays() {}\n"
                                        + "    @Test public void testRemoved() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.MetadataTest",
                                "package fixture;\n"
                                        + "import org.junit.Test;\n"
                                        + "public class MetadataTest {\n"
                                        + "    @Test public void testStays() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertTrue(
                "Removed test method must appear in removed-test metadata. " + diagnostic(result),
                result.removedTestNames().contains("fixture.MetadataTest#testRemoved")
        );
        assertFalse(
                "Surviving test method must not appear in removed-test metadata. " + diagnostic(result),
                result.removedTestNames().contains("fixture.MetadataTest#testStays")
        );
    }

    @Test
    public void jUnit5MethodNamedTestPrefixWithoutAnnotationIsNotATest() throws Exception {
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.Junit5NegativeTest",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.Test;\n"
                                        + "public class Junit5NegativeTest {\n"
                                        + "    @Test public void correctAnnotatedTest() {}\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.Junit5NegativeTest",
                                "package fixture;\n"
                                        + "import org.junit.jupiter.api.Test;\n"
                                        + "public class Junit5NegativeTest {\n"
                                        + "    @Test public void correctAnnotatedTest() {}\n"
                                        + "    public void testWithoutAnnotation() {}\n"
                                        + "}\n"
                        )
                )
        );
        assertFalse(
                "A JUnit 5 method named test* without @Test must not be selected as a runnable test. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.Junit5NegativeTest#testWithoutAnnotation")
        );
    }

    private static String diagnostic(FixtureAnalysisSupport.AnalysisResult result) {
        return "Selected tests were " + result.selectedTestNames()
                + ", new methods were " + result.newMethodNames()
                + ", changed methods were " + result.changedMethodNames()
                + ", different tests were " + result.differentTestNames();
    }
}
