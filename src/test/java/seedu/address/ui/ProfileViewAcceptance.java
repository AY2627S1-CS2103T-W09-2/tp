package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.model.person.Telegram;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in real-window profile display probe; runs separately from the headless unit suite.
 * Lays the window out at the logical sizes of 1920x1080 at 100% and 125% and 1280x720 at 150% scaling,
 * checks that maximum-length values and many enrolments stay readable, and drives keyboard focus.
 * Uses only fictional data under build/reports/profile-view and refuses any save attempt.
 */
public class ProfileViewAcceptance extends Application {
    private static final String LONG_NAME = "Alexandria Catherine Tan Wei Ling Kumaraswamy Abdullah Rahman";
    private static final String LONG_EMAIL = "profile.view.long-email.check.for.the.readability.of.nus.emails9"
            + "@u.nus.edu";
    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport("1920x1080-100", 1920, 1080),
            new Viewport("1920x1080-125", 1536, 864),
            new Viewport("1280x720-150", 853, 480));

    private final Path output = Path.of("build", "reports", "profile-view");
    private MainWindow window;
    private ModelManager model;
    private Stage stage;
    private int assertions;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Files.createDirectories(output);
        Platform.setImplicitExit(false);
        new Thread(() -> {
            try {
                fx(() -> System.out.println("Runtime: " + System.getProperty("java.runtime.version") + "; JavaFX "
                        + System.getProperty("javafx.runtime.version") + "; " + System.getProperty("os.name") + " "
                        + System.getProperty("os.version") + " " + System.getProperty("os.arch") + "; output scale "
                        + primaryStage.getOutputScaleX()));
                for (Viewport viewport : VIEWPORTS) {
                    verify(viewport);
                }
                System.out.println("PASS Profile view JavaFX assertions: " + assertions);
                Platform.exit();
            } catch (Exception | AssertionError failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }).start();
    }

    /** Runs one step on the JavaFX thread and waits for it, so native window resizes can settle between steps. */
    private static void fx(FxStep step) throws Exception {
        java.util.concurrent.CompletableFuture<Void> done = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                step.run();
                done.complete(null);
            } catch (Exception | AssertionError e) {
                done.completeExceptionally(e);
            }
        });
        try {
            done.get();
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw (AssertionError) e.getCause();
        }
    }

    private interface FxStep {
        void run() throws Exception;
    }

    private static AddressBook roster() {
        String[][] contexts = {
            {"CS2103T", "AY26/27 S1"}, {"CS1231S", "AY23/24 S1"}, {"MA1521", "AY24/25 S2"},
            {"CS2101", "AY26/27 S1"}, {"CS2040S", "AY24/25 S1"}, {"ST2334", "AY25/26 S2"},
            {"CS2100", "AY24/25 S2"}, {"CS2106", "AY25/26 S1"}, {"CS3230", "AY26/27 S2"},
            {"IS1108", "AY23/24 S2"}, {"CS2109S", "AY25/26 S1"}, {"CS3219", "AY26/27 S2"}};
        List<Enrolment> enrolments = new ArrayList<>();
        for (int i = 0; i < contexts.length; i++) {
            String suffix = String.format(java.util.Locale.ROOT, "%04d", i);
            enrolments.add(new Enrolment(new ModuleCode(contexts[i][0]), new Semester(contexts[i][1]),
                    i % 3 == 2 ? Optional.empty() : Optional.of(new Section("Laboratory Section Number " + suffix)),
                    i % 4 == 3 ? Optional.empty() : Optional.of(new Team("Project Team Alpha Bravo " + suffix))));
        }
        AddressBook roster = new AddressBook();
        roster.addPerson(new Person(new Name(LONG_NAME), new Email(LONG_EMAIL),
                Optional.of(new Telegram("Long_telegram_handle_for_view_32")),
                Optional.of(new GitHub("long-github-username-for-profile-view9")),
                false, new Remark(""), Set.of(), enrolments));
        roster.addPerson(new Person(new Name(LONG_NAME), new Email("e9100002@u.nus.edu"), Optional.empty(),
                Optional.empty(), true, new Remark(""), Set.of(), List.of(enrolments.get(0))));
        roster.addPerson(new Person(new Name("Bala Devi"), new Email("e9100003@u.nus.edu"), Optional.empty(),
                Optional.empty(), false, new Remark(""), Set.of(), List.of()));
        return roster;
    }

    private void open(String filename) throws Exception {
        Path file = output.resolve(filename);
        AddressBook roster = roster();
        new JsonAddressBookStorage(file).saveAddressBook(roster);
        model = new ModelManager(roster, new UserPrefs());
        JsonAddressBookStorage neverSave = new JsonAddressBookStorage(file) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) {
                throw new AssertionError("Viewing attempted a roster save");
            }
        };
        Logic logic = new LogicManager(model,
                new StorageManager(neverSave, new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        stage = new Stage();
        window = new MainWindow(stage, logic, file);
        window.fillInnerParts();
    }

    private void verify(Viewport viewport) throws Exception {
        fx(() -> {
            open(viewport.name + ".json");
            // The real window's content, laid out at the exact logical size; a shown window is clamped to this screen.
            Scene original = stage.getScene();
            javafx.scene.Parent root = original.getRoot();
            original.setRoot(new javafx.scene.layout.StackPane());
            Scene sized = new Scene(root, viewport.width, viewport.height);
            sized.getStylesheets().setAll(original.getStylesheets());
            stage.setScene(sized);
            verifyLaidOut(viewport);
        });
        fx(() -> {
            ScrollPane details = (ScrollPane) stage.getScene().lookup(".details-pane");
            details.setVvalue(details.getVmax());
        });
        Thread.sleep(500);
        fx(() -> {
            ScrollPane details = (ScrollPane) stage.getScene().lookup(".details-pane");
            ImageIO.write(SwingFXUtils.fromFXImage(details.snapshot(null, null), null), "png",
                    output.resolve(viewport.name + "-profile-scrolled.png").toFile());
            stage.close();
        });
    }

    private void verifyLaidOut(Viewport viewport) throws Exception {
        byte[] before = Files.readAllBytes(output.resolve(viewport.name + ".json"));
        layout();
        Scene scene = stage.getScene();
        System.out.printf("VIEWPORT %s logical scene=%.0fx%.0f%n", viewport.name, scene.getWidth(),
                scene.getHeight());
        check(scene.getWidth() == viewport.width && scene.getHeight() == viewport.height,
                viewport.name + " scene uses the logical size");

        submit("view " + LONG_EMAIL.toUpperCase(java.util.Locale.ROOT));
        Person target = model.getAddressBook().getPersonList().getFirst();
        check(feedback().equals("Viewing student: " + LONG_NAME + "."), viewport.name + " view feedback");
        check(target.equals(window.getPersonListPanel().getSelectedPerson()), viewport.name + " target selected");
        List<String> expected = new ArrayList<>(List.of(LONG_NAME));
        expected.addAll(PersonDetailsPanel.profileLines(target));
        List<Label> lines = detailLabels();
        check(lines.stream().map(Label::getText).toList().equals(expected),
                viewport.name + " profile shows every line in display order");
        check(expected.contains(PersonCard.EMAIL_PREFIX + LONG_EMAIL) && expected.size() == 17,
                viewport.name + " full email and 12 enrolments listed");
        ScrollPane details = (ScrollPane) scene.lookup(".details-pane");
        for (Label line : lines) {
            checkReadable(line, viewport.name + " profile line");
            check(line.getBoundsInParent().getMaxX() <= details.getViewportBounds().getWidth(),
                    viewport.name + " profile line fits without horizontal scrolling: " + line.getText());
        }
        boolean scrolls = details.getContent().getBoundsInLocal().getHeight() > details.getViewportBounds()
                .getHeight();
        snapshot(viewport.name + "-top.png");
        details.setVvalue(details.getVmax());
        layout();
        Bounds last = lines.getLast().localToScene(lines.getLast().getBoundsInLocal());
        Bounds viewportBounds = details.localToScene(details.getLayoutBounds());
        System.out.printf("SCROLL %s v=%.2f last=%.0f-%.0f viewport=%.0f-%.0f%n", viewport.name, details.getVvalue(),
                last.getMinY(), last.getMaxY(), viewportBounds.getMinY(), viewportBounds.getMaxY());
        check(last.getMinY() >= viewportBounds.getMinY() && last.getMaxY() <= viewportBounds.getMaxY() + 1,
                viewport.name + " last enrolment reachable by scroll");
        System.out.printf("PROFILE %s lines=%d vertical-scroll=%s detailsViewport=%.0fx%.0f%n", viewport.name,
                lines.size(), scrolls, details.getViewportBounds().getWidth(),
                details.getViewportBounds().getHeight());

        int cardLabels = 0;
        for (Node node : scene.getRoot().lookupAll(".cell_small_label")) {
            Label label = (Label) node;
            if (!label.getText().isEmpty() && label.isVisible() && label.getParent().isVisible()) {
                checkReadable(label, viewport.name + " result row");
                cardLabels++;
            }
        }
        check(cardLabels > 0, viewport.name + " result rows rendered");

        verifyKeyboard(viewport, target);
        check(Arrays.equals(before, Files.readAllBytes(output.resolve(viewport.name + ".json"))),
                viewport.name + " file bytes unchanged");
    }

    private void verifyKeyboard(Viewport viewport, Person target) {
        Scene scene = stage.getScene();
        TextField input = (TextField) scene.lookup("#commandTextField");
        ListView<?> list = (ListView<?>) scene.lookup(".list-view");
        input.requestFocus();
        layout();
        List<String> order = new ArrayList<>();
        for (int i = 0; i < 6 && scene.getFocusOwner() != list; i++) {
            press(KeyCode.TAB, false);
            order.add(describe(scene.getFocusOwner()));
        }
        System.out.println("FOCUS " + viewport.name + " Tab from command box: " + order);
        check(scene.getFocusOwner() == list, viewport.name + " Tab reaches the result list");
        int start = list.getSelectionModel().getSelectedIndex();
        press(KeyCode.DOWN, false);
        Person next = window.getPersonListPanel().getSelectedPerson();
        check(list.getSelectionModel().getSelectedIndex() == start + 1 && !next.equals(target),
                viewport.name + " Down selects the next row");
        check(detailLabels().getFirst().getText().equals(next.getName().fullName)
                && detailLabels().stream().anyMatch(l -> l.getText().equals(PersonCard.EMAIL_PREFIX
                        + next.getEmail().value)), viewport.name + " profile follows keyboard selection");
        press(KeyCode.UP, false);
        check(target.equals(window.getPersonListPanel().getSelectedPerson())
                && detailLabels().getFirst().getText().equals(LONG_NAME), viewport.name + " Up restores profile");
        List<String> back = new ArrayList<>();
        for (int i = 0; i < 6 && scene.getFocusOwner() != input; i++) {
            press(KeyCode.TAB, true);
            back.add(describe(scene.getFocusOwner()));
        }
        System.out.println("FOCUS " + viewport.name + " Shift+Tab from list: " + back);
        check(scene.getFocusOwner() == input, viewport.name + " Shift+Tab returns to the command box");
    }

    private List<Label> detailLabels() {
        ScrollPane details = (ScrollPane) stage.getScene().lookup(".details-pane");
        return ((javafx.scene.layout.VBox) details.getContent()).getChildren().stream()
                .map(Label.class::cast).toList();
    }

    /** Fails if the label's rendered text is elided or its accessible text differs from the full value. */
    private void checkReadable(Label label, String description) {
        Text rendered = (Text) label.lookup(".text");
        check(rendered != null && rendered.getText().equals(label.getText()),
                description + " not truncated: " + label.getText());
        check(label.getText().equals(label.queryAccessibleAttribute(AccessibleAttribute.TEXT)),
                description + " accessible text complete: " + label.getText());
    }

    private void press(KeyCode code, boolean shift) {
        Node target = stage.getScene().getFocusOwner();
        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
        target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, shift, false, false, false));
        layout();
    }

    private static String describe(Node node) {
        return node == null ? "none" : node.getId() != null ? "#" + node.getId() : node.getClass().getSimpleName();
    }

    private void submit(String text) {
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(text);
        input.fireEvent(new ActionEvent());
        layout();
    }

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private void snapshot(String filename) throws Exception {
        ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png",
                output.resolve(filename).toFile());
    }

    private String feedback() {
        return ((TextArea) stage.getScene().lookup("#resultDisplay")).getText();
    }

    private void check(boolean condition, String description) {
        assertions++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    /** A screen's usable size in JavaFX logical pixels: physical resolution divided by the display scaling. */
    private record Viewport(String name, int width, int height) {
    }
}
