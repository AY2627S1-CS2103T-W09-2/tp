package seedu.address.model.person;

import static java.util.Objects.requireNonNull;

/**
 * Represents an optional remark attached to a person in the address book.
 * Guarantees: immutable; value is not null.
 */
public class Remark {

    public final String value;

    /**
     * Creates a remark with the given text.
     */
    public Remark(String remark) {
        requireNonNull(remark);
        value = remark;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Remark otherRemark)) {
            return false;
        }

        return value.equals(otherRemark.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
