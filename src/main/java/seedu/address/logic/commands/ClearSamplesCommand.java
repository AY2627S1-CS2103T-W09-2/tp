package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.List;

import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/** Removes every profile that carries the persistent fictional-sample classification. */
public class ClearSamplesCommand extends Command {
    public static final String MESSAGE_USAGE = "Sample clear does not accept parameters. Usage: sample clear";
    public static final String MESSAGE_SUCCESS =
            "Removed %d fictional sample profiles and %d enrolments. Your non-sample records were not changed.";
    public static final String MESSAGE_NO_SAMPLES = "No fictional sample profiles were found.";
    public static final String MESSAGE_SAVE_FAILURE =
            "Sample profiles could not be removed. No data was changed.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        List<Person> samples = model.getAddressBook().getPersonList().stream()
                .filter(Person::isSample)
                .toList();

        if (samples.isEmpty()) {
            return new CommandResult(MESSAGE_NO_SAMPLES);
        }

        int enrolmentCount = samples.stream()
                .mapToInt(person -> person.getEnrolments().size())
                .sum();
        samples.forEach(model::deletePerson);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forSurvivingSelection(
                String.format(MESSAGE_SUCCESS, samples.size(), enrolmentCount));
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return MESSAGE_SAVE_FAILURE;
    }

    @Override
    public String getDisplayFailureMessage(String defaultMessage) {
        return MESSAGE_SAVE_FAILURE;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof ClearSamplesCommand;
    }
}
