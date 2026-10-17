package seedu.address.ui;

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
import seedu.address.Main;
import seedu.address.commons.util.AppPaths;
import seedu.address.storage.JsonAddressBookStorage;

/** Opt-in real production entry-point check, run seed/restart in separate JVMs from an external directory. */
public final class EditEnrolAcceptance {
    private EditEnrolAcceptance() {
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
        String selectors = "edit-enrol /email e9000001@u.nus.edu /module CS2103T /semester AY26/27 S1";
        if (phase.equals("seed")) {
            submit(stage, "sample load");
            submit(stage, "find Ravi");
            submit(stage, selectors + " /section T14 /team clear");
            check(feedback(stage).equals("Updated enrolment for Alex Tan: CS2103T, AY26/27 S1."),
                    "affiliations changed from a hidden owner");
            byte[] saved = Files.readAllBytes(roster);
            submit(stage, selectors + " /section T14 /team ___");
            check(feedback(stage).startsWith("Team must contain"), "invalid request rejected");
            check(java.util.Arrays.equals(saved, Files.readAllBytes(roster)), "invalid request preserves bytes");
            submit(stage, selectors + " /section T14 /team clear");
            check(feedback(stage).equals("No values changed."), "normalised no-op reported");
            check(java.util.Arrays.equals(saved, Files.readAllBytes(roster)), "no-op preserves bytes");
            submit(stage, selectors + " /section t14");
            check(feedback(stage).startsWith("Updated enrolment"), "case-only change saved");
        }
        var persons = new JsonAddressBookStorage(roster, home).readAddressBook().orElseThrow().getPersonList();
        var alex = persons.stream().filter(person -> person.getEmail().value.equals("e9000001@u.nus.edu"))
                .findFirst().orElseThrow();
        var context = alex.getEnrolments().stream().filter(enrolment ->
                enrolment.getModuleCode().value.equals("CS2103T")).findFirst().orElseThrow();
        check(persons.size() == 5 && alex.isSample() && alex.getEnrolments().size() == 2,
                "other profiles/enrolments and sample classification survive");
        check(context.getSection().orElseThrow().value.equals("t14") && context.getTeam().isEmpty(),
                "case-only section and cleared team survive reload");
        submit(stage, selectors + " /section t14 /team clear");
        check(feedback(stage).equals("No values changed."), "reloaded model matches stored affiliations");
        var list = (javafx.scene.control.ListView<?>) stage.getScene().lookup("#personListView");
        check(list.getSelectionModel().getSelectedItem() != null, "owner selected");
        var pane = (javafx.scene.control.ScrollPane) stage.getScene().lookup(".details-pane");
        String details = ((javafx.scene.layout.VBox) pane.getContent()).getChildren().stream()
                .filter(javafx.scene.control.Label.class::isInstance)
                .map(node -> ((javafx.scene.control.Label) node).getText())
                .collect(java.util.stream.Collectors.joining());
        check(details.contains("t14") && details.contains("Not assigned") && details.contains("Fictional sample"),
                "complete selected profile displays persisted affiliations");
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
