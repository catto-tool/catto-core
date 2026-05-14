package project;

import CATTO.code.analyzer.CodeAnalyzer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.gen5.api.Disabled;
import org.junit.jupiter.api.Assertions;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.project.Project;
import CATTO.test.selector.TestSelector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;

public class ProjectTest {

    public static final String OUT_PRODUCTION_P ="whatTestProjectForTesting" + File.separator + "out" + File.separator + "production" + File.separator + "p";
    public static final String OUT_PRODUCTION_P_1 = "whatTestProjectForTesting" + File.separator + "out" + File.separator + "production" + File.separator + "p1";;

    private static String[] classPath = new String[0];

    private final String[] targetWithoutTest = {".\\target\\classes"};


    @Test
    public void getTarget() throws NoSuchMethodException, InvalidTargetPaths, IOException, NoTestFoundedException, IllegalAccessException, InvocationTargetException {
        Project p = null;

        p = new Project(classPath,  OUT_PRODUCTION_P);


        Assert.assertEquals(   Arrays.asList(OUT_PRODUCTION_P), p.getTarget());
    }

    @Test
    @Disabled
    @Ignore
    //fintoTest -> vedi todo in Project
    public void testClassPath() throws NoSuchMethodException, InvalidTargetPaths, IOException, NoTestFoundedException, IllegalAccessException, InvocationTargetException {
        String path;
        TreeSet<String> directoryList;
        DirectoryStream.Filter<Path> filter;
        String[] target;
        ArrayList<String> libs;

        PreviousProject p = null;
        path = "C:\\Users\\Dario\\workspace-experimental-object-commons-configuration";
        target = new String[]{"C:\\Users\\Dario\\workspace-experimental-object-commons-configuration\\commons-configuration-1.10\\target\\classes", "C:\\Users\\Dario\\workspace-experimental-object-commons-configuration\\commons-configuration-1.10\\target\\test-classes"};
        libs = new ArrayList<>();
        String lib = "C:\\Users\\Dario\\workspace-experimental-object-commons-configuration\\commons-configuration-1.10\\lib";

        //get a list of file
        List<File> file = (List<File>) FileUtils.listFiles(new File(lib), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File f : file) {
            libs.add(f.getAbsolutePath());
        }


        libs.add("C:\\Users\\Dario\\.m2\\repository\\org\\hamcrest\\hamcrest-all\\1.3\\hamcrest-all-1.3.jar");
         libs.add("C:\\Program Files (x86)\\Java\\jre6\\lib\\rt.jar");
        libs.add("C:\\Program Files (x86)\\Java\\jre6\\lib\\jce.jar");
        libs.add("C:\\Users\\Dario\\.m2\\repository\\junit\\junit\\4.12\\junit-4.12.jar");
        final String[] cls = libs.toArray(new String[0]);

        p = new PreviousProject(cls, target);
       NewProject  p1 = new NewProject(cls, target);
       CodeAnalyzer codeAnalyzer = new CodeAnalyzer(p1, p);
       codeAnalyzer.analyze();
       TestSelector frb = new TestSelector(p1,codeAnalyzer.getDifferentMethods() ,codeAnalyzer.getDifferentTest(),codeAnalyzer.getNewMethods() ,codeAnalyzer.getDifferentObject());
       frb.selectTest();


       // Assert.assertEquals(p.getTarget(),   Arrays.asList(OUT_PRODUCTION_P));
    }
    @Test
    public void getClassPath() throws NoSuchMethodException, InvalidTargetPaths, IOException, NoTestFoundedException, IllegalAccessException, InvocationTargetException {
        Project p = null;



        p = new Project(classPath,  OUT_PRODUCTION_P);


        Assert.assertEquals(p.getClassPath(),   Arrays.asList(classPath));
    }


    @Test(expected = InvalidTargetPaths.class)
    public void noDirectory() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {

            new Project(null, null, "out/production/p/sootTest/CallGraphExample.class");

    }

    @Test(expected = InvalidTargetPaths.class)
    public void noDirectory2() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {

        new Project(null, null );

    }

    @Test(expected = NotDirectoryException.class)
    public void noDirectory3() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {

        new Project(null, "" );

    }

    @Test(expected = NoTestFoundedException.class)
    public void noEntryPoints() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {

            new NewProject(classPath,  OUT_PRODUCTION_P_1);


    }


    //inserire classPath
    @Test
    public void equalsTest() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath,  OUT_PRODUCTION_P);

        Assert.assertEquals(p, p);

    }

    @Test
    public void noEquals() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath,  OUT_PRODUCTION_P);

        Project p1 = null;

            p1 = new Project(classPath, OUT_PRODUCTION_P_1);

        Assertions.assertNotEquals(p, p1);

    }

    @Test
    public void differentObjectEquals() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath, OUT_PRODUCTION_P);

        Assertions.assertNotEquals("", p);

    }

    @Test
    public void nullObjectEquals() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath,  OUT_PRODUCTION_P);

        Assert.assertNotEquals(null, p);

    }

    @Test
    public void getApplicationMethods() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath, OUT_PRODUCTION_P);

        assertTrue(!p.getApplicationMethod().isEmpty());

    }

    @Test
    public void differentHasCode() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath,  OUT_PRODUCTION_P);

        Project p1 = null;

            p1 = new Project(classPath,  OUT_PRODUCTION_P_1);


        assertTrue(p.hashCode() != p1.hashCode());

    }

    @Test
    public void equalsHasCode() throws IOException, NoTestFoundedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InvalidTargetPaths {
        Project p = null;

            p = new Project(classPath, OUT_PRODUCTION_P);

        Project p1 = null;

            p1 = new Project(classPath,  OUT_PRODUCTION_P);


        Assertions.assertNotEquals(p.hashCode(), p1.hashCode());

    }
}
