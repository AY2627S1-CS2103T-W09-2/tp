package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression test of the built JAR in another working directory, including a distinct JVM restart. */
public class JarSandboxTest {
    @TempDir
    public Path folder;

    @Test
    public void packagedStorage_launchFromExternalDirectory_neverTouchesExternalFiles() throws Exception {
        Path home = Files.createDirectory(folder.resolve("SoCdex 空 %t"));
        Path external = Files.createDirectory(folder.resolve("external"));
        Files.createDirectory(external.resolve("data"));
        Files.writeString(external.resolve("preferences.json"), "external preferences sentinel");
        Files.writeString(external.resolve("data/addressbook.json"), "external roster sentinel");
        Map<String, String> original = snapshot(external);
        Path jar = Files.copy(Path.of("build", "libs", "addressbook.jar"), home.resolve("addressbook.jar"));
        Path helpers = Path.of(SandboxPathsProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        for (String phase : new String[] {"seed", "restart"}) {
            Path output = home.resolve(phase + ".log");
            Process process = new ProcessBuilder(java, "-cp", helpers + File.pathSeparator + jar,
                    SandboxPathsProbe.class.getName(), phase).directory(external.toFile())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }
            assertTrue(finished, "Packaged storage subprocess timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
            assertTrue(Files.readString(output).contains("PASS sandbox storage " + phase));
            assertEquals(original, snapshot(external));
        }
        assertTrue(Files.exists(home.resolve("preferences.json")));
        assertTrue(Files.exists(home.resolve("data/addressbook.json")));
        assertTrue(Files.exists(home.resolve("logs/addressbook.log.0")));
        assertTrue(Files.isDirectory(home.resolve(".javafx-cache")));
        assertTrue(Files.isDirectory(home.resolve(".tmp")));
    }

    private Map<String, String> snapshot(Path root) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.toList()) {
                result.put(root.relativize(path).toString(), Files.isDirectory(path) ? "directory"
                        : java.util.HexFormat.of().formatHex(Files.readAllBytes(path)));
            }
        }
        return result;
    }
}
