package CATTO.util;

import org.apache.log4j.Logger;
import soot.SootClass;
import soot.SootMethod;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;

import java.lang.reflect.Modifier;


 public class JunitUtil {

    public static final String JUNIT_3_TEST_PREFIX = "test";
    public static final String JUNIT_3_TEST_RETURN_TYPE = "void";
    public static final String JUNIT_3_CLASS_TO_BE_EXTENDED = "junit.framework.TestCase";
    public static final String JUNIT_3_TEAR_DOWN_NAME = "tearDown";
    public static final String JUNIT_3_SET_UP_NAME = "setUp";

    public static final String JUNIT_4_5_TEST_TAG = "Test";
    public static final String JUNIT_4_5_TAG = "junit";
    public static final String JUNIT_4_5_BEFORE_TAG = "Before";
    public static final String JUNIT_4_5_AFTER_TAG = "After";
     private static final Logger LOGGER = Logger.getLogger(JunitUtil.class);


     private JunitUtil()
    {

    }

    public static boolean isATestClass(SootMethod m){

         if(soot.Modifier.isAbstract(m.getDeclaringClass().getModifiers()) || soot.Modifier.isInterface(m.getDeclaringClass().getModifiers()))
             return false;

         if(isJunitTestCase(m))
             return true;

         if(isAssignableFromJunitTestCaseClass(m.getDeclaringClass()))
             return true;
         else
             for(SootMethod sm : m.getDeclaringClass().getMethods()) {
                 if(isJunitTestCase(sm))
                     return true;

             }
         return false;
    }

    private static boolean isJUNIT3TestCase(SootMethod method) {
        return (method.getName().startsWith(JUNIT_3_TEST_PREFIX) && checkJunit3Condition(method));
    }

    private static boolean checkJunit3Condition(SootMethod method) {
        //method.getParameterTypes o restituisce null o un set pieno. dovremmo poter eliminare method.getParameterTypes().isEmpty()
        return ((isAssignableFromJunitTestCaseClass(method.getDeclaringClass())) && (Modifier.isPublic(method.getModifiers()) && method.getReturnType().toString().equals(JUNIT_3_TEST_RETURN_TYPE) && (method.getParameterTypes() == null || method.getParameterTypes().isEmpty())));
    }

    //Todo Aggiungere eccezzione.
    private static boolean isAssignableFromJunitTestCaseClass(SootClass clazz) {
        SootClass superClass = null;
        try {
            superClass = clazz.getSuperclass();
            if (superClass == null )
                return false;
        } catch (RuntimeException e) {
            return false;
        }
        String s = null;
        try {
            s = superClass.getName();
            //c'è una superclasse ma soot non la riesce a trovare -> è stato settato male il classpath.
        } catch (NullPointerException e) {
            LOGGER.error(e);
        }
        if (s.equals(JUNIT_3_CLASS_TO_BE_EXTENDED))
            return true;
        else
            return isAssignableFromJunitTestCaseClass(superClass);
    }

    private static boolean isJunit4or5TestCase(SootMethod sootMethod) {

        for (Tag t : sootMethod.getTags()) {
            if (checkJunit4or5Condition(t) && t.toString().contains(JUNIT_4_5_TEST_TAG)
                    && !t.toString().contains("TestFactory") && !t.toString().contains("TestTemplate"))
                return true;
        }

        SootMethod inheritedMethod = getInheritedMethod(sootMethod);
        if (inheritedMethod != null) return isJunit4or5TestCase(inheritedMethod);
        return false;
    }

    /**
     * <pre>
     * This method check if the T object passed is a Junit3/4/5 test method.
     * A method is a Junit3 test method if the method's name starts with "test" and if the class of the methods extend JUnit TestClass
     * A method is a Junit4 test method if the method's is noted with JUnit 4 @Test annotation.
     * A method is a Junit5 test method if the method's is noted with JUnit 5 @Test annotation.
     * @param m the method to check. This can be or a Method objcet or a SootMethod object.
     * @return true if is a JUnit3/4/5 method false if not.
     * </pre>
     */
    public static  boolean isJunitTestCase(SootMethod m) {
            return isJUNIT3TestCase(m) || isJunit4or5TestCase(m);
    }

    /**
     * Check if a SootMethod ia a JUnit 3/4/5 Method, so if is noted with @Before, @BeforeClass, @After, @AfterClass or @Test.
     *
     * @param m            the method to check
     * @return true if is a JUnit 3/4/5 method, false if not.
     */
    public static boolean isATestMethod(SootMethod m) {
        return isJunitTestCase(m) || isJunitTestMethod(m);
    }

    private static boolean isJunitTestMethod(SootMethod m) {
        if(isTearDown(m) || isSetup(m))
            return true;

        SootMethod inheritedMethod = getInheritedMethod(m);
        if (inheritedMethod != null) return isJunitTestMethod(inheritedMethod);

        return false;
    }

    private static boolean checkJunit4or5Condition(Tag t) {
        return t.getClass().equals(VisibilityAnnotationTag.class) && t.toString().contains(JUNIT_4_5_TAG);
    }


    private static SootMethod getInheritedMethod(SootMethod m) {
        try {
            SootClass superClass = m.getDeclaringClass().getSuperclass();
                SootMethod inheritedMethod = superClass.getMethod(m.getName(), m.getParameterTypes());
                if (inheritedMethod != null)
                    return inheritedMethod;
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }


    /**
     * Chek if a method is a setUp method or not.
     * A method ia a tear down method in Junit 3 if it's name is equal to "setUp"
     * A method ia a tear down method in Junit 4 if has as tag "Before" or "BeforeClass"
     * A method ia a tear down method in Junit 5 if has as tag "BeforeEach" or "BeforeAll"
     *
     * @param testMethod   the sootMethod to check
     * @return true if is a setUp method, false otherwise.
     */
    public static boolean isSetup(SootMethod testMethod) {

        for (Tag t : testMethod.getTags()) {
            if (checkJunit4or5Condition(t) && t.toString().contains(JUNIT_4_5_BEFORE_TAG))
                return true;
        }

        if (testMethod.getName().equals(JUNIT_3_SET_UP_NAME) && checkJunit3Condition(testMethod))
            return true;

        SootMethod inheritedMethod = getInheritedMethod(testMethod);
        if (inheritedMethod != null) return isSetup(inheritedMethod);

        return false;
    }

    /**
     * Chek if a method is a tear down method or not.
     * A method ia a tear down method in Junit 3 if it's name is equal to "tearDown"
     * A method ia a tear down method in Junit 4 if has as tag "After" or "AfterClass"
     * A method ia a tear down method in Junit 5 if has as tag "AfterEach" or "AfterAll"
     *
     * @param testMethod   the sootMethod to check
     * @return true if is a tear donwn method, false otherwise.
     */
    public static boolean isTearDown(SootMethod testMethod) {
        for (Tag t : testMethod.getTags()) {
            if (checkJunit4or5Condition(t) && t.toString().contains(JUNIT_4_5_AFTER_TAG))
                return true;
        }

        if (testMethod.getName().equals(JUNIT_3_TEAR_DOWN_NAME) && checkJunit3Condition(testMethod))
            return true;

        SootMethod inheritedMethod = getInheritedMethod(testMethod);
        if (inheritedMethod != null) return isTearDown(inheritedMethod);
        return false;
    }
}