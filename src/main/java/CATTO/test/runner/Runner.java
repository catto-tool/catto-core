package CATTO.test.runner;

import org.apache.log4j.Logger;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import CATTO.test.Test;
import CATTO.util.ClassPathUpdater;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;


public class Runner {

    private Runner(){

    }

    static final Logger LOGGER = Logger.getLogger(Runner.class);

    public static TestExecutionSummary run(Test testsToRun, String[] pathForJarFiles, List<String> pathForClassFiles) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ClassNotFoundException {




        List<Class> classes = ClassPathUpdater.add(pathForClassFiles,TestExecutionSummary.class.getClassLoader() );
     //   ClassPathUpdater.addJar(pathForJarFiles);
        Class testClass = null;
        for(Class c : classes){
            if(c.getName().equals(testsToRun.getTestMethod().getDeclaringClass().toString())){

                testClass = c;
            }
        }
        Set<Path> paths = new HashSet<>();
        for(String path: pathForClassFiles ){
            paths.add(Path.of(path));
        }




/*        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(

                    selectMethod(testClass, testsToRun.getTestMethod().getName()),
                        (DiscoverySelector) DiscoverySelectors.selectClasspathRoots(paths)


                        //selectMethod(testsToRun.getTestMethod().getDeclaringClass().toString(),
                                //testsToRun.getTestMethod().getName())
                )
                .build();*/


        LauncherDiscoveryRequest request = request()
                .selectors(


                        selectClasspathRoots(paths)


                        //selectMethod(testsToRun.getTestMethod().getDeclaringClass().toString(),
                        //testsToRun.getTestMethod().getName())
                ).selectors(
                        selectMethod(testClass, testsToRun.getTestMethod().getName())
                ).selectors(

                        selectDirectory(pathForClassFiles.get(1)),
                        selectDirectory(pathForClassFiles.get(0))



                )
                .build();



        Launcher launcher = LauncherFactory.create();

        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();

        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        if (!failures.isEmpty())
            failures.forEach(failure -> LOGGER.error("The following test case is failed: " + testsToRun.getTestMethod().getDeclaringClass() + "." + testsToRun.getTestMethod().getName() + System.lineSeparator() + "caused by: ", failure.getException()));

        if (summary.getTestsSucceededCount() > 0)
            LOGGER.info("The following test case is passed: " + testsToRun.getTestMethod().getDeclaringClass() + "." + testsToRun.getTestMethod().getName());

        return summary;


    }
}


