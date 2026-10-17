package seedu.address;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies that the packaged application contains its required resources but no runtime logs. */
public class JarContentsTest {
    private static final Path PACKAGED_JAR = Path.of("build", "libs", "addressbook.jar");
    private static final Set<String> REQUIRED_ENTRIES = Set.of(
            "META-INF/MANIFEST.MF",
            "META-INF/services/com.fasterxml.jackson.databind.Module",
            "seedu/address/Main.class",
            "images/address_book_32.png",
            "view/MainWindow.fxml",
            "view/DarkTheme.css",
            "javafx/application/Application.class",
            "glass.dll",
            "libglass.dylib",
            "libglass.so");

    @TempDir
    public Path folder;

    @Test
    public void packagedJar_requiredEntriesPresentAndRuntimeLogsAbsent() throws Exception {
        assertSafeArchive(PACKAGED_JAR, true);
    }

    @Test
    public void archiveVerifier_runtimeLogEntry_throwsAssertionError() throws Exception {
        Path contaminatedJar = folder.resolve("contaminated.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(contaminatedJar))) {
            output.putNextEntry(new JarEntry("logs/packaging-marker.log"));
            output.write("synthetic marker".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            output.closeEntry();
        }

        AssertionError error = assertThrows(AssertionError.class, () -> assertSafeArchive(contaminatedJar, false));
        assertTrue(error.getMessage().contains("logs/packaging-marker.log"));
    }

    private static void assertSafeArchive(Path archive, boolean checkRequiredEntries) throws IOException {
        Set<String> entries = new HashSet<>();
        try (var jar = new JarFile(archive.toFile())) {
            jar.stream().map(JarEntry::getName).forEach(entries::add);
        }

        Set<String> runtimeLogs = new HashSet<>();
        for (String entry : entries) {
            if (entry.equals("logs") || entry.startsWith("logs/")) {
                runtimeLogs.add(entry);
            }
        }
        assertTrue(runtimeLogs.isEmpty(), "Packaged JAR contains runtime logs: " + runtimeLogs);

        if (checkRequiredEntries) {
            Set<String> missingEntries = new HashSet<>(REQUIRED_ENTRIES);
            missingEntries.removeAll(entries);
            assertTrue(missingEntries.isEmpty(), "Packaged JAR is missing required entries: " + missingEntries);
        }
    }
}
