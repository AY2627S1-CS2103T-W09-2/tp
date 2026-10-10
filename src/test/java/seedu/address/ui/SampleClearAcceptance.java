package seedu.address.ui;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
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
import seedu.address.model.tag.Tag;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Opt-in real-window sample-clear acceptance; uses fictional files under build/reports/sample-clear. */
public final class SampleClearAcceptance {
    private static final String REAL_EMAIL = "real.alex@u.nus.edu";

    private final Path output = Path.of("build", "reports", "sample-clear");
    private Stage stage;
    private MainWindow window;
    private ModelManager model;
    private int assertions;

    private SampleClearAcceptance() {
    }

    /** Runs the real-window acceptance scenario and reports its actual environment. */
    public static void main(String[] args) throws Exception {
        SampleClearAcceptance acceptance = new SampleClearAcceptance();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            acceptance.stage = new Stage();
            try {
                Files.createDirectories(acceptance.output);
                acceptance.verifySelectedRealSurvivesAndRestarts();
                acceptance.verifySelectedSampleIsCleared();
                acceptance.verifyEmptyRosterGuidance();
                System.out.println("PASS Sample clear JavaFX assertions: " + acceptance.assertions);
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
            throw new AssertionError("Timed out waiting for sample-clear acceptance");
        }
        if (failure.get() != null) {
            throw new AssertionError("Sample-clear acceptance failed", failure.get());
        }
    }

    private void verifySelectedRealSurvivesAndRestarts() throws Exception {
        Path file = output.resolve("selected-real.json");
        Person real = realProfile();
        open(mixedRoster(real), file);
        submit("view " + REAL_EMAIL);
        check(real.equals(window.getPersonListPanel().getSelectedPerson()), "real profile selected before clear");

        submit("sample clear");

        check(feedback().equals(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 5, 5)),
                "actual sample and enrolment counts reported");
        check(model.getAddressBook().getPersonList().equals(List.of(real)), "only complete real profile remains");
        check(real.equals(window.getPersonListPanel().getSelectedPerson()), "selected real profile survives clear");
        check(detailTexts().equals(expectedDetails(real)), "selected real profile stays fully displayed");
        snapshot("selected-real-survives.png");

        stage.close();
        reopen(file);
        check(model.getAddressBook().getPersonList().equals(List.of(real)), "complete real profile reloads");
        submit("view " + REAL_EMAIL);
        check(real.equals(window.getPersonListPanel().getSelectedPerson()), "reloaded real profile selectable");
        check(detailTexts().equals(expectedDetails(real)), "reloaded real profile keeps every displayed field");
        stage.close();
    }

    private void verifySelectedSampleIsCleared() throws Exception {
        Path file = output.resolve("selected-sample.json");
        Person real = realProfile();
        open(mixedRoster(real), file);
        submit("view e9000001@u.nus.edu");
        check(window.getPersonListPanel().getSelectedPerson().isSample(), "sample selected before clear");

        submit("sample clear");

        check(window.getPersonListPanel().getSelectedPerson() == null, "removed sample selection cleared");
        check(model.getAddressBook().getPersonList().equals(List.of(real)), "unrelated real profile preserved");
        check(detailTexts().equals(List.of(PersonDetailsPanel.SELECTION_PROMPT)),
                "non-empty roster shows ordinary selection prompt");
        stage.close();
    }

    private void verifyEmptyRosterGuidance() throws Exception {
        Path file = output.resolve("all-samples.json");
        open(new AddressBook(SampleDataUtil.getSampleAddressBook()), file);
        submit("view e9000001@u.nus.edu");
        check(window.getPersonListPanel().getSelectedPerson().isSample(), "all-sample target selected before clear");

        submit("sample clear");

        check(model.getAddressBook().getPersonList().isEmpty(), "all classified samples removed");
        check(window.getPersonListPanel().getSelectedPerson() == null, "empty roster has no selection");
        check(detailTexts().equals(List.of(Messages.MESSAGE_EMPTY_ROSTER)), "empty-roster guidance displayed");
        snapshot("empty-roster-guidance.png");
    }

    private void open(AddressBook roster, Path file) throws Exception {
        Files.deleteIfExists(file);
        new JsonAddressBookStorage(file).saveAddressBook(roster);
        show(roster, file);
    }

    private void reopen(Path file) throws Exception {
        AddressBook roster = new AddressBook(new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        show(roster, file);
    }

    private void show(AddressBook roster, Path file) {
        model = new ModelManager(roster, new UserPrefs());
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        window = new MainWindow(stage, logic, file);
        window.fillInnerParts();
        window.show();
        stage.setWidth(900);
        stage.setHeight(800);
        layout();
    }

    private static AddressBook mixedRoster(Person real) {
        AddressBook roster = new AddressBook(SampleDataUtil.getSampleAddressBook());
        roster.addPerson(real);
        return roster;
    }

    private static Person realProfile() {
        List<Enrolment> enrolments = List.of(
                new Enrolment(new ModuleCode("CS2103T"), new Semester("AY26/27 S1"),
                        Optional.of(new Section("T12")), Optional.of(new Team("SEED"))),
                new Enrolment(new ModuleCode("CS2100"), new Semester("AY26/27 S2")));
        return new Person(new Name("Alex Tan"), new Email(REAL_EMAIL), Optional.of(new Telegram("real_alex")),
                Optional.of(new GitHub("real-alex")), false, new Remark("Prefers email"),
                Set.of(new Tag("returningstudent")), enrolments);
    }

    private static List<String> expectedDetails(Person person) {
        List<String> expected = new ArrayList<>(List.of(person.getName().fullName));
        expected.addAll(PersonDetailsPanel.profileLines(person));
        return expected;
    }

    private List<String> detailTexts() {
        ScrollPane pane = (ScrollPane) stage.getScene().lookup(".details-pane");
        return ((VBox) pane.getContent()).getChildren().stream()
                .map(Label.class::cast)
                .map(Label::getText)
                .toList();
    }

    private void submit(String command) {
        System.out.println("COMMAND " + command);
        TextField input = (TextField) stage.getScene().lookup("#commandTextField");
        input.setText(command);
        input.fireEvent(new ActionEvent());
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

    private void snapshot(String filename) throws Exception {
        WritableImage snapshot = stage.getScene().snapshot(null);
        BufferedImage image = new BufferedImage((int) snapshot.getWidth(), (int) snapshot.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, snapshot.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(image, "png", output.resolve(filename).toFile());
    }

    private void check(boolean condition, String description) {
        assertions++;
        if (!condition) {
            throw new AssertionError(description);
        }
    }
}
