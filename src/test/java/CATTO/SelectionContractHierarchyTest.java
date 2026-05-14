package CATTO;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionContractHierarchyTest {

    private static final Map<String, String> STABLE_PRODUCTION = Map.of(
            "fixture.StableProduction",
            "package fixture;\n"
                    + "public class StableProduction {\n"
                    + "    public int value() { return 42; }\n"
                    + "}\n"
    );

    /**
     * Base.method() deleted from superclass. Child overrides method() with the same body in both versions.
     * Hierarchy analysis must mark Child.method() as changed, selecting the covering test.
     */
    @Test
    public void productionHierarchyDeletedMethodTriggersTestSelection() throws Exception {
        String previousChild =
                "package fixture;\n"
                        + "public class HierarchyChild extends HierarchyBase {\n"
                        + "    @Override public int method() { return 42; }\n"
                        + "}\n";
        String newChild =
                "package fixture;\n"
                        + "public class HierarchyChild extends HierarchyBase {\n"
                        + "    public int method() { return 42; }\n"
                        + "}\n";
        String testClass =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "import static org.junit.Assert.assertEquals;\n"
                        + "public class HierarchyTest {\n"
                        + "    @Test public void testChildMethod() { assertEquals(42, new HierarchyChild().method()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.HierarchyBase",
                                "package fixture;\n"
                                        + "public class HierarchyBase {\n"
                                        + "    public int method() { return 1; }\n"
                                        + "}\n",
                                "fixture.HierarchyChild",
                                previousChild
                        ),
                        Map.of("fixture.HierarchyTest", testClass)
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.HierarchyBase",
                                "package fixture;\n"
                                        + "public class HierarchyBase {\n"
                                        + "}\n",
                                "fixture.HierarchyChild",
                                newChild
                        ),
                        Map.of("fixture.HierarchyTest", testClass)
                )
        );
        assertTrue(
                "Deleting Base.method() must mark Child.method() as changed via hierarchy analysis "
                        + "and select the covering test. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.HierarchyTest#testChildMethod")
        );
    }

    /**
     * Inherited setUp from abstract superclass changes.
     * All concrete subclass @Test methods must be selected.
     */
    @Test
    public void inheritedSetUpChangeTriggesConcreteSubclassTests() throws Exception {
        String concreteTest =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "public class ConcreteSetUpTest extends AbstractSetUpBase {\n"
                        + "    @Test public void testOne() {}\n"
                        + "    @Test public void testTwo() {}\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.AbstractSetUpBase",
                                "package fixture;\n"
                                        + "import org.junit.Before;\n"
                                        + "public abstract class AbstractSetUpBase {\n"
                                        + "    protected int value;\n"
                                        + "    @Before public void setUp() { value = 1; }\n"
                                        + "}\n",
                                "fixture.ConcreteSetUpTest",
                                concreteTest
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.AbstractSetUpBase",
                                "package fixture;\n"
                                        + "import org.junit.Before;\n"
                                        + "public abstract class AbstractSetUpBase {\n"
                                        + "    protected int value;\n"
                                        + "    @Before public void setUp() { value = 2; }\n"
                                        + "}\n",
                                "fixture.ConcreteSetUpTest",
                                concreteTest
                        )
                )
        );
        Set<String> selected = result.selectedTestNames();
        assertTrue(
                "Inherited setUp change must select testOne in concrete subclass. " + diagnostic(result),
                selected.contains("fixture.ConcreteSetUpTest#testOne")
        );
        assertTrue(
                "Inherited setUp change must select testTwo in concrete subclass. " + diagnostic(result),
                selected.contains("fixture.ConcreteSetUpTest#testTwo")
        );
    }

    /**
     * Inherited tearDown from abstract superclass changes.
     * Concrete subclass tests must NOT be selected due to tearDown change alone.
     */
    @Test
    public void inheritedTearDownChangeDoesNotDirectlySelectTests() throws Exception {
        String concreteTest =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "public class ConcreteTearDownTest extends AbstractTearDownBase {\n"
                        + "    @Test public void testOne() {}\n"
                        + "    @Test public void testTwo() {}\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.AbstractTearDownBase",
                                "package fixture;\n"
                                        + "import org.junit.After;\n"
                                        + "public abstract class AbstractTearDownBase {\n"
                                        + "    protected int value;\n"
                                        + "    @After public void tearDown() { value = 0; }\n"
                                        + "}\n",
                                "fixture.ConcreteTearDownTest",
                                concreteTest
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        STABLE_PRODUCTION,
                        Map.of(
                                "fixture.AbstractTearDownBase",
                                "package fixture;\n"
                                        + "import org.junit.After;\n"
                                        + "public abstract class AbstractTearDownBase {\n"
                                        + "    protected int value;\n"
                                        + "    @After public void tearDown() { value = -1; }\n"
                                        + "}\n",
                                "fixture.ConcreteTearDownTest",
                                concreteTest
                        )
                )
        );
        assertFalse(
                "Inherited tearDown change alone must not select testOne. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.ConcreteTearDownTest#testOne")
        );
        assertFalse(
                "Inherited tearDown change alone must not select testTwo. " + diagnostic(result),
                result.selectedTestNames().contains("fixture.ConcreteTearDownTest#testTwo")
        );
    }

    /**
     * A test method in a concrete subclass is selected when a production method it calls changes,
     * even if that test method was inherited from an abstract superclass.
     */
    @Test
    public void inheritedTestMethodSelectedWhenCoveredProductionChanges() throws Exception {
        String abstractBase =
                "package fixture;\n"
                        + "import org.junit.Test;\n"
                        + "import static org.junit.Assert.assertEquals;\n"
                        + "public abstract class AbstractTestBase {\n"
                        + "    @Test public void testFromBase() { assertEquals(42, new InheritedProduction().compute()); }\n"
                        + "}\n";
        FixtureAnalysisSupport.AnalysisResult result = FixtureAnalysisSupport.analyzeSources(
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.InheritedProduction",
                                "package fixture;\n"
                                        + "public class InheritedProduction {\n"
                                        + "    public int compute() { return 42; }\n"
                                        + "}\n"
                        ),
                        Map.of(
                                "fixture.AbstractTestBase",
                                abstractBase,
                                "fixture.ConcreteInheritedTest",
                                "package fixture;\n"
                                        + "public class ConcreteInheritedTest extends AbstractTestBase {\n"
                                        + "}\n"
                        )
                ),
                new FixtureAnalysisSupport.SourceSet(
                        Map.of(
                                "fixture.InheritedProduction",
                                "package fixture;\n"
                                        + "public class InheritedProduction {\n"
                                        + "    public int compute() { return 99; }\n"
                                        + "}\n"
                        ),
                        Map.of(
                                "fixture.AbstractTestBase",
                                abstractBase,
                                "fixture.ConcreteInheritedTest",
                                "package fixture;\n"
                                        + "public class ConcreteInheritedTest extends AbstractTestBase {\n"
                                        + "}\n"
                        )
                )
        );
        assertTrue(
                "Inherited test in concrete subclass must be selected when the production method it covers changes. "
                        + diagnostic(result),
                result.selectedTestNames().contains("fixture.ConcreteInheritedTest#testFromBase")
        );
    }

    private static String diagnostic(FixtureAnalysisSupport.AnalysisResult result) {
        return "Selected=" + result.selectedTestNames()
                + " Changed=" + result.changedMethodNames()
                + " DiffTests=" + result.differentTestNames();
    }
}
