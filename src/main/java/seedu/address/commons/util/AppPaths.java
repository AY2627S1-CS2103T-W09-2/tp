package seedu.address.commons.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Locates the application home independently of the launcher's working directory. */
public final class AppPaths {
    private static final Path HOME = discoverHome();

    private AppPaths() {
    }

    public static Path getHomeDirectory() {
        return HOME;
    }

    /** Returns an absolute application-managed path; access is checked separately before I/O. */
    public static Path resolve(String first, String... more) {
        Path path = HOME.resolve(Path.of(first, more)).normalize();
        if (!path.startsWith(HOME)) {
            throw new IllegalArgumentException("Application data must remain inside its home folder.");
        }
        return path;
    }

    private static Path discoverHome() {
        try {
            return homeForCodeLocation(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (IOException | URISyntaxException | RuntimeException e) {
            throw new IllegalStateException(
                    "The application home could not be located. No working-directory fallback.", e);
        }
    }

    /** Resolves encoded spaces/Unicode and physical JAR links; a development classes directory is its own home. */
    static Path homeForCodeLocation(URI location) throws IOException {
        if (!"file".equals(location.getScheme())) {
            throw new IOException("Application code must be loaded from a local JAR or classes directory.");
        }
        Path code = Path.of(location).toRealPath();
        return Files.isDirectory(code) ? code : code.getParent();
    }

    /** Rejects escapes and symbolic-link components before application-managed reads or writes. */
    public static void requireInside(Path home, Path target) throws IOException {
        Path root = home.toAbsolutePath().normalize();
        Path absolute = target.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) {
            throw new AccessDeniedException(target.toString(), null, "Path is outside the application home.");
        }
        Path current = root;
        if (Files.isSymbolicLink(current)) {
            throw new AccessDeniedException(current.toString(), null, "Symbolic links are not managed data paths.");
        }
        for (Path component : root.relativize(absolute)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                throw new AccessDeniedException(current.toString(), null, "Symbolic links are not managed data paths.");
            }
        }
    }

    /** Creates a dependency directory only inside home and rejects pre-existing redirected resources. */
    public static Path prepareDirectory(String name) throws IOException {
        Path directory = resolve(name);
        requireInside(HOME, directory);
        Files.createDirectories(directory);
        try (var entries = Files.walk(directory)) {
            for (Path entry : entries.toList()) {
                requireInside(HOME, entry);
            }
        }
        if (!Files.isWritable(directory)) {
            throw new AccessDeniedException(directory.toString());
        }
        return directory;
    }

    /** Keeps bundled JavaFX native extraction and application-controlled temporary files inside home. */
    public static void configureRuntimeDirectories() throws IOException {
        System.setProperty("javafx.cachedir", prepareDirectory(".javafx-cache").toString());
        System.setProperty("java.io.tmpdir", prepareDirectory(".tmp").toString());
    }
}
