package CATTO.cache;

import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class StructuralFingerprintTest {

    @Test
    public void sameSourceProducesSameFingerprint() throws Exception {
        String source = "package fp; public class Same { public int compute() { return 1; } }";
        Path a = compileToTemp(source);
        Path b = compileToTemp(source);

        String fpA = StructuralFingerprintComputer.computeProjectFingerprint(List.of(a));
        String fpB = StructuralFingerprintComputer.computeProjectFingerprint(List.of(b));

        assertEquals("Same source must produce same fingerprint", fpA, fpB);
    }

    @Test
    public void bodyOnlyChangePreservesFingerprint() throws Exception {
        Path v1 = compileToTemp("package fp; public class BodyChange { public int compute() { return 1; } }");
        Path v2 = compileToTemp("package fp; public class BodyChange { public int compute() { return 2; } }");

        String fp1 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v1));
        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v2));

        assertEquals("Body-only change must not alter structural fingerprint", fp1, fp2);
    }

    @Test
    public void newMethodChangesFingerprint() throws Exception {
        Path v1 = compileToTemp("package fp; public class NewMethod { public int foo() { return 1; } }");
        Path v2 = compileToTemp("package fp; public class NewMethod { public int foo() { return 1; } public int bar() { return 2; } }");

        String fp1 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v1));
        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v2));

        assertNotEquals("Adding a method must change the structural fingerprint", fp1, fp2);
    }

    @Test
    public void removedMethodChangesFingerprint() throws Exception {
        Path v1 = compileToTemp("package fp; public class RemoveMethod { public int foo() { return 1; } public int bar() { return 2; } }");
        Path v2 = compileToTemp("package fp; public class RemoveMethod { public int foo() { return 1; } }");

        String fp1 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v1));
        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v2));

        assertNotEquals("Removing a method must change the structural fingerprint", fp1, fp2);
    }

    @Test
    public void newInstantiatedTypeChangesFingerprint() throws Exception {
        Path helper = compileToTemp("package fp; public class Helper { public int value() { return 99; } }");
        Path v1 = compileToTemp("package fp; public class WithNew { public int compute() { return 1; } }", helper);
        Path v2 = compileToTemp("package fp; public class WithNew { public int compute() { return new Helper().value(); } }", helper);

        String fp1 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v1));
        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v2));

        assertNotEquals("Adding a NEW instruction must change the structural fingerprint", fp1, fp2);
    }

    @Test
    public void superclassChangeChangesFingerprint() throws Exception {
        Path v1 = compileToTemp("package fp; public class Sub extends java.util.ArrayList { public int x() { return 1; } }");
        Path v2 = compileToTemp("package fp; public class Sub extends java.util.LinkedList { public int x() { return 1; } }");

        String fp1 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v1));
        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(v2));

        assertNotEquals("Changing superclass must change the structural fingerprint", fp1, fp2);
    }

    @Test
    public void emptyDirectoryProducesStableFingerprint() throws Exception {
        Path empty = Files.createTempDirectory("fp-empty-");
        String fp = StructuralFingerprintComputer.computeProjectFingerprint(List.of(empty));
        assertNotNull(fp);
        assertFalse(fp.isBlank());

        String fp2 = StructuralFingerprintComputer.computeProjectFingerprint(List.of(empty));
        assertEquals("Empty directory must produce stable fingerprint", fp, fp2);
    }

    private static Path compileToTemp(String source, Path... extraClasspathDirs) throws Exception {
        Path srcDir = Files.createTempDirectory("fp-src-");
        Path outDir = Files.createTempDirectory("fp-out-");

        String packagePath = "fp";
        String className = extractClassName(source);
        Path sourceFile = srcDir.resolve(packagePath).resolve(className + ".java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("No JavaCompiler — run with JDK");

        StringBuilder cp = new StringBuilder(System.getProperty("java.class.path", ""));
        for (Path extra : extraClasspathDirs) {
            cp.append(File.pathSeparator).append(extra.toString());
        }

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Boolean ok = compiler.getTask(null, fm, null,
                    Arrays.asList("-d", outDir.toString(), "-classpath", cp.toString()),
                    null,
                    fm.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()))
            ).call();
            if (!Boolean.TRUE.equals(ok)) throw new AssertionError("Compile failed: " + source);
        }
        return outDir;
    }

    private static String extractClassName(String source) {
        for (String token : source.split("\\s+")) {
            if (token.equals("class") || token.equals("interface") || token.equals("enum")) {
                continue;
            }
            if (source.contains("class " + token) || source.contains("interface " + token)) {
                return token.replaceAll("[{(].*", "");
            }
        }
        throw new IllegalArgumentException("Cannot extract class name from: " + source);
    }
}
