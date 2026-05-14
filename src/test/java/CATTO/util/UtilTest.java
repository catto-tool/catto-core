package CATTO.util;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UtilTest {

    @org.junit.Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getFilesPathReturnsFilesRecursively() throws Exception {
        File root = temporaryFolder.newFolder("root");
        File nested = new File(root, "nested");
        assertTrue(nested.mkdir());
        File first = new File(root, "first.class");
        File second = new File(nested, "second.class");
        assertTrue(first.createNewFile());
        assertTrue(second.createNewFile());

        List<String> paths = Util.getFilesPath(new String[]{root.getAbsolutePath()});

        assertEquals(2, paths.size());
        assertTrue(paths.contains(first.getAbsolutePath()));
        assertTrue(paths.contains(second.getAbsolutePath()));
    }
}
