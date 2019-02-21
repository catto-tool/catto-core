package testselector;

import org.apache.log4j.BasicConfigurator;
import testselector.exception.NoNameException;
import testselector.exception.NoPathException;
import testselector.exception.NoTestFoundedException;
import testselector.project.Project;
import testselector.testSelector.Test;

import java.io.File;
import java.nio.file.NotDirectoryException;
import java.util.Collection;
import java.util.Set;

public class ControlFlowTestSelector {

    private static Set<Test> TEST_TO_RUN_FINDED;
    private static Project PREVIOUS_VERSION_PROJECT;
    private static Project NEW_VERSION_PROJECT;
    private static Collection<Set<String>> NEW_METHOD_FINDED;
    private static Collection<Set<String>> CHANGED_METHOD_FINDED;
    private static final String[] classPath = {"C:\\Users\\Dario\\.m2\\repository\\org\\hamcrest\\hamcrest-all\\1.3\\hamcrest-all-1.3.jar;C:\\Program Files\\Java\\jdk1.8.0_112\\jre\\lib\\rt.jar;C:\\Program Files\\Java\\jdk1.8.0_112\\jre\\lib\\jce.jar;C:\\Users\\Dario\\.m2\\repository\\junit\\junit\\4.12\\junit-4.12.jar"};


    @org.junit.BeforeClass
    public static void setUp() throws NoPathException, NotDirectoryException, NoTestFoundedException, NoNameException {
        BasicConfigurator.configure();

        PREVIOUS_VERSION_PROJECT = new Project(classPath, "out" + File.separator + File.separator + "production" + File.separator + File.separator + "p");
        NEW_VERSION_PROJECT = new Project(classPath, "out" + File.separator + File.separator + "production" + File.separator + File.separator + "p1");
        PREVIOUS_VERSION_PROJECT.saveCallGraph("ProjectForTesting", "old");
        NEW_VERSION_PROJECT.saveCallGraph("ProjectForTesting", "new");

        testselector.testSelector.ControlFlowTestSelector u = new testselector.testSelector.ControlFlowTestSelector(PREVIOUS_VERSION_PROJECT, NEW_VERSION_PROJECT, true);
        TEST_TO_RUN_FINDED = u.selectTest();
        CHANGED_METHOD_FINDED = u.getChangedMethods();
        NEW_METHOD_FINDED = u.getNewOrRemovedMethods();
    }

    @org.junit.Test
    public void test() {

    }
}
