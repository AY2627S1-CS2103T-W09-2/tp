package seedu.address.ui;

import java.nio.file.Path;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Person;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private final DisplayedCommandExecutor commandExecutor;
    private Path dataFilePath;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private PersonDetailsPanel personDetailsPanel;
    private ResultDisplay resultDisplay;
    private StatusBarFooter statusBarFooter;
    private HelpWindow helpWindow;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane personDetailsPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage}, {@code Logic},
     * and the data file path to show in the status bar.
     */
    public MainWindow(Stage primaryStage, Logic logic, Path dataFilePath) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;
        this.dataFilePath = dataFilePath;
        commandExecutor = new DisplayedCommandExecutor(logic, this::isPersonListCurrent,
                this::presentSearch, this::refreshPersonList);

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in a later version of the SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField and
         * ResultDisplay contains a TextArea, some accelerators (e.g., F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add the following event filter to capture such key events and open
         * the help window purposely so as to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = createPersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());
        personDetailsPanel = createPersonDetailsPanel(null);
        personDetailsPanelPlaceholder.getChildren().add(personDetailsPanel.getRoot());
        followSelection(personListPanel);

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        if (logic.isReadOnly()) {
            resultDisplay.setFeedbackToUser(Messages.MESSAGE_LOAD_FAILURE);
        } else if (logic.getFilteredPersonList().isEmpty()) {
            resultDisplay.setFeedbackToUser(Messages.MESSAGE_EMPTY_ROSTER);
        }

        statusBarFooter = new StatusBarFooter(dataFilePath, logic.isReadOnly());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application. The {@code bye} command, the window's close request and the menu all end here.
     */
    @FXML
    private void handleExit() {
        // Closing never confirms a deletion: any preview awaiting confirmation is discarded and nothing is saved.
        logic.cancelPendingDeletion();
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /** Creates a detached panel so a failed card never replaces the current results. */
    PersonListPanel createPersonListPanel(ObservableList<Person> persons) {
        return new PersonListPanel(persons);
    }

    /** Creates a detached profile display so a failed profile never replaces the current one. */
    PersonDetailsPanel createPersonDetailsPanel(Person person) {
        return new PersonDetailsPanel(person);
    }

    /** Creates the guidance shown when a removal leaves the roster empty. */
    PersonDetailsPanel createEmptyRosterDetailsPanel() {
        return new PersonDetailsPanel(null, Messages.MESSAGE_EMPTY_ROSTER);
    }

    private void presentSearch(CommandResult result) {
        PersonListPanel replacement = createPersonListPanel(logic.getFilteredPersonList());
        if (result.isClearSelection()) {
            // No profile stays selected, even when exactly one result remains.
            replacement.restoreSelection(null);
        } else if (result.isPreserveSurvivingSelection()) {
            replacement.restoreSelection(personListPanel.getSelectedPerson());
        } else if (result.getSelectionTarget() == null) {
            replacement.selectOnlyResult();
        } else {
            replacement.selectTarget(result.getSelectionTarget());
        }
        // A removal that leaves no students shows the empty-roster guidance instead of the selection prompt.
        boolean isRemovalRefresh = result.isClearSelection() || result.isPreserveSurvivingSelection();
        prepareAndReplaceDisplay(replacement, isRemovalRefresh && logic.getFilteredPersonList().isEmpty());
    }

    private boolean isPersonListCurrent() {
        return personListPanel.hasSameResults(logic.getFilteredPersonList());
    }

    private void refreshPersonList() {
        PersonListPanel replacement = createPersonListPanel(logic.getFilteredPersonList());
        replacement.restoreSelection(personListPanel.getSelectedPerson());
        prepareAndReplaceDisplay(replacement);
    }

    /**
     * Prepares the list and its selected profile off screen, then replaces both together.
     * Any failure keeps the previous complete list and profile.
     */
    private void prepareAndReplaceDisplay(PersonListPanel replacement) {
        prepareAndReplaceDisplay(replacement, false);
    }

    private void prepareAndReplaceDisplay(PersonListPanel replacement, boolean showEmptyRosterGuidance) {
        PersonDetailsPanel details = showEmptyRosterGuidance
                ? createEmptyRosterDetailsPanel()
                : createPersonDetailsPanel(replacement.getSelectedPerson());
        prepare(replacement.getRoot(), personListPanelPlaceholder);
        prepare(details.getRoot(), personDetailsPanelPlaceholder);
        PersonListPanel previousList = personListPanel;
        PersonDetailsPanel previousDetails = personDetailsPanel;
        try {
            personListPanelPlaceholder.getChildren().setAll(replacement.getRoot());
            personDetailsPanelPlaceholder.getChildren().setAll(details.getRoot());
        } catch (RuntimeException | AssertionError e) {
            personListPanelPlaceholder.getChildren().setAll(previousList.getRoot());
            personDetailsPanelPlaceholder.getChildren().setAll(previousDetails.getRoot());
            throw e;
        }
        personListPanel = replacement;
        personDetailsPanel = details;
        followSelection(replacement);
    }

    private void prepare(Region root, Region placeholder) {
        Scene preparationScene = new Scene(root);
        preparationScene.getStylesheets().setAll(primaryStage.getScene().getStylesheets());
        root.resize(placeholder.getWidth(), placeholder.getHeight());
        root.applyCss();
        root.layout();
        preparationScene.setRoot(new StackPane());
    }

    /** Follows selection changes on {@code panel} while it is the displayed list. */
    private void followSelection(PersonListPanel panel) {
        panel.selectedPersonProperty().addListener((observable, previous, selected) -> {
            if (panel == personListPanel) {
                handleSelectionChange(panel, selected);
            }
        });
    }

    /**
     * Shows the complete profile of the selected student. A different student selected by the user, rather than by
     * the application, cancels any pending deletion.
     */
    private void handleSelectionChange(PersonListPanel panel, Person selected) {
        boolean isDeletionCancelled = panel.isUserSelectionChange() && selected != null
                && logic.cancelPendingDeletionUnlessTarget(selected);
        if (isDeletionCancelled) {
            statusBarFooter.setDeletionPending(logic.hasPendingDeletion());
            resultDisplay.setFeedbackToUser(Messages.MESSAGE_PENDING_DELETION_CANCELLED);
        }
        if (selected == personDetailsPanel.getPerson()) {
            return;
        }
        try {
            PersonDetailsPanel details = createPersonDetailsPanel(selected);
            prepare(details.getRoot(), personDetailsPanelPlaceholder);
            personDetailsPanelPlaceholder.getChildren().setAll(details.getRoot());
            personDetailsPanel = details;
        } catch (RuntimeException | AssertionError e) {
            logger.warning("Profile could not be displayed: " + e);
            resultDisplay.setFeedbackToUser(isDeletionCancelled
                    ? Messages.MESSAGE_PENDING_DELETION_CANCELLED + "\n" + Messages.MESSAGE_PROFILE_DISPLAY_FAILURE
                    : Messages.MESSAGE_PROFILE_DISPLAY_FAILURE);
            // Keep the row selection consistent with the complete profile that is still shown. This restoration is
            // made by the application, so it never cancels or restores a pending deletion.
            Platform.runLater(() -> panel.restoreSelection(personDetailsPanel.getPerson()));
        }
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        Person previousSelection = personListPanel.getSelectedPerson();
        try {
            CommandResult commandResult = commandExecutor.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            if (!(e instanceof DuplicateStudentException)) {
                personListPanel.restoreSelection(previousSelection);
            }
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        } finally {
            // Every submission, including one rejected before it runs, may start, consume or cancel a deletion.
            statusBarFooter.setDeletionPending(logic.hasPendingDeletion());
        }
    }
}
