package seedu.address.model.util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.enrolment.ModuleCode;
import seedu.address.model.enrolment.Section;
import seedu.address.model.enrolment.Semester;
import seedu.address.model.enrolment.Team;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.model.person.Telegram;
import seedu.address.model.tag.Tag;

/**
 * Contains utility methods for populating {@code AddressBook} with sample data.
 */
public class SampleDataUtil {
    private static final Remark EMPTY_REMARK = new Remark("");

    private SampleDataUtil() {
    }

    /** Returns the fixed fictional fixture in name-and-email display order. */
    public static Person[] getSamplePersons() {
        return new Person[] {
            samplePerson("Alex Tan", "e9000001@u.nus.edu", "socdex_demo_alex", "socdex-demo-alex",
                    enrolment("CS2103T", "AY26/27 S1", "T12", "SEED"),
                    enrolment("CS2113T", "AY26/27 S2", "T14", null)),
            samplePerson("Alex Tan", "e9000002@u.nus.edu", null, "socdex-demo-alex2",
                    enrolment("CS2103T", "AY26/27 S1", "T14", "SPROUT")),
            samplePerson("Mei Lim", "e9000003@u.nus.edu", "socdex_demo_mei", null,
                    enrolment("CS2103T", "AY26/27 S1", "T12", "SEED")),
            samplePerson("Nur Aisyah", "e9000005@u.nus.edu", null, null),
            samplePerson("Ravi Kumar", "e9000004@u.nus.edu", "socdex_demo_ravi", "socdex-demo-ravi",
                    enrolment("CS2103T", "AY26/27 S1", null, null))
        };
    }

    /** Returns one classified fictional profile with no tags or remark. */
    private static Person samplePerson(String name, String email, String telegram, String github,
            Enrolment... enrolments) {
        Optional<Telegram> optionalTelegram = Optional.ofNullable(telegram).map(Telegram::new);
        Optional<GitHub> optionalGitHub = Optional.ofNullable(github).map(GitHub::new);
        return new Person(new Name(name), new Email(email), optionalTelegram, optionalGitHub, true,
                EMPTY_REMARK, Set.of(), List.of(enrolments));
    }

    /** Returns one fixture enrolment, keeping absent affiliations as absent values. */
    private static Enrolment enrolment(String module, String semester, String section, String team) {
        Optional<Section> optionalSection = Optional.ofNullable(section).map(Section::new);
        Optional<Team> optionalTeam = Optional.ofNullable(team).map(Team::new);
        return new Enrolment(new ModuleCode(module), new Semester(semester), optionalSection, optionalTeam);
    }

    /** Returns a fresh address book containing the complete fixed fixture. */
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
