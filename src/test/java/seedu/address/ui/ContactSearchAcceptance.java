package seedu.address.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import seedu.address.logic.commands.FindCommand;
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
import seedu.address.model.person.Remark;
import seedu.address.model.person.Telegram;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in JavaFX acceptance and timing probe; runs separately from the headless unit suite.
 * Uses only fictional data under build/reports/contact-search and refuses any search save attempt.
 */
public class ContactSearchAcceptance extends Application {
    private final Path output = Path.of("build", "reports", "contact-search");
    private ProbeWindow window;
    private ModelManager model;
    private Stage stage;
    private int assertions;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        Files.createDirectories(output);
        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            try {
                verifyWorkflow();
                measureSearches();
                System.out.println("PASS Contact search JavaFX assertions: " + assertions);
                stage.close();
                Platform.exit();
            } catch (Exception | AssertionError failure) {
                failure.printStackTrace();
                System.exit(1);
            }
        });
    }

    private void open(ReadOnlyAddressBook roster, String filename) throws Exception {
        Path file = output.resolve(filename);
        new JsonAddressBookStorage(file).saveAddressBook(roster);
        model = new ModelManager(roster, new UserPrefs());
        JsonAddressBookStorage neverSave = new JsonAddressBookStorage(file) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) {
                throw new AssertionError("Search attempted a roster save");
            }
        };
        Logic logic = new LogicManager(model,
                new StorageManager(neverSave, new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        window = new ProbeWindow(stage, logic, file);
        window.fillInnerParts();
        window.show();
        stage.setWidth(850);
        stage.setHeight(750);
    }

    private void verifyWorkflow() throws Exception {
        AddressBook roster = new AddressBook(SampleDataUtil.getSampleAddressBook());
        for (Person original : List.copyOf(roster.getPersonList()).subList(0, 2)) {
            roster.setPerson(original, original.withContacts(Optional.of(new Telegram("shared_chat")),
                    Optional.of(new GitHub("shared-repo"))));
        }
        open(roster, "acceptance.json");
        byte[] before = Files.readAllBytes(output.resolve("acceptance.json"));
        submit("find @SOCDEX_DEMO_MEI");
        check(selectedEmail().equals("e9000003@u.nus.edu"), "Telegram-only match selected");
        check(feedback().equals("1 student found for \"@SOCDEX_DEMO_MEI\"."), "query case preserved");
        submit("find socdex-demo-ravi");
        check(selectedEmail().equals("e9000004@u.nus.edu"), "GitHub-only match selected from full roster");
        submit("find @SHARED_CHAT");
        check(model.getFilteredPersonList().size() == 2 && selectedEmail().isEmpty(), "shared handle clears selection");
        check(model.getFilteredPersonList().getFirst().getEmail().value.equals("e9000001@u.nus.edu"),
                "same names sorted by canonical email");
        submit("find shared-repo");
        check(model.getFilteredPersonList().size() == 2 && selectedEmail().isEmpty(), "shared GitHub matches");
        submit("find @");
        check(model.getFilteredPersonList().size() == 5 && selectedEmail().isEmpty(), "@ still matches emails");
        for (String query : List.of("Not provided", "SEED", "@@shared_chat", "*")) {
            submit("find " + query);
            check(model.getFilteredPersonList().isEmpty() && selectedEmail().isEmpty(), "literal nonmatch: " + query);
        }
        submit("find @socdex_demo_mei");
        Person selected = window.getPersonListPanel().getSelectedPerson();
        for (String invalid : List.of("find", "find " + "x".repeat(101), "find mei\n")) {
            submit(invalid);
            check(selected.equals(window.getPersonListPanel().getSelectedPerson()),
                    "invalid query preserves selection");
        }
        submit("find");
        check(feedback().startsWith(FindCommand.MESSAGE_EMPTY_QUERY), "four-identifier empty-query guidance");
        window.failPresentation = true;
        submit("find shared-repo");
        check(feedback().equals(Messages.MESSAGE_SEARCH_DISPLAY_FAILURE), "render failure feedback");
        check(model.getFilteredPersonList().equals(List.of(selected)), "render failure restores results");
        check(selected.equals(window.getPersonListPanel().getSelectedPerson()),
                "render failure restores selection");
        window.failPresentation = false;
        submit("find @socdex_demo_mei");
        check(Arrays.equals(before, Files.readAllBytes(output.resolve("acceptance.json"))), "file bytes unchanged");
        ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png",
                output.resolve("acceptance.png").toFile());
        stage.close();
    }

    private void measureSearches() throws Exception {
        AddressBook roster = new AddressBook();
        List<Enrolment> enrolments = List.of(new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1")),
                new Enrolment(new ModuleCode("CS2113T"), new Semester("AY26/27 S2")));
        for (int i = 0; i < 500; i++) {
            String suffix = String.format(java.util.Locale.ROOT, "%03d", i);
            roster.addPerson(new Person(new Name("Student " + suffix), new Email("bench" + suffix + "@u.nus.edu"),
                    Optional.of(new Telegram("Bench_" + suffix)), Optional.of(new GitHub("bench-repo-" + suffix)),
                    true, new Remark(""), Set.of(), enrolments));
        }
        open(roster, "benchmark.json");
        System.out.println("Runtime: " + System.getProperty("java.runtime.version") + "; JavaFX "
                + System.getProperty("javafx.runtime.version") + "; " + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        System.out.println("500 profiles / 1000 enrolments; action event through feedback/layout and scene snapshot.");
        for (String query : List.of("@Bench_499", "repo-499", "unmatched", "bench")) {
            double[] millis = new double[6];
            for (int i = 0; i < millis.length; i++) {
                long started = System.nanoTime();
                submit("find " + query);
                stage.getScene().snapshot(null);
                millis[i] = (System.nanoTime() - started) / 1_000_000.0;
                int expected = query.equals("bench") ? 500 : query.equals("unmatched") ? 0 : 1;
                check(model.getFilteredPersonList().size() == expected
                        && feedback().equals(Messages.formatSearchResult(query, expected)), "benchmark result");
            }
            System.out.println("TIMING " + query + " results=" + model.getFilteredPersonList().size()
                    + " first+5repeats_ms=" + Arrays.toString(millis));
        }
    }

    private void submit(String text) {
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(text);
        input.fireEvent(new ActionEvent());
        stage.getScene().getRoot().applyCss();
        stage.getScene().getRoot().layout();
    }

    private String selectedEmail() {
        Person selected = window.getPersonListPanel().getSelectedPerson();
        return selected == null ? "" : selected.getEmail().value;
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
