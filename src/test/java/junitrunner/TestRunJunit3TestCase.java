package junitrunner;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import soot.SootClass;
import soot.SootMethod;
import CATTO.test.runner.Runner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRunJunit3TestCase {
    private SootMethod sootMethodMock;
    private List<String> targetPath;

    @Before
    public  void setUp(){
        BasicConfigurator.configure();


        sootMethodMock = mock(SootMethod.class);

        sootMethodMock.setDeclaringClass(new SootClass("sootexampleTestJUnit3"));
        when(sootMethodMock.getDeclaringClass()).thenReturn(new SootClass("sootexampleTestJUnit3"));

        targetPath = new ArrayList<String>();
        targetPath.add("whatTestProjectForTesting" + File.separator + "out"+ File.separator +"test"+ File.separator +"Junit3Test"+ File.separator + "test");




    }

    @Test
    public void runJunit3PassTest() throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {


        when(sootMethodMock.getName()).thenReturn("testPass");

        CATTO.test.Test t = new CATTO.test.Test(sootMethodMock);


        assertEquals(1, Runner.run(t, new String[0], targetPath ).getTestsSucceededCount());

    }

    @Test
    public void runJunit3FailTest() throws NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {


        when(sootMethodMock.getName()).thenReturn("testFail");

        CATTO.test.Test t = new CATTO.test.Test(sootMethodMock);

        assertEquals(1,Runner.run(t, new String[0], targetPath ).getTestsFailedCount());



    }


}
