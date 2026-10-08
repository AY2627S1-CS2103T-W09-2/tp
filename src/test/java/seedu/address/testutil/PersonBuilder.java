package seedu.address.testutil;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.person.Address;
import seedu.address.model.person.Email;
import seedu.address.model.person.GitHub;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.Remark;
import seedu.address.model.person.Telegram;
import seedu.address.model.tag.Tag;
import seedu.address.model.util.SampleDataUtil;

/**
 * A utility class to help with building Person objects.
 */
public class PersonBuilder {

    public static final String DEFAULT_NAME = "Amy Bee";
    public static final String DEFAULT_PHONE = "85355255";
    public static final String DEFAULT_EMAIL = "amy.bee@u.nus.edu";
    public static final String DEFAULT_ADDRESS = "123, Jurong West Ave 6, #08-111";
    public static final String DEFAULT_REMARK = "";

    private Name name;
    private Phone phone;
    private Email email;
    private Address address;
    private Optional<Telegram> telegram = Optional.empty();
    private Optional<GitHub> github = Optional.empty();
    private boolean isSample = false;
    private Remark remark;
    private Set<Tag> tags;
    private List<Enrolment> enrolments = List.of();

    /**
     * Creates a {@code PersonBuilder} with the default details.
     */
    public PersonBuilder() {
        name = new Name(DEFAULT_NAME);
        phone = new Phone(DEFAULT_PHONE);
        email = new Email(DEFAULT_EMAIL);
        address = new Address(DEFAULT_ADDRESS);
        remark = new Remark(DEFAULT_REMARK);
        tags = new HashSet<>();
    }

    /**
     * Initializes the PersonBuilder with the data of {@code personToCopy}.
     */
    public PersonBuilder(Person personToCopy) {
        enrolments = personToCopy.getEnrolments();
        name = personToCopy.getName();
        phone = personToCopy.getPhone();
        email = personToCopy.getEmail();
        address = personToCopy.getAddress();
        telegram = personToCopy.getTelegram();
        github = personToCopy.getGitHub();
        isSample = personToCopy.isSample();
        remark = personToCopy.getRemark();
        tags = new HashSet<>(personToCopy.getTags());
    }

    /**
     * Sets the {@code Name} of the {@code Person} that we are building.
     */
    public PersonBuilder withName(String name) {
        this.name = new Name(name);
        return this;
    }

    /**
     * Parses the {@code tags} into a {@code Set<Tag>} and sets it to the {@code Person} that we are building.
     */
    public PersonBuilder withTags(String ... tags) {
        this.tags = SampleDataUtil.getTagSet(tags);
        return this;
    }

    /**
     * Sets the {@code Address} of the {@code Person} that we are building.
     */
    public PersonBuilder withAddress(String address) {
        this.address = new Address(address);
        return this;
    }

    /**
     * Sets the {@code Phone} of the {@code Person} that we are building.
     */
    public PersonBuilder withPhone(String phone) {
        this.phone = new Phone(phone);
        return this;
    }

    /**
     * Sets the {@code Email} of the {@code Person} that we are building.
     */
    public PersonBuilder withEmail(String email) {
        this.email = new Email(email);
        return this;
    }

    /** Sets the {@code Remark} of the {@code Person} that we are building. */
    public PersonBuilder withRemark(String remark) {
        this.remark = new Remark(remark);
        return this;
    }

    /** Sets the enrolments of the profile being built. */
    public PersonBuilder withEnrolments(Enrolment... enrolments) {
        this.enrolments = List.of(enrolments);
        return this;
    }

    /** Sets the Telegram handle of the profile being built. */
    public PersonBuilder withTelegram(String telegram) {
        this.telegram = Optional.of(new Telegram(telegram));
        return this;
    }

    /** Sets the GitHub username of the profile being built. */
    public PersonBuilder withGitHub(String github) {
        this.github = Optional.of(new GitHub(github));
        return this;
    }

    /** Removes both contact handles of the profile being built. */
    public PersonBuilder withoutContacts() {
        this.telegram = Optional.empty();
        this.github = Optional.empty();
        return this;
    }

    /** Sets the sample classification of the profile being built. */
    public PersonBuilder withSample(boolean isSample) {
        this.isSample = isSample;
        return this;
    }

    public Person build() {
        return new Person(name, phone, email, address, telegram, github, isSample, remark, tags, enrolments);
    }

}
