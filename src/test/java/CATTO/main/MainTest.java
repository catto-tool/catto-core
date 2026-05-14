package CATTO.main;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainTest {

    @org.junit.Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void runRequiresProjectPath() throws Exception {
        Main.run(new String[0]);
    }

    @Test
    public void resolveConfiguredPathKeepsAbsolutePaths() {
        Path absolute = Paths.get("/tmp/catto/classes");

        Path resolved = Main.resolveConfiguredPath(Paths.get("/project"), absolute.toString());

        assertEquals(absolute, resolved);
    }

    @Test
    public void resolveConfiguredPathResolvesRelativePathsAgainstProjectPath() {
        Path project = Paths.get("/project").toAbsolutePath().normalize();

        Path resolved = Main.resolveConfiguredPath(project, "target/classes");

        assertEquals(project.resolve("target/classes").normalize(), resolved);
    }

    @Test
    public void listfReturnsOnlyJarFilesRecursively() throws Exception {
        File root = temporaryFolder.newFolder("deps");
        File nested = new File(root, "nested");
        assertTrue(nested.mkdir());
        File rootJar = new File(root, "root.jar");
        File nestedJar = new File(nested, "nested.jar");
        File notAJar = new File(nested, "notes.txt");
        assertTrue(rootJar.createNewFile());
        assertTrue(nestedJar.createNewFile());
        assertTrue(notAJar.createNewFile());

        List<String> jars = Main.listf(root.getAbsolutePath());

        assertEquals(2, jars.size());
        assertTrue(jars.contains(rootJar.getAbsolutePath()));
        assertTrue(jars.contains(nestedJar.getAbsolutePath()));
    }
}
