package seedu.address.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.TextNode;

import seedu.address.commons.exceptions.IllegalValueException;
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

/**
 * Jackson-friendly version of {@link Person}.
 */
class JsonAdaptedPerson {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Person's %s field is missing!";
    public static final String MESSAGE_NON_NORMALISED_NAME =
            "Stored name must already have normalised spaces and tabs.";
    public static final String MESSAGE_NON_CANONICAL_EMAIL = "Stored NUS email must already be lowercase, "
            + "with no surrounding spaces or tabs.";
    public static final String CONTACT_TYPE_MESSAGE_FORMAT = "Person's %s field must be a string or null.";
    public static final String MESSAGE_NON_NORMALISED_CONTACT =
            "Stored contact handles must already be in their saved form.";
    public static final String SAMPLE_FIELD = "sample classification";
    public static final String MESSAGE_INVALID_SAMPLE = "Person's sample classification must be true or false.";

    private final String name;
    private final String phone;
    private final String email;
    private final String address;
    private final String remark;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    private final List<JsonAdaptedEnrolment> enrolments = new ArrayList<>();
    // Raw JSON values, so that wrong types are rejected rather than coerced; Java null means omitted
    private JsonNode telegram;
    private JsonNode github;
    private JsonNode sample;

    /**
     * Constructs a {@code JsonAdaptedPerson} with the given person details.
     */
    @JsonCreator
    public JsonAdaptedPerson(@JsonProperty("name") String name, @JsonProperty("phone") String phone,
            @JsonProperty("email") String email, @JsonProperty("address") String address,
            @JsonProperty("remark") String remark, @JsonProperty("tags") List<JsonAdaptedTag> tags) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.remark = remark;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    /**
     * Constructs a non-sample {@code JsonAdaptedPerson} without contacts, for tests of the other fields.
     */
    JsonAdaptedPerson(String name, String phone, String email, String address, List<JsonAdaptedTag> tags) {
        this(name, phone, email, address, "", tags);
        sample = BooleanNode.FALSE;
    }

    /**
     * Converts a given {@code Person} into this class for Jackson use.
     */
    public JsonAdaptedPerson(Person source) {
        enrolments.addAll(source.getEnrolments().stream().map(JsonAdaptedEnrolment::new).collect(Collectors.toList()));
        name = source.getName().fullName;
        phone = source.getPhone().value;
        email = source.getEmail().value;
        address = source.getAddress().value;
        telegram = source.getTelegram().map(handle -> TextNode.valueOf(handle.value)).orElse(null);
        github = source.getGitHub().map(username -> TextNode.valueOf(username.value)).orElse(null);
        sample = BooleanNode.valueOf(source.isSample());
        remark = source.getRemark().value;
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
    }

    /** Reads the stored Telegram handle; an omitted or null property means no handle. */
    @JsonSetter("telegram")
    public void setTelegram(JsonNode telegram) {
        this.telegram = telegram;
    }

    /** Reads the stored GitHub username; an omitted or null property means no username. */
    @JsonSetter("github")
    public void setGitHub(JsonNode github) {
        this.github = github;
    }

    /** Reads the required sample classification, which is validated when converting to the model. */
    @JsonSetter("sample")
    public void setSample(JsonNode sample) {
        this.sample = sample;
    }

    /** Reads an explicitly supplied enrolment array; an omitted property retains the legacy empty list. */
    @JsonSetter("enrolments")
    public void setEnrolments(List<JsonAdaptedEnrolment> enrolments) {
        if (enrolments == null) {
            throw new IllegalArgumentException("The enrolments field must be an array, not null.");
        }
        this.enrolments.clear();
        this.enrolments.addAll(enrolments);
    }

    /**
     * Converts this Jackson-friendly adapted person object into the model's {@code Person} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted person.
     */
    public Person toModelType() throws IllegalValueException {
        final List<Tag> personTags = new ArrayList<>();
        for (JsonAdaptedTag tag : tags) {
            personTags.add(tag.toModelType());
        }

        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        final Name modelName = new Name(name);
        // Stored names are never corrected: the decoded stored value must already be normalised.
        if (!modelName.fullName.equals(name)) {
            throw new IllegalValueException(MESSAGE_NON_NORMALISED_NAME);
        }

        if (phone == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName()));
        }
        if (!Phone.isValidPhone(phone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        final Phone modelPhone = new Phone(phone);

        if (email == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName()));
        }
        if (!Email.isValidEmail(email)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        final Email modelEmail = new Email(email);
        // Stored emails are never corrected: the decoded stored value must already be canonical.
        if (!modelEmail.value.equals(email)) {
            throw new IllegalValueException(MESSAGE_NON_CANONICAL_EMAIL);
        }

        if (address == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Address.class.getSimpleName()));
        }
        if (!Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        final Address modelAddress = new Address(address);

        final Optional<Telegram> modelTelegram = toOptionalContact(telegram, "telegram", Telegram::isValidTelegram,
                Telegram.MESSAGE_CONSTRAINTS, Telegram::new, handle -> handle.value);
        final Optional<GitHub> modelGitHub = toOptionalContact(github, "github", GitHub::isValidGitHub,
                GitHub.MESSAGE_CONSTRAINTS, GitHub::new, username -> username.value);

        if (sample == null || sample.isNull()) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, SAMPLE_FIELD));
        }
        if (!sample.isBoolean()) {
            throw new IllegalValueException(MESSAGE_INVALID_SAMPLE);
        }
        final boolean modelIsSample = sample.booleanValue();

        final Remark modelRemark = new Remark(remark == null ? "" : remark);

        final List<Enrolment> modelEnrolments = new ArrayList<>();
        for (JsonAdaptedEnrolment enrolment : enrolments) {
            if (enrolment == null) {
                throw new IllegalValueException("Enrolment records must not be null.");
            }
            modelEnrolments.add(enrolment.toModelType());
        }
        final Set<Tag> modelTags = new HashSet<>(personTags);
        try {
            return new Person(modelName, modelPhone, modelEmail, modelAddress, modelTelegram, modelGitHub,
                    modelIsSample, modelRemark, modelTags, modelEnrolments);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException(e.getMessage());
        }
    }

    /**
     * Converts an optional stored contact. An omitted or null value is absent; any other value must be a string
     * that is valid and already equal to its saved form, which {@code savedForm} returns.
     */
    private static <T> Optional<T> toOptionalContact(JsonNode stored, String fieldName, Predicate<String> isValid,
            String constraints, Function<String, T> create, Function<T, String> savedForm)
            throws IllegalValueException {
        if (stored == null || stored.isNull()) {
            return Optional.empty();
        }
        if (!stored.isTextual()) {
            throw new IllegalValueException(String.format(CONTACT_TYPE_MESSAGE_FORMAT, fieldName));
        }
        String value = stored.textValue();
        if (!isValid.test(value)) {
            throw new IllegalValueException(constraints);
        }
        T contact = create.apply(value);
        // Stored handles are never corrected: the decoded stored value must already be in its saved form.
        if (!savedForm.apply(contact).equals(value)) {
            throw new IllegalValueException(MESSAGE_NON_NORMALISED_CONTACT);
        }
        return Optional.of(contact);
    }

}
