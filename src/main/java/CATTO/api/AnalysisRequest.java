package CATTO.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AnalysisRequest {
    private final List<Path> previousClassesPaths;
    private final List<Path> newClassesPaths;
    private final List<Path> dependencies;
    private final Path callGraphCacheDirectory;
    private final Long analysisTimeoutSeconds;

    private AnalysisRequest(Builder builder) {
        if (builder.previousClassesPaths.isEmpty()) {
            throw new IllegalArgumentException("at least one previousClassesPath required");
        }
        this.previousClassesPaths = Collections.unmodifiableList(new ArrayList<>(builder.previousClassesPaths));
        this.newClassesPaths = Collections.unmodifiableList(new ArrayList<>(builder.newClassesPaths));
        this.dependencies = Collections.unmodifiableList(new ArrayList<>(builder.dependencies));
        this.callGraphCacheDirectory = builder.callGraphCacheDirectory;
        this.analysisTimeoutSeconds = builder.analysisTimeoutSeconds;
    }

    /** Returns all previous-version class directories (one per module in multi-module projects). */
    public List<Path> previousClassesPaths() { return previousClassesPaths; }

    /** Convenience accessor for single-module use — returns the first previous path. */
    public Path previousClassesPath() { return previousClassesPaths.get(0); }

    public List<Path> newClassesPaths() { return newClassesPaths; }
    public List<Path> dependencies() { return dependencies; }
    public Optional<Path> callGraphCacheDirectory() { return Optional.ofNullable(callGraphCacheDirectory); }

    /** Returns the configured timeout in seconds, or empty if no timeout is set. */
    public Optional<Long> analysisTimeoutSeconds() { return Optional.ofNullable(analysisTimeoutSeconds); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<Path> previousClassesPaths = new ArrayList<>();
        private final List<Path> newClassesPaths = new ArrayList<>();
        private final List<Path> dependencies = new ArrayList<>();
        private Path callGraphCacheDirectory;
        private Long analysisTimeoutSeconds;

        private Builder() {}

        /** Sets a single previous classes directory (single-module convenience). */
        public Builder previousClassesPath(Path path) {
            this.previousClassesPaths.clear();
            this.previousClassesPaths.add(path);
            return this;
        }

        /** Adds a previous classes directory — use multiple times for multi-module projects. */
        public Builder addPreviousClassesPath(Path path) {
            this.previousClassesPaths.add(path);
            return this;
        }

        public Builder addNewClassesPath(Path path) {
            newClassesPaths.add(path);
            return this;
        }

        public Builder newClassesPaths(List<Path> paths) {
            newClassesPaths.addAll(paths);
            return this;
        }

        public Builder addDependency(Path path) {
            dependencies.add(path);
            return this;
        }

        public Builder dependencies(List<Path> paths) {
            dependencies.addAll(paths);
            return this;
        }

        public Builder callGraphCacheDirectory(Path dir) {
            this.callGraphCacheDirectory = dir;
            return this;
        }

        /** Sets the maximum time in seconds the analysis may run. No timeout if not set. */
        public Builder analysisTimeoutSeconds(long seconds) {
            this.analysisTimeoutSeconds = seconds;
            return this;
        }

        public AnalysisRequest build() {
            return new AnalysisRequest(this);
        }
    }
}
