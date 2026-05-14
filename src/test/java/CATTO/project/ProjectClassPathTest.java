package CATTO.project;

import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

public class ProjectClassPathTest {

    @Test
    public void sootClassPathIncludesJdkVirtualFileSystemOnModernJava() throws Exception {
        Project project = fixtureProject();
        String classPath = buildSootClassPath(project);

        assertTrue(classPath.contains("VIRTUAL_FS_FOR_JDK"));
        assertTrue(classPath.contains("whatTestProjectForTesting" + File.separator + "out"
                + File.separator + "production" + File.separator + "p1"));
        assertTrue(classPath.contains("junit"));
    }

    private static Project fixtureProject() throws IOException, NoTestFoundedException, InvalidTargetPaths {
        return new Project(
                new String[0],
                "whatTestProjectForTesting" + File.separator + "out"
                        + File.separator + "production" + File.separator + "p1",
                "whatTestProjectForTesting" + File.separator + "out"
                        + File.separator + "test" + File.separator + "p1"
        );
    }

    private static String buildSootClassPath(Project project) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        Method method = Project.class.getDeclaredMethod("buildSootClassPath");
        method.setAccessible(true);
        return (String) method.invoke(project);
    }
}
