package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class NameOrEmailContainsQueryPredicateTest {
    private final Person alex = new PersonBuilder().withName("Alex Tan").withEmail("alex.tan@u.nus.edu").build();

    @Test
    public void test_partialNamesAndEmails_matchIgnoringCase() {
        for (String query : new String[] {"LEX T", "aLeX", ".TAN@U.NUS", "@"}) {
            assertTrue(new NameOrEmailContainsQueryPredicate(query).test(alex));
        }
    }

    @Test
    public void test_literalQueries_doNotExpandOrRemoveAccents() {
        for (String query : new String[] {"*", ".*", "Alex  Tan", "Álex", "/name Alex", "Not provided", "SEED"}) {
            assertFalse(new NameOrEmailContainsQueryPredicate(query).test(alex));
        }
    }

    @Test
    public void test_defaultLocale_doesNotAffectMatching() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person fictional = new PersonBuilder().withName("IRIS").withEmail("IRIS@u.nus.edu").build();
            assertTrue(new NameOrEmailContainsQueryPredicate("iris").test(fictional));
            assertTrue(new NameOrEmailContainsQueryPredicate("IRIS@").test(fictional));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void test_sameNameProfiles_bothMatchIndependently() {
        Person otherAlex = new PersonBuilder(alex).withEmail("e9000002@u.nus.edu").build();
        NameOrEmailContainsQueryPredicate predicate = new NameOrEmailContainsQueryPredicate("Alex Tan");
        assertTrue(predicate.test(alex));
        assertTrue(predicate.test(otherAlex));
    }
}
