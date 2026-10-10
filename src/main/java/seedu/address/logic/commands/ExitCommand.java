package seedu.address.logic.commands;

import seedu.address.model.Model;

/**
 * Terminates the program. A deletion awaiting confirmation is discarded, never confirmed.
 */
public class ExitCommand extends Command {

    public static final String COMMAND_WORD = "bye";

    /** The inherited command word, kept with identical behaviour and validation. */
    public static final String COMMAND_WORD_ALIAS = "exit";

    public static final String MESSAGE_USAGE = "Bye does not accept parameters. Usage: bye";

    public static final String MESSAGE_EXIT_ACKNOWLEDGEMENT = "Exiting Address Book as requested ...";

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public CommandResult execute(Model model) {
        return new CommandResult(MESSAGE_EXIT_ACKNOWLEDGEMENT, false, true);
    }

}
