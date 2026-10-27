package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in real-window probe for compact layouts; runs separately from the headless unit suite.
 * Lays the window out at the default 740x600, the minimum 450x600 and the 853x480 logical size of 1280x720 at 150%
 * scaling, checks that the deletion preview, long feedback and the status bar stay complete without horizontal
 * scrolling, that feedback quoting a long name and email wraps, and checks the command box's
 * focused state and keyboard focus.
 * Uses only fictional data under build/reports/compact-feedback.
 */
public class CompactFeedbackAcceptance extends Application {
    private static final String TARGET = "e9000003@u.nus.edu";
    private static final String LONG_LOCAL_PART = "a".repeat(65);
    private static final Color FOCUS_COLOUR = Color.web("#e2e2e2");
    // The command box's border width in DarkTheme.css, in both the focused and unfocused states.
    private static final double FOCUS_BORDER_WIDTH = 2;
    private static final List<Viewport> VIEWPORTS = List.of(
            new Viewport("default-740x600", 740, 600),
            new Viewport("minimum-450x600", 450, 600),
            new Viewport("1280x720-150", 853, 480));

    private final Path output = Path.of("build", "reports", "compact-feedback");
    private ModelManager model;
    private LogicManager logic;
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
                    fx(() -> verify(viewport));
                }
                System.out.println("PASS Compact feedback JavaFX assertions: " + assertions);
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

    /** Opens the real window content on an empty roster whose path is long enough to need wrapping. */
    private void open(Viewport viewport) throws Exception {
        Path folder = output.resolve(viewport.name).resolve("a long folder name like a school laptop profile path")
                .resolve("SoCdex teaching assistant data");
        Path file = folder.resolve("addressbook.json");
        Files.createDirectories(folder);
        Files.deleteIfExists(file);
        model = new ModelManager(new AddressBook(), new UserPrefs());
        logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        stage = new Stage();
        MainWindow window = new MainWindow(stage, logic, file);
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

    private void verify(Viewport viewport) throws Exception {
        open(viewport);
        String name = viewport.name;
        TextField input = commandField();

        submit("sample load");
        check(feedback().equals(SampleCommand.MESSAGE_SUCCESS), name + " sample load feedback");
        checkWrapped(name + " sample load feedback");
        check(input.getText().isEmpty(), name + " accepted command clears the box");
        snapshot(name + "-sample-load.png");

        submit("delete " + TARGET);
        String preview = String.format(DeleteCommand.MESSAGE_PREVIEW, "Mei Lim", TARGET, 1, TARGET);
        check(feedback().equals(preview), name + " deletion preview shown");
        checkWrapped(name + " deletion preview");
        checkFullyVisible(name + " deletion preview");
        check(logic.hasPendingDeletion(), name + " deletion pending after preview");
        checkLabelComplete("#deletionStatus", StatusBarFooter.DELETION_PENDING, name + " status");
        checkLabelComplete("#saveLocationStatus", null, name + " data-file path");
        checkStatusBarInWindow(name);
        checkFocusBorder(input, name + " command box after a preview");
        snapshot(name + "-deletion-preview.png");

        String rejected = "student add /name Long Email /email " + LONG_LOCAL_PART + "@u.nus.edu";
        submit(rejected);
        check(feedback().equals(Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\n" + Email.MESSAGE_CONSTRAINTS),
                name + " rejected command cancels and explains");
        checkWrapped(name + " long error");
        check(input.getText().equals(rejected), name + " rejected command stays in the box");
        check(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS), name + " rejected command marked");
        check(!logic.hasPendingDeletion(), name + " no deletion pending after cancellation");
        checkLabelComplete("#deletionStatus", "", name + " status cleared");
        checkFocusBorder(input, name + " command box after a rejected command");
        snapshot(name + "-long-error.png");

        verifyTabOrder(name);
        verifyLongValidValues(name);
        stage.close();
    }

    /** Checks that feedback quoting a 100-character name and a 64-character email wraps within the result area. */
    private void verifyLongValidValues(String name) throws Exception {
        String longName = ("Wolfeschlegelsteinhausen " + "a".repeat(20) + " ").repeat(3).substring(0, Name.MAX_LENGTH);
        String longEmail = "b".repeat(64) + "@u.nus.edu";
        submit("student add /name " + longName + " /email " + longEmail);
        check(feedback().equals(String.format(AddCommand.MESSAGE_SUCCESS, longName)), name + " long student added");
        checkWrapped(name + " long name feedback");

        submit("delete " + longEmail);
        check(feedback().startsWith("Delete this student?\n" + longName), name + " long deletion preview shown");
        checkWrapped(name + " long deletion preview");
        snapshot(name + "-long-deletion-preview.png");
        submit("list");
        check(!logic.hasPendingDeletion(), name + " long deletion cancelled");
    }

    /** Fails unless the result area wraps its text so that no line needs horizontal scrolling. */
    private void checkWrapped(String description) {
        TextArea result = resultArea();
        ScrollPane scroll = (ScrollPane) result.lookup(".scroll-pane");
        check(result.isWrapText(), description + " wraps");
        double contentWidth = scroll.getContent().getLayoutBounds().getWidth();
        double viewportWidth = scroll.getViewportBounds().getWidth();
        System.out.printf("RESULT %s content=%.0fx%.0f viewport=%.0fx%.0f%n", description, contentWidth,
                scroll.getContent().getLayoutBounds().getHeight(), viewportWidth,
                scroll.getViewportBounds().getHeight());
        check(contentWidth <= viewportWidth + 1, description + " needs no horizontal scrolling");
    }

    /**
     * Fails unless every line of the result is in view without scrolling. The text node is measured because the
     * scroll content always stretches to fill the viewport.
     */
    private void checkFullyVisible(String description) {
        ScrollPane scroll = (ScrollPane) resultArea().lookup(".scroll-pane");
        Text text = (Text) resultArea().lookup(".content .text");
        Bounds textBounds = scroll.sceneToLocal(text.localToScene(text.getBoundsInLocal()));
        System.out.printf("TEXT %s text=%.0f-%.0f viewport=%.0f%n", description, textBounds.getMinY(),
                textBounds.getMaxY(), scroll.getViewportBounds().getHeight());
        check(textBounds.getMaxY() <= scroll.getViewportBounds().getHeight() + 1,
                description + " needs no vertical scrolling");
    }

    /** Fails if the label's rendered text is elided, or differs from {@code expected} when one is given. */
    private void checkLabelComplete(String id, String expected, String description) {
        Label label = (Label) stage.getScene().lookup(id);
        check(expected == null || label.getText().equals(expected), description + " text: " + label.getText());
        Text rendered = (Text) label.lookup(".text");
        check(label.getText().isEmpty() || (rendered != null && rendered.getText().equals(label.getText())),
                description + " not truncated: " + (rendered == null ? "none" : rendered.getText()));
    }

    /** Fails unless the whole status bar, including a wrapped path, lies inside the window. */
    private void checkStatusBarInWindow(String name) {
        Node bar = stage.getScene().lookup(".status-bar");
        Bounds bounds = bar.localToScene(bar.getLayoutBounds());
        System.out.printf("STATUS %s bar=%.0f-%.0f scene=%.0f%n", name, bounds.getMinY(), bounds.getMaxY(),
                stage.getScene().getHeight());
        check(bounds.getMaxY() <= stage.getScene().getHeight() + 1, name + " status bar inside the window");
    }

    /**
     * Checks that the theme outlines the command box on every side only while it is focused, without changing its
     * size. This window is never shown, so JavaFX never reports it focused; the focused state is applied directly.
     */
    private void checkFocusBorder(TextField field, String description) {
        PseudoClass focused = PseudoClass.getPseudoClass("focused");
        Bounds unfocusedBounds = field.getLayoutBounds();
        check(!hasFocusBorder(field), description + " has no focus outline when unfocused");
        field.pseudoClassStateChanged(focused, true);
        layout();
        check(hasFocusBorder(field), description + " is outlined when focused");
        check(field.getLayoutBounds().equals(unfocusedBounds), description + " keeps its size when focused");
        field.pseudoClassStateChanged(focused, false);
        layout();
        check(!hasFocusBorder(field), description + " loses its outline when unfocused");
    }

    /** Returns whether {@code field} draws the theme's focus colour on every side at a visible width. */
    private static boolean hasFocusBorder(TextField field) {
        Border border = field.getBorder();
        return border != null && !border.getStrokes().isEmpty() && border.getStrokes().stream()
                .allMatch(CompactFeedbackAcceptance::isFocusStroke);
    }

    private static boolean isFocusStroke(BorderStroke stroke) {
        return Stream.of(stroke.getTopStroke(), stroke.getRightStroke(), stroke.getBottomStroke(),
                stroke.getLeftStroke()).allMatch(FOCUS_COLOUR::equals)
                && Stream.of(stroke.getWidths().getTop(), stroke.getWidths().getRight(),
                        stroke.getWidths().getBottom(), stroke.getWidths().getLeft())
                        .allMatch(width -> width >= FOCUS_BORDER_WIDTH);
    }

    private void verifyTabOrder(String name) {
        Scene scene = stage.getScene();
        TextField input = commandField();
        ListView<?> list = (ListView<?>) scene.lookup("#personListView");
        input.requestFocus();
        layout();
        for (int i = 0; i < 6 && scene.getFocusOwner() != list; i++) {
            press(KeyCode.TAB, false);
        }
        check(scene.getFocusOwner() == list, name + " Tab reaches the result list");
        for (int i = 0; i < 6 && scene.getFocusOwner() != input; i++) {
            press(KeyCode.TAB, true);
        }
        check(scene.getFocusOwner() == input, name + " Shift+Tab returns to the command box");
        check(input.getText().startsWith("student add"), name + " Tab keeps the rejected text");
    }

    private void press(KeyCode code, boolean shift) {
        Node target = stage.getScene().getFocusOwner();
        target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
        target.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", code, shift, false, false, false));
        layout();
    }

    private void submit(String text) {
        TextField input = commandField();
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

    private TextField commandField() {
        return (TextField) stage.getScene().lookup("#commandTextField");
    }

    private TextArea resultArea() {
        return (TextArea) stage.getScene().lookup("#resultDisplay");
    }

    private String feedback() {
        return resultArea().getText();
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
