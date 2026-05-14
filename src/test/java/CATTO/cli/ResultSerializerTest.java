package CATTO.cli;

import CATTO.api.AnalysisResult;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class ResultSerializerTest {

    private static AnalysisResult result(String status, Set<String> selected,
                                         Set<String> changed, Set<String> newM, Set<String> removed) {
        return new AnalysisResult(selected, changed, newM, removed, false);
    }

    @Test
    public void jsonContainsStatusTestsFound() {
        AnalysisResult r = result("TESTS_FOUND",
                Set.of("pkg.FooTest#testA"), Set.of("pkg.Foo.compute"), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"status\": \"TESTS_FOUND\""));
    }

    @Test
    public void jsonContainsStatusNoTestsFound() {
        AnalysisResult r = result("NO_TESTS_FOUND",
                Set.of(), Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"status\": \"NO_TESTS_FOUND\""));
    }

    @Test
    public void jsonContainsSelectedTests() {
        AnalysisResult r = result("TESTS_FOUND",
                Set.of("pkg.FooTest#testA", "pkg.BarTest#testB"),
                Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"pkg.FooTest#testA\""));
        assertTrue(json.contains("\"pkg.BarTest#testB\""));
    }

    @Test
    public void jsonContainsChangedAndNewAndRemovedMethods() {
        AnalysisResult r = result("TESTS_FOUND",
                Set.of("pkg.T#t"),
                Set.of("pkg.Foo.bar"),
                Set.of("pkg.Foo.added"),
                Set.of("pkg.OldTest#gone"));
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"pkg.Foo.bar\""));
        assertTrue(json.contains("\"pkg.Foo.added\""));
        assertTrue(json.contains("\"pkg.OldTest#gone\""));
    }

    @Test
    public void emptyArraysProduceValidJson() {
        AnalysisResult r = result("NO_TESTS_FOUND",
                Set.of(), Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"selectedTests\": []"));
        assertTrue(json.contains("\"changedMethods\": []"));
        assertTrue(json.contains("\"newMethods\": []"));
        assertTrue(json.contains("\"removedTests\": []"));
    }

    @Test
    public void jsonHasAllTopLevelKeys() {
        AnalysisResult r = result("NO_TESTS_FOUND",
                Set.of(), Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\"status\""));
        assertTrue(json.contains("\"selectedTests\""));
        assertTrue(json.contains("\"changedMethods\""));
        assertTrue(json.contains("\"newMethods\""));
        assertTrue(json.contains("\"removedTests\""));
    }

    @Test
    public void specialCharactersInNamesAreEscaped() {
        AnalysisResult r = result("TESTS_FOUND",
                Set.of("pkg.Test#method\"with\"quotes"),
                Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r);
        assertTrue(json.contains("\\\"with\\\""));
    }

    @Test
    public void outputStartsAndEndsWithBraces() {
        AnalysisResult r = result("NO_TESTS_FOUND",
                Set.of(), Set.of(), Set.of(), Set.of());
        String json = ResultSerializer.toJson(r).trim();
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }
}
