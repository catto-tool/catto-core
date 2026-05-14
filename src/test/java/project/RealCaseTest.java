package project;

import CATTO.code.analyzer.CodeAnalyzer;
import CATTO.exception.InvalidTargetPaths;
import CATTO.main.Main;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.selector.TestSelector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import soot.SootMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class RealCaseTest {


    @Test
    public void copiedMethods() throws Exception {
        Main.run(new String[]{"/Users/ncdaam/IdeaProjects/catto-CG/"});
    }

}
