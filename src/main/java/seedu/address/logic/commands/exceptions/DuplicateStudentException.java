package seedu.address.logic.commands.exceptions;

import seedu.address.model.person.Person;

/** A rejected creation that deliberately reveals the existing profile without changing roster data. */
public class DuplicateStudentException extends CommandException {
    private final Person existing;

    /** Records the existing profile that the rejected creation should reveal. */
    public DuplicateStudentException(String message, Person existing) {
        super(message);
        this.existing = existing;
    }

    public Person getExisting() {
        return existing;
    }
}
