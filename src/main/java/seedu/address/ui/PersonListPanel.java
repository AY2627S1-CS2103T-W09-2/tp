package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.model.person.Person;

/**
 * Displays a prepared snapshot of the person list, independent of pending model changes.
 */
public class PersonListPanel extends UiPart<Region> {
    private static final String FXML = "PersonListPanel.fxml";
    private final List<Region> cards = new ArrayList<>();

    @FXML
    private ListView<Person> personListView;
    @FXML
    private Label selectionPrompt;

    /**
     * Creates every result card before the panel can replace the previous complete display.
     */
    public PersonListPanel(ObservableList<Person> personList) {
        this(personList, (person, index) -> new PersonCard(person, index).getRoot());
    }

    PersonListPanel(ObservableList<Person> personList, BiFunction<Person, Integer, Region> createCard) {
        super(FXML);
        ObservableList<Person> snapshot = FXCollections.observableArrayList(personList);
        for (int i = 0; i < snapshot.size(); i++) {
            cards.add(createCard.apply(snapshot.get(i), i + 1));
        }
        personListView.setItems(FXCollections.unmodifiableObservableList(snapshot));
        personListView.setCellFactory(listView -> new PersonListViewCell());
        selectionPrompt.visibleProperty().bind(personListView.getSelectionModel().selectedItemProperty().isNull());
        selectionPrompt.managedProperty().bind(selectionPrompt.visibleProperty());
    }

    /** Returns whether this panel already represents the supplied results in the same order. */
    public boolean hasSameResults(ObservableList<Person> persons) {
        return personListView.getItems().equals(persons);
    }

    /** Returns the currently selected profile, or null if no row is selected. */
    public Person getSelectedPerson() {
        return personListView.getSelectionModel().getSelectedItem();
    }

    /** Restores a surviving selection when another command refreshes the displayed results. */
    public void restoreSelection(Person person) {
        personListView.getSelectionModel().clearSelection();
        if (person != null && personListView.getItems().contains(person)) {
            personListView.getSelectionModel().select(person);
        }
    }

    /** Selects and scrolls to a requested target in the prepared roster. */
    public void selectTarget(Person target) {
        int index = personListView.getItems().indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("The selection target must be displayed.");
        }
        personListView.getSelectionModel().clearAndSelect(index);
        personListView.scrollTo(index);
    }

    /**
     * Selects and reveals the sole search result, or clears selection for an ambiguous or empty result.
     */
    public void selectOnlyResult() {
        personListView.getSelectionModel().clearSelection();
        if (personListView.getItems().size() == 1) {
            personListView.getSelectionModel().selectFirst();
            personListView.scrollTo(0);
        }
    }

    /**
     * Reuses prepared cards so scrolling cannot defer FXML loading or profile formatting until after success.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            setText(null);
            setGraphic(empty || person == null ? null : cards.get(getIndex()));
        }
    }
}
