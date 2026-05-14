package CATTO.test.runner;

import junit.framework.TestResult;
import org.apache.log4j.Logger;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import CATTO.test.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Runner {

    private Runner() {
    }

    static final Logger LOGGER = Logger.getLogger(Runner.class);

    public static void run(Test testsToRun, String[] pathForJarFiles, List<String> pathForClassFiles) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException, ClassNotFoundException {

        // Create a list of URLs for the classpath
        List<URL> urls = new ArrayList<>();

        // Add class directories to the URLs
        for (String path : pathForClassFiles) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                urls.add(dir.toURI().toURL());
            } else {
                throw new IllegalArgumentException("Invalid directory: " + path);
            }
        }

        // Add JAR files to the URLs
        if (pathForJarFiles != null) {
            for (String jarPath : pathForJarFiles) {
                File jarFile = new File(jarPath);
                if (jarFile.exists() && jarFile.isFile()) {
                    urls.add(jarFile.toURI().toURL());
                } else {
                    throw new IllegalArgumentException("Invalid JAR file: " + jarPath);
                }
            }
        }

        // Create a URLClassLoader with the collected URLs
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Runner.class.getClassLoader());

        // Load classes from the specified directories
        List<Class<?>> classes = new ArrayList<>();
        for (String path : pathForClassFiles) {
            File dir = new File(path);
            classes.addAll(findClasses(dir, classLoader, dir.getPath()));
        }

        // Log loaded classes
        for (Class<?> cls : classes) {
            LOGGER.debug("Loaded class: " + cls.getName());
        }

        // Find the test class
        Class<?> testClass = null;
        Method testMethod = null;
        for (Class<?> c : classes) {
            if (c.getName().equals(testsToRun.getTestMethod().getDeclaringClass().getName())) {
                testClass = c;
                // Get the Method object using reflection
                testMethod = testClass.getDeclaredMethod(testsToRun.getTestMethod().getName());
                LOGGER.info("Found test method: " + testClass.getName() + "." + testMethod.getName());
                break;
            }
        }

        if (testClass == null || testMethod == null) {
            throw new ClassNotFoundException("Test class or method not found: " + testsToRun.getTestMethod().getDeclaringClass().getName() + "." + testsToRun.getTestMethod().getName());
        }

        LOGGER.info("Executing: " + testClass.getName() + "." + testMethod.getName());

        // Execute the test using JUnitCore
        JUnitCore junit = new JUnitCore();
//        Result result = junit.run(testClass);
        Result result = junit.run(testsToRun);
//        junit.framework.Test test = new

        // Log the test results
        if (!result.getFailures().isEmpty()) {
            for (Failure failure : result.getFailures()) {
                LOGGER.error("The following test case failed: " +
                        testsToRun.getTestMethod().getDeclaringClass().getName() + "." +
                        testsToRun.getTestMethod().getName() + System.lineSeparator() + "caused by: ", failure.getException());
            }
        }

        if (result.wasSuccessful()) {
            LOGGER.info("The following test case passed: " +
                    testsToRun.getTestMethod().getDeclaringClass().getName() + "." +
                    testsToRun.getTestMethod().getName());
        }

        LOGGER.debug("Found: " + result.getRunCount());
        LOGGER.debug("Failed: " + result.getFailureCount());
        LOGGER.debug("Ignored: " + result.getIgnoreCount());
        LOGGER.debug("Run Time: " + result.getRunTime() + "ms");
    }

    private static List<Class<?>> findClasses(File directory, ClassLoader classLoader, String basePath) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, classLoader, basePath));
                } else if (file.getName().endsWith(".class")) {
                    String className = file.getPath()
                            .replace(basePath + File.separator, "")
                            .replace(File.separator, ".")
                            .replace(".class", "");
                    if (className.startsWith(".")) {
                        className = className.substring(1);
                    }
                    classes.add(Class.forName(className, true, classLoader));
                }
            }
        }
        return classes;
    }
}
