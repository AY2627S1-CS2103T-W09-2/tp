package seedu.address.logic.parser;

/**
 * Contains Command Line Interface (CLI) syntax definitions common to multiple commands
 */
public class CliSyntax {
    public static final Prefix STUDENT_NAME = new Prefix("/name");
    public static final Prefix STUDENT_EMAIL = new Prefix("/email");
    public static final Prefix STUDENT_TELEGRAM = new Prefix("/telegram");
    public static final Prefix STUDENT_GITHUB = new Prefix("/github");

    public static final Prefix ENROL_MODULE = new Prefix("/module");
    public static final Prefix ENROL_SEMESTER = new Prefix("/semester");
    public static final Prefix ENROL_SECTION = new Prefix("/section");
    public static final Prefix ENROL_TEAM = new Prefix("/team");

    /* Prefix definitions */
    public static final Prefix PREFIX_NAME = new Prefix("n/");
    public static final Prefix PREFIX_PHONE = new Prefix("p/");
    public static final Prefix PREFIX_EMAIL = new Prefix("e/");
    public static final Prefix PREFIX_ADDRESS = new Prefix("a/");
    public static final Prefix PREFIX_REMARK = new Prefix("r/");
    public static final Prefix PREFIX_TAG = new Prefix("t/");

}
