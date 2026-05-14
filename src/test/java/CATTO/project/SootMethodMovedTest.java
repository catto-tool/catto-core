package CATTO.project;

import org.junit.Test;
import soot.Modifier;
import soot.SootClass;
import soot.SootMethod;
import soot.VoidType;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SootMethodMovedTest {

    @Test
    public void isMovedMatchesMethodSignatureFromOriginalClass() {
        SootClass targetClass = new SootClass("example.Child");
        SootClass originalClass = new SootClass("example.Parent");
        SootMethod movedMethod = method("testInherited");
        SootMethod originalMethod = method("testInherited");
        targetClass.addMethod(movedMethod);
        originalClass.addMethod(originalMethod);

        SootMethodMoved moved = new SootMethodMoved(targetClass);
        moved.addMethodMoved(movedMethod, originalClass);

        assertTrue(moved.isMoved(originalMethod));
    }

    @Test
    public void isMovedRejectsMethodsWithDifferentSignature() {
        SootClass targetClass = new SootClass("example.Child");
        SootClass originalClass = new SootClass("example.Parent");
        SootMethod movedMethod = method("testInherited");
        SootMethod otherMethod = method("otherTest");
        targetClass.addMethod(movedMethod);
        originalClass.addMethod(otherMethod);

        SootMethodMoved moved = new SootMethodMoved(targetClass);
        moved.addMethodMoved(movedMethod, originalClass);

        assertFalse(moved.isMoved(otherMethod));
    }

    @Test
    public void methodDeclaredInSameClassIsNotMarkedAsMoved() {
        SootClass testClass = new SootClass("example.TestClass");
        SootMethod method = method("testLocal");
        testClass.addMethod(method);

        SootMethodMoved moved = new SootMethodMoved(testClass);
        moved.addMethodMoved(method, testClass);

        assertTrue(moved.getOriginalClasses().isEmpty());
        assertFalse(moved.isMoved(method));
    }

    private static SootMethod method(String name) {
        return new SootMethod(name, Collections.emptyList(), VoidType.v(), Modifier.PUBLIC);
    }
}
