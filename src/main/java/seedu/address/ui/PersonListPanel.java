package seedu.address.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

    /** Set while the application, rather than the user, is changing the selection. */
    private boolean isApplyingSelection;

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
    }

    /** Returns the observable selection, so the window can display the complete selected profile. */
    public ReadOnlyObjectProperty<Person> selectedPersonProperty() {
        return personListView.getSelectionModel().selectedItemProperty();
    }

    /** Returns whether this panel already represents the supplied results in the same order. */
    public boolean hasSameResults(ObservableList<Person> persons) {
        return personListView.getItems().equals(persons);
    }

    /** Returns the currently selected profile, or null if no row is selected. */
    public Person getSelectedPerson() {
        return personListView.getSelectionModel().getSelectedItem();
    }

    /**
     * Returns whether the selection change being reported to a listener was made by the user, with the mouse or
     * keyboard, rather than by one of this panel's selection methods.
     */
    public boolean isUserSelectionChange() {
        return !isApplyingSelection;
    }

    /** Restores a surviving selection when another command refreshes the displayed results. */
    public void restoreSelection(Person person) {
        int index = person == null ? -1 : personListView.getItems().indexOf(person);
        applySelection(() -> {
            if (index < 0) {
                personListView.getSelectionModel().clearSelection();
            } else {
                personListView.getSelectionModel().clearAndSelect(index);
            }
        });
    }

    /** Selects and scrolls to a requested target in the prepared roster. */
    public void selectTarget(Person target) {
        int index = personListView.getItems().indexOf(target);
        if (index < 0) {
            throw new IllegalArgumentException("The selection target must be displayed.");
        }
        applySelection(() -> {
            personListView.getSelectionModel().clearAndSelect(index);
            personListView.scrollTo(index);
        });
    }

    /**
     * Selects and reveals the sole search result, or clears selection for an ambiguous or empty result.
     */
    public void selectOnlyResult() {
        applySelection(() -> {
            personListView.getSelectionModel().clearSelection();
            if (personListView.getItems().size() == 1) {
                personListView.getSelectionModel().selectFirst();
                personListView.scrollTo(0);
            }
        });
    }

    /** Runs an application-made selection change, so that its listeners do not treat it as the user's choice. */
    private void applySelection(Runnable selectionChange) {
        boolean wasApplyingSelection = isApplyingSelection;
        isApplyingSelection = true;
        try {
            selectionChange.run();
        } finally {
            isApplyingSelection = wasApplyingSelection;
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
