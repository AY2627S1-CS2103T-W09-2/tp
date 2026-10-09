package seedu.address.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.EnrolCommand;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Opt-in real-window enrolment acceptance; uses fictional files under build/reports/enrolment. */
public class EnrolmentAcceptance extends Application {
    private static final String COMMAND = "enrol /email E9000001@U.NUS.EDU /module cs2113t /semester ay25/26 s1";
    private final Path output = Path.of("build", "reports", "enrolment");
    private final Path roster = output.resolve("roster.json");
    private Stage stage;
    private ProbeWindow window;
    private ProbeStorage storage;
    private ModelManager model;
    private int assertions;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Files.createDirectories(output);
        Files.deleteIfExists(roster);
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                verify();
                System.out.println("PASS Enrolment JavaFX assertions: " + assertions);
                System.out.println("Runtime: " + System.getProperty("java.runtime.version") + "; "
                        + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
                stage.close();
                Platform.exit();
            } catch (Exception | AssertionError failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        });
    }

    private void open() throws Exception {
        storage = new ProbeStorage(roster);
        model = new ModelManager(storage.readAddressBook().orElseGet(seedu.address.model.AddressBook::new),
                new UserPrefs());
        Logic logic = new LogicManager(model,
                new StorageManager(storage, new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        window = new ProbeWindow(stage, logic, roster);
        window.fillInnerParts();
        window.show();
        stage.setWidth(900);
        stage.setHeight(800);
    }

    private void verify() throws Exception {
        open();
        submit("sample load");
        Person original = model.getAddressBook().getPersonList().getFirst();
        submit("find e9000002");
        Person previouslySelected = window.getPersonListPanel().getSelectedPerson();
        byte[] previous = Files.readAllBytes(roster);
        storage.fail = true;
        submit(COMMAND);
        check(feedback().equals(EnrolCommand.MESSAGE_SAVE_FAILURE), "save failure feedback");
        check(previouslySelected.equals(window.getPersonListPanel().getSelectedPerson()), "failure restores selection");
        check(model.getFilteredPersonList().equals(List.of(previouslySelected)), "failure restores filter");
        check(Arrays.equals(previous, Files.readAllBytes(roster)), "failure preserves bytes");
        storage.fail = false;
        window.failPresentation = true;
        submit(COMMAND);
        check(feedback().equals(Messages.MESSAGE_PROFILE_DISPLAY_FAILURE), "presentation failure feedback");
        check(previouslySelected.equals(window.getPersonListPanel().getSelectedPerson()), "render failure selection");
        check(Arrays.equals(previous, Files.readAllBytes(roster)), "render failure never saves");
        window.failPresentation = false;
        submit(COMMAND + " /section Lab 2 /team SEED-2");
        Person updated = window.getPersonListPanel().getSelectedPerson();
        check(updated.getEmail().equals(original.getEmail()), "selects exact same-name owner");
        check(updated.equals(original.withEnrolments(updated.getEnrolments())), "preserves contacts and sample status");
        check(updated.getEnrolments().size() == original.getEnrolments().size() + 1, "adds one context");
        check(model.getFilteredPersonList().size() == 5, "reveals full roster");
        check(feedback().equals("Added enrolment for Alex Tan: CS2113T, AY25/26 S1.\nModule: CS2113T\n"
                + "Semester: AY25/26 S1\nSection: Lab 2\nTeam: SEED-2"), "visible saved context");
        ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png",
                output.resolve("enrolment.png").toFile());
        byte[] saved = Files.readAllBytes(roster);
        submit("find e9000002");
        submit(COMMAND + " /team Changed");
        check(feedback().equals(String.format(EnrolCommand.MESSAGE_DUPLICATE, "CS2113T", "AY25/26 S1")),
                "duplicate keeps affiliations");
        check(previouslySelected.equals(window.getPersonListPanel().getSelectedPerson()), "duplicate selection");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "duplicate preserves bytes");
        submit(COMMAND.replace("cs2113t", "ma1521"));
        check(feedback().endsWith("Section: Not assigned\nTeam: Not assigned"), "absent affiliation labels");
        ReadOnlyAddressBook expected = new seedu.address.model.AddressBook(model.getAddressBook());
        stage.close();
        open();
        check(expected.equals(model.getAddressBook()), "fresh model/window reload preserves whole roster");
        submit("find e9000001");
        check(window.getPersonListPanel().getSelectedPerson().getEnrolments().size()
                == original.getEnrolments().size() + 2, "reloaded owner has both added contexts");
    }

    private void submit(String command) {
        System.out.println("COMMAND " + command);
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(command);
        input.fireEvent(new ActionEvent());
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
        System.out.println("RESULT " + feedback());
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

    private static class ProbeStorage extends JsonAddressBookStorage {
        private boolean fail;

        ProbeStorage(Path file) {
            super(file);
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
            if (fail) {
                throw new IOException("Injected failure");
            }
            super.saveAddressBook(data);
        }
    }

    private static class ProbeWindow extends MainWindow {
        private boolean failPresentation;

        ProbeWindow(Stage stage, Logic logic, Path file) {
            super(stage, logic, file);
        }

        @Override
        PersonListPanel createPersonListPanel(ObservableList<Person> persons) {
            if (failPresentation) {
                throw new IllegalStateException("Injected rendering failure");
            }
            return super.createPersonListPanel(persons);
        }
    }
}
