package CATTO.api;

import CATTO.cache.CallGraphCacheStore;
import CATTO.cache.StructuralFingerprintComputer;
import CATTO.code.analyzer.CodeAnalyzer;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.Test;
import CATTO.test.selector.TestSelector;
import org.apache.log4j.Logger;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class CattoAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(CattoAnalyzer.class);

    private CattoAnalyzer() {}

    public static AnalysisResult analyze(AnalysisRequest request)
            throws IOException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InvalidTargetPaths, NoTestFoundedException {

        String[] dependencyArray = toStringArray(request.dependencies());
        String[] newClassesArray = toStringArray(request.newClassesPaths());
        String previousClassesPath = request.previousClassesPath().toString();

        String newFingerprint = StructuralFingerprintComputer.computeProjectFingerprint(request.newClassesPaths());

        boolean cacheUsed = false;
        NewProject current;

        Optional<Path> cacheDir = request.callGraphCacheDirectory();
        if (cacheDir.isPresent()) {
            Optional<String> cachedFingerprint = CallGraphCacheStore.loadFingerprint(cacheDir.get());
            if (cachedFingerprint.isPresent() && cachedFingerprint.get().equals(newFingerprint)) {
                Optional<CallGraph> cachedGraph = CallGraphCacheStore.loadCallGraph(cacheDir.get());
                if (cachedGraph.isPresent()) {
                    LOGGER.info("Call graph cache hit — skipping Spark RTA");
                    PreviousProject previous = new PreviousProject(dependencyArray, previousClassesPath);
                    current = new NewProject(dependencyArray, cachedGraph.get(), newClassesArray);
                    cacheUsed = true;
                    return runAnalysis(request, previous, current, cacheUsed, cacheDir, newFingerprint);
                }
            }
        }

        PreviousProject previous = new PreviousProject(dependencyArray, previousClassesPath);
        current = new NewProject(dependencyArray, newClassesArray);

        if (cacheDir.isPresent()) {
            LOGGER.info("Call graph cache miss — running Spark RTA and saving cache");
            CallGraphCacheStore.saveCallGraph(cacheDir.get(), newFingerprint, current.getCallGraph());
        }

        return runAnalysis(request, previous, current, false, cacheDir, newFingerprint);
    }

    private static AnalysisResult runAnalysis(AnalysisRequest request, PreviousProject previous,
                                              NewProject current, boolean cacheUsed,
                                              Optional<Path> cacheDir, String newFingerprint)
            throws NoTestFoundedException, IOException {
        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(current, previous);
        codeAnalyzer.analyze();

        TestSelector selector = new TestSelector(
                current,
                codeAnalyzer.getDifferentMethods(),
                codeAnalyzer.getDifferentTest(),
                codeAnalyzer.getNewMethods(),
                codeAnalyzer.getDifferentObject()
        );
        Set<Test> selected = selector.selectTest();

        return new AnalysisResult(
                toTestNameSet(selected),
                new HashSet<>(codeAnalyzer.getChangedMethods()),
                new HashSet<>(codeAnalyzer.getStringNewMethods()),
                toMethodNameSet(codeAnalyzer.getRemovedTests()),
                cacheUsed
        );
    }

    private static Set<String> toTestNameSet(Set<Test> tests) {
        Set<String> names = new HashSet<>();
        for (Test t : tests) {
            SootMethod m = t.getTestMethod();
            names.add(m.getDeclaringClass().getName() + "#" + m.getName());
        }
        return names;
    }

    private static Set<String> toMethodNameSet(Set<SootMethod> methods) {
        Set<String> names = new HashSet<>();
        for (SootMethod m : methods) {
            names.add(m.getDeclaringClass().getName() + "#" + m.getName());
        }
        return names;
    }

    private static String[] toStringArray(List<Path> paths) {
        return paths.stream().map(Path::toString).toArray(String[]::new);
    }
}
