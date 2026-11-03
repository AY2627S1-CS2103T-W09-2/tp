---
layout: page
title: Spencer Chu's Project Portfolio Page
---

### Project: SoCdex

SoCdex is a Java desktop application for NUS School of Computing tutors. It keeps student identities,
contact routes, and module-semester enrolments in one local roster. Its keyboard-first command interface
helps a tutor identify and contact the correct student without repeatedly checking Canvas, spreadsheets,
and technical platforms. The application uses JavaFX for its graphical interface.

Given below are my contributions to the project.

* **Code contributed:** [RepoSense team report](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=AY2627S1-CS2103T-W09-2&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other)
  and [GitHub author history](https://github.com/AY2627S1-CS2103T-W09-2/tp/commits/master/?author=spencercdz).

* **Search and profile identification:**
  * Implemented literal student search by name or email and integrated it with the displayed roster,
    result feedback, and failure recovery
    ([#67](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/67)). The change included parser,
    command, model, UI, and regression tests so a failed presentation did not silently replace the
    tutor's previous view.
  * Extended the same search path to optional Telegram and GitHub identifiers while preserving the
    established name and email behaviour
    ([#83](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/83)).
  * Made search responsiveness measurable and repeatable. I separated ordinary card-layout timing from
    screenshot diagnostics, rejected measurements taken with an external JavaFX runtime, recorded
    fresh-process evidence, and removed repeated FXML parsing when search cards are prepared
    ([#112](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/112),
    [#118](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/118),
    [#126](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/126)).

* **Safety and feedback improvements:**
  * Kept an off-screen pending-deletion target visible before confirmation
    ([#153](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/153)) and retained the selected student
    after a failed save ([#165](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/165)).
  * Removed complete roster and command data from runtime logs and added a focused privacy regression
    test ([#157](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/157)).
  * Tightened command parsing so malformed parameterless and `remark` commands fail explicitly
    ([#161](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/161)), retired the unsafe whole-roster
    `clear` command ([#166](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/166)), and kept list and remark
    results in a stable name-and-email order
    ([#154](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/154)).

* **Documentation:**
  * Defined the initial target-user problem, value proposition, and scope boundaries for SoCdex
    ([#43](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/43)).
  * Added the Developer Guide activity diagram for search dispatch, result presentation, selection, and
    failure recovery ([#113](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/113)).
  * Documented the teaching-team clarification for JavaFX runtime-file access
    ([#125](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/125)) and reconciled the final v1.5 User
    Guide with verified product behaviour and acceptance evidence
    ([#190](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/190)).

* **Project management and team-based tasks:**
  * As team lead, coordinated the v1.1 to v1.6 task sequence, milestone handoffs, reviewer assignments,
    release gates, and blocker follow-up. The main coordination records are
    [#39](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/39),
    [#66](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/66),
    [#75](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/75),
    [#94](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/94),
    [#131](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/131), and
    [#198](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/198).
  * Coordinated the final v1.6 freeze and submitted the accepted JAR to Canvas. Jingchun independently
    verified that the downloaded submission matched the accepted filename, size, and SHA-256 checksum
    ([#205](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/205)).
  * Performed assigned alpha testing, recorded reproducible findings, and converted the highest-impact
    findings into focused fixes with tests
    ([#136](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/136)).

* **Review and quality assurance:**
  * Reviewed contributions across documentation, model, UI, acceptance testing, and release work.
    Examples include the product README
    ([#41](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/41)), sample loading
    ([#74](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/74)), profile display
    ([#84](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/84)), integrated acceptance testing
    ([#93](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/93)), profile UML
    ([#123](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/123)), command feedback and focus
    ([#178](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/178)), and the final result-area sizing fix
    ([#209](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/209)).
  * Used unit, integration, acceptance, Checkstyle, archive-content, and three-platform CI evidence as
    merge gates. I requested changes when acceptance evidence or edge-case handling was incomplete and
    re-reviewed the final commit before approval.

* **Tools and attribution:** Used Codex to assist with parts of the implementation and coordination work
  under the team's declared AI-use process. The disclosure and verification record is in
  [#156](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/156).
