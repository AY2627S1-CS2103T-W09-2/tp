package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void isSamePerson() {
        // same object -> returns true
        assertTrue(ALICE.isSamePerson(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSamePerson(null));

        // same email, all other attributes different -> returns true
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB)
                .withTags(VALID_TAG_HUSBAND).withRemark("Different remark").build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // same name, different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.isSamePerson(editedAlice));

        // email entered with different case and surrounding whitespace -> returns true
        Person editedBob = new PersonBuilder(BOB).withEmail(" " + VALID_EMAIL_BOB.toUpperCase(Locale.ROOT) + "\t")
                .build();
        assertTrue(BOB.isSamePerson(editedBob));

        // email differs only by a plus suffix -> returns false
        editedBob = new PersonBuilder(BOB).withEmail("bob+cs2103@u.nus.edu").build();
        assertFalse(BOB.isSamePerson(editedBob));
    }

    @Test
    public void isSamePerson_sameEmailDifferentDetails_notEqual() {
        Person editedAlice = new PersonBuilder(ALICE).withTelegram("other_handle").build();
        assertTrue(ALICE.isSamePerson(editedAlice));
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void hashCode_equalPersons_sameHashCode() {
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertEquals(ALICE.hashCode(), aliceCopy.hashCode());

        // an equivalent email entered in uppercase gives an equal person
        Person uppercaseEmailAlice = new PersonBuilder(ALICE)
                .withEmail(ALICE.getEmail().value.toUpperCase(Locale.ROOT)).build();
        assertEquals(ALICE, uppercaseEmailAlice);
        assertEquals(ALICE.hashCode(), uppercaseEmailAlice.hashCode());
    }

    @Test
    public void equals() {
        // same values -> returns true
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different person -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));

        // different remark -> returns false
        editedAlice = new PersonBuilder(ALICE).withRemark("Different remark").build();
        assertFalse(ALICE.equals(editedAlice));

        // name differing only in surrounding or repeated spaces -> same normalised name -> returns true
        editedAlice = new PersonBuilder(ALICE)
                .withName(" " + ALICE.getName().fullName.replace(" ", "  ") + " ").build();
        assertTrue(ALICE.equals(editedAlice));

        // a no-break space is not normalised -> different name -> returns false, but the same email identity
        editedAlice = new PersonBuilder(ALICE).withName(ALICE.getName().fullName.replace(" ", "\u00A0")).build();
        assertFalse(ALICE.equals(editedAlice));
        assertTrue(ALICE.isSamePerson(editedAlice));
    }

    @Test
    public void equals_contactsAndSampleClassification_compared() {
        // different Telegram, including a change of letter case only -> returns false
        assertFalse(BENSON.equals(new PersonBuilder(BENSON).withTelegram("Other_Handle").build()));
        assertFalse(BENSON.equals(new PersonBuilder(BENSON).withTelegram("benson_meier").build()));

        // different GitHub, including a change of letter case only -> returns false
        assertFalse(BENSON.equals(new PersonBuilder(BENSON).withGitHub("other-user").build()));
        assertFalse(BENSON.equals(new PersonBuilder(BENSON).withGitHub("bensonm").build()));

        // contacts removed -> returns false
        Person withoutContacts = new PersonBuilder(BENSON).withoutContacts().build();
        assertFalse(BENSON.equals(withoutContacts));

        // a leading @ is not part of the saved Telegram handle -> returns true
        Person prefixedTelegram = new PersonBuilder(BENSON).withTelegram("@Benson_Meier").build();
        assertTrue(BENSON.equals(prefixedTelegram));
        assertEquals(BENSON.hashCode(), prefixedTelegram.hashCode());

        // different sample classification -> returns false
        Person sampleBenson = new PersonBuilder(BENSON).withSample(true).build();
        assertFalse(BENSON.equals(sampleBenson));

        // contacts and sample classification do not change the email identity
        assertTrue(BENSON.isSamePerson(withoutContacts));
        assertTrue(BENSON.isSamePerson(sampleBenson));
    }

    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + BENSON.getName()
                + ", email=" + BENSON.getEmail()
                + ", telegram=" + BENSON.getTelegram() + ", github=" + BENSON.getGitHub() + ", sample=false"
                + ", remark=" + BENSON.getRemark() + ", tags=" + BENSON.getTags()
                + ", enrolments=" + BENSON.getEnrolments() + "}";
        assertEquals(expected, BENSON.toString());
    }
}
