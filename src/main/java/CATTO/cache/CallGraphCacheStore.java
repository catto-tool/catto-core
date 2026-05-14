package CATTO.cache;

import org.apache.log4j.Logger;
import soot.Kind;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("deprecation")

public final class CallGraphCacheStore {

    private static final Logger LOGGER = Logger.getLogger(CallGraphCacheStore.class);
    static final String CACHE_FILE = "catto-callgraph.cache";

    private static final Map<String, Kind> KIND_BY_NAME = Map.ofEntries(
            Map.entry("INVALID", Kind.INVALID),
            Map.entry("STATIC", Kind.STATIC),
            Map.entry("VIRTUAL", Kind.VIRTUAL),
            Map.entry("INTERFACE", Kind.INTERFACE),
            Map.entry("SPECIAL", Kind.SPECIAL),
            Map.entry("CLINIT", Kind.CLINIT),
            Map.entry("GENERIC_FAKE", Kind.GENERIC_FAKE),
            Map.entry("THREAD", Kind.THREAD),
            Map.entry("EXECUTOR", Kind.EXECUTOR),
            Map.entry("ASYNCTASK", Kind.ASYNCTASK),
            Map.entry("FINALIZE", Kind.FINALIZE),
            Map.entry("HANDLER", Kind.HANDLER),
            Map.entry("PRIVILEGED", Kind.PRIVILEGED),
            Map.entry("NEWINSTANCE", Kind.NEWINSTANCE)
    );

    private CallGraphCacheStore() {}

    public static Optional<String> loadFingerprint(Path cacheDir) throws IOException {
        Path file = cacheDir.resolve(CACHE_FILE);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = r.readLine();
            if (line != null && line.startsWith("FP:")) {
                return Optional.of(line.substring(3));
            }
        }
        return Optional.empty();
    }

    public static Optional<CallGraph> loadCallGraph(Path cacheDir) throws IOException {
        Path file = cacheDir.resolve(CACHE_FILE);
        if (!Files.isRegularFile(file)) return Optional.empty();

        CallGraph cg = new CallGraph();
        int loaded = 0;
        int skipped = 0;

        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            r.readLine(); // skip fingerprint line
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("E:")) continue;
                String[] parts = line.substring(2).split("\\|", 3);
                if (parts.length != 3) continue;
                SootMethod src = resolveMethod(parts[0]);
                SootMethod tgt = resolveMethod(parts[1]);
                if (src == null || tgt == null) {
                    skipped++;
                    continue;
                }
                Kind kind = KIND_BY_NAME.getOrDefault(parts[2], Kind.INVALID);
                cg.addEdge(new Edge(src, null, tgt, kind));
                loaded++;
            }
        }
        LOGGER.info("Call graph cache loaded: " + loaded + " edges, " + skipped + " unresolvable edges skipped");
        return Optional.of(cg);
    }

    public static void saveCallGraph(Path cacheDir, String fingerprint, CallGraph cg) throws IOException {
        Files.createDirectories(cacheDir);
        Path file = cacheDir.resolve(CACHE_FILE);
        int saved = 0;
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("FP:" + fingerprint);
            w.newLine();
            Iterator<Edge> it = cg.iterator();
            while (it.hasNext()) {
                Edge edge = it.next();
                SootMethod src = edge.getSrc().method();
                SootMethod tgt = edge.getTgt().method();
                if (src == null || tgt == null) continue;
                w.write("E:" + src.getSignature() + "|" + tgt.getSignature() + "|" + edge.kind().name());
                w.newLine();
                saved++;
            }
        }
        LOGGER.info("Call graph cache saved: " + saved + " edges to " + file);
    }

    private static SootMethod resolveMethod(String signature) {
        try {
            return Scene.v().getMethod(signature);
        } catch (Exception e) {
            return null;
        }
    }

}
