package CATTO.config;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ConfigWrapperTest {

    @org.junit.Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void readsConfigYamlFromProjectDirectory() throws Exception {
        File project = temporaryFolder.newFolder("project");
        try (FileWriter writer = new FileWriter(new File(project, "config.yaml"))) {
            writer.write("---\n");
            writer.write("outputPath:\n");
            writer.write("  - target/classes\n");
            writer.write("  - target/test-classes\n");
            writer.write("dependencies:\n");
            writer.write("  - lib/junit.jar\n");
            writer.write("tempFolderPath: .tmp\n");
            writer.write("javaVersion: \"17\"\n");
        }

        Configurator config = new ConfigWrapper(project.getAbsolutePath()).getCONFIG();

        assertEquals(Arrays.asList("target/classes", "target/test-classes"), config.getOutputPath());
        assertEquals(Arrays.asList("lib/junit.jar"), config.getDependencies());
        assertEquals(".tmp", config.getTempFolderPath());
        assertEquals("17", config.getJavaVersion());
    }

    @Test(expected = RuntimeException.class)
    public void throwsWhenConfigYamlIsMissing() throws Exception {
        File project = temporaryFolder.newFolder("project-without-config");

        new ConfigWrapper(project.getAbsolutePath());
    }
}
