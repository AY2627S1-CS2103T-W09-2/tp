package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.model.util.SampleDataUtil;
import seedu.address.testutil.PersonBuilder;

public class IdentifierContainsQueryPredicateTest {
    private final Person alex = new PersonBuilder().withName("Alex Tan").withEmail("alex.tan@u.nus.edu").build();

    @Test
    public void test_partialNamesAndEmails_matchIgnoringCase() {
        for (String query : new String[] {"LEX T", "aLeX", ".TAN@U.NUS", "@"}) {
            assertTrue(new IdentifierContainsQueryPredicate(query).test(alex));
        }
    }

    @Test
    public void test_literalQueries_doNotExpandOrRemoveAccents() {
        for (String query : new String[] {"*", ".*", "Alex  Tan", "Álex", "/name Alex", "Not provided", "SEED"}) {
            assertFalse(new IdentifierContainsQueryPredicate(query).test(alex));
        }
    }

    @Test
    public void test_defaultLocale_doesNotAffectMatching() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Person fictional = new PersonBuilder().withName("IRIS").withEmail("IRIS@u.nus.edu")
                    .withTelegram("INDIGO").withGitHub("INFINITY").build();
            assertTrue(new IdentifierContainsQueryPredicate("iris").test(fictional));
            assertTrue(new IdentifierContainsQueryPredicate("IRIS@").test(fictional));
            assertTrue(new IdentifierContainsQueryPredicate("@indigo").test(fictional));
            assertTrue(new IdentifierContainsQueryPredicate("infinity").test(fictional));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    public void test_sameNameProfiles_bothMatchIndependently() {
        Person otherAlex = new PersonBuilder(alex).withEmail("e9000002@u.nus.edu").build();
        IdentifierContainsQueryPredicate predicate = new IdentifierContainsQueryPredicate("Alex Tan");
        assertTrue(predicate.test(alex));
        assertTrue(predicate.test(otherAlex));
    }

    @Test
    public void test_contacts_matchWholeLiteralSubstringIgnoringCase() {
        Person contacts = new PersonBuilder(alex).withTelegram("Chat_One").withGitHub("Repo-One").build();
        for (String query : new String[] {"CHAT_ONE", "hat_", "@CHAT_ONE", "@hat_", "REPO-ONE", "po-o"}) {
            assertTrue(new IdentifierContainsQueryPredicate(query).test(contacts), query);
        }
        for (String query : new String[] {"Chat One", "Chat  One", "repo.*", "chat*", "@@Chat_One"}) {
            assertFalse(new IdentifierContainsQueryPredicate(query).test(contacts), query);
        }
    }

    @Test
    public void test_leadingAt_isRemovedOnlyForTelegram() {
        Person contacts = new PersonBuilder(alex).withTelegram("Chat_One").withGitHub("Repo-One").build();
        assertFalse(new IdentifierContainsQueryPredicate("@Repo-One").test(contacts));
        assertFalse(new IdentifierContainsQueryPredicate("@Alex Tan").test(contacts));
        assertFalse(new IdentifierContainsQueryPredicate("@alex.tan").test(contacts));
        assertTrue(new IdentifierContainsQueryPredicate("@u.nus.edu").test(contacts));
        assertTrue(new IdentifierContainsQueryPredicate("@").test(contacts));
        assertTrue(new IdentifierContainsQueryPredicate("@").test(alex));
    }

    @Test
    public void test_absentContacts_neverMatchDisplayPlaceholders() {
        assertFalse(new IdentifierContainsQueryPredicate("Not provided").test(alex));
        Person namedPlaceholder = new PersonBuilder(alex).withName("Not provided").build();
        assertTrue(new IdentifierContainsQueryPredicate("Not provided").test(namedPlaceholder));
    }

    @Test
    public void test_enrolmentAndSampleLabels_areNotIdentifiers() {
        Person sample = SampleDataUtil.getSamplePersons()[0];
        for (String query : new String[] {"SEED", "CS2103T", "AY26/27 S1", "T12", "Fictional sample"}) {
            assertFalse(new IdentifierContainsQueryPredicate(query).test(sample), query);
        }
    }

}
