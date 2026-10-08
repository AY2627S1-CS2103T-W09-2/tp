package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import seedu.address.logic.commands.exceptions.DuplicateStudentException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/** Creates a student profile without manufacturing unrelated fields. */
public class AddCommand extends Command {
    public static final String COMMAND_WORD = "student";
    public static final String MESSAGE_USAGE =
            "student add /name NAME /email EMAIL [/telegram HANDLE] [/github USERNAME]";
    public static final String MESSAGE_SUCCESS = "Added student: %s.";
    public static final String MESSAGE_DUPLICATE_PERSON = "A student with this NUS email already exists.";
    private final Person toAdd;

    public AddCommand(Person person) {
        toAdd = requireNonNull(person);
    }

    @Override
    public CommandResult execute(Model model) throws DuplicateStudentException {
        requireNonNull(model);
        for (Person existing : model.getAddressBook().getPersonList()) {
            if (existing.isSamePerson(toAdd)) {
                throw new DuplicateStudentException(MESSAGE_DUPLICATE_PERSON, existing);
            }
        }
        model.addPerson(toAdd);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forTarget(String.format(MESSAGE_SUCCESS, toAdd.getName()), toAdd);
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return "The student could not be saved. No data was changed.";
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof AddCommand command && toAdd.equals(command.toAdd);
    }
}
