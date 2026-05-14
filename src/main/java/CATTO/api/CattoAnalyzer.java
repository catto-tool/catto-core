package CATTO.api;

import CATTO.code.analyzer.CodeAnalyzer;
import CATTO.exception.InvalidTargetPaths;
import CATTO.exception.NoTestFoundedException;
import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.Test;
import CATTO.test.selector.TestSelector;
import soot.SootMethod;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CattoAnalyzer {

    private CattoAnalyzer() {}

    public static AnalysisResult analyze(AnalysisRequest request)
            throws IOException, InvocationTargetException, NoSuchMethodException,
            IllegalAccessException, InvalidTargetPaths, NoTestFoundedException {

        String[] dependencyArray = toStringArray(request.dependencies());
        String[] newClassesArray = toStringArray(request.newClassesPaths());
        String previousClassesPath = request.previousClassesPath().toString();

        PreviousProject previous = new PreviousProject(dependencyArray, previousClassesPath);
        NewProject current = new NewProject(dependencyArray, newClassesArray);

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
                toMethodNameSet(codeAnalyzer.getRemovedTests())
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
