package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in real-window probe for issue #191; runs separately from the headless unit suite.
 * At the default 740x600 and the minimum 450x600 logical sizes, saves a long remark through the command box, then
 * checks that the complete remark is shown on the student's result card and in the profile panel opened by
 * {@code view EMAIL}, before and after the saved file is loaded again in a new window. Also checks that the stored
 * text and the student's other fields are unchanged, and that the displayed index and selection still refer to the
 * same student.
 * Uses only fictional data under build/reports/remark-readability.
 */
public class RemarkReadabilityAcceptance extends Application {
    private static final String EMAIL = "e9370002@u.nus.edu";
    private static final String SECOND_EMAIL = "e9370003@u.nus.edu";
    private static final String REMARK_END = "END-OF-NOTE";
    private static final String REMARK = "Please arrange a consultation about project progress before Friday "
            + "afternoon and confirm the room with the teaching team. " + REMARK_END;
    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport("default-740x600", 740, 600),
            new Viewport("minimum-450x600", 450, 600));

    private final Path output = Path.of("build", "reports", "remark-readability");
    private ModelManager model;
    private Stage stage;
    private MainWindow window;
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
                        + System.getProperty("os.version") + " " + System.getProperty("os.arch")));
                for (Viewport viewport : VIEWPORTS) {
                    fx(() -> verify(viewport));
                }
                System.out.println("PASS Remark readability JavaFX assertions: " + assertions);
                Platform.exit();
            } catch (Exception | AssertionError failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        }).start();
    }

    /** Runs one step on the JavaFX thread and waits for it. */
    private static void fx(FxStep step) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
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
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw (AssertionError) e.getCause();
        }
    }

    private interface FxStep {
        void run() throws Exception;
    }

    private void verify(Viewport viewport) throws Exception {
        String name = viewport.name;
        Path file = output.resolve(name).resolve("addressbook.json");
        Files.createDirectories(file.getParent());
        Files.deleteIfExists(file);

        open(viewport, file, new AddressBook());
        submit("student add /name Alpha Check /email " + EMAIL);
        submit("student add /name Beta Check /email " + SECOND_EMAIL);
        submit("remark 1 r/" + REMARK);
        check(feedback().startsWith("Added remark to Person: "), name + " remark saved: " + feedback());
        checkRemarkReadable(name + " after remark");
        snapshot(name + "-after-remark.png");
        stage.close();

        ReadOnlyAddressBook saved = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();
        Person stored = personWithEmail(saved, EMAIL);
        check(stored.getRemark().value.equals(REMARK), name + " stored remark is exactly the typed text");

        open(viewport, file, new AddressBook(saved));
        checkRemarkReadable(name + " after loading the saved file again");
        submit("remark 2 r/Second note");
        check(personWithEmail(model.getAddressBook(), SECOND_EMAIL).getRemark().value.equals("Second note"),
                name + " index 2 still targets the second student");
        check(personWithEmail(model.getAddressBook(), EMAIL).equals(stored),
                name + " first student's remark and other fields unchanged");
        Person selected = window.getPersonListPanel().getSelectedPerson();
        check(selected != null && selected.getEmail().value.equals(EMAIL),
                name + " selection unchanged by remarking another student");
        snapshot(name + "-after-reload.png");
        stage.close();
    }

    /** Opens the real window content at the exact logical size, on {@code roster} saved to {@code file}. */
    private void open(Viewport viewport, Path file, AddressBook roster) throws Exception {
        model = new ModelManager(roster, new UserPrefs());
        LogicManager logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        stage = new Stage();
        window = new MainWindow(stage, logic, file);
        window.fillInnerParts();
        // The real window's content, laid out at the exact logical size; a shown window is clamped to this screen.
        Scene original = stage.getScene();
        Parent root = original.getRoot();
        original.setRoot(new StackPane());
        Scene sized = new Scene(root, viewport.width, viewport.height);
        sized.getStylesheets().setAll(original.getStylesheets());
        stage.setScene(sized);
        layout();
    }

    /** Views the student and checks that the card and the profile each show the whole remark. */
    private void checkRemarkReadable(String description) {
        submit("view " + EMAIL);
        Person selected = window.getPersonListPanel().getSelectedPerson();
        check(selected != null && selected.getEmail().value.equals(EMAIL), description + " student selected");

        Label card = (Label) stage.getScene().getRoot().lookupAll("#remark").stream()
                .filter(node -> REMARK.equals(((Label) node).getText())).findFirst().orElseThrow(() ->
                        new AssertionError(description + " card shows the remark"));
        checkComplete(card, description + " card remark");
        check(card.isWrapText(), description + " card remark wraps");

        ScrollPane details = (ScrollPane) stage.getScene().lookup(".details-pane");
        Label profile = (Label) details.getContent().lookupAll(".details_label").stream()
                .filter(node -> ((Label) node).getText().equals(PersonDetailsPanel.REMARK_PREFIX + REMARK))
                .findFirst().orElseThrow(() -> new AssertionError(description + " profile shows the remark"));
        checkComplete(profile, description + " profile remark");
        check(profile.getBoundsInParent().getMaxX() <= details.getViewportBounds().getWidth(),
                description + " profile remark needs no horizontal scrolling");
        details.setVvalue(details.getVmax());
        layout();
        Bounds profileBounds = profile.localToScene(profile.getBoundsInLocal());
        Bounds viewportBounds = details.localToScene(details.getLayoutBounds());
        System.out.printf("REMARK %s card=%.0fx%.0f profile=%.0f-%.0f view=%.0f-%.0f%n", description,
                card.getWidth(), card.getHeight(), profileBounds.getMinY(), profileBounds.getMaxY(),
                viewportBounds.getMinY(), viewportBounds.getMaxY());
        check(profileBounds.getMinY() >= viewportBounds.getMinY()
                        && profileBounds.getMaxY() <= viewportBounds.getMaxY() + 1,
                description + " profile remark reachable by scrolling");
    }

    /** Fails unless {@code label} draws and exposes its complete text, so it is neither elided nor clipped. */
    private void checkComplete(Label label, String description) {
        Text rendered = (Text) label.lookup(".text");
        check(rendered != null && rendered.getText().equals(label.getText()), description + " not truncated: "
                + (rendered == null ? "none" : rendered.getText()));
        check(label.getText().equals(label.queryAccessibleAttribute(AccessibleAttribute.TEXT)),
                description + " accessible text complete");
        check(rendered.getText().endsWith(REMARK_END), description + " reaches " + REMARK_END);
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
        // A window that is never shown never repaints a cached scroll viewport, so the image would be stale.
        stage.getScene().getRoot().lookupAll(".viewport").forEach(viewport -> viewport.setCache(false));
        ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png",
                output.resolve(filename).toFile());
    }

    private static Person personWithEmail(ReadOnlyAddressBook roster, String email) {
        return roster.getPersonList().stream()
                .filter(person -> person.getEmail().value.equals(email)).findFirst().orElseThrow();
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

    /** A window's usable size in JavaFX logical pixels. */
    private record Viewport(String name, int width, int height) {
    }
}
