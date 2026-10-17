package seedu.address.logic.commands;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;

import java.util.ArrayList;
import java.util.Optional;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.model.person.PersonOrder;

/** Replaces or explicitly clears affiliations without changing an enrolment's owner or key. */
public class EditEnrolCommand extends Command {
    public static final String COMMAND_WORD = "edit-enrol";
    public static final String MESSAGE_USAGE = "edit-enrol /email EMAIL /module MODULE /semester SEMESTER "
            + "[/section SECTION|clear] [/team TEAM|clear]";
    public static final String MESSAGE_NOT_EDITED = "Provide at least one field to update.";
    public static final String MESSAGE_NO_CHANGE = "No values changed.";
    public static final String MESSAGE_SUCCESS = "Updated enrolment for %s: %s, %s.";
    public static final String MESSAGE_MISSING_STUDENT = "No student found with NUS email: %s.";
    public static final String MESSAGE_MISSING_ENROLMENT = "No matching enrolment found for %s, %s, %s. "
            + "Use enrol to add it first.";
    public static final String MESSAGE_FAILURE = "The record could not be updated. No data was changed.";

    private final Email email;
    private final ModuleCode module;
    private final Semester semester;
    // Outer absence preserves the value; inner absence explicitly clears it.
    private final Optional<Optional<Section>> section;
    private final Optional<Optional<Team>> team;

    /** Creates an update to an existing context with validated selectors and proposed affiliations. */
    public EditEnrolCommand(Email email, ModuleCode module, Semester semester,
            Optional<Optional<Section>> section, Optional<Optional<Team>> team) {
        requireAllNonNull(email, module, semester, section, team);
        if (section.isEmpty() && team.isEmpty()) {
            throw new IllegalArgumentException(MESSAGE_NOT_EDITED);
        }
        this.email = email;
        this.module = module;
        this.semester = semester;
        this.section = section;
        this.team = team;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireAllNonNull(model);
        Person owner = model.getAddressBook().getPersonList().stream()
                .filter(person -> person.getEmail().equals(email)).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_MISSING_STUDENT, email)));
        Enrolment key = new Enrolment(module, semester);
        Enrolment existing = owner.getEnrolments().stream().filter(key::hasSameKey).findFirst()
                .orElseThrow(() -> new CommandException(String.format(MESSAGE_MISSING_ENROLMENT,
                        email, module, semester)));
        Enrolment replacement = new Enrolment(module, semester,
                section.orElse(existing.getSection()), team.orElse(existing.getTeam()));
        boolean unchanged = existing.equals(replacement);
        Person updated = owner;
        if (!unchanged) {
            var contexts = new ArrayList<>(owner.getEnrolments());
            contexts.set(contexts.indexOf(existing), replacement);
            updated = owner.withEnrolments(contexts);
            model.setPerson(owner, updated);
        }
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS, PersonOrder.BY_NAME_THEN_EMAIL);
        return CommandResult.forTarget(unchanged ? MESSAGE_NO_CHANGE
                : String.format(MESSAGE_SUCCESS, owner.getName(), module, semester), updated);
    }

    @Override
    public String getSaveFailureMessage(String defaultMessage) {
        return MESSAGE_FAILURE;
    }

    @Override
    public String getDisplayFailureMessage(String defaultMessage) {
        return MESSAGE_FAILURE;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof EditEnrolCommand command && email.equals(command.email)
                && module.equals(command.module) && semester.equals(command.semester)
                && section.equals(command.section) && team.equals(command.team);
    }
}
