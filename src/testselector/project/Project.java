package testselector.project;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import soot.*;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.dot.DotGraph;
import soot.util.queue.QueueReader;
import testselector.exception.NoNameException;
import testselector.exception.NoPathException;
import testselector.exception.NoTestFoundedException;
import testselector.util.Util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.NotDirectoryException;
import java.util.*;

//TODO: REfattorizzare: Non si occupa di "troppe cose" questa classe?

public class Project {
    private ArrayList<SootMethod> applicationMethod;
    private ArrayList<SootClass> projectClasses;
    private ArrayList<SootMethod> entryPoints;
    private CallGraph callGraph;
    private ArrayList<String> target;
    private ArrayList<String> classPath;

    public Map<SootClass, ArrayList<SootMethod>> getTestingClass() {
        return testingClass;
    }

    private Map<SootClass, ArrayList<SootMethod>> testingClass;
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    @Nullable
    private Object o;

    /**
     * The Project's constructor load in soot all class that are in the paths given as a parametrer,
     * after set all tests method present in project as entry point to produce a CallGraph.
     *
     * @param classPath
     * @param target the paths of the classes module
     */
    public Project(String[] classPath, @Nonnull String... target) throws NoTestFoundedException, NotDirectoryException {
        //validate the project paths
        validatePaths(target);

        this.classPath = new ArrayList<>();
        this.target = new ArrayList<>();
        this.projectClasses = new ArrayList<>();
        this.applicationMethod = new ArrayList<>();
        this.entryPoints = new ArrayList<>();
        this.testingClass = new HashMap<>();

        setTarget(target);

        setClassPath(classPath);

        //reset soot
        soot.G.reset();

        //set soot options
        setSootOptions();

        //load all project class in soot
        //   loadClassesAndSupport();

        //load all class needed
        Scene.v().loadNecessaryClasses();
        // Scene.v().loadBasicClasses();
        // Scene.v().loadDynamicClasses();

        //add all classes to this project classes
        setApplicationClass();

        //load all methods of this project
        setApplicationMethod();

        //set all test methoda in projecy as entry points
        setEntryPoints();

        //run the pack and so the callgraph transformation
        runPacks();
    }


    /*
     * Popolate <code>paths</code> ArrayList with the passed string path.
     *
     * @param classPath
     */
    private void setClassPath(String[] classPath) {
        for (int i = 0; i < classPath.length; i++) {
            this.classPath.add(classPath[i]);
        }
    }

    /*
     * Check if the paths passed are valid directories or not
     *
     * @param modulePath the project paths
     * @throws NotDirectoryException if the paths passed are not valid directories
     */
    private void validatePaths(@Nonnull String[] modulePath) throws NotDirectoryException {
        //are the parameter paths valid?
        for (int i = 0; i < modulePath.length; i++) {
            File f = new File(modulePath[i]);
            if (!f.isDirectory())
                throw new NotDirectoryException(f.getAbsolutePath());
        }
    }

    /*
     * Popolate <code>paths</code> ArrayList with the passed string path.
     *
     * @param target
     */
    private void setTarget(@Nonnull String[] target) {
        for (int i = 0; i < target.length; i++) {
            this.target.add(target[i]);
        }
    }

    /*
     * Add the application classes loaded in soot in <code>projectClasses</code> ArrayList
     */
    private void setApplicationClass() {
        projectClasses.addAll(Scene.v().getApplicationClasses());
    }


    /*
     * Loads the class of the project in soot
     */
    private void loadClassesAndSupport() {
        List<String> classToLoad = processClasses();
        for (String classPath : classToLoad) {
            //add all classes founded in the passed directory first in SootScene as application class and then in projectClass ArrayList
            loadClass(classPath);
        }
    }


    /*
     * Load class using soot method loadClassAndSupport
     *
     * @param name the name in soot-format of the class to losd
     * @return the sootClass that rappresented the class loaded.
     */
    private SootClass loadClass(@Nonnull String name) {
        //Load class in Soot Scene with SIGNATURE level
        SootClass c = Scene.v().loadClassAndSupport(name);
        //set the Soot Class as application class
        c.setApplicationClass();
        //return the class loaded
        return c;
    }

    /**
     * Set the option for soot.
     */
    private void setSootOptions() {
        List<String> argsList = new ArrayList<>();
        argsList.add("-verbose"); //verbose mode
        argsList.add("--via-grimp");
        argsList.add("-W"); // whole program mode
        argsList.add("-no-bodies-for-excluded"); //don't load bodies for excluded classes, so for non-application-classes
        argsList.add("-allow-phantom-refs"); // allow to don't load some classes (it's necessary for "no-bodies-for-excluded" option)
        argsList.add("-cp");// Soot class-paths
        //add all modules path to Soot class-paths
        String s = new String();
        for (int i = 0; i < classPath.size(); i++) {
            s += classPath.get(i) + ";";
        }
        argsList.add(s);

        //"C:\\Users\\Dario\\.m2\\repository\\org\\hamcrest\\hamcrest-all\\1.3\\hamcrest-all-1.3.jar;C:\\Program Files\\Java\\jdk1.8.0_112\\jre\\lib\\rt.jar;C:\\Program Files\\Java\\jdk1.8.0_112\\jre\\lib\\jce.jar;C:\\Users\\Dario\\.m2\\repository\\junit\\junit\\4.12\\junit-4.12.jar"

        //set all modules path as directories to process
        for (int i = 0; i < target.size(); i++) {
            argsList.add("-process-dir");
            argsList.add(target.get(i));
        }

        //parse the option
        Options.v().parse(argsList.toArray(new String[0]));

    }
    //  https://www.spankingtube.com/video/72545/ok-boss-i-m-ready-to-be-strapped-the-extended-cut

    /*
     * Run spark transformation
     */
    private void runPacks() {
        Transform sparkTranform = new Transform("cg.spark", null);

        PhaseOptions.v().setPhaseOption(sparkTranform, "enabled:true"); //enable spark transformation
        PhaseOptions.v().setPhaseOption(sparkTranform, "rta:true"); //enable rta mode for call-graph
        PhaseOptions.v().setPhaseOption(sparkTranform, "verbose:true");
        PhaseOptions.v().setPhaseOption(sparkTranform, "on-fly-cg:false"); //disable default call-graph construction mode (soot not permitted to use rta and on-fly-cg options together)
        PhaseOptions.v().setPhaseOption(sparkTranform, "force-gc:true"); //force call a System.cg() to increase tue available space on garbage collector
        //PhaseOptions.v().setPhaseOption(sparkTranform, "apponly:true"); //indicate to process only the application-classes

        Map<String, String> opt = PhaseOptions.v().getPhaseOptions(sparkTranform); //get the option setted


        LOGGER.info("rta call graph building...");
        sparkTransform(sparkTranform, opt); //build the spark call-graph with the option setted

        CallGraph c = Scene.v().getCallGraph(); //take the call-graph builded
        setCallGraph(c); //set the callgraph as call-graph of this project
    }

    private void sparkTransform(Transform sparkTranform, Map<String, String> opt) {
        SparkTransformer.v().transform(sparkTranform.getPhaseName(), opt);

    }

    /**
     * Save the generated call graph in .dot format. To get a claer callgraph all java,sun,org,jdk,javax methods and calls in the saved callgraph not appear
     *
     * @param path a string that represent the path where save the callgraph
     * @param name the name with wich save the callgraph
     * @throws NoPathException if the path passed is empty or null
     * @throws NoNameException if the name passed is empty or null
     */
    public void saveCallGraph(String path, String name) throws NoPathException, NoNameException {
        if (path == null || path.isEmpty())
            throw new NoPathException();
        if (name == null || name.isEmpty())
            throw new NoNameException();
        LOGGER.info("Serialize call graph...");
        DotGraph canvas = new DotGraph(name + "-call-graph");
        QueueReader<Edge> listener = this.getCallGraph().listener();
        while (listener.hasNext()) {
            Edge next = listener.next();
            MethodOrMethodContext src = next.getSrc();
            MethodOrMethodContext tgt = next.getTgt();
            String srcToString = src.toString();
            String tgtToString = tgt.toString();
            if ((!srcToString.startsWith("<sun.") && !srcToString.startsWith("<org.") && !srcToString.startsWith("<jdk.") && !srcToString.startsWith("<javax.")) || (!tgtToString.startsWith("<java.") && !tgtToString.startsWith("<sun.") && !tgtToString.startsWith("<org.") && !tgtToString.startsWith("<jdk.") && !tgtToString.startsWith("<javax."))) {
                canvas.drawNode(srcToString);
                canvas.drawNode(tgtToString);
                canvas.drawEdge(srcToString, tgtToString);
            }
        }


        canvas.plot(path + File.separator + File.separator + name + "-call-graph" + DotGraph.DOT_EXTENSION);
        new File(path);
    }

    /**
     * Get all methods in this project.
     * @return a {@link soot.SootMethod} list with all methods in this project.
     */
    public List<SootMethod> getApplicationMethod() {
        return applicationMethod;
    }

    private void setApplicationMethod() {
        for (SootClass projectClass : this.projectClasses) {
            this.applicationMethod.addAll(projectClass.getMethods());
        }
    }

    /**
     * Get the {@link CallGraph} generated for this project
     * @return a {@link CallGraph} object that represent the callgraph generated for this project
     */
    public CallGraph getCallGraph() {
        return callGraph;
    }

    /**
     * Set the {@link CallGraph} for this project
     * @param callGraph the {@link CallGraph} to set for this project
     */
    public void setCallGraph(CallGraph callGraph) {
        this.callGraph = callGraph;
    }

    /**
     * Get the target setted for this project
     *
     * @return a String List with the path of the modules setted for this project
     */
    public List<String> getTarget() {
        return target;
    }

    /**
     * Get the all classes in this project
     * @return a {@link SootClass} List with the path of the modules setted for this project    )
     */
    public List<SootClass> getProjectClasses() {
        return projectClasses;
    }

    /**
     * Get the entry points for this project. The entry points in this case are the tests methods present in this project, so tha {@link CallGraph} start from this entry points.
     * @return a  {@link SootMethod} List which contains the entry points for this project
     */
    public List<SootMethod> getEntryPoints() {
        return entryPoints;
    }

    /*
     * Scan all the folders of the project and return the soot-format-name of the classes.
     *
     * @return An ArrayList with the soot-format-name of the all classes in the project
     */
    private List<String> processClasses() {
        List<File> fileToAdd;
        fileToAdd = processDirectory();
        List<String> classToProcess = new ArrayList<>();
        for (File f : fileToAdd) {
            String fName = f.getName().replace(".class", "");
            String fPath = f.getAbsolutePath().replace("\\", "-");
            String[] fPackage = fPath.split("-");
            int i = fPackage.length - 2;
            classToProcess.add(fPackage[i].concat(".").concat(fName));
        }

        return classToProcess;
    }

    /**
     * Get the hashcode for this project calculated with the method {@link Objects}.hash().
     * @return a int hashcode for this project.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getProjectClasses());
    }

    /**
     * Check if two project are equal.
     *
     * @param o the project to confront
     * @return true only if the two project contains the same classes
     */
    @Override
    public boolean equals(@Nullable Object o) {
        this.o = o;
        if (o == null)
            return false;


        if (o.getClass() != this.getClass())
            return false;

        Project p = (Project) o;
        boolean check = true;
        for (SootClass sc : this.getProjectClasses()) {
            if (!p.getProjectClasses().contains(sc))
                check = false;
        }
        for (SootClass sc : p.getProjectClasses()) {
            if (!this.getProjectClasses().contains(sc))
                check = false;
        }
        return check;
    }


    /*
     * Scan all the folders of the project and retunr the class file of the project
     *
     * @return a list that contains all classes of the project in file format
     */
    private List<File> processDirectory() {
        ArrayList<File> classFile = new ArrayList<>();
        //for each modules path
        for (String path : target) {
            //get a list of file
            List<File> file = (List<File>) FileUtils.listFiles(new File(path), TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            //for each file
            for (File f : file) {
                //if the file is .class
                if ("class".equals(FilenameUtils.getExtension(f.getAbsolutePath())))
                    //add file
                    classFile.add(f);
            }
        }
        //return the class file of the project
        return classFile;
    }


    /*
     * Set all test-methods of the project as entry point for soot.
     */
    private void setEntryPoints() throws NoTestFoundedException {

        LOGGER.info("setting all test methods as entry points...");
        //get all project classes
        List<SootClass> appClass = getProjectClasses();
        //for all project classes
        for (SootClass s : appClass) {
            //get all methods of class
            List<SootMethod> classMethods = s.getMethods();
            //for each method
            for (SootMethod sootMethod : classMethods) {
                // if is a JUnit test method
                //Todo: perchè selezionare come entry point anche i metodi annotati con @before, @after, @beforeClass, @afterClass? Solo per farli comparire nel callgraph?
                if (Util.isATestMethod(sootMethod)) {
                    //add methos as entry point
                    entryPoints.add(sootMethod);
                    if (testingClass.containsKey(s))
                        testingClass.get(s).add(sootMethod);
                    else {
                        ArrayList<SootMethod> sm = new ArrayList<>();
                        sm.add(sootMethod);
                        testingClass.put(s, sm);
                    }
                }
            }
        }


        //if there isn't test
        if (entryPoints.isEmpty())
            //get exception
            throw new NoTestFoundedException();
        //set all test-methods founded as soot entry points
        Scene.v().setEntryPoints(entryPoints);

    }

    public void removeEntryPoint(SootMethod entryPointToRemovc) {
        entryPoints.remove(entryPointToRemovc);
    }
}


