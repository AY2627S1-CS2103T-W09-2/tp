package seedu.address.testutil;

import seedu.address.model.person.Person;

/** Command input for a profile using only supported creation fields. */
public class PersonUtil {
    public static String getAddCommand(Person person) {
        return "student add " + getPersonDetails(person);
    }

    public static String getPersonDetails(Person person) {
        return "/name " + person.getName() + " /email " + person.getEmail()
                + person.getTelegram().map(value -> " /telegram " + value).orElse("")
                + person.getGitHub().map(value -> " /github " + value).orElse("");
    }
}
