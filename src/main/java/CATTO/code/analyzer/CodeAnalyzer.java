package CATTO.code.analyzer;

import CATTO.project.NewProject;
import CATTO.project.PreviousProject;
import CATTO.test.Test;
import CATTO.util.JunitUtil;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import soot.Modifier;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CodeAnalyzer {
    private final HashSet<SootMethod> equalsMethods;
    private final HashSet<Test> differentTest;
    private final HashSet<SootClass> differentObject;
    private final HashSet<SootMethod> newMethods;
    NewProject newProjectVersion;
    PreviousProject previousProjectVersion;
    private final HashSet<SootMethod> differentMethods;
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(CodeAnalyzer.class);

    public CodeAnalyzer(NewProject newProjectVersion, PreviousProject previousProjectVersion){
        this.newProjectVersion = newProjectVersion;
        this.previousProjectVersion = previousProjectVersion;
        this.differentMethods = new HashSet<>();
        this.equalsMethods = new HashSet<>();
        this.differentTest = new HashSet<Test>();
        this.differentObject = new HashSet<SootClass>();
        this.newMethods = new HashSet<>();

    }

    public void analyze(){
        LOGGER.info("searching changes in hierarchies");
        findDifferenceInHierarchy();
        LOGGER.info("searching modified methods");
        findDifferentMethods();
        LOGGER.info("searching new methods");
        findNewMethods();
        LOGGER.info("searching different tests");
        comparingTest();
        LOGGER.info("comparing classes (to check if the constructors are equals)");
        isTheSameObject();
    }

    public HashSet<SootClass> getDifferentObject() {
        return differentObject;
    }

    public HashSet<SootMethod> getDifferentMethods() {
        return differentMethods;
    }

    public HashSet<Test> getDifferentTest() {
        return differentTest;
    }

    private void findDifferenceInHierarchy() {
        ArrayList<SootMethod> differentHierarchy = new ArrayList<>();
        ArrayList<SootMethod> deletedMethods = new ArrayList<>();
        for (SootMethod m : previousProjectVersion.getApplicationMethod()) {
            boolean isIn = false;
            for (SootMethod m1 : newProjectVersion.getApplicationMethod()) {
                if (m.getSignature().equals(m1.getSignature()))
                    isIn = true;
            }
            if (!isIn) {
                deletedMethods.add(m);

            }

        }

        for (SootMethod deleted : deletedMethods) {
            for (SootClass subClass : previousProjectVersion.getHierarchy().getSubclassesOf(deleted.getDeclaringClass())) {
                for (SootMethod override : subClass.getMethods()) {
                    if (override.getSubSignature().equals(deleted.getSubSignature()))
                        differentHierarchy.add(override);
                }
            }

            for (SootClass subClass : previousProjectVersion.getHierarchy().getSuperclassesOf(deleted.getDeclaringClass())) {
                for (SootMethod override : subClass.getMethods()) {
                    if (override.getSubSignature().equals(deleted.getSubSignature()))
                        differentHierarchy.add(override);
                }
            }
        }

        for (SootMethod toMarkBecauseCallDeleteMethods : newProjectVersion.getApplicationMethod()) {
            for (SootMethod methodDifferentInHierarchy : differentHierarchy) {


                if (methodDifferentInHierarchy.getSignature().equals(toMarkBecauseCallDeleteMethods.getSignature())) {
                    LOGGER.info("The method: " + toMarkBecauseCallDeleteMethods.getDeclaringClass().getName() + "." + toMarkBecauseCallDeleteMethods.getName() + " has been marked has modified because the method in his hierarchy " + methodDifferentInHierarchy.getDeclaringClass() + "." + methodDifferentInHierarchy.getName() + " has been deleted");
                    differentMethods.add(toMarkBecauseCallDeleteMethods);
                }
            }
        }


    }

    private void findDifferentMethods() {
        Date start = new Date();

        LOGGER.debug("start find different methods at " + start.getTime());
        HashSet<SootClass> p1Class = (HashSet<SootClass>) newProjectVersion.getProjectClasses();
        HashSet<SootClass> copyPClass = (HashSet<SootClass>) previousProjectVersion.getProjectClasses();
        for (SootClass s1 : p1Class) {
            SootClass classToRemove;
            List<SootClass> pClass = new ArrayList<>(copyPClass);
            for (SootClass s : pClass) {
                if (s.getName().equals(s1.getName())) {
                    classToRemove = s;
                    List<SootMethod> ms1 = s1.getMethods();
                    for (SootMethod m1 : ms1) {
                        if (Modifier.isAbstract(m1.getModifiers())) {
                            equalsMethods.add(m1);
                            continue;
                        }
//                        // mi assicuro che il metodo che sto confrontando non sia il metodo della classe madre ma quello della classe figlia
//
                        for (SootMethod m : s.getMethods()) {
                            if (haveSameParameter(m, m1) && m.getName().equals(m1.getName())) {
                                if (!isEquals(m, m1)) {

                                    differentMethods.add(m1);
                                } else
                                    equalsMethods.add(m1);

                                break;
                            }
                        }
                    }
                    copyPClass.remove(classToRemove);
                    break;
                }
            }

        }
        start = new Date();
        LOGGER.debug("finish find different methods at " + start.getTime());

    }


    /*
     * Compare every test in the two versions of the project.
     * If there is a test method with the same name, in the same class and in the same package in the
     * both versions of the project this method is compared and if it's not equals is selected regardless
     * of the methods it tests.
     */
    private void comparingTest() {

        HashSet<SootMethod> toDelete = new HashSet<>();
        HashSet<SootMethod> promotedMethods = new HashSet<>();
        boolean toAdd = false;
        for (SootMethod testMethod : differentMethods) {

            toAdd = ((testMethod.getName().equals("<init>") || testMethod.getName().equals("<clinit>")) && JunitUtil.isATestClass(testMethod));


            if (!toAdd && JunitUtil.isATestMethod(testMethod)) {
                if (Modifier.isAbstract(testMethod.getDeclaringClass().getModifiers())) {
                    if (JunitUtil.isSetup(testMethod)) {
                        for (SootClass subClass : soot.Scene.v().getActiveHierarchy().getSubclassesOf(testMethod.getDeclaringClass())) {
                            if (!Modifier.isAbstract(subClass.getModifiers())) {
                                for (SootMethod s : subClass.getMethods()) {
                                    if (JunitUtil.isJunitTestCase(s)) {
                                        differentTest.add(new Test(s));
                                    }
                                }
                            }
                        }
                    }
                    toDelete.add(testMethod);
                    continue;
                }
                if (JunitUtil.isTearDown(testMethod)) {
                    toDelete.add(testMethod);
                    continue;
                }
                toAdd = JunitUtil.isSetup(testMethod);
                if (!toAdd && JunitUtil.isJunitTestCase(testMethod)) {
                    //aggiungo ai test differenti solo i test -> metodi con @Test. I @Before,@After ecc ecc verrano eseguiti lo stesso

                    LOGGER.info("The test: " + testMethod.getDeclaringClass().getName() + "." + testMethod.getName() + " has been added because it is in both versions of the project but has been changed");
                    differentTest.add(new Test(testMethod));


                }
            }
            if (toAdd)
                for (SootMethod s : testMethod.getDeclaringClass().getMethods()) {
                    if (JunitUtil.isJunitTestCase(s)) {
                        differentTest.add(new Test(s));
                    }
                }
            if (toAdd)
                promotedMethods.add(testMethod);
        }

        differentTest.forEach(test -> differentMethods.remove(test.getTestMethod()));
        toDelete.forEach(differentMethods::remove);
        promotedMethods.forEach(differentMethods::remove);

    }


    /*
    This method check if all the object in both project are the same.
    if it'snt, so there are differences in constructor (different fields, different variables, different constants)
    all tests with a reference to that onbect are selecting
     */
    private void isTheSameObject() {

        differentMethods.forEach(sootMethod -> {
            if (sootMethod.getName().startsWith("<clinit>"))
                differentObject.add(sootMethod.getDeclaringClass());
        });


    }





    private boolean isEquals(SootMethod previousMethod, SootMethod newMethod) {
        if (requiresStructuralComparison(newMethod)) {
            Optional<String> previousBytecode = bytecodeFingerprint(previousProjectVersion, previousMethod);
            Optional<String> newBytecode = bytecodeFingerprint(newProjectVersion, newMethod);
            if (previousBytecode.isPresent() && newBytecode.isPresent()) {
                return previousBytecode.get().equals(newBytecode.get());
            }
        }
        return previousMethod.getActiveBody().toString().equals(newMethod.getActiveBody().toString());
    }

    private boolean requiresStructuralComparison(SootMethod method) {
        return JunitUtil.isATestMethod(method)
                || ((method.getName().equals("<init>") || method.getName().equals("<clinit>"))
                && JunitUtil.isATestClass(method));
    }

    private Optional<String> bytecodeFingerprint(PreviousProject project, SootMethod method) {
        return bytecodeFingerprint((CATTO.project.Project) project, method);
    }

    private Optional<String> bytecodeFingerprint(NewProject project, SootMethod method) {
        return bytecodeFingerprint((CATTO.project.Project) project, method);
    }

    private Optional<String> bytecodeFingerprint(CATTO.project.Project project, SootMethod method) {
        Optional<Path> classFile = findClassFile(project, method.getDeclaringClass());
        if (classFile.isEmpty()) {
            return Optional.empty();
        }

        String methodNameAndDescriptor = methodNameAndDescriptor(method);
        int descriptorStart = methodNameAndDescriptor.indexOf('(');
        if (descriptorStart < 0) {
            return Optional.empty();
        }

        MethodFingerprintVisitor visitor = new MethodFingerprintVisitor(
                methodNameAndDescriptor.substring(0, descriptorStart),
                methodNameAndDescriptor.substring(descriptorStart)
        );
        try {
            new ClassReader(Files.readAllBytes(classFile.get())).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.getFingerprint();
        } catch (IOException e) {
            LOGGER.warn("Unable to read bytecode for " + method.getSignature(), e);
            return Optional.empty();
        }
    }

    private Optional<Path> findClassFile(CATTO.project.Project project, SootClass sootClass) {
        String classFileName = sootClass.getName().replace('.', File.separatorChar) + ".class";
        for (String target : project.getTarget()) {
            Path candidate = new File(target, classFileName).toPath();
            if (Files.isRegularFile(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String methodNameAndDescriptor(SootMethod method) {
        String bytecodeSignature = method.getBytecodeSignature();
        int separator = bytecodeSignature.indexOf(": ");
        int end = bytecodeSignature.lastIndexOf('>');
        if (separator < 0 || end < separator) {
            return "";
        }
        return bytecodeSignature.substring(separator + 2, end);
    }

    private static final class MethodFingerprintVisitor extends ClassVisitor {
        private final String methodName;
        private final String descriptor;
        private String fingerprint;

        private MethodFingerprintVisitor(String methodName, String descriptor) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (!methodName.equals(name) || !this.descriptor.equals(descriptor)) {
                return null;
            }
            return new MethodVisitor(Opcodes.ASM9) {
                private final StringBuilder builder = new StringBuilder("access=").append(access).append(';');
                private final Map<Label, Integer> labels = new IdentityHashMap<>();

                @Override
                public void visitInsn(int opcode) {
                    append("insn", opcode);
                }

                @Override
                public void visitIntInsn(int opcode, int operand) {
                    append("int", opcode, operand);
                }

                @Override
                public void visitVarInsn(int opcode, int variable) {
                    append("var", opcode, variable);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    append("type", opcode, type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    append("field", opcode, owner, name, descriptor);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    append("method", opcode, owner, name, descriptor, isInterface);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                                   Object... bootstrapMethodArguments) {
                    append("dynamic", name, descriptor, bootstrapMethodHandle, Arrays.toString(bootstrapMethodArguments));
                }

                @Override
                public void visitJumpInsn(int opcode, Label label) {
                    append("jump", opcode, labelId(label));
                }

                @Override
                public void visitLabel(Label label) {
                    append("label", labelId(label));
                }

                @Override
                public void visitLdcInsn(Object value) {
                    append("ldc", value);
                }

                @Override
                public void visitIincInsn(int variable, int increment) {
                    append("iinc", variable, increment);
                }

                @Override
                public void visitEnd() {
                    fingerprint = builder.toString();
                }

                private void append(Object... values) {
                    for (Object value : values) {
                        builder.append(value).append('|');
                    }
                    builder.append(';');
                }

                private int labelId(Label label) {
                    return labels.computeIfAbsent(label, ignored -> labels.size());
                }
            };
        }

        private Optional<String> getFingerprint() {
            return Optional.ofNullable(fingerprint);
        }
    }


    private boolean haveSameParameter(SootMethod m, SootMethod m1) {
        return m.getSubSignature().equals(m1.getSubSignature());
    }


    private void findNewMethods() {
        newMethods.addAll(newProjectVersion.getApplicationMethod());
        newMethods.removeAll(differentMethods);
        newMethods.removeAll(equalsMethods);
        for (SootMethod newMethod : newMethods) {
            if (JunitUtil.isJunitTestCase(newMethod) && bytecodeFingerprint(newProjectVersion, newMethod).isPresent()) {
                differentTest.add(new Test(newMethod));
            }
        }

    }


    /**
     * Get a string collection with the name of the methods that are dfferent from the old project version
     *
     * @return a collection with the java style name (package.classname) of the methods that are different from the old project version
     */
    public Collection<String> getChangedMethods() {
        Collection<String> changedMethodsCopy = new ArrayList<>();
        differentMethods.forEach(changedMethod -> changedMethodsCopy.add(changedMethod.getDeclaringClass().getName() + "." + changedMethod.getName()));
        return changedMethodsCopy;
    }

    /**
     * Get a string collection with the name of the methods that are new, so that aren't in the old project version
     *
     * @return a collection with the java style name (package.classname) of the methods that are new
     */
    public Collection<String> getStringNewMethods() {

        Collection<String> newMethodsCopy = new ArrayList<>();
        newMethods.forEach(newMethod -> newMethodsCopy.add(newMethod.getDeclaringClass().getName() + "." + newMethod.getName()));
        return newMethodsCopy;
    }


    public HashSet<SootMethod> getNewMethods() {

        return newMethods;
    }

    public Set<SootMethod> getRemovedTests() {
        Set<String> newSignatures = new HashSet<>();
        for (SootMethod m : newProjectVersion.getApplicationMethod()) {
            newSignatures.add(m.getSignature());
        }
        Set<SootMethod> removed = new HashSet<>();
        for (SootMethod prev : previousProjectVersion.getApplicationMethod()) {
            if (!newSignatures.contains(prev.getSignature()) && isAnnotatedAsJunitTest(prev)) {
                removed.add(prev);
            }
        }
        return removed;
    }

    private static boolean isAnnotatedAsJunitTest(SootMethod method) {
        for (soot.tagkit.Tag t : method.getTags()) {
            String s = t.toString();
            if (t.getClass().equals(soot.tagkit.VisibilityAnnotationTag.class)
                    && s.contains("junit") && s.contains("Test")
                    && !s.contains("TestFactory") && !s.contains("TestTemplate")) {
                return true;
            }
        }
        return false;
    }
}
