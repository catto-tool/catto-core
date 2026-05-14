package CATTO.project;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.callgraph.CallGraph;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import CATTO.util.JunitUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NewProject extends Project {

    public NewProject(String[] classPath, @Nonnull String... target) throws NoTestFoundedException, IOException, InvocationTargetException, NoSuchMethodException, InvalidTargetPaths, IllegalAccessException {

        super(classPath, target);

        hierarchy = Scene.v().getActiveHierarchy();
        createEntryPoints(getMoved());
        createCallgraph();

    }

    /**
     * Constructor that injects a pre-built call graph, skipping Spark RTA construction.
     * Used when the structural fingerprint of the new project matches a cached fingerprint.
     */
    public NewProject(String[] classPath, CallGraph prebuiltCallGraph, @Nonnull String... target) throws NoTestFoundedException, IOException, InvocationTargetException, NoSuchMethodException, InvalidTargetPaths, IllegalAccessException {

        super(classPath, target);

        hierarchy = Scene.v().getActiveHierarchy();
        createEntryPoints(getMoved());
        setCallGraph(prebuiltCallGraph);
        Scene.v().setCallGraph(prebuiltCallGraph);

    }

    /*
     * Set all test-methods of the project as entry point for soot.
     */

    private void createEntryPoints(List<SootMethodMoved> toAdd) throws NoTestFoundedException {
        for (SootMethodMoved sootMethodMoved : toAdd) {

            SootMethod entry = createTestMethod((HashSet<SootMethod>) sootMethodMoved.getMethodsMoved(), sootMethodMoved.getInToMoved());


            if (entry != null)
                //settalo come entrypoints per il callgraph
                getEntryPoints().add(entry);
        }


        if (getEntryPoints().isEmpty())
            throw new NoTestFoundedException();
        Scene.v().setEntryPoints(new ArrayList<>(getEntryPoints()));
    }


    private SootMethod createTestMethod(HashSet<SootMethod> allTesting, SootClass leaf) {
        SootMethod method = new SootMethod("testMethodForTestClass" + leaf.getShortName(),
                null,
                VoidType.v(), Modifier.PUBLIC);

        SootClass sc = new SootClass("testClass" + leaf.getShortName());
        ArrayList<SootMethod> toWriteAsLasts = new ArrayList<>();

        for (SootMethod test : allTesting) {
            if (JunitUtil.isTearDown(test)) {
                toWriteAsLasts.add(test);
                continue;
            }
            if (JunitUtil.isATestMethod(test)) {
                Local testTypeLocal = new JimpleLocal("try", RefType.v(leaf.getName()));
                JimpleBody body;
                try {
                    body = (JimpleBody) method.retrieveActiveBody();
                } catch (RuntimeException e) {
                    body = Jimple.v().newBody(method);
                    body.getLocals().add(testTypeLocal);
                    body.getUnits().add(Jimple.v().newAssignStmt(testTypeLocal, new JNewExpr(RefType.v(leaf.getName()))));
                    body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(testTypeLocal, Scene.v().makeConstructorRef(Scene.v().getSootClass(leaf.getName()), null))));

                }
                InvokeExpr invoke;
                if (!test.isStatic())
                    invoke = Jimple.v().newSpecialInvokeExpr(testTypeLocal, test.makeRef());
                else
                    invoke = Jimple.v().newStaticInvokeExpr(test.makeRef());

                if (JunitUtil.isSetup(test)) {
                    try {
                        body.getUnits().insertAfter(Jimple.v().newInvokeStmt(invoke), body.getUnits().getSuccOf(body.getUnits().getFirst()));
                        //TODO: Serve davvero?
                    } catch (NoSuchElementException e) {

                        body.getUnits().add(Jimple.v().newInvokeStmt(invoke));

                    }
                } else
                    body.getUnits().add(Jimple.v().newInvokeStmt(invoke));


                method.setActiveBody(body);


            }

        }

        if (!toWriteAsLasts.isEmpty()) {
            for (SootMethod toWriteAsLast : toWriteAsLasts) {
                JimpleBody body;
                Local testTypeLocal = new JimpleLocal("try", RefType.v(leaf.getName()));
                try {
                    body = (JimpleBody) method.retrieveActiveBody();
                    testTypeLocal = body.getLocals().getFirst();

                } catch (RuntimeException e) {
                    body = Jimple.v().newBody(method);
                    body.getLocals().add(testTypeLocal);
                    body.getUnits().add(Jimple.v().newAssignStmt(testTypeLocal, new JNewExpr(RefType.v(leaf.getName()))));
                    body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(testTypeLocal, Scene.v().makeConstructorRef(Scene.v().getSootClass(leaf.getName()), null))));

                }

                InvokeExpr invoke;
                if (!toWriteAsLast.isStatic())
                    invoke = Jimple.v().newSpecialInvokeExpr(testTypeLocal, toWriteAsLast.makeRef());
                else
                    invoke = Jimple.v().newStaticInvokeExpr(toWriteAsLast.makeRef());

                try {
                    body.getUnits().insertAfter(Jimple.v().newInvokeStmt(invoke), body.getUnits().getLast());
                    //TODO: Serve davvero?
                } catch (NoSuchElementException e) {
                    body.getUnits().add(Jimple.v().newInvokeStmt(invoke));
                }

                method.setActiveBody(body);
            }
        }

        sc.addMethod(method);
        //do we really need to add this fake class as application class?
        //  sc.setApplicationClass();
        Scene.v().addClass(sc);


        return allTesting.isEmpty() ? null : method;
    }


    /*
     * Run spark transformation
     */
    private void createCallgraph() {


        LOGGER.info("rta call graph building...");
        Transform sparkTransform = PackManager.v().getTransform("cg.spark");
        PhaseOptions.v().setPhaseOption(sparkTransform, "enabled:true"); //enable spark transformation
        PhaseOptions.v().setPhaseOption(sparkTransform, "apponly:true");
        PhaseOptions.v().setPhaseOption(sparkTransform, "rta:true"); //enable rta mode for call-graph
        PhaseOptions.v().setPhaseOption(sparkTransform, "verbose:false");
        PhaseOptions.v().setPhaseOption(sparkTransform, "on-fly-cg:false"); //disable default call-graph construction mode (soot not permitted to use rta and on-fly-cg options together)
        PhaseOptions.v().setPhaseOption(sparkTransform, "force-gc:true"); //force call a System.cg() to increase tue available space on garbage collector

        //     Map<String, String> opt = PhaseOptions.v().getPhaseOptions(sparkTranform);
        //     sparkTransform(sparkTranform, opt);
        sparkTransform.apply();

        CallGraph c = Scene.v().getCallGraph(); //take the call-graph builded
        setCallGraph(c); //set the callgraph as call-graph of this project




        // Pack wjpppack = PackManager.v().getPack("wjtp");
        //  wjpppack.add(preprocessingTransfrom);


        //build the spark call-graph with the option setted
        //get the option setted


        //      PackManager.v().runPacks();


    }
}
