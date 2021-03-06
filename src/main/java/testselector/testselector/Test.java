package testselector.testselector;

import org.apache.log4j.Logger;
import soot.SootMethod;
import testselector.main.Main;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Test {

    private static final Logger LOGGER = Logger.getLogger(Main.class);
    private SootMethod testMethod;
    //Linked-List ?!
    private HashSet<String> testingMethods;

    /**
     * Construct s runnable Test object. Contains information about a test method and which methods this test tests.
     *
     * @param testMethod    the test method
     * @param testingMethod the methods that this test tests.
     */
    public Test(SootMethod testMethod, Set<String> testingMethod) {
        this.testMethod = testMethod;
        this.testingMethods = new HashSet<String>(testingMethod);
    }

    /**
     * Construct s runnable Test object. Contains information about a test method.
     * @param testMethod the test method
     */
    public Test(SootMethod testMethod) {
        this(testMethod, new HashSet<>());
    }

    /**
     * Get the test method
     * @return a method that represent the test
     */
    public SootMethod getTestMethod() {
        return testMethod;
    }

    /**
     * Get all mehtods that this test tests
     * @return a collection of String that represent the name of the merthods that this test tests
     */
    public Set<String> getTestingMethods() {
        return testingMethods;
    }

    /**
     * Add a methot tested by this test.
     * @param testingMethod the name of the method to add
     */
    public void addTestingMethod(String testingMethod) {
        this.testingMethods.add(testingMethod);
    }

    /**
     * Run this test. Can be run JUnit 3, JUnit4 and JUnit 5 test.
     * @return the result of the test.
     *//*
    public TestExecutionSummary runTest() {

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectMethod(testMethod.getDeclaringClass().toString(),
                                testMethod.getName())
                )
                .build();

        Launcher launcher = LauncherFactory.create();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();

        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        if (!failures.isEmpty())
            failures.forEach(failure -> LOGGER.error("The following test case is failed: " + testMethod.getDeclaringClass() + "." + testMethod.getName() + System.lineSeparator() + "caused by: ", failure.getException()));

        if (summary.getTestsSucceededCount() > 0)
            LOGGER.info("The following test case is passed: " + testMethod.getDeclaringClass() + "." + testMethod.getName());

        return summary;

    }*/

    /**
     * Check if two project are equal.
     *
     * @param o the project to confront
     * @return true only if the two Test contain the same test method.
     * only check if the same object is present in the two project.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Test)) return false;
        Test test = (Test) o;
        return Objects.equals(getTestMethod(), test.getTestMethod());
    }

    /**
     * Get the hashcode for this Test calculated  with the method {@link Objects}.hash().
     * @return a int hashcode for this Test.
     **/
    @Override
    public int hashCode() {
        return Objects.hash(getTestMethod());
    }


}
