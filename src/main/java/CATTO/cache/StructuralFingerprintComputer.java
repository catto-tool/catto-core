package CATTO.cache;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

public final class StructuralFingerprintComputer {

    private StructuralFingerprintComputer() {}

    /**
     * Compute a fingerprint for all class files in the given directories.
     * Two projects with the same fingerprint have identical:
     * - class names, superclasses, interfaces
     * - method sub-signatures (name + descriptor)
     * - types instantiated via NEW
     * - virtual/interface call targets
     *
     * Body-only changes (arithmetic rewrites, variable renames, return value changes
     * that do not add new instantiated types or new virtual calls) leave the
     * fingerprint unchanged.
     */
    public static String computeProjectFingerprint(List<Path> classDirs) throws IOException {
        TreeMap<String, String> classFingerprints = new TreeMap<>();
        for (Path dir : classDirs) {
            if (!Files.isDirectory(dir)) continue;
            Files.walk(dir)
                 .filter(p -> p.toString().endsWith(".class"))
                 .forEach(classFile -> {
                     try {
                         String fp = computeClassFingerprint(classFile);
                         String name = relativeClassName(dir, classFile);
                         classFingerprints.put(name, fp);
                     } catch (IOException ignored) {}
                 });
        }
        return sha256(classFingerprints.toString());
    }

    private static String computeClassFingerprint(Path classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        ClassStructureVisitor visitor = new ClassStructureVisitor();
        new ClassReader(bytes).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return visitor.fingerprint();
    }

    private static String relativeClassName(Path baseDir, Path classFile) {
        return baseDir.relativize(classFile).toString()
                .replace('/', '.').replace('\\', '.').replace(".class", "");
    }

    static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class ClassStructureVisitor extends ClassVisitor {
        private final StringBuilder sb = new StringBuilder();

        ClassStructureVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            sb.append("C:").append(name).append(';');
            sb.append("S:").append(superName).append(';');
            if (interfaces != null) {
                String[] sorted = interfaces.clone();
                Arrays.sort(sorted);
                for (String iface : sorted) {
                    sb.append("I:").append(iface).append(';');
                }
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                        String signature, String[] exceptions) {
            sb.append("M:").append(name).append(descriptor).append(';');
            return new MethodStructureVisitor(sb);
        }

        String fingerprint() {
            return sb.toString();
        }
    }

    private static final class MethodStructureVisitor extends MethodVisitor {
        private final StringBuilder sb;

        MethodStructureVisitor(StringBuilder sb) {
            super(Opcodes.ASM9);
            this.sb = sb;
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW) {
                sb.append("N:").append(type).append(';');
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKEINTERFACE) {
                sb.append("V:").append(owner).append('.').append(name).append(descriptor).append(';');
            }
        }
    }
}
