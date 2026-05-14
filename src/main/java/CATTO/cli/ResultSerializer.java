package CATTO.cli;

import CATTO.api.AnalysisResult;

import java.util.Set;

public final class ResultSerializer {

    private ResultSerializer() {}

    public static String toJson(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"status\": \"").append(result.status().name()).append("\",\n");
        sb.append("  \"selectedTests\": ").append(arrayOf(result.selectedTests())).append(",\n");
        sb.append("  \"changedMethods\": ").append(arrayOf(result.changedMethods())).append(",\n");
        sb.append("  \"newMethods\": ").append(arrayOf(result.newMethods())).append(",\n");
        sb.append("  \"removedTests\": ").append(arrayOf(result.removedTests())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private static String arrayOf(Set<String> values) {
        if (values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[\n");
        boolean first = true;
        for (String v : values) {
            if (!first) sb.append(",\n");
            sb.append("    \"").append(escape(v)).append("\"");
            first = false;
        }
        sb.append("\n  ]");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
