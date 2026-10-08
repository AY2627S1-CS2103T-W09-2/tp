---
layout: page
title: User Guide
---

AddressBook Level 3 (AB3) is a **desktop application for managing contacts, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, AB3 can help you manage contacts faster than traditional GUI applications.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/se-edu/addressbook-level3/releases).

1. Copy the file to the folder you want to use as the _home folder_ for your AddressBook.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br>
   A GUI should appear in a few seconds. A new installation starts with an empty roster; it does not create sample records automatically. The image below illustrates the planned interface.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add n/John Doe p/98765432 e/johnd@u.nus.edu a/John street, block 123, #01-01` : Adds a contact named `John Doe` to the Address Book.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `clear` : Deletes all contacts.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<div markdown="block" class="alert alert-info">

**:information_source: Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `…`​ can appear zero or more times.<br>
  For example, `[t/TAG]…​` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</div>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a person: `add`

Adds a person to the address book.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]…​`

* `EMAIL` must be an NUS email address: a local part followed by `@u.nus.edu`, such as `e1234567@u.nus.edu`.
  * The local part has 1 to 64 ASCII letters, ASCII digits, dots (`.`), underscores (`_`), plus signs (`+`), or hyphens (`-`). It must start and end with an ASCII letter or digit, and it must not contain spaces or consecutive dots. Non-ASCII letters, such as accented letters, are not accepted.
  * Letter case does not matter, and surrounding spaces and tabs are ignored. The email is saved in lowercase, so `E1234567@U.NUS.EDU` is saved as `e1234567@u.nus.edu`. If you edit the data file directly, write each email in this saved form; the app does not convert emails in the data file.
  * Dots and plus signs are kept as entered. `alex.tan@u.nus.edu`, `alextan@u.nus.edu`, and `alex.tan+cs2103@u.nus.edu` are three different emails.
  * The app checks only the format of the email. It does not check that the NUS account exists.
* Each person is identified by their email. Two persons can have the same name if their emails differ. If another person already has the same email (after it is converted to lowercase), the app shows `A student with this NUS email already exists.` and does not change any data.

<div markdown="span" class="alert alert-primary">:bulb: **Tip:**
A person can have any number of tags, including zero.
</div>

Examples:
* `add n/John Doe p/98765432 e/johnd@u.nus.edu a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@u.nus.edu a/Newgate Prison p/1234567 t/criminal`
* `add n/John Doe p/91234567 e/johndoe@u.nus.edu a/Clementi Ave 1` adds a second person named `John Doe`, because the email is different.
* `add n/Alex Tan p/91234567 e/alex@nus.edu.sg a/Clementi Ave 1` is rejected, because the email does not end with `@u.nus.edu`.

### Listing all persons: `list`

Shows a list of all persons in the address book.

Format: `list`

### Editing a person: `edit`

Edits an existing person in the address book.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]…​`

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, …​
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.
* A new `EMAIL` must follow the same rules as in `add`. You cannot change a person's email to an email that another person already has; the app then shows `A student with the new NUS email already exists.` and does not change any data. Entering a person's current email in different letter case leaves the email unchanged.

Examples:
*  `edit 1 p/91234567 e/johndoe@u.nus.edu` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@u.nus.edu` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Finding students by name or email: `find`

Search the complete roster using a name, email, or part of either identifier.

Format: `find QUERY`

* Enter one literal query of 1 to 100 Unicode characters. `find Alex Tan` searches for the phrase `Alex Tan`; it does not match `Alex Lim` or `Mei Tan`.
* Matching ignores letter case and allows partial names and emails. Accents remain significant, and punctuation such as `*` is literal; wildcards and regular expressions are not supported.
* Surrounding spaces and tabs are ignored. Internal tabs become spaces, and repeated internal spaces remain significant. For example, `find Alex  Tan` (two spaces) does not match `Alex Tan` (one space).
* Every search starts from the complete roster, including students hidden by a previous search.
* Results appear in name order, ignoring case, with email used to break ties. Each matching profile appears once and shows its name, email, and currently supported fields.
* A single match is selected automatically. Zero or multiple matches clear the selection.
* A blank query, more than 100 characters, a line break, or an unsupported control character is rejected. The previous results and selection remain available; correct the command and retry.
* Searching does not change or save roster data.
* If results cannot be displayed, the app shows `Search results could not be displayed. Try the search again.` and retains the previous results and selected profile. Retry the search.

Examples using fictional records:

* `find Alex Tan` finds every profile whose name or email contains `Alex Tan`.
* `find E9000001@U.NUS.EDU` finds a profile with the email `e9000001@u.nus.edu`, even after an earlier search returned no results.
* `find @u.nus.edu` lists matching student emails.
* `find *` searches for a literal asterisk. If no profile matches, the app displays `No students found for "*". Check the spelling or search with another identifier.`

Telegram and GitHub searches are not available in v1.2. Complete enrolment details and the `view EMAIL` command are delivered separately; this increment displays the fields currently available on each result card.

If the student list cannot be refreshed after a command, the app blocks further command execution until it can show the current list. Submit again to refresh it, then check the displayed indexes before re-entering your intended command. The earlier command may already have changed data; refreshing the list does not repeat it.

### Deleting a person: `delete`

Deletes the specified person from the address book.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, …​

Examples:
* `list` followed by `delete 2` deletes the 2nd person in the address book.
* `find Betsy` followed by `delete 1` deletes the 1st person in the results of the `find` command.

### Clearing all entries: `clear`

Clears all entries from the address book.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

SoCdex saves roster changes before reporting success. You do not need to save manually. Read-only commands (`find`, `list`, `help`, and `exit`), rejected commands, and changes that leave all stored values unchanged do not rewrite the roster file.

If saving fails, the app reports that no data was changed and restores the previous roster, result list, and selected profile. Correct the file or folder permissions, or free disk space, then retry. A failed first save does not create a partial roster file. If the filesystem cannot safely replace the file, saving fails instead of overwriting it in place; use a local filesystem that supports atomic file replacement.

This protects against detected loading and saving failures. It does not provide backups, undo, or guarantees against hardware failure or power loss. Use only one running instance for a roster and do not edit its file while the app is running.

### Starting empty or recovering from a load failure

When no saved roster exists, the app starts empty and explains how to add a student. This increment still uses the `add` syntax above. `student add` and explicit fictional sample loading are separate feature increments and are not available yet.

If an existing roster is unreadable or invalid, the app preserves it and opens an empty **read-only recovery session**. The initial message is:

> Stored data could not be loaded. The existing file was preserved. This session is read-only. Restore a valid data file and restart SoCdex.

**Storage unavailable — read-only recovery** remains visible in the status bar, including after searches. An empty recovery view does not mean that the saved roster is empty. Data-changing commands are disabled for the entire session. Searching, listing, help, and exiting remain available. Closing the app does not overwrite the preserved roster.

To recover:

1. Close the app and keep a separate copy of the preserved file before making repairs.
1. Restore a known-valid roster to the displayed data-file location, or correct invalid records and file permissions while the app is closed.
1. Restart SoCdex. Confirm that the expected records load and the status says **Storage available** before making changes.

### Editing the data file

AddressBook data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
Edit the file only while the app is closed, and keep a separate copy before editing it. If your changes make it invalid, SoCdex preserves it and opens a read-only recovery session at the next run. Restore a valid file and restart; commands and normal exit do not replace the invalid roster.<br>
Furthermore, certain edits can cause the AddressBook to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</div>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous AddressBook home folder.

**Q**: Why does my data file from an earlier version no longer load?<br>
**A**: Every email must now be a valid NUS email (see the `add` command), and no two persons can share an email. In the data file, each email must also already be in its saved form: all lowercase, with no spaces or tabs before or after it. Commands such as `add` convert uppercase letters and remove surrounding spaces and tabs, but the app does not convert emails in the data file. A data file that contains another kind of email address (such as one ending in `@example.com`), an email that is not in its saved form (such as `E1234567@u.nus.edu`), or two persons with the same email is treated as an invalid data file. SoCdex then preserves the file and opens a read-only recovery session; see [Starting empty or recovering from a load failure](#starting-empty-or-recovering-from-a-load-failure). Keep a copy of the original file, then correct each email or remove the duplicate record while the app is closed, and restart SoCdex.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action | Format, Examples
--------|------------------
**Add** | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]…​` <br> e.g., `add n/James Ho p/22224444 e/jamesho@u.nus.edu a/123, Clementi Rd, 1234665 t/friend t/colleague`
**Clear** | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit** | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]…​`<br> e.g., `edit 2 n/James Lee e/jameslee@u.nus.edu`
**Find** | `find QUERY`<br> e.g., `find Alex Tan`
**List** | `list`
**Help** | `help`
