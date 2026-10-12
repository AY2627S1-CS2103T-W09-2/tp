package seedu.address.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearSamplesCommand;
import seedu.address.logic.commands.ConfirmDeleteCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.EnrolCommand;
import seedu.address.logic.commands.SampleCommand;
import seedu.address.logic.commands.ViewCommand;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/**
 * Opt-in real-window acceptance of the integrated MVP workflow in one sequence: first use, samples, a non-sample
 * student across restarts, deletion and its cancellations, selective sample removal, the duplicate and no-op guards,
 * injected save and display failures, read-only recovery from invalid files, and command-box focus and retention.
 * Uses only fictional data under build/reports/mvp-acceptance and writes the command transcript there.
 */
public final class MvpAcceptance {
    private static final String CANCELLED = Messages.MESSAGE_PENDING_DELETION_CANCELLED;
    private static final String ALEX_EMAIL = "e9000001@u.nus.edu";
    private static final String MEI_EMAIL = "e9000003@u.nus.edu";
    private static final String RAVI_EMAIL = "e9000004@u.nus.edu";
    private static final String PRIYA_EMAIL = "e9100001@u.nus.edu";
    private static final String PRIYA_TELEGRAM = "priya_raman_demo";
    private static final String PRIYA_GITHUB = "priya-raman-demo";

    private final Path output = Path.of("build", "reports", "mvp-acceptance");
    private final List<String> transcript = new ArrayList<>();
    private Stage stage;
    private MainWindow window;
    private ModelManager model;
    private boolean isSaveFailing;
    private boolean isListDisplayFailing;
    private String failingProfileEmail;
    private int assertions;

    private MvpAcceptance() {
    }

    /** Runs the integrated scenario in order and reports its actual environment. */
    public static void main(String[] args) throws Exception {
        MvpAcceptance acceptance = new MvpAcceptance();
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            acceptance.stage = new Stage();
            try {
                Files.createDirectories(acceptance.output);
                Path roster = acceptance.output.resolve("roster.json");
                acceptance.verifyFirstUseAndSamples(roster);
                acceptance.verifyNonSampleStudentAcrossRestart(roster);
                acceptance.verifyDeletionAndCancellations(roster);
                acceptance.verifySelectiveSampleClear(roster);
                acceptance.verifyGuardsAndNoOps(roster);
                acceptance.verifyInjectedFailures();
                acceptance.verifyInvalidFilesStartReadOnly(roster);
                List<String> summary = List.of("PASS MVP JavaFX assertions: " + acceptance.assertions,
                        "Runtime: " + System.getProperty("java.runtime.version") + "; JavaFX "
                        + System.getProperty("javafx.runtime.version") + "; " + System.getProperty("os.name") + " "
                        + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
                summary.forEach(System.out::println);
                acceptance.transcript.addAll(summary);
            } catch (Exception | AssertionError error) {
                failure.set(error);
            } finally {
                try {
                    Files.write(acceptance.output.resolve("transcript.txt"), acceptance.transcript);
                } catch (IOException e) {
                    failure.compareAndSet(null, e);
                }
                Platform.setImplicitExit(true);
                acceptance.stage.close();
                finished.countDown();
            }
        });
        if (!finished.await(60, TimeUnit.SECONDS)) {
            throw new AssertionError("Timed out waiting for MVP acceptance");
        }
        if (failure.get() != null) {
            throw new AssertionError("MVP acceptance failed", failure.get());
        }
    }

    private void verifyFirstUseAndSamples(Path roster) throws Exception {
        Files.deleteIfExists(roster);
        start(roster);
        check(model.getAddressBook().getPersonList().isEmpty(), "first use starts with an empty roster");
        check(feedback().equals(Messages.MESSAGE_EMPTY_ROSTER), "first use shows the empty-roster guidance");
        check(!Files.exists(roster), "first use creates no roster file");

        submit("sample load");
        check(feedback().equals(SampleCommand.MESSAGE_SUCCESS), "sample load reports 5 profiles and 5 enrolments");
        check(emails(displayed()).equals(List.of(ALEX_EMAIL, "e9000002@u.nus.edu", MEI_EMAIL, "e9000005@u.nus.edu",
                RAVI_EMAIL)),
                "samples listed by name, then email");
        check(displayed().stream().allMatch(Person::isSample)
                && displayed().stream().mapToInt(person -> person.getEnrolments().size()).sum() == 5,
                "every sample is classified and 5 enrolments were loaded");
        check(selected() == null, "sample load selects no profile");
        checkSucceeded("sample load");

        submit("find Alex Tan");
        check(feedback().equals("2 students found for \"Alex Tan\"."), "same-name search reports two results");
        check(emails(displayed()).equals(List.of(ALEX_EMAIL, "e9000002@u.nus.edu")), "both Alex Tan profiles shown");
        check(renderedRow(0).equals(List.of("1. ", "Alex Tan", "NUS email: " + ALEX_EMAIL,
                "Telegram: socdex_demo_alex", "GitHub: socdex-demo-alex", "Fictional sample",
                "Enrolments: 2\nCS2103T | AY26/27 S1 | Section: T12 | Team: SEED\n"
                        + "CS2113T | AY26/27 S2 | Section: T14 | Team: Not assigned")),
                "first same-name row shows its identifiers, classification and enrolments");
        check(renderedRow(1).equals(List.of("2. ", "Alex Tan", "NUS email: e9000002@u.nus.edu",
                "Telegram: Not provided", "GitHub: socdex-demo-alex2", "Fictional sample",
                "Enrolments: 1\nCS2103T | AY26/27 S1 | Section: T14 | Team: SPROUT")),
                "second same-name row is distinguished by its email");
        check(selected() == null && detailTexts().equals(List.of(PersonDetailsPanel.SELECTION_PROMPT)),
                "same-name results make no automatic choice");

        submit("find @socdex_demo_mei");
        check(feedback().equals("1 student found for \"@socdex_demo_mei\"."), "unique Telegram match reported");
        checkShowsFullProfile(MEI_EMAIL, "unique identifier match");
        check(detailTexts().equals(List.of("Mei Lim", "Fictional sample", "NUS email: " + MEI_EMAIL,
                "Telegram: socdex_demo_mei", "GitHub: Not provided", "Enrolments: 1",
                "CS2103T | AY26/27 S1 | Section: T12 | Team: SEED")), "profile shows every literal field");

        submit("view " + ALEX_EMAIL.toUpperCase());
        check(feedback().equals(String.format(ViewCommand.MESSAGE_SUCCESS, "Alex Tan")), "view feedback");
        checkShowsFullProfile(ALEX_EMAIL, "view of a hidden same-name student");
        check(detailTexts().contains("CS2113T | AY26/27 S2 | Section: T14 | Team: Not assigned"),
                "every enrolment shown, including an unassigned team");
        stage.close();
    }

    private void verifyNonSampleStudentAcrossRestart(Path roster) throws Exception {
        reopen(roster);
        submit("student add /name Priya Raman /email " + PRIYA_EMAIL + " /github " + PRIYA_GITHUB);
        check(feedback().equals(String.format(AddCommand.MESSAGE_SUCCESS, "Priya Raman")), "student add feedback");
        checkShowsFullProfile(PRIYA_EMAIL, "added student");
        check(!person(PRIYA_EMAIL).isSample(), "added student is not a fictional sample");

        submit("enrol /email " + PRIYA_EMAIL + " /module CS2103T /semester AY26/27 S1 /section T12 /team SEED");
        check(feedback().startsWith(String.format(EnrolCommand.MESSAGE_SUCCESS, "Priya Raman", "CS2103T",
                "AY26/27 S1")), "first enrolment added");
        submit("enrol /email " + PRIYA_EMAIL + " /module CS2101 /semester AY26/27 S2");
        check(feedback().startsWith(String.format(EnrolCommand.MESSAGE_SUCCESS, "Priya Raman", "CS2101",
                "AY26/27 S2")), "second enrolment added");
        submit("edit /email " + PRIYA_EMAIL + " /telegram @" + PRIYA_TELEGRAM);
        check(feedback().equals(String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, "Priya Raman")),
                "optional contact edited");
        checkShowsFullProfile(PRIYA_EMAIL, "edited student");
        check(detailTexts().containsAll(List.of("Telegram: " + PRIYA_TELEGRAM, "GitHub: " + PRIYA_GITHUB,
                "Enrolments: 2", "CS2101 | AY26/27 S2 | Section: Not assigned | Team: Not assigned",
                "CS2103T | AY26/27 S1 | Section: T12 | Team: SEED")), "every enrolment and contact inspected");

        for (String query : List.of("Priya Raman", PRIYA_EMAIL.toUpperCase(), "@" + PRIYA_TELEGRAM, PRIYA_GITHUB)) {
            submit("find " + query);
            check(feedback().equals("1 student found for \"" + query + "\"."), "found by identifier: " + query);
            checkShowsFullProfile(PRIYA_EMAIL, "search by " + query);
        }

        List<Person> beforeRestart = List.copyOf(model.getAddressBook().getPersonList());
        submit("bye");
        check(!stage.isShowing(), "bye closes the window");
        reopen(roster);
        check(model.getAddressBook().getPersonList().equals(beforeRestart), "restart returns the same records");
        check(model.getAddressBook().getPersonList().size() == 6, "restart returns 5 samples and Priya Raman");
        submit("view " + PRIYA_EMAIL);
        checkShowsFullProfile(PRIYA_EMAIL, "restarted student");
        stage.close();
    }

    private void verifyDeletionAndCancellations(Path roster) throws Exception {
        reopen(roster);
        byte[] saved = Files.readAllBytes(roster);
        String preview = "delete " + MEI_EMAIL;
        String confirmation = "confirm-delete " + MEI_EMAIL;

        checkCancels(preview, "find Alex", CANCELLED + "\n2 students found for \"Alex\".", "another command");
        checkCancels(preview, "", CANCELLED + "\n" + Messages.MESSAGE_ENTER_COMMAND, "blank submission");
        checkCancels(preview, "unknowncommand", CANCELLED + "\n" + Messages.MESSAGE_UNKNOWN_COMMAND,
                "unknown command");
        checkCancels(preview, "confirm-delete", CANCELLED + "\n" + ConfirmDeleteCommand.MESSAGE_MISSING_EMAIL,
                "malformed confirmation");
        checkCancels(preview, "confirm-delete " + RAVI_EMAIL, ConfirmDeleteCommand.MESSAGE_MISMATCH,
                "mismatched confirmation");
        submit(preview);
        submit("delete " + RAVI_EMAIL);
        check(feedback().equals(CANCELLED + "\n" + String.format(DeleteCommand.MESSAGE_PREVIEW, "Ravi Kumar",
                RAVI_EMAIL, 1, RAVI_EMAIL)), "a replacement preview reports the cancellation above the new preview");
        check(model.getPendingDeletion().map(email -> email.value.equals(RAVI_EMAIL)).orElse(false)
                && person(RAVI_EMAIL).equals(selected()), "only the replacement target is pending and selected");
        submit(confirmation);
        check(feedback().equals(ConfirmDeleteCommand.MESSAGE_MISMATCH) && hasEmail(MEI_EMAIL)
                && hasEmail(RAVI_EMAIL), "the replaced preview can no longer be confirmed");

        submit(preview);
        model.updateFilteredPersonList(person -> person.getEmail().value.equals(RAVI_EMAIL)); // display is now stale
        submit(confirmation);
        check(feedback().equals(CANCELLED + "\n" + DisplayedCommandExecutor.MESSAGE_DISPLAY_CHANGED),
                "a stale display rejects the confirmation and reports the cancellation");
        check(model.getPendingDeletion().isEmpty() && deletionStatus().isEmpty(), "stale display cancels");
        check(emails(displayed()).equals(List.of(RAVI_EMAIL)), "the refreshed rows match the model");
        check(hasEmail(MEI_EMAIL), "the stale confirmation deletes nothing");
        submit("list");

        submit(preview);
        pressListKey(selected().equals(person(MEI_EMAIL)) ? KeyCode.DOWN : KeyCode.UP);
        check(!person(MEI_EMAIL).equals(selected()) && feedback().equals(CANCELLED), "keyboard selection cancels");
        check(deletionStatus().isEmpty(), "keyboard cancellation removes the indicator");
        submit(confirmation);
        check(feedback().equals(ConfirmDeleteCommand.MESSAGE_NO_PENDING), "cancelled deletion needs a fresh preview");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "cancellations never rewrite the roster");

        submit("find Mei");
        submit("delete " + MEI_EMAIL.toUpperCase());
        check(feedback().equals(String.format(DeleteCommand.MESSAGE_PREVIEW, "Mei Lim", MEI_EMAIL, 1, MEI_EMAIL)),
                "preview lists the target, its enrolment count and the exact confirmation");
        check(deletionStatus().equals(StatusBarFooter.DELETION_PENDING), "preview shows the pending indicator");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "preview does not save");
        submit(confirmation);
        check(feedback().equals(String.format(ConfirmDeleteCommand.MESSAGE_SUCCESS, "Mei Lim", MEI_EMAIL, 1)),
                "confirmed deletion reports the student and enrolments removed");
        check(!stored(roster).contains(MEI_EMAIL) && stored(roster).size() == 5, "only Mei Lim was deleted and saved");
        check(selected() == null && deletionStatus().isEmpty(), "deletion clears selection and indicator");
        stage.close();
    }

    private void verifySelectiveSampleClear(Path roster) throws Exception {
        reopen(roster);
        submit("find Priya");
        Person priya = person(PRIYA_EMAIL);
        check(priya.equals(selected()), "non-sample student selected in a search hiding the samples");
        submit("sample clear");
        check(feedback().equals(String.format(ClearSamplesCommand.MESSAGE_SUCCESS, 4, 4)),
                "sample clear reports the actual remaining sample counts");
        check(displayed().equals(List.of(priya)) && priya.equals(selected()), "non-sample student kept selected");
        checkShowsFullProfile(PRIYA_EMAIL, "non-sample student after sample clear");
        byte[] saved = Files.readAllBytes(roster);
        submit("sample clear");
        check(feedback().equals(ClearSamplesCommand.MESSAGE_NO_SAMPLES), "repeated sample clear finds none");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "repeated sample clear does not save");
        stage.close();

        reopen(roster);
        check(model.getAddressBook().getPersonList().equals(List.of(priya)), "restart returns only the real record");
        stage.close();
    }

    private void verifyGuardsAndNoOps(Path roster) throws Exception {
        reopen(roster);
        byte[] saved = Files.readAllBytes(roster);
        submit("find nobody");
        checkRejected("student add /name Priya Raman Again /email " + PRIYA_EMAIL.toUpperCase(),
                AddCommand.MESSAGE_DUPLICATE_PERSON, "duplicate email");
        check(person(PRIYA_EMAIL).equals(selected()), "duplicate email reveals the existing student");
        checkRejected("enrol /email " + PRIYA_EMAIL + " /module CS2103T /semester AY26/27 S1 /section T01",
                String.format(EnrolCommand.MESSAGE_DUPLICATE, "CS2103T", "AY26/27 S1"), "duplicate enrolment");
        submit("edit /email " + PRIYA_EMAIL + " /telegram " + PRIYA_TELEGRAM);
        check(feedback().equals(EditCommand.MESSAGE_NO_CHANGE), "unchanged edit is reported as a no-op");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "the no-op edit does not save");
        checkRejected("sample load", SampleCommand.MESSAGE_NON_EMPTY, "sample load into a non-empty roster");
        checkRejected("student add /name Bad Email /email bad@example.com",
                Email.MESSAGE_CONSTRAINTS, "invalid email");
        check(Arrays.equals(saved, Files.readAllBytes(roster)), "guards and no-ops never rewrite the roster");
        stage.close();
    }

    private void verifyInjectedFailures() throws Exception {
        Path roster = output.resolve("failures.json");
        Files.deleteIfExists(roster);
        start(roster);
        submit("sample load");
        submit("find Alex");
        submit("view e9000002@u.nus.edu");
        Snapshot before = snapshot(roster);

        isSaveFailing = true;
        checkPreserved("edit /email e9000002@u.nus.edu /github changed-handle",
                "The record could not be updated. No data was changed.", before, "edit save");
        checkPreserved("enrol /email e9000002@u.nus.edu /module CS2101 /semester AY26/27 S1",
                EnrolCommand.MESSAGE_SAVE_FAILURE, before, "enrol save");
        checkPreserved("student add /name Hidden Student /email e9100002@u.nus.edu",
                "The student could not be saved. No data was changed.", before, "add save");
        submit("delete e9000002@u.nus.edu");
        checkPreserved("confirm-delete e9000002@u.nus.edu", ConfirmDeleteCommand.MESSAGE_FAILURE, before,
                "confirm-delete save");
        check(model.getPendingDeletion().isEmpty() && deletionStatus().isEmpty(),
                "failed deletion leaves nothing pending");
        isSaveFailing = false;
        submit("edit /email e9000002@u.nus.edu /github changed-handle");
        check(feedback().equals(String.format(EditCommand.MESSAGE_EDIT_PERSON_SUCCESS, "Alex Tan")),
                "the same edit succeeds once storage recovers");
        check(storedPerson(roster, "e9000002@u.nus.edu").getGitHub().map(Object::toString)
                .equals(Optional.of("changed-handle")), "recovered storage saves the edited GitHub username");

        submit("find Alex");
        submit("view e9000002@u.nus.edu");
        before = snapshot(roster);
        failingProfileEmail = MEI_EMAIL;
        checkPreserved("view " + MEI_EMAIL, ViewCommand.MESSAGE_DISPLAY_FAILURE, before, "view display");
        failingProfileEmail = "e9100002@u.nus.edu";
        checkPreserved("student add /name Hidden Student /email e9100002@u.nus.edu",
                Messages.MESSAGE_PROFILE_DISPLAY_FAILURE, before, "add display");
        failingProfileEmail = null;
        isListDisplayFailing = true;
        checkPreserved("find Mei", Messages.MESSAGE_SEARCH_DISPLAY_FAILURE, before, "search display");
        isListDisplayFailing = false;
        submit("find Mei");
        checkShowsFullProfile(MEI_EMAIL, "search after the display recovers");
        stage.close();
    }

    private void verifyInvalidFilesStartReadOnly(Path validRoster) throws Exception {
        String badEmailFile = "{\"persons\":[{\"name\":\"Alex Tan\",\"email\":\"alex@example.com\"}]}";
        String truncatedFile = "{\"persons\": [ {\"name\": \"Alex";
        Path roster = output.resolve("invalid.json");
        for (String contents : List.of(truncatedFile, badEmailFile)) {
            Files.writeString(roster, contents, StandardCharsets.UTF_8);
            byte[] saved = Files.readAllBytes(roster);
            start(roster);
            check(model.isReadOnly() && displayed().isEmpty(), "invalid file opens an empty read-only session");
            check(feedback().equals(Messages.MESSAGE_LOAD_FAILURE), "invalid file explains the preservation");
            check(saveStatus().equals("Storage unavailable \u2014 read-only recovery | "
                    + Path.of(".").resolve(roster)), "recovery status and path shown");
            for (String change : List.of("sample load", "student add /name Test Student /email " + PRIYA_EMAIL,
                    "enrol /email " + PRIYA_EMAIL + " /module CS2103T /semester AY26/27 S1", "sample clear",
                    "delete " + PRIYA_EMAIL)) {
                checkRejected(change, LogicManager.MESSAGE_READ_ONLY, "read-only recovery: " + change);
            }
            submit("find Alex");
            check(feedback().startsWith("No students found for \"Alex\"."), "search remains available");
            if (contents.equals(truncatedFile)) {
                submit("bye");
            } else {
                transcript.add("WINDOW close request");
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
            check(!stage.isShowing(), "recovery session closes");
            check(Arrays.equals(saved, Files.readAllBytes(roster)), "closing never overwrites the preserved file");
        }

        // The user restores a valid file outside the application; the next start loads it normally.
        Files.copy(validRoster, roster, StandardCopyOption.REPLACE_EXISTING);
        reopen(roster);
        check(emails(model.getAddressBook().getPersonList()).equals(List.of(PRIYA_EMAIL)),
                "restored file returns its records after a restart");
        check(saveStatus().equals("Storage available | " + Path.of(".").resolve(roster)), "storage available again");
        submit("view " + PRIYA_EMAIL);
        checkShowsFullProfile(PRIYA_EMAIL, "restored student");
        stage.close();
    }

    /** Previews the deletion of Mei Lim, then checks that {@code command} cancels it and saves nothing. */
    private void checkCancels(String preview, String command, String expected, String cancellation) {
        submit(preview);
        check(deletionStatus().equals(StatusBarFooter.DELETION_PENDING), cancellation + ": preview pending");
        submit(command);
        check(feedback().equals(expected), cancellation + " cancels and reports: " + feedback());
        check(deletionStatus().isEmpty(), cancellation + " removes the indicator");
        check(hasEmail(MEI_EMAIL), cancellation + " deletes nothing");
        check(isCommandBoxFocused(), cancellation + " keeps focus in the command box");
    }

    /** Checks that a failed command keeps the roster, the file, the results, the selection and the profile shown. */
    private void checkPreserved(String command, String expected, Snapshot before, String failure) throws Exception {
        checkRejected(command, expected, failure);
        Snapshot after = snapshot(before.file());
        check(after.records().equals(before.records()), failure + " keeps every record unchanged");
        check(Arrays.equals(after.bytes(), before.bytes()), failure + " keeps the file bytes");
        check(after.results().equals(before.results()), failure + " keeps the displayed results");
        check(before.shown().equals(after.shown()), failure + " keeps the selection");
        check(detailTexts().equals(expectedDetails(before.shown())), failure + " keeps the complete profile");
        check(model.getFilteredPersonList().equals(after.results()), failure + ": rows target the displayed students");
    }

    /** Checks that {@code command} is rejected with {@code expected} and stays in the command box for correction. */
    private void checkRejected(String command, String expected, String rejection) {
        submit(command);
        check(feedback().equals(expected), rejection + " reports: " + feedback());
        check(commandField().getText().equals(command), rejection + " keeps the rejected input");
        check(commandField().getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS), rejection + " marks the input");
        check(isCommandBoxFocused(), rejection + " keeps focus in the command box");
    }

    private void checkSucceeded(String command) {
        check(commandField().getText().isEmpty(), command + " clears the command box");
        check(isCommandBoxFocused(), command + " returns focus to the command box");
    }

    private void checkShowsFullProfile(String email, String context) {
        Person person = person(email);
        check(person.equals(selected()), context + " selects " + email);
        check(detailTexts().equals(expectedDetails(person)), context + " shows the complete profile");
        check(detailTexts().contains(PersonCard.EMAIL_PREFIX + email), context + " shows the full email");
        check(isCommandBoxFocused(), context + " keeps focus in the command box");
    }

    /** Opens a session on {@code file} as {@code MainApp} does: empty if absent, read-only recovery if invalid. */
    private void start(Path file) throws Exception {
        ReadOnlyAddressBook data;
        boolean isReadOnly = false;
        try {
            data = new JsonAddressBookStorage(file).readAddressBook().orElseGet(AddressBook::new);
        } catch (DataLoadingException e) {
            data = new AddressBook();
            isReadOnly = true;
        }
        model = new ModelManager(data, new UserPrefs(), isReadOnly);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                if (isSaveFailing) {
                    throw new IOException("Injected save failure");
                }
                super.saveAddressBook(addressBook);
            }
        };
        LogicManager logic = new LogicManager(model,
                new StorageManager(storage, new JsonUserPrefsStorage(output.resolve("prefs.json"))));
        window = new MainWindow(stage, logic, file) {
            @Override
            PersonListPanel createPersonListPanel(ObservableList<Person> persons) {
                if (isListDisplayFailing) {
                    throw new IllegalStateException("Injected result list display failure");
                }
                return super.createPersonListPanel(persons);
            }

            @Override
            PersonDetailsPanel createPersonDetailsPanel(Person person) {
                if (person != null && person.getEmail().value.equals(failingProfileEmail)) {
                    throw new IllegalStateException("Injected profile display failure");
                }
                return super.createPersonDetailsPanel(person);
            }
        };
        window.fillInnerParts();
        window.show();
        stage.setWidth(1280);
        stage.setHeight(720);
        runPendingFxTasks();
        // Initial focus depends on the OS granting window focus, which a programmatic launch may not get, so it is
        // recorded rather than asserted. Focus is then placed in the command box, as a user's first click would.
        transcript.add("FOCUS at start: window=" + stage.isFocused() + " owner="
                + describe(stage.getScene().getFocusOwner()));
        commandField().requestFocus();
        transcript.add("SESSION " + file + (isReadOnly ? " (read-only recovery)" : ""));
        transcript.add("RESULT " + feedback());
    }

    /** Opens a new session on the saved file, as a restart would, with a fresh model and logic. */
    private void reopen(Path file) throws Exception {
        start(file);
        check(!model.isReadOnly(), "restart loads the saved roster");
    }

    private Person person(String email) {
        return model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().value.equals(email)).findFirst().orElseThrow();
    }

    private boolean hasEmail(String email) {
        return emails(model.getAddressBook().getPersonList()).contains(email);
    }

    private static Person storedPerson(Path file, String email) throws Exception {
        return new JsonAddressBookStorage(file).readAddressBook().orElseThrow().getPersonList().stream()
                .filter(person -> person.getEmail().value.equals(email)).findFirst().orElseThrow();
    }

    private Snapshot snapshot(Path file) throws IOException {
        return new Snapshot(file, List.copyOf(model.getAddressBook().getPersonList()), Files.readAllBytes(file),
                displayed(), selected());
    }

    private static List<String> stored(Path file) throws Exception {
        return emails(new JsonAddressBookStorage(file).readAddressBook().orElseThrow().getPersonList());
    }

    private static List<String> emails(List<Person> persons) {
        return persons.stream().map(person -> person.getEmail().value).toList();
    }

    private static List<String> expectedDetails(Person person) {
        List<String> expected = new ArrayList<>(List.of(person.getName().fullName));
        expected.addAll(PersonDetailsPanel.profileLines(person));
        return expected;
    }

    private Person selected() {
        return window.getPersonListPanel().getSelectedPerson();
    }

    /** Returns the text of every label in the rendered row at {@code index}, in display order. */
    private List<String> renderedRow(int index) {
        ListCell<?> cell = stage.getScene().lookup("#personListView").lookupAll(".list-cell").stream()
                .map(node -> (ListCell<?>) node)
                .filter(candidate -> candidate.getIndex() == index && !candidate.isEmpty())
                .findFirst().orElseThrow();
        return cell.lookupAll(".label").stream()
                .map(Label.class::cast)
                .filter(label -> label.isVisible() && !label.getText().isEmpty())
                .map(Label::getText)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Person> displayed() {
        return List.copyOf(((ListView<Person>) stage.getScene().lookup("#personListView")).getItems());
    }

    /** Sends a key press to the focused student list, then places focus back in the command box. */
    private void pressListKey(KeyCode key) {
        ListView<?> list = (ListView<?>) stage.getScene().lookup("#personListView");
        list.requestFocus();
        list.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
        layout();
        transcript.add("KEY " + key + " on the student list");
        transcript.add("RESULT " + feedback());
        commandField().requestFocus();
    }

    /** Lets the window process queued work, such as granting initial focus, as a shown window would. */
    private void runPendingFxTasks() {
        for (int i = 0; i < 3; i++) {
            Object key = new Object();
            Platform.runLater(() -> Platform.exitNestedEventLoop(key, null));
            Platform.enterNestedEventLoop(key);
        }
        layout();
    }

    private static String describe(Node node) {
        return node == null ? "none" : node.getId() != null ? "#" + node.getId() : node.getClass().getSimpleName();
    }

    private TextField commandField() {
        return (TextField) stage.getScene().lookup("#commandTextField");
    }

    private boolean isCommandBoxFocused() {
        return stage.getScene().getFocusOwner() == commandField();
    }

    private void submit(String command) {
        transcript.add("COMMAND [" + command + "]");
        commandField().setText(command);
        commandField().fireEvent(new ActionEvent());
        layout();
        transcript.add("RESULT " + feedback().replace("\n", " / "));
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

    /** What a failed command must leave unchanged: the roster, the saved file, the results and the selection. */
    private record Snapshot(Path file, List<Person> records, byte[] bytes, List<Person> results, Person shown) {
    }
}
