package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class CommandResultTest {
    @Test
    public void equals() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns true
        assertTrue(commandResult.equals(new CommandResult("feedback")));
        assertTrue(commandResult.equals(new CommandResult("feedback", false, false)));

        // same object -> returns true
        assertTrue(commandResult.equals(commandResult));

        // null -> returns false
        assertFalse(commandResult.equals(null));

        // different types -> returns false
        assertFalse(commandResult.equals(0.5f));

        // different feedbackToUser value -> returns false
        assertFalse(commandResult.equals(new CommandResult("different")));

        // different showHelp value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", true, false)));

        // different exit value -> returns false
        assertFalse(commandResult.equals(new CommandResult("feedback", false, true)));
    }

    @Test
    public void searchResult_requestsSelectionOnlyForSearch() {
        CommandResult search = CommandResult.forSearch("feedback");
        assertTrue(search.isUpdateSelection());
        assertFalse(search.isPreserveSurvivingSelection());
        assertFalse(new CommandResult("feedback").isUpdateSelection());
        assertNotEquals(search, new CommandResult("feedback"));
        assertEquals(search, CommandResult.forSearch("feedback"));
        assertEquals(search.hashCode(), CommandResult.forSearch("feedback").hashCode());
    }

    @Test
    public void clearedSelectionResult_requestsRefreshWithoutAnySelection() {
        CommandResult cleared = CommandResult.forClearedSelection("feedback");
        assertTrue(cleared.isUpdateSelection());
        assertTrue(cleared.isClearSelection());
        assertNull(cleared.getSelectionTarget());
        assertFalse(CommandResult.forSearch("feedback").isClearSelection());
        assertNotEquals(CommandResult.forSearch("feedback"), cleared);
        assertNotEquals(CommandResult.forSearch("feedback").hashCode(), cleared.hashCode());
        assertEquals(cleared, CommandResult.forClearedSelection("feedback"));
    }

    @Test
    public void withNotice_prefixesFeedbackAndKeepsEveryOtherField() {
        Person target = new PersonBuilder().build();
        List<Function<String, CommandResult>> factories = List.of(CommandResult::new,
                feedback -> new CommandResult(feedback, true, false),
                feedback -> new CommandResult(feedback, false, true),
                CommandResult::forSearch,
                feedback -> CommandResult.forTarget(feedback, target),
                CommandResult::forSurvivingSelection,
                CommandResult::forClearedSelection);
        for (Function<String, CommandResult> factory : factories) {
            CommandResult original = factory.apply("feedback");
            assertEquals(factory.apply("Notice.\nfeedback"), original.withNotice("Notice."));
            assertEquals(factory.apply("feedback"), original);
        }
    }

    @Test
    public void survivingSelectionResult_requestsRefreshAndPreservation() {
        CommandResult result = CommandResult.forSurvivingSelection("feedback");

        assertTrue(result.isUpdateSelection());
        assertTrue(result.isPreserveSurvivingSelection());
        assertNotEquals(result, CommandResult.forSearch("feedback"));
        assertEquals(result, CommandResult.forSurvivingSelection("feedback"));
    }

    @Test
    public void selectionModes_survivingAndClearedRemainDistinctAfterNotice() {
        CommandResult surviving = CommandResult.forSurvivingSelection("feedback").withNotice("Notice.");
        CommandResult cleared = CommandResult.forClearedSelection("feedback").withNotice("Notice.");

        assertTrue(surviving.isPreserveSurvivingSelection());
        assertFalse(surviving.isClearSelection());
        assertTrue(cleared.isClearSelection());
        assertFalse(cleared.isPreserveSurvivingSelection());
        for (CommandResult result : List.of(surviving, cleared)) {
            assertTrue(result.isUpdateSelection());
            assertNull(result.getSelectionTarget());
            assertFalse(result.isShowHelp());
            assertFalse(result.isExit());
        }
        assertNotEquals(surviving, cleared);
        assertNotEquals(surviving.hashCode(), cleared.hashCode());
        assertFalse(CommandResult.forSearch("feedback").isPreserveSurvivingSelection());
        assertFalse(CommandResult.forSearch("feedback").isClearSelection());
    }

    @Test
    public void hashcode() {
        CommandResult commandResult = new CommandResult("feedback");

        // same values -> returns same hashcode
        assertEquals(commandResult.hashCode(), new CommandResult("feedback").hashCode());

        // different feedbackToUser value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("different").hashCode());

        // different showHelp value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", true, false).hashCode());

        // different exit value -> returns different hashcode
        assertNotEquals(commandResult.hashCode(), new CommandResult("feedback", false, true).hashCode());
    }

    @Test
    public void toStringMethod() {
        CommandResult commandResult = new CommandResult("feedback");
        String expected = CommandResult.class.getCanonicalName() + "{feedbackToUser="
                + commandResult.getFeedbackToUser() + ", showHelp=" + commandResult.isShowHelp()
                + ", exit=" + commandResult.isExit() + ", updateSelection=" + commandResult.isUpdateSelection()
                + ", preserveSurvivingSelection=" + commandResult.isPreserveSurvivingSelection()
                + ", clearSelection=" + commandResult.isClearSelection() + "}";
        assertEquals(expected, commandResult.toString());
    }
}
