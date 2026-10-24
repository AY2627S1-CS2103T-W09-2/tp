package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.AddressBookParser;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

class PrivacySafeLoggingTest {
    private static final String NAME = "Private Test Student";
    private static final String EMAIL = "e8123456@u.nus.edu";
    private static final String TELEGRAM = "private_chat";
    private static final String GITHUB = "private-repo";
    private static final String SECTION = "Private Section";
    private static final String TEAM = "Private Team";
    private static final String REMARK = "Private tutor note";
    private static final String UNKNOWN = "private-unknown-command";

    @TempDir
    private Path temporaryFolder;

    @Test
    void commandLifecycleLogs_doNotContainRosterDataOrRawInput() throws Exception {
        Logic logic = createLogic();
        Logger logicLogger = LogsCenter.getLogger(LogicManager.class);
        Logger parserLogger = LogsCenter.getLogger(AddressBookParser.class);
        RecordingHandler records = new RecordingHandler();
        try (LoggerCapture ignoredLogic = new LoggerCapture(logicLogger, records);
                LoggerCapture ignoredParser = new LoggerCapture(parserLogger, records)) {
            logic.execute("student add /name " + NAME + " /email " + EMAIL
                    + " /telegram " + TELEGRAM + " /github " + GITHUB);
            logic.execute("enrol /email " + EMAIL + " /module CS2103T /semester AY26/27 S1"
                    + " /section " + SECTION + " /team " + TEAM);
            logic.execute("edit /email " + EMAIL + " /telegram private_edit");
            logic.execute("delete " + EMAIL);
            logic.execute("remark 1 r/" + REMARK);
            assertRejected(() -> logic.execute(UNKNOWN + " " + EMAIL));
        }

        String log = records.messages();
        for (String command : List.of("AddCommand", "EnrolCommand", "EditCommand", "DeleteCommand",
                "RemarkCommand")) {
            assertTrue(log.contains("Executing command: " + command));
            assertTrue(log.contains("Command completed: " + command));
        }
        assertTrue(log.contains("Command rejected during parsing."));
        for (String privateValue : List.of(NAME, EMAIL, TELEGRAM, GITHUB, SECTION, TEAM, REMARK,
                "private_edit", UNKNOWN)) {
            assertFalse(log.contains(privateValue), () -> "log exposed: " + privateValue);
        }
    }

    private Logic createLogic() {
        return new LogicManager(new ModelManager(), new StorageManager(
                new JsonAddressBookStorage(temporaryFolder.resolve("addressbook.json")),
                new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))));
    }

    private static void assertRejected(ThrowingCommand command) {
        try {
            command.run();
        } catch (CommandException | ParseException expected) {
            return;
        }
        throw new AssertionError("Command should have been rejected.");
    }

    @FunctionalInterface
    private interface ThrowingCommand {
        void run() throws CommandException, ParseException;
    }

    private static final class RecordingHandler extends Handler {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            messages.add(record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        String messages() {
            return String.join("\n", messages);
        }
    }

    private static final class LoggerCapture implements AutoCloseable {
        private final Logger logger;
        private final Level previousLevel;
        private final boolean previousUseParentHandlers;
        private final Handler handler;

        LoggerCapture(Logger logger, Handler handler) {
            this.logger = logger;
            this.handler = handler;
            previousLevel = logger.getLevel();
            previousUseParentHandlers = logger.getUseParentHandlers();
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
            logger.setUseParentHandlers(previousUseParentHandlers);
        }
    }
}
