package seedu.address.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.person.Person;

/**
 * A UI component that displays information of a {@code Person}.
 */
public class PersonCard extends UiPart<Region> {

    static final String TELEGRAM_LABEL = "Telegram";
    static final String GITHUB_LABEL = "GitHub";
    static final String NOT_PROVIDED = "Not provided";
    static final String NOT_ASSIGNED = "Not assigned";
    static final String SAMPLE_LABEL = "Fictional sample";
    static final String EMAIL_PREFIX = "NUS email: ";
    static final String ENROLMENTS_PREFIX = "Enrolments: ";

    private static final String FXML = "PersonListCard.fxml";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Person person;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label email;
    @FXML
    private Label telegram;
    @FXML
    private Label github;
    @FXML
    private Label sample;
    @FXML
    private Label remark;
    @FXML
    private Label enrolments;
    @FXML
    private FlowPane tags;

    /**
     * Creates a {@code PersonCard} with the given {@code Person} and index to display.
     */
    public PersonCard(Person person, int displayedIndex) {
        super(FXML);
        this.person = person;
        id.setText(displayedIndex + ". ");
        name.setText(person.getName().fullName);
        email.setText(EMAIL_PREFIX + person.getEmail().value);
        enrolments.setText(enrolmentText(person.getEnrolments()));
        telegram.setText(contactText(TELEGRAM_LABEL, person.getTelegram()));
        github.setText(contactText(GITHUB_LABEL, person.getGitHub()));
        sample.setText(SAMPLE_LABEL);
        sample.setVisible(person.isSample());
        sample.setManaged(person.isSample());
        remark.setText(person.getRemark().value);
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
    }

    /**
     * Returns the card line for an optional contact. The not-provided text is display only and is never stored.
     */
    static String contactText(String label, Optional<?> contact) {
        return label + ": " + contact.map(Object::toString).orElse(NOT_PROVIDED);
    }

    /** Returns one enrolment line. The not-assigned text is display only and is never stored. */
    static String enrolmentLine(Enrolment enrolment) {
        return enrolment.getModuleCode() + " | " + enrolment.getSemester()
                + " | Section: " + enrolment.getSection().map(Object::toString).orElse(NOT_ASSIGNED)
                + " | Team: " + enrolment.getTeam().map(Object::toString).orElse(NOT_ASSIGNED);
    }

    /**
     * Returns the enrolment count followed by every enrolment in display order, or {@code Enrolments: none}.
     * The stored order is not changed.
     */
    static String enrolmentText(List<Enrolment> enrolments) {
        return String.join("\n", enrolmentLines(enrolments));
    }

    /** Returns the lines of {@link #enrolmentText(List)}, one per displayed line. */
    static List<String> enrolmentLines(List<Enrolment> enrolments) {
        if (enrolments.isEmpty()) {
            return List.of(ENROLMENTS_PREFIX + "none");
        }
        return Stream.concat(Stream.of(ENROLMENTS_PREFIX + enrolments.size()),
                enrolments.stream().sorted(Enrolment.DISPLAY_ORDER).map(PersonCard::enrolmentLine)).toList();
    }
}
