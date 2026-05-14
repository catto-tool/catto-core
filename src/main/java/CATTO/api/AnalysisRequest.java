package CATTO.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class AnalysisRequest {
    private final Path previousClassesPath;
    private final List<Path> newClassesPaths;
    private final List<Path> dependencies;
    private final Path callGraphCacheDirectory;

    private AnalysisRequest(Builder builder) {
        this.previousClassesPath = Objects.requireNonNull(builder.previousClassesPath, "previousClassesPath");
        this.newClassesPaths = Collections.unmodifiableList(new ArrayList<>(builder.newClassesPaths));
        this.dependencies = Collections.unmodifiableList(new ArrayList<>(builder.dependencies));
        this.callGraphCacheDirectory = builder.callGraphCacheDirectory;
    }

    public Path previousClassesPath() { return previousClassesPath; }
    public List<Path> newClassesPaths() { return newClassesPaths; }
    public List<Path> dependencies() { return dependencies; }
    public Optional<Path> callGraphCacheDirectory() { return Optional.ofNullable(callGraphCacheDirectory); }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Path previousClassesPath;
        private final List<Path> newClassesPaths = new ArrayList<>();
        private final List<Path> dependencies = new ArrayList<>();
        private Path callGraphCacheDirectory;

        private Builder() {}

        public Builder previousClassesPath(Path path) {
            this.previousClassesPath = path;
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

        public AnalysisRequest build() {
            return new AnalysisRequest(this);
        }
    }
}
