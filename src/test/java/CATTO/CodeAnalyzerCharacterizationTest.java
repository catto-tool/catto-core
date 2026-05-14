package CATTO;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CodeAnalyzerCharacterizationTest {
    private static FixtureAnalysisSupport.AnalysisResult result;

    @BeforeClass
    public static void setUp() throws Exception {
        result = FixtureAnalysisSupport.analyzeFixture();
    }

    @Test
    public void detectsRepresentativeChangedProductionMethods() {
        Set<String> changed = result.changedMethodNames();

        assertTrue(changed.contains("sootTest.sootexample.c"));
        assertTrue(changed.contains("sootTest.sootexample.privateMethodWithChange"));
        assertTrue(changed.contains("sootTest.sootexample.differenceInSignature"));
        assertTrue(changed.contains("sootTest.object.<init>"));
        assertTrue(changed.contains("sootTest.object.<clinit>"));
        assertTrue(changed.contains("sootTest.B.method"));
        assertTrue(changed.contains("sootTest.FirstClass.foo"));
    }

    @Test
    public void ignoresSemanticallyEquivalentStaticRewrite() {
        assertFalse(result.changedMethodNames().contains("sootTest.sootexample.differentStaticMethod"));
    }

    @Test
    public void promotesChangedTestConstructorOutOfChangedMethods() {
        assertFalse(result.changedMethodNames().contains("TestClassInitChanged.<init>"));
        assertTrue(result.differentTestNames().contains("TestClassInitChanged#toAddForChangeInInitEqual"));
        assertTrue(result.differentTestNames().contains("TestClassInitChanged#toAddForChangeInInitDifferent"));
    }

    @Test
    public void detectsRepresentativeNewProductionAndTestMethods() {
        Set<String> newMethods = result.newMethodNames();

        assertTrue(newMethods.contains("sootTest.sootexample.newMethod"));
        assertTrue(newMethods.contains("sootTest.sootexample.realMethodToTest"));
        assertTrue(newMethods.contains("sootexampleTest.testNewMethod"));
        assertTrue(newMethods.contains("objectTest.testGetStaticField"));
    }

    @Test
    public void promotesLifecycleAndChangedTestsOutOfChangedMethods() {
        Set<String> differentTests = result.differentTestNames();

        assertTrue(differentTests.contains("setUpChange#toAddForChangeInSetUpEqual"));
        assertTrue(differentTests.contains("setUpChange#toAddForChangeInSetUpDifferent"));
        assertFalse(differentTests.contains("tearDownChanged#toAddForChangeInTearDownEqual"));
        assertTrue(differentTests.contains("tearDownChanged#toAddForChangeInTearDownDifferent"));
        assertTrue(differentTests.contains("TestClassClinitChanged#toAddForChangeInClinitEqual"));
        assertTrue(differentTests.contains("TestClassClinitChanged#toAddForChangeInClinitDifferent"));
        assertTrue(differentTests.contains("sootexampleTest#differentTest"));
    }

    @Test
    public void tracksObjectsWithStaticInitializerDifferences() {
        assertEquals(Set.of("sootTest.object"), result.differentObjectNames());
    }
}
