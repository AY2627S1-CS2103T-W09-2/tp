package seedu.address;

import java.nio.file.Files;

import seedu.address.commons.util.AppPaths;

/** Subprocess helper: production initialization and storage, with production classes loaded from the tested JAR. */
public final class SandboxPathsProbe {
    private SandboxPathsProbe() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Classpath: " + System.getProperty("java.class.path"));
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            var path = java.nio.file.Path.of(entry);
            System.out.println("Classpath entry exists: " + Files.exists(path) + " " + path);
        }
        AppPaths.configureRuntimeDirectories();
        MainApp app = new MainApp();
        app.init();
        if (args[0].equals("seed")) {
            if (!app.model.getAddressBook().getPersonList().isEmpty() || app.model.isReadOnly()) {
                throw new AssertionError("External working-directory roster was used");
            }
            app.logic.execute("student add /name Alex Demo /email e9000001@u.nus.edu /telegram @Alex_Demo");
            app.logic.execute("enrol /email e9000001@u.nus.edu /module CS2103T /semester AY26/27 S1 /team SEED-2");
        } else {
            var persons = app.model.getAddressBook().getPersonList();
            if (persons.size() != 1 || !persons.getFirst().getEmail().value.equals("e9000001@u.nus.edu")
                    || !persons.getFirst().getTelegram().orElseThrow().value.equals("Alex_Demo")
                    || !persons.getFirst().getEnrolments().getFirst().getTeam().orElseThrow().value.equals("SEED-2")) {
                throw new AssertionError("Complete saved roster did not reload from JAR home");
            }
        }
        if (!app.storage.getAddressBookFilePath().equals(AppPaths.resolve("data", "addressbook.json"))
                || !app.storage.getUserPrefsFilePath().equals(AppPaths.resolve("preferences.json"))) {
            throw new AssertionError("Production storage escaped JAR home");
        }
        app.logic.execute("exit");
        app.stop();
        if (!Files.exists(AppPaths.resolve("preferences.json")) || !Files.exists(AppPaths.resolve("logs"))) {
            throw new AssertionError("Preferences or logs missing from JAR home");
        }
        System.out.println("PASS sandbox storage " + args[0]);
    }
}
