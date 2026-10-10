package seedu.address.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AppPathsTest {
    @TempDir
    public Path folder;

    @Test
    public void homeForCodeLocation_jarAndClassesIgnoreWorkingDirectory() throws Exception {
        Path home = Files.createDirectory(folder.resolve("SoCdex 空 folder %t"));
        Path jar = Files.writeString(home.resolve("addressbook.jar"), "fixture");
        assertEquals(home.toRealPath(), AppPaths.homeForCodeLocation(jar.toUri()));
        Path classes = Files.createDirectory(folder.resolve("classes"));
        assertEquals(classes.toRealPath(), AppPaths.homeForCodeLocation(classes.toUri()));
        assertThrows(IOException.class, () -> AppPaths.homeForCodeLocation(URI.create("https://example.com/app.jar")));
        assertThrows(IOException.class, () -> AppPaths.homeForCodeLocation(folder.resolve("missing.jar").toUri()));
    }

    @Test
    public void requireInside_rejectsAbsoluteAndParentEscapes() throws Exception {
        Path home = Files.createDirectory(folder.resolve("home"));
        AppPaths.requireInside(home, home.resolve("data/addressbook.json"));
        assertThrows(IOException.class, () -> AppPaths.requireInside(home, folder.resolve("outside.json")));
        assertThrows(IOException.class, () -> AppPaths.requireInside(home, home.resolve("../outside.json")));
    }
}
