---
layout: page
title: DevOps guide
---

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Build automation

This project uses Gradle for **build automation and dependency management**. **We recommend reading [this Gradle tutorial from se-edu/guides](https://se-education.org/guides/tutorials/gradle.html).**


The following commands perform common Gradle tasks.


* **`clean`**: Deletes the files created during the previous build tasks (e.g. files in the `build` folder).<br>
  For example: `./gradlew clean`

* **`shadowJar`**: Uses the Shadow plugin to create the fat JAR file `build/libs/addressbook.jar`.<br>
  For example: `./gradlew shadowJar`

* **`run`**: Builds and runs the application.<br>
  **`runShadow`**: Builds the application as a fat JAR, then runs it.

* **`checkstyleMain`**: Runs the code style check for the main code base.<br>
  **`checkstyleTest`**: Runs the code style check for the test code base.

* **`test`**
  * `./gradlew test`: Runs all tests.
  * `./gradlew clean test`: Cleans the project before running all tests

--------------------------------------------------------------------------------------------------------------------

## Continuous integration (CI)

This project uses GitHub Actions for CI. The necessary workflow configuration files are in `.github/workflows`. No further setup is required.

### Code coverage

As part of CI, Gradle generates JaCoCo coverage reports from the tests, and CI uploads the coverage data to Codecov. Codecov then provides information about test coverage.

However, because Codecov is known to run into intermittent problems (e.g., report upload fails) due to issues on the Codecov service side, the CI is configured to pass even if the Codecov task failed. Therefore, developers are advised to check the code coverage levels periodically and take corrective actions if the coverage level falls below desired levels.

To enable Codecov for forks of this project, follow the steps given in [this se-edu guide](https://se-education.org/guides/tutorials/codecov.html).

### Repository-wide checks

In addition to Gradle checks, CI runs repository-wide checks. Unlike Gradle checks, which cover files used in the build, these checks cover every repository file and enforce rules that are hard to apply on development machines, such as line-ending requirements.

These checks are implemented as POSIX shell scripts, and thus can only be run on POSIX-compliant operating systems such as macOS and Linux. To run all checks locally on these operating systems, execute the following in the repository root directory:

`./.github/run-checks.sh`

Any warnings or errors will be printed out to the console.

**If adding new checks:**

* Checks are implemented as executable `check-*` scripts within the `.github` directory. The `run-checks.sh` script will automatically pick up and run files named as such. That is, you can add more such files if you need and the CI will do the rest.

* Check scripts should print out errors in the format `SEVERITY:FILENAME:LINE: MESSAGE`
  * SEVERITY is either ERROR or WARN.
  * FILENAME is the path to the file relative to the current directory.
  * LINE is the line of the file where the error occurred and MESSAGE is the message explaining the error.

* Check scripts must exit with a non-zero exit code if any errors occur.

--------------------------------------------------------------------------------------------------------------------

## Making a release

Here are the steps to create a new release.

1. Update the version number in [`MainApp.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java).
1. Generate a fat JAR file using Gradle (i.e., `./gradlew shadowJar`).
1. Tag the repo with the version number. e.g. `v0.1`
1. [Create a new release using GitHub](https://help.github.com/articles/creating-releases/). Upload the JAR file you created.


### v1.3 candidate packaging and smoke evidence

Build one reviewed, feature-complete master commit with Java 25:

```text
./gradlew check coverage shadowJar
```

`MainApp.VERSION` identifies v1.3. This metadata does not mean a release has been published. `shadowJar` produces `build/libs/addressbook.jar`; do not change its bytes after testing. Record the full source SHA (`git rev-parse HEAD`), Java version (`java -version`), filename, byte size and SHA-256. The JAR must stay below 100 MB. Record dry runs separately from final-candidate sign-off in #79.

The JAR retains Windows and Linux JavaFX libraries and packages macOS libraries as universal Mach-O binaries containing both Intel and Apple Silicon slices. `universalMacNatives` combines the existing JavaFX 17.0.7 vendor binaries without changing their slices or upgrading dependencies. It runs on all build hosts and needs no Apple tools. Native resource names collide between the two macOS classifiers, so the task replaces the Intel-only root resources with the combined files. On macOS, `file build/generated/universal-mac-natives/*.dylib` provides an independent architecture check. Native inspection and unit-test CI are not GUI launch evidence.

Copy the same JAR into a new disposable folder on Windows, macOS and Linux. Use a plain JDK 25 without preinstalled JavaFX where possible, so an external JavaFX runtime does not mask missing packaged libraries. Record the operator, OS/architecture, Java vendor/version, source SHA and artifact checksum for each run. On macOS/Linux use `shasum -a 256 addressbook.jar` and `wc -c addressbook.jar`; on Windows use `Get-FileHash addressbook.jar -Algorithm SHA256` and `(Get-Item addressbook.jar).Length` in PowerShell.

1. Run `java -jar addressbook.jar`. Confirm the production window opens and the roster starts empty. Do not seed the test with an existing preferences or roster file.
1. Enter `student add /name Alex Demo /email E9000001@U.NUS.EDU /telegram @Alex_Demo /github alex-demo`. Expect `Added student: Alex Demo.` and the selected canonical email `e9000001@u.nus.edu`.
1. Enter `find e9000001`. Expect one selected result. In the final feature-complete candidate, also run #80's enrol/view/delete scenario against this same artifact.
1. Enter `exit`, confirm normal process termination, then run the identical launch command again. Verify the complete saved profile and contacts return. Search again and exit; keep logs and a screenshot. Record any failure rather than marking the platform as passed.
1. Hand the accepted artifact and checksum to Jingchun for #81, with Yujie's #79 review and #80 evidence. Rebuild and rerun affected checks if the candidate source changes. Publication is separate: download the uploaded JAR and compare its checksum.

An optional reproducible programmatic smoke utility exercises production `MainApp` startup and stop and the actual command box. Build test classes using `./gradlew testClasses shadowJar`. From a new disposable working directory, copy the tested JAR and run these as **two separate JVM invocations**, using the absolute path to this checkout's test classes:

```text
java -cp /ABSOLUTE/CHECKOUT/build/classes/java/test:addressbook.jar seedu.address.release.PackagedJarSmoke seed
java -cp /ABSOLUTE/CHECKOUT/build/classes/java/test:addressbook.jar seedu.address.release.PackagedJarSmoke restart
```

On Windows, replace the classpath separator `:` with `;`. The seed refuses an existing roster. The utility writes fictional roster/preferences files and `smoke-seed.png` / `smoke-restart.png`, verifies byte preservation through search and shutdown, and prints actual results. Production classes and JavaFX come from the tested JAR; the extra classpath supplies only the test driver. This is programmatic UI acceptance and a real JVM restart, not manual keyboard interaction or a substitute for the direct `java -jar` launch check. Run and record each platform separately; three CI unit-test jobs do not certify three packaged GUI launches.
