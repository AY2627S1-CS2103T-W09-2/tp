package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.Person;

/**
 * Displays one complete selected profile, or a selection prompt when no student is selected.
 * Each panel is an immutable snapshot, so a failed replacement never mixes fields from two profiles.
 */
public class PersonDetailsPanel extends UiPart<Region> {
    static final String SELECTION_PROMPT = "Select a student to view their profile.";

    private static final String FXML = "PersonDetailsPanel.fxml";

    private final Person person;

    @FXML
    private VBox details;
    @FXML
    private Label name;

    /** Creates the complete display for {@code person}, or the selection prompt if it is null. */
    public PersonDetailsPanel(Person person) {
        this(person, SELECTION_PROMPT);
    }

    /** Creates the complete display, using {@code emptyPrompt} when no profile is selected. */
    PersonDetailsPanel(Person person, String emptyPrompt) {
        super(FXML);
        this.person = person;
        if (person == null) {
            name.setText(emptyPrompt);
            return;
        }
        name.setText(person.getName().fullName);
        for (String line : profileLines(person)) {
            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("details_label");
            details.getChildren().add(label);
        }
    }

    /** Returns the displayed profile, or null when the prompt is shown. */
    public Person getPerson() {
        return person;
    }

    /**
     * Returns every line below the name: classification, contacts, then all enrolments in display order.
     * Absent values use display labels that are never stored.
     */
    static List<String> profileLines(Person person) {
        List<String> lines = new ArrayList<>();
        if (person.isSample()) {
            lines.add(PersonCard.SAMPLE_LABEL);
        }
        lines.add(PersonCard.EMAIL_PREFIX + person.getEmail().value);
        lines.add(PersonCard.contactText(PersonCard.TELEGRAM_LABEL, person.getTelegram()));
        lines.add(PersonCard.contactText(PersonCard.GITHUB_LABEL, person.getGitHub()));
        lines.addAll(PersonCard.enrolmentLines(person.getEnrolments()));
        return lines;
    }
}
