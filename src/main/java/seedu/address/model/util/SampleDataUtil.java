package seedu.address.model.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.model.tag.Tag;

/**
 * Contains utility methods for populating {@code AddressBook} with sample data.
 */
public class SampleDataUtil {
    public static Person[] getSamplePersons() {
        return new Person[] {
            samplePerson(new Name("Alex Yeoh"), new Email("alexyeoh@u.nus.edu"),
                getTagSet("friends")),
            samplePerson(new Name("Bernice Yu"), new Email("berniceyu@u.nus.edu"),
                getTagSet("colleagues", "friends")),
            samplePerson(new Name("Charlotte Oliveiro"), new Email("charlotte@u.nus.edu"),
                getTagSet("neighbours")),
            samplePerson(new Name("David Li"), new Email("lidavid@u.nus.edu"),
                getTagSet("family")),
            samplePerson(new Name("Irfan Ibrahim"), new Email("irfan@u.nus.edu"),
                getTagSet("classmates")),
            samplePerson(new Name("Roy Balakrishnan"), new Email("royb@u.nus.edu"),
                getTagSet("colleagues"))
        };
    }

    /**
     * Returns a fictional sample profile with no contact handles, remark or enrolments.
     */
    private static Person samplePerson(Name name, Email email, Set<Tag> tags) {
        return new Person(name, email, Optional.empty(), Optional.empty(), true, new Remark(""),
                tags, List.of());
    }

    public static ReadOnlyAddressBook getSampleAddressBook() {
        AddressBook sampleAb = new AddressBook();
        for (Person samplePerson : getSamplePersons()) {
            sampleAb.addPerson(samplePerson);
        }
        return sampleAb;
    }

    /**
     * Returns a tag set containing the list of strings given.
     */
    public static Set<Tag> getTagSet(String... strings) {
        return Arrays.stream(strings)
                .map(Tag::new)
                .collect(Collectors.toSet());
    }

}
