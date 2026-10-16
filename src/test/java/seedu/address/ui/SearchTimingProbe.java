package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;
import seedu.address.commons.util.AppPaths;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.model.person.Remark;
import seedu.address.model.person.Telegram;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in packaged-JAR search probe. Separates natural layout readiness from forced CSS/snapshot diagnostics.
 * Each invocation is one fresh process; no functional acceptance driver runs before its first search.
 */
public final class SearchTimingProbe {
    private static final double TARGET_MILLIS = 1000;
    private static Stage stage;
    private static TimingWindow window;
    private static ModelManager model;

    private SearchTimingProbe() {
    }

    /** Runs MODE (ordinary or snapshot), with a source revision and a new output directory as arguments. */
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || !(args[0].equals("ordinary") || args[0].equals("snapshot"))) {
            throw new IllegalArgumentException(
                    "Usage: SearchTimingProbe ordinary|snapshot SOURCE NEW_OUTPUT_DIRECTORY");
        }
        Path output = Path.of(args[2]);
        Files.createDirectory(output);
        Path jar = Path.of(MainWindow.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isRegularFile(jar)) {
            throw new IllegalArgumentException("Application classes must come from the candidate JAR.");
        }
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar)));
        AppPaths.configureRuntimeDirectories();
        Platform.startup(() -> { });
        Platform.setImplicitExit(false);
        StringBuilder report = new StringBuilder();
        try {
            fx(() -> open(output));
            awaitPulse();
            awaitPulse();
            fx(() -> report.append("source=").append(args[1]).append("\njar=").append(jar)
                    .append("\nsha256=").append(digest)
                    .append("\njava=").append(System.getProperty("java.runtime.version"))
                    .append("\njavafx=").append(System.getProperty("javafx.runtime.version"))
                    .append("\nos=").append(System.getProperty("os.name")).append(' ')
                    .append(System.getProperty("os.version")).append(' ').append(System.getProperty("os.arch"))
                    .append("\nenvironment=").append(System.getProperty("probe.environment", "UNSPECIFIED"))
                    .append("\nscreen=").append(Screen.getPrimary().getBounds())
                    .append("\nwindow=").append(stage.getWidth()).append('x').append(stage.getHeight())
                    .append("\nscale=").append(stage.getOutputScaleX()).append('x').append(stage.getOutputScaleY())
                    .append("\nwindowFocused=").append(stage.isFocused())
                    .append("\n500 fictional profiles / 1000 enrolments; first search is full-roster.\n")
                    .append("Natural post-layout readiness is a rendering proxy, ")
                    .append("not compositor/physical display timing.\n")
                    .append("mode,query,iteration,results,command_ms,ready_ms,")
                    .append("forced_css_ms,snapshot_ms,cards_ms,target\n"));
            for (String query : List.of("bench", "@Bench_499", "repo-499", "unmatched")) {
                for (int iteration = 0; iteration < 6; iteration++) {
                    Measurement measurement = measure(query, args[0].equals("snapshot"));
                    fx(() -> verify(query));
                    int expected = expectedCount(query);
                    report.append(String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%s%n",
                            args[0], query, iteration, expected, measurement.commandMillis, measurement.readyMillis,
                            measurement.forcedCssMillis, measurement.snapshotMillis, measurement.cardMillis,
                            args[0].equals("snapshot") ? "DIAGNOSTIC"
                                    : measurement.readyMillis <= TARGET_MILLIS ? "WITHIN" : "MISS"));
                }
            }
            report.append("PASS: result counts, order, selection, visible cells and feedback verified; ")
                    .append("no save allowed.\n");
        } finally {
            Files.writeString(output.resolve("timings.csv"), report);
            System.out.print(report);
            fx(() -> {
                if (stage != null) {
                    stage.close();
                }
            });
            Platform.exit();
        }
    }

    private static void open(Path output) {
        AddressBook roster = new AddressBook();
        List<Enrolment> enrolments = List.of(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")),
                new Enrolment(new ModuleCode("CS2113T"), new Semester("AY26/27 S2")));
        for (int i = 0; i < 500; i++) {
            String suffix = String.format(Locale.ROOT, "%03d", i);
            roster.addPerson(new Person(new Name("Student " + suffix), new Email("bench" + suffix + "@u.nus.edu"),
                    Optional.of(new Telegram("Bench_" + suffix)), Optional.of(new GitHub("bench-repo-" + suffix)),
                    true, new Remark(""), Set.of(), enrolments));
        }
        model = new ModelManager(roster, new UserPrefs());
        JsonAddressBookStorage neverSave = new JsonAddressBookStorage(output.resolve("roster.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) {
                throw new AssertionError("Search attempted a roster save");
            }
        };
        var logic = new LogicManager(model,
                new StorageManager(neverSave, new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        stage = new Stage();
        window = new TimingWindow(stage, logic, output.resolve("roster.json"));
        window.fillInnerParts();
        window.show();
        stage.setWidth(1280);
        stage.setHeight(720);
    }

    private static Measurement measure(String query, boolean snapshot) throws Exception {
        CompletableFuture<Measurement> done = new CompletableFuture<>();
        fx(() -> {
            Scene scene = stage.getScene();
            TextField input = (TextField) scene.lookup("#commandTextField");
            input.setText("find " + query);
            window.cardNanos = 0;
            long start = System.nanoTime();
            input.fireEvent(new ActionEvent());
            long commandEnd = System.nanoTime();
            long cssEnd;
            long snapshotEnd;
            if (snapshot) {
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                cssEnd = System.nanoTime();
                scene.snapshot(null);
                snapshotEnd = System.nanoTime();
            } else {
                cssEnd = commandEnd;
                snapshotEnd = commandEnd;
            }
            double cards = window.cardNanos / 1_000_000.0;
            // Observe the scene's own pulse. Do not force layout/snapshot in the ordinary measurement.
            Runnable listener = new Runnable() {
                @Override
                public void run() {
                    scene.removePostLayoutPulseListener(this);
                    double ready = (System.nanoTime() - start) / 1_000_000.0;
                    double forcedCss = (cssEnd - commandEnd) / 1_000_000.0;
                    double forcedSnapshot = (snapshotEnd - cssEnd) / 1_000_000.0;
                    done.complete(new Measurement((commandEnd - start) / 1_000_000.0, ready,
                            forcedCss, forcedSnapshot, cards));
                }
            };
            scene.addPostLayoutPulseListener(listener);
            Platform.requestNextPulse();
        });
        return done.get(30, TimeUnit.SECONDS);
    }

    private static void verify(String query) {
        int expected = expectedCount(query);
        ListView<?> list = (ListView<?>) stage.getScene().lookup(".list-view");
        TextArea feedback = (TextArea) stage.getScene().lookup("#resultDisplay");
        require(list.getItems().size() == expected && model.getFilteredPersonList().size() == expected,
                "Result count changed");
        require(list.getItems().equals(model.getFilteredPersonList().sorted(PersonOrder.BY_NAME_THEN_EMAIL)),
                "Result order changed");
        require(feedback.getText().equals(Messages.formatSearchResult(query, expected)), "Feedback changed");
        Person selected = window.getPersonListPanel().getSelectedPerson();
        require(expected == 1 ? selected != null && selected.getEmail().value.equals("bench499@u.nus.edu")
                : selected == null, "Selection changed");
        if (expected > 0) {
            require(list.lookupAll(".list-cell").stream().anyMatch(node -> node instanceof ListCell<?> cell
                    && !cell.isEmpty() && cell.getGraphic() != null && cell.getBoundsInParent().getHeight() > 0),
                    "No rendered result cell");
        }
    }

    private static int expectedCount(String query) {
        return query.equals("bench") ? 500 : query.equals("unmatched") ? 0 : 1;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void awaitPulse() throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        fx(() -> {
            Scene scene = stage.getScene();
            scene.addPostLayoutPulseListener(new Runnable() {
                @Override
                public void run() {
                    scene.removePostLayoutPulseListener(this);
                    done.complete(null);
                }
            });
            Platform.requestNextPulse();
        });
        done.get(30, TimeUnit.SECONDS);
    }

    private static void fx(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable failure) {
                done.completeExceptionally(failure);
            }
        });
        done.get(30, TimeUnit.SECONDS);
    }

    private record Measurement(double commandMillis, double readyMillis, double forcedCssMillis,
                               double snapshotMillis, double cardMillis) { }

    private static class TimingWindow extends MainWindow {
        private long cardNanos;

        TimingWindow(Stage stage, LogicManager logic, Path file) {
            super(stage, logic, file);
        }

        @Override
        PersonListPanel createPersonListPanel(ObservableList<Person> persons) {
            long start = System.nanoTime();
            try {
                return super.createPersonListPanel(persons);
            } finally {
                cardNanos += System.nanoTime() - start;
            }
        }
    }
}
