package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.GitHub;
import seedu.address.model.person.Telegram;

public class PersonCardTest {
    @Test
    public void contactText_presentContact_showsSavedValue() {
        assertEquals("Telegram: Alex_Tan",
                PersonCard.contactText(PersonCard.TELEGRAM_LABEL, Optional.of(new Telegram("@Alex_Tan"))));
        assertEquals("GitHub: clear",
                PersonCard.contactText(PersonCard.GITHUB_LABEL, Optional.of(new GitHub("clear"))));
    }

    @Test
    public void contactText_absentContact_showsNotProvided() {
        assertEquals("Telegram: Not provided", PersonCard.contactText(PersonCard.TELEGRAM_LABEL, Optional.empty()));
        assertEquals("GitHub: Not provided", PersonCard.contactText(PersonCard.GITHUB_LABEL, Optional.empty()));
    }
}
