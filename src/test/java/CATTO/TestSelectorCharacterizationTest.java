package CATTO;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSelectorCharacterizationTest {
    private static FixtureAnalysisSupport.AnalysisResult result;
    private static Set<String> selectedTests;
    private static Map<String, Set<String>> testedMethods;

    @BeforeClass
    public static void setUp() throws Exception {
        result = FixtureAnalysisSupport.analyzeFixture();
        selectedTests = result.selectedTestNames();
        testedMethods = result.selectedTestingMethods();
    }

    @Test
    public void selectsTestsCoveringChangedPrivateAndSignatureMethods() {
        assertSelected("sootexampleTest#testDifferenceInAPrivateMethod", "sootTest.sootexample.privateMethodWithChange");
        assertSelected("sootexampleTest#testDifferenceInSignature", "sootTest.sootexample.differenceInSignature");
    }

    @Test
    public void selectsTestsCoveringNewMethods() {
        assertSelected("sootexampleTest#testNewMethod", "sootTest.sootexample.newMethod");
        assertSelected("objectTest#testGetStaticField", "sootTest.object.<clinit>");
    }

    @Test
    public void selectsTestsAffectedByObjectInitializers() {
        assertSelected("objectTest#testField", "sootTest.object.<init>");
        assertSelected("objectTest#testField", "sootTest.object.<clinit>");
        assertSelected("objectTest#testFoo", "sootTest.object.foo");
    }

    @Test
    public void selectsTestsAffectedByChangedLifecycleMethods() {
        assertTrue(selectedTests.contains("setUpChange#toAddForChangeInSetUpEqual"));
        assertTrue(selectedTests.contains("setUpChange#toAddForChangeInSetUpDifferent"));
        assertTrue(selectedTests.contains("TestClassClinitChanged#toAddForChangeInClinitEqual"));
        assertTrue(selectedTests.contains("TestClassClinitChanged#toAddForChangeInClinitDifferent"));
        assertTrue(selectedTests.contains("TestClassInitChanged#toAddForChangeInInitDifferent"));
    }

    @Test
    public void selectsTestsForChangedTestConstructor() {
        assertTrue(selectedTests.contains("TestClassInitChanged#toAddForChangeInInitEqual"));
    }

    @Test
    public void doesNotSelectTestsForChangedTearDown() {
        assertFalse(selectedTests.contains("tearDownChanged#toAddForChangeInTearDownEqual"));
        assertTrue(selectedTests.contains("tearDownChanged#toAddForChangeInTearDownDifferent"));
    }

    @Test
    public void doesNotSelectLifecycleMethodsAsRunnableTests() {
        assertFalse(selectedTests.contains("setUpChange#setUp"));
        assertFalse(selectedTests.contains("tearDownChanged#tearDown"));
        assertFalse(selectedTests.contains("TestClassClinitChanged#<clinit>"));
        assertFalse(selectedTests.contains("TestClassInitChanged#<init>"));
    }

    @Test
    public void selectsTestsCoveringStaticAnalysisCases() {
        assertSelected("TestStaticAnalyses#testA", "sootTest.B.method");
        assertSelected("TestStaticAnalyses#testB", "sootTest.B.method");
        assertSelected("TestStaticAnalyses#testC", "sootTest.B.method");
    }

    @Test
    public void selectsInnerClassTests() {
        assertSelected("TestClassWIthInnerClass$innerClass#testInInnerClass2", "sootTest.sootexample.c");
    }

    @Test
    public void doesNotSelectTestsForSemanticallyEquivalentStaticRewrite() {
        assertFalse(selectedTests.contains("sootexampleTest#testStaticDifferentMethod"));
    }

    private static void assertSelected(String testName, String testedMethod) {
        assertTrue("Expected selected test " + testName, selectedTests.contains(testName));
        assertTrue(
                "Expected " + testName + " to cover " + testedMethod + " but got " + testedMethods.get(testName),
                testedMethods.getOrDefault(testName, Set.of()).contains(testedMethod)
        );
    }
}
