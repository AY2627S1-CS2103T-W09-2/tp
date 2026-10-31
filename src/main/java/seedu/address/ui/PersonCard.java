package seedu.address.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    public final Person person;

    /**
     * Creates a complete card before it can replace a displayed result. Constructing the repeated layout directly
     * avoids parsing and reflecting over the same FXML document for every student in a large search.
     */
    public PersonCard(Person person, int displayedIndex) {
        super(createRoot(person, displayedIndex));
        this.person = person;
    }

    private static Region createRoot(Person person, int displayedIndex) {
        Label id = label("id", displayedIndex + ". ", "cell_big_label", false);
        id.setMinWidth(Region.USE_PREF_SIZE);
        Label name = label("name", person.getName().fullName, "cell_big_label", true);
        HBox heading = new HBox(0.5, id, name);
        heading.setAlignment(Pos.CENTER_LEFT);

        FlowPane tags = new FlowPane();
        tags.setId("tags");
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));
        Label sample = label("sample", SAMPLE_LABEL, "cell_small_label", true);
        sample.setVisible(person.isSample());
        sample.setManaged(person.isSample());
        VBox content = new VBox(heading, tags,
                label("email", EMAIL_PREFIX + person.getEmail().value, "cell_small_label", true),
                label("telegram", contactText(TELEGRAM_LABEL, person.getTelegram()), "cell_small_label", true),
                label("github", contactText(GITHUB_LABEL, person.getGitHub()), "cell_small_label", true),
                sample,
                label("enrolments", enrolmentText(person.getEnrolments()), "cell_small_label", true),
                label("remark", person.getRemark().value, "cell_small_label", true));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMinHeight(105);
        content.setPadding(new Insets(5, 5, 5, 15));
        GridPane.setColumnIndex(content, 0);

        ColumnConstraints column = new ColumnConstraints();
        column.setHgrow(Priority.SOMETIMES);
        column.setMinWidth(10);
        column.setPrefWidth(150);
        GridPane grid = new GridPane();
        grid.getColumnConstraints().add(column);
        grid.getChildren().add(content);
        HBox.setHgrow(grid, Priority.ALWAYS);
        HBox root = new HBox(grid);
        root.setId("cardPane");
        return root;
    }

    private static Label label(String id, String text, String styleClass, boolean wrapText) {
        Label label = new Label(text);
        label.setId(id);
        label.getStyleClass().add(styleClass);
        label.setWrapText(wrapText);
        return label;
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
