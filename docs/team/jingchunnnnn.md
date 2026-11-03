---
layout: page
title: Jingchun Zhou's Project Portfolio Page
---

### Project: SoCdex

SoCdex is a Java desktop application for NUS School of Computing tutors. It keeps student identities, contact routes and module-semester enrolments in one local roster. Its keyboard-first command interface helps a tutor identify and contact the correct student without repeatedly checking Canvas, spreadsheets and technical platforms. The application uses JavaFX for its graphical interface.

Given below are my contributions to the project.

* **Code contributed:** [RepoSense team report](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=AY2627S1-CS2103T-W09-2&sort=groupTitle&sortWithin=title&timeframe=commit&mergegroup=&groupSelect=groupByRepos&breakdown=true&checkedFileTypes=docs~functional-code~test-code~other) and [GitHub author history](https://github.com/AY2627S1-CS2103T-W09-2/tp/commits/master/?author=jingchunnnnn).

* **Enhancements implemented:**
  * Implemented `sample load`, which atomically loads a fixed fictional roster for product demonstrations and first-time use ([#74](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/74)). The implementation validates the complete fixture before replacing the model, rejects unsafe states, preserves sample classification after edits and reloads, and rolls back presentation or storage failures. I implemented the core command and tests. Spencer later added the final selection prompt, documentation corrections and three preservation tests during review, as recorded in the PR.
  * Implemented `sample clear`, which removes only fictional sample profiles and their enrolments while preserving real roster data ([#82](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/82)). The command reports the actual numbers removed, avoids saving when there is nothing to remove, restores state after failures and keeps any surviving selected profile visible.
  * Prevented runtime logs from entering packaged JAR files and added archive-content checks for required and forbidden resources ([#109](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/109)). This reduced the risk of distributing local runtime data and made clean and non-clean package verification repeatable.
  * Added warning-level logging for presentation and storage rollback failures without logging command text or student data ([#120](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/120)).
  * Integrated the MVP acceptance driver onto the current codebase and addressed review findings on deterministic row traversal, candidate checksums and evidence boundaries ([#93](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/93)). `@YujieJWang` wrote the original driver commit, which remains credited to him.

* **Documentation:**
  * Established the SoCdex product description and terminology in the README, glossary, published homepage and User Guide ([#41](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/41), [#42](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/42), [#57](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/57), [#73](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/73)).
  * Reconciled the guides with delivered behaviour for the v1.3 and v1.5 releases, including installation, recovery, command scope, known limitations and Help guidance ([#92](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/92), [#151](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/151), [#184](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/184)).
  * Added the Developer Guide activity diagram for sample loading and clearing. It documents validation, sample classification, rollback, presentation and storage paths ([#110](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/110)).
  * Prepared the final v1.6 User Guide and product website audit, including supported remark behaviour and accepted search and affiliation limitations ([#208](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/208)).

* **Project management and team-based tasks:**
  * Coordinated documentation, deliverables and deadlines for the team. I maintained evidence-backed release records and checked that project decisions, owners and follow-up actions were recorded.
  * Published and verified the v1.3, v1.4 and v1.5 releases, including their public pages and release artefacts ([#81](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/81), [#103](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/103), [#143](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/143)).
  * Performed assigned v1.5 alpha testing and recorded reproducible evidence for the tested workflows ([#137](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/137)).
  * Prepared the final v1.6 User Guide, website and publication workflow ([#200](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/200), [#204](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/204)).

* **Review and quality assurance:**
  * Reviewed changes across the model, UI, storage, testing and documentation components. Examples include student-profile editing ([#70](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/70)), enrolment creation ([#85](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/85)), data-folder isolation ([#89](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/89)), focus visibility ([#91](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/91)), profile-presentation diagrams ([#122](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/122)), search presentation and performance ([#126](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/126), [#127](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/127)), and final source and documentation fixes ([#179](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/179), [#180](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/180), [#181](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/181), [#197](https://github.com/AY2627S1-CS2103T-W09-2/tp/pull/197)).
  * Used focused acceptance drivers, unit tests, Checkstyle, repository checks and three-platform CI as merge gates. I also recorded when a local JavaFX environment could not provide complete evidence instead of treating an incomplete run as a pass.
