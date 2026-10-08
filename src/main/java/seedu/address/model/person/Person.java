package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.enrolment.Enrolment;
import seedu.address.model.tag.Tag;

/**
 * Represents a Person in the address book.
 * Guarantees: details are present and not null, field values are validated, immutable.
 */
public class Person {
    public static final String MESSAGE_DUPLICATE_ENROLMENT = "Duplicate module-semester enrolment.";

    // Details; only the canonical email identifies the person (see isSamePerson)
    private final Name name;
    private final Email email;

    // Optional contact routes and the fictional sample classification
    private final Optional<Telegram> telegram;
    private final Optional<GitHub> github;
    private final boolean isSample;

    // Data fields
    private final Remark remark;
    private final Set<Tag> tags = new HashSet<>();
    private final List<Enrolment> enrolments;

    /**
     * Creates a profile from every stored field, with a defensive immutable copy of its uniquely keyed enrolments.
     * An absent contact is {@code Optional.empty()}. Every argument must be non-null.
     */
    public Person(Name name, Email email, Optional<Telegram> telegram,
            Optional<GitHub> github, boolean isSample, Remark remark, Set<Tag> tags,
            Collection<Enrolment> enrolments) {
        requireAllNonNull(name, email, telegram, github, remark, tags, enrolments);
        List<Enrolment> copy = List.copyOf(enrolments);
        for (int i = 0; i < copy.size(); i++) {
            for (int j = 0; j < i; j++) {
                if (copy.get(i).hasSameKey(copy.get(j))) {
                    throw new IllegalArgumentException(MESSAGE_DUPLICATE_ENROLMENT);
                }
            }
        }
        this.enrolments = copy;
        this.name = name;
        this.email = email;
        this.telegram = telegram;
        this.github = github;
        this.isSample = isSample;
        this.remark = remark;
        this.tags.addAll(tags);
    }

    /** Creates a new non-sample student with no enrolments, tags or remark. */
    public Person(Name name, Email email, Optional<Telegram> telegram, Optional<GitHub> github) {
        this(name, email, telegram, github, false, new Remark(""), Set.of(), List.of());
    }

    /** Returns the immutable enrolment list in stored order. */
    public List<Enrolment> getEnrolments() {
        return enrolments;
    }

    /** Returns a new profile with the supplied enrolments and all other fields preserved. */
    public Person withEnrolments(Collection<Enrolment> updatedEnrolments) {
        return new Person(name, email, telegram, github, isSample, remark, tags, updatedEnrolments);
    }

    /** Replaces only the contacts, preserving identity, classification and teaching context. */
    public Person withContacts(Optional<Telegram> updatedTelegram, Optional<GitHub> updatedGitHub) {
        return new Person(name, email, updatedTelegram, updatedGitHub, isSample, remark, tags, enrolments);
    }

    public Name getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public Optional<Telegram> getTelegram() {
        return telegram;
    }

    public Optional<GitHub> getGitHub() {
        return github;
    }

    /** Returns true if this profile is classified as a fictional sample record. */
    public boolean isSample() {
        return isSample;
    }

    public Remark getRemark() {
        return remark;
    }

    /**
     * Returns an immutable tag set, which throws {@code UnsupportedOperationException}
     * if modification is attempted.
     */
    public Set<Tag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    /**
     * Returns true if both persons have the same canonical NUS email.
     * Persons with equal names but different emails are different persons.
     * This defines a weaker notion of equality between two persons.
     */
    public boolean isSamePerson(Person otherPerson) {
        if (otherPerson == this) {
            return true;
        }

        return otherPerson != null
                && otherPerson.getEmail().equals(getEmail());
    }

    /**
     * Returns true if both persons have the same identity and data fields.
     * This defines a stronger notion of equality between two persons.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof Person otherPerson)) {
            return false;
        }

        return name.equals(otherPerson.name)
                && email.equals(otherPerson.email)
                && telegram.equals(otherPerson.telegram)
                && github.equals(otherPerson.github)
                && isSample == otherPerson.isSample
                && remark.equals(otherPerson.remark)
                && tags.equals(otherPerson.tags)
                && enrolments.equals(otherPerson.enrolments);
    }

    @Override
    public int hashCode() {
        // use this method for custom fields hashing instead of implementing your own
        return Objects.hash(name, email, telegram, github, isSample, remark, tags, enrolments);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("name", name)
                .add("email", email)
                .add("telegram", telegram)
                .add("github", github)
                .add("sample", isSample)
                .add("remark", remark)
                .add("tags", tags)
                .add("enrolments", enrolments)
                .toString();
    }

}
