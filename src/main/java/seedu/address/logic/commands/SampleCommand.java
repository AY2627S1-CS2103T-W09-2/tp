package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.List;
import java.util.function.Supplier;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;
import seedu.address.model.util.SampleDataUtil;

/** Loads the fixed fictional fixture into an empty writable roster. */
public class SampleCommand extends Command {
    public static final String COMMAND_WORD = "sample";
    public static final String MESSAGE_USAGE = "Sample load does not accept parameters. Usage: sample load";
    public static final String MESSAGE_SUCCESS = "Loaded 5 fictional student profiles and 5 enrolments. "
            + "You can explore SoCdex without using real student data.";
    public static final String MESSAGE_NON_EMPTY = "Sample data can only be loaded into an empty roster. "
            + "Existing data was not changed.";
    public static final String MESSAGE_INVALID_FIXTURE = "Sample data is invalid. No data was changed.";
    public static final String MESSAGE_SAVE_FAILURE = "Sample data could not be saved. No data was changed.";

    private static final int EXPECTED_PROFILE_COUNT = 5;
    private static final int EXPECTED_ENROLMENT_COUNT = 5;

    private final Supplier<ReadOnlyAddressBook> sampleProvider;

    /** Creates a command backed by the single production fixture provider. */
    public SampleCommand() {
        this(SampleDataUtil::getSampleAddressBook);
    }

    /** Creates a command with an injectable provider for atomic invalid-fixture tests. */
    SampleCommand(Supplier<ReadOnlyAddressBook> sampleProvider) {
        this.sampleProvider = requireNonNull(sampleProvider);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        if (!model.getAddressBook().getPersonList().isEmpty()) {
            throw new CommandException(MESSAGE_NON_EMPTY);
        }

        AddressBook fixture;
        try {
            fixture = new AddressBook(requireNonNull(sampleProvider.get()));
            validateFixture(fixture.getPersonList());
        } catch (RuntimeException e) {
            throw new CommandException(MESSAGE_INVALID_FIXTURE, e);
        }
        model.setAddressBook(fixture);
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forSearch(MESSAGE_SUCCESS);
    }

    private static void validateFixture(List<Person> profiles) {
        int enrolmentCount = profiles.stream().mapToInt(person -> person.getEnrolments().size()).sum();
        boolean allClassified = profiles.stream().allMatch(Person::isSample);
        if (profiles.size() != EXPECTED_PROFILE_COUNT
                || enrolmentCount != EXPECTED_ENROLMENT_COUNT
                || !allClassified) {
            throw new IllegalArgumentException("The fictional fixture is incomplete.");
        }
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return MESSAGE_SAVE_FAILURE;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof SampleCommand;
    }
}
