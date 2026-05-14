package CATTO.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class AnalysisResult {
    public enum Status { TESTS_FOUND, NO_TESTS_FOUND }

    private final Set<String> selectedTests;
    private final Set<String> changedMethods;
    private final Set<String> newMethods;
    private final Set<String> removedTests;
    private final Map<String, Set<String>> selectionReasons;
    private final Status status;
    private final boolean callGraphFromCache;

    public AnalysisResult(Set<String> selectedTests, Set<String> changedMethods,
                          Set<String> newMethods, Set<String> removedTests,
                          boolean callGraphFromCache) {
        this(selectedTests, changedMethods, newMethods, removedTests, Map.of(), callGraphFromCache);
    }

    public AnalysisResult(Set<String> selectedTests, Set<String> changedMethods,
                          Set<String> newMethods, Set<String> removedTests,
                          Map<String, Set<String>> selectionReasons, boolean callGraphFromCache) {
        this.selectedTests = Collections.unmodifiableSet(new TreeSet<>(selectedTests));
        this.changedMethods = Collections.unmodifiableSet(new TreeSet<>(changedMethods));
        this.newMethods = Collections.unmodifiableSet(new TreeSet<>(newMethods));
        this.removedTests = Collections.unmodifiableSet(new TreeSet<>(removedTests));
        Map<String, Set<String>> copy = new TreeMap<>();
        for (Map.Entry<String, Set<String>> e : selectionReasons.entrySet()) {
            copy.put(e.getKey(), Collections.unmodifiableSet(new HashSet<>(e.getValue())));
        }
        this.selectionReasons = Collections.unmodifiableMap(copy);
        this.status = selectedTests.isEmpty() ? Status.NO_TESTS_FOUND : Status.TESTS_FOUND;
        this.callGraphFromCache = callGraphFromCache;
    }

    public Set<String> selectedTests() { return selectedTests; }
    public Set<String> changedMethods() { return changedMethods; }
    public Set<String> newMethods() { return newMethods; }
    public Set<String> removedTests() { return removedTests; }
    public Map<String, Set<String>> selectionReasons() { return selectionReasons; }
    public Status status() { return status; }
    public boolean callGraphFromCache() { return callGraphFromCache; }

    public int exitCode() {
        return status == Status.TESTS_FOUND ? 0 : 2;
    }
}
