package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Opt-in real-window acceptance for blank submissions, stale-display cancellation and the pending-deletion
 * status; uses fictional files under build/reports/deletion-status.
 */
public final class DeletionStatusAcceptance {
    private static final String CANCELLED = Messages.MESSAGE_PENDING_DELETION_CANCELLED;
    private static final String TARGET = "e9000001@u.nus.edu";

    private final Path output = Path.of("build", "reports", "deletion-status");
    private final Person target = new PersonBuilder().withName("Alex Tan").withEmail(TARGET)
            .withEnrolments(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"))).build();
    private final Person survivor = new PersonBuilder().withName("Mei Lim").withEmail("e9000003@u.nus.edu").build();
    private Stage stage;
    private MainWindow window;
    private ModelManager model;
    private CountingLogic logic;
    private int assertions;

    private DeletionStatusAcceptance() {
    }

    /** Runs the real-window acceptance scenario and reports its actual environment. */
    public static void main(String[] args) throws Exception {
        DeletionStatusAcceptance acceptance = new DeletionStatusAcceptance();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            acceptance.stage = new Stage();
            try {
                Files.createDirectories(acceptance.output);
                acceptance.verifyIndicatorBlankSubmissionsAndNonSubmissions();
                acceptance.verifyStaleDisplayRejectionCancels();
                acceptance.verifyRecoveryStatusIsPreserved();
                System.out.println("PASS Deletion status JavaFX assertions: " + acceptance.assertions);
                System.out.println("Runtime: " + System.getProperty("java.runtime.version") + "; JavaFX "
                        + System.getProperty("javafx.runtime.version") + "; " + System.getProperty("os.name") + " "
                        + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
            } catch (Exception | AssertionError error) {
                failure.set(error);
            } finally {
                Platform.setImplicitExit(true);
                acceptance.stage.close();
                finished.countDown();
            }
        });
        if (!finished.await(30, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for deletion-status acceptance");
        }
        if (failure.get() != null) {
            throw new AssertionError("Deletion-status acceptance failed", failure.get());
        }
    }

    private void verifyIndicatorBlankSubmissionsAndNonSubmissions() throws Exception {
        Path file = open(false, "normal.json");
        String storageStatus = "Storage available | " + Paths.get(".").resolve(file);
        check(saveStatus().equals(storageStatus), "storage status and data-file path shown");
        check(deletionStatus().isEmpty(), "no deletion pending at start");

        submit("");
        check(logic.executions == 1, "exactly empty input submitted once");
        check(feedback().equals(Messages.MESSAGE_ENTER_COMMAND), "blank input without pending deletion");

        submit("delete " + TARGET);
        check(deletionStatus().equals(StatusBarFooter.DELETION_PENDING), "indicator shown after preview");
        check(saveStatus().equals(storageStatus), "storage status unchanged while pending");
        check(target.equals(selected()), "preview selects the target");

        int executions = logic.executions;
        commandField().setText("find Mei");
        layout();
        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
        list.scrollTo(1);
        stage.setWidth(1280);
        stage.setHeight(720);
        layout();
        commandField().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F1, false, false, false, false));
        check(Window.getWindows().stream().anyMatch(shown -> shown != stage && shown.isShowing()), "F1 opens help");
        Window.getWindows().stream().filter(shown -> shown != stage).toList().forEach(Window::hide);
        check(logic.executions == executions, "typing, scrolling, resizing and F1 submit nothing");
        check(logic.hasPendingDeletion(), "deletion still pending after non-submissions");
        check(deletionStatus().equals(StatusBarFooter.DELETION_PENDING), "indicator kept after non-submissions");

        commandField().setText("");
        submit("");
        check(logic.executions == executions + 1, "exactly empty input forwarded once while pending");
        check(feedback().equals(CANCELLED + "\n" + Messages.MESSAGE_ENTER_COMMAND), "blank input cancels");
        check(deletionStatus().isEmpty(), "indicator removed after blank cancellation");

        submit("delete " + TARGET);
        submit("unknowncommand");
        check(feedback().equals(CANCELLED + "\n" + Messages.MESSAGE_UNKNOWN_COMMAND), "failed command cancels");
        check(deletionStatus().isEmpty(), "indicator removed after failed submission");

        submit("delete " + TARGET);
        submit("confirm-delete " + TARGET);
        check(model.getAddressBook().getPersonList().equals(List.of(survivor)), "only the target is deleted");
        check(deletionStatus().isEmpty(), "indicator removed after confirmation");
        check(selected() == null, "selection cleared with one survivor");
        check(detailTexts().equals(List.of(PersonDetailsPanel.SELECTION_PROMPT)), "selection prompt shown");
        stage.close();
    }

    private void verifyStaleDisplayRejectionCancels() throws Exception {
        open(false, "stale.json");
        submit("delete " + TARGET);
        check(deletionStatus().equals(StatusBarFooter.DELETION_PENDING), "indicator shown before stale rejection");
        model.updateFilteredPersonList(survivor::equals); // the model changes without the display following
        int executions = logic.executions;

        submit("confirm-delete " + TARGET);

        check(logic.executions == executions, "guard rejects before the command reaches logic");
        check(feedback().equals(CANCELLED + "\n" + DisplayedCommandExecutor.MESSAGE_DISPLAY_CHANGED),
                "stale rejection reports the cancellation");
        check(!logic.hasPendingDeletion(), "stale rejection cancels the pending deletion");
        check(deletionStatus().isEmpty(), "indicator removed after rejection before execution");
        check(model.hasPerson(target), "rejected confirmation never deletes");
        stage.close();
    }

    private void verifyRecoveryStatusIsPreserved() throws Exception {
        Path file = open(true, "recovery.json");
        check(saveStatus().equals("Storage unavailable \u2014 read-only recovery | " + Paths.get(".").resolve(file)),
                "recovery status and path shown");
        submit("delete " + TARGET);
        check(feedback().equals(LogicManager.MESSAGE_READ_ONLY), "recovery blocks the preview");
        check(deletionStatus().isEmpty(), "no indicator in recovery");
        stage.close();
    }

    private Path open(boolean isReadOnly, String filename) throws Exception {
        Path file = output.resolve(filename);
        AddressBook roster = new AddressBook();
        roster.addPerson(target);
        roster.addPerson(survivor);
        Files.deleteIfExists(file);
        new JsonAddressBookStorage(file).saveAddressBook(roster);
        model = new ModelManager(roster, new UserPrefs(), isReadOnly);
        logic = new CountingLogic(new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(output.resolve("prefs.json")))));
        window = new MainWindow(stage, logic, file);
        window.fillInnerParts();
        window.show();
        stage.setWidth(900);
        stage.setHeight(800);
        layout();
        return file;
    }

    private Person selected() {
        return window.getPersonListPanel().getSelectedPerson();
    }

    private TextField commandField() {
        return (TextField) stage.getScene().lookup("#commandTextField");
    }

    private void submit(String command) {
        System.out.println("COMMAND [" + command + "]");
        commandField().setText(command);
        commandField().fireEvent(new ActionEvent());
        layout();
        System.out.println("RESULT " + feedback());
    }

    private void layout() {
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private String feedback() {
        return ((TextArea) stage.getScene().lookup("#resultDisplay")).getText();
    }

    private String saveStatus() {
        return ((Label) stage.getScene().lookup("#saveLocationStatus")).getText();
    }

    private String deletionStatus() {
        return ((Label) stage.getScene().lookup("#deletionStatus")).getText();
    }

    private List<String> detailTexts() {
        ScrollPane pane = (ScrollPane) stage.getScene().lookup(".details-pane");
        return ((VBox) pane.getContent()).getChildren().stream()
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
    }

    private void check(boolean condition, String description) {
        assertions++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    /** Delegates to the real logic and counts every submitted command that reaches it. */
    private static final class CountingLogic implements Logic {
        private final Logic delegate;
        private int executions;

        CountingLogic(Logic delegate) {
            this.delegate = delegate;
        }

        @Override
        public CommandResult execute(String commandText) throws CommandException, ParseException {
            executions++;
            return delegate.execute(commandText);
        }

        @Override
        public CommandResult execute(String commandText, Consumer<CommandResult> presentSearch)
                throws CommandException, ParseException {
            executions++;
            return delegate.execute(commandText, presentSearch);
        }

        @Override
        public ObservableList<Person> getFilteredPersonList() {
            return delegate.getFilteredPersonList();
        }

        @Override
        public GuiSettings getGuiSettings() {
            return delegate.getGuiSettings();
        }

        @Override
        public void setGuiSettings(GuiSettings guiSettings) {
            delegate.setGuiSettings(guiSettings);
        }

        @Override
        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        @Override
        public boolean hasPendingDeletion() {
            return delegate.hasPendingDeletion();
        }

        @Override
        public boolean cancelPendingDeletion() {
            return delegate.cancelPendingDeletion();
        }
    }
}
