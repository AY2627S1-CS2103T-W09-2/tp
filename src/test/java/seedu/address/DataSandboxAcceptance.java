package seedu.address;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.commons.util.AppPaths;
import seedu.address.storage.JsonAddressBookStorage;

/** Opt-in real production entry-point check, run seed/restart in separate JVMs from an external directory. */
public final class DataSandboxAcceptance {
    private DataSandboxAcceptance() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("seed") || args[0].equals("restart"))) {
            throw new IllegalArgumentException("Use seed or restart with a disposable JAR home and external cwd.");
        }
        Path home = AppPaths.getHomeDirectory();
        Path roster = AppPaths.resolve("data", "addressbook.json");
        if (args[0].equals("seed") && Files.exists(roster)) {
            throw new IllegalStateException("Seed refuses an existing roster beside the JAR.");
        }
        AtomicBoolean submitted = new AtomicBoolean();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread observer = new Thread(() -> {
            long deadline = System.nanoTime() + 30_000_000_000L;
            while (!submitted.get() && System.nanoTime() < deadline) {
                try {
                    Thread.sleep(100);
                    Platform.runLater(() -> {
                        Stage stage = Window.getWindows().stream().filter(window -> window.isShowing()
                                && window.getScene() != null
                                && window.getScene().lookup("#commandTextField") != null)
                                .map(window -> (Stage) window).findFirst().orElse(null);
                        if (stage == null || !submitted.compareAndSet(false, true)) {
                            return;
                        }
                        try {
                            verify(stage, args[0], home, roster);
                            completed.set(true);
                            submit(stage, "exit");
                        } catch (Exception | Error e) {
                            failure.set(e);
                            stage.hide();
                            Platform.exit();
                        }
                    });
                } catch (IllegalStateException e) {
                    // The observer may start before the production entry point has initialised JavaFX.
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "sandbox-acceptance-observer");
        observer.setDaemon(true);
        observer.start();
        try {
            Main.main(new String[0]);
        } finally {
            observer.interrupt();
        }
        if (failure.get() != null || !completed.get()) {
            throw new AssertionError("Production sandbox scenario did not complete", failure.get());
        }
        check(Files.exists(home.resolve("preferences.json")), "preferences saved beside JAR after stop");
        System.out.println("PASS production sandbox " + args[0] + "; home=" + home
                + "; cwd=" + Path.of("").toAbsolutePath() + "; Java " + System.getProperty("java.runtime.version"));
    }

    private static void verify(Stage stage, String phase, Path home, Path roster) throws Exception {
        check(Path.of(System.getProperty("javafx.cachedir")).startsWith(home), "native cache inside home");
        check(Path.of(System.getProperty("java.io.tmpdir")).startsWith(home), "temporary directory inside home");
        if (phase.equals("seed")) {
            submit(stage, "list");
            check(!Files.exists(roster), "startup did not copy external roster");
            submit(stage, "student add /name Alex Demo /email E9000001@U.NUS.EDU /telegram @Alex_Demo");
            check(feedback(stage).equals("Added student: Alex Demo."), "writable empty startup creates profile");
            submit(stage, "enrol /email e9000001@u.nus.edu /module cs2103t /semester ay26/27 s1 "
                    + "/section Lab 2 /team SEED-2");
            check(feedback(stage).startsWith("Added enrolment for Alex Demo: CS2103T, AY26/27 S1."),
                    "enrolment saved in home");
        }
        var persons = new JsonAddressBookStorage(roster, home).readAddressBook().orElseThrow().getPersonList();
        check(persons.size() == 1, "exact saved roster");
        var person = persons.getFirst();
        check(person.getEmail().value.equals("e9000001@u.nus.edu")
                && person.getTelegram().orElseThrow().value.equals("Alex_Demo")
                && person.getEnrolments().size() == 1
                && person.getEnrolments().getFirst().getTeam().orElseThrow().value.equals("SEED-2"),
                "complete teaching context persisted");
        submit(stage, "find e9000001");
        check(feedback(stage).equals("1 student found for \"e9000001\"."), "production model reloaded from home");
    }

    private static void submit(Stage stage, String command) {
        System.out.println("COMMAND " + command);
        TextField field = (TextField) stage.getScene().lookup("#commandTextField");
        field.setText(command);
        field.fireEvent(new ActionEvent());
        System.out.println("RESULT " + feedback(stage));
    }

    private static String feedback(Stage stage) {
        return ((TextArea) stage.getScene().lookup("#resultDisplay")).getText();
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
