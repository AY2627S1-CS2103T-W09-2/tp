# Search timing evidence — 16 October 2026

Issue: #98. Application source: `24f0107eef57387d22d99ec06e9610166352303a`.
Probe implementation: `8bf6869869823f53e4f6fc9702c832672e102ade`.
Candidate JAR SHA-256: `519722c7255d870b59744111eda406823b87fe3cf239f7cea4d22bf86a0bed1c`.
No production code changed for this investigation. This development candidate includes the merged profile-heading fix; it is not the v1.3 release artifact.

## Environment and method

Apple M4 Pro, 24 GiB RAM, SSD, macOS 26.6.2 (25G83), Homebrew OpenJDK 25.0.4.1 and the JAR's JavaFX 17.0.7+2. Window: 1280×720 logical units, output scale 2×; primary screen: 1512×982 logical units. The programmatic launch did not acquire native window focus. This is not the specified i5-1035G1 / 8 GB / Windows 11 reference environment or manual keyboard evidence.

Used the Developer Guide's `SearchTimingProbe` instructions, with three separate fresh processes per mode, run sequentially. Each process loads 500 fictional profiles with 1,000 enrolments, waits for initial layout, and first submits `find bench`. It repeats that query five times before six Telegram, six GitHub and six unmatched queries. Each CSV preserves all 24 measurements plus metadata; 144 observations are retained across six processes. No outliers were removed.

`ordinary` measures action submission to the shown scene's next natural post-layout pulse. This is a rendering-readiness proxy, not physical display/compositor completion. `snapshot` adds forced CSS/layout and a scene snapshot before waiting for a pulse; its total is diagnostic only. Assertions run outside the timed interval. Application-owned detached preparation/layout stays included in both modes. `cards_ms` is a subset of command time, not a duration to add to it.

## Results

| Measurement | Run 1 | Run 2 | Run 3 |
| --- | --- | --- | --- |
| First full-roster ordinary readiness (ms) | 169.795 | 175.333 | 175.662 |
| First full-roster snapshot-mode total (ms) | 191.895 | 191.719 | 203.809 |

| Query | Ordinary readiness range, all three runs (ms) | Snapshot-mode total range (ms) |
| --- | --- | --- |
| Full roster, first plus five repeats | 109.936–175.662 | 117.408–203.809 |
| Unique Telegram | 7.851–15.603 | 14.116–23.915 |
| Unique GitHub | 7.647–10.094 | 13.094–20.596 |
| No match | 5.020–8.094 | 7.084–8.613 |

All 72 ordinary observations were below the existing 1,000 ms target on this machine. Preparing all cards took 138.324–142.990 ms in the three first full-roster ordinary searches and was the dominant measured part of command execution. There is no demonstrated target miss here that justifies changing the existing atomic display-preparation contract. The outcome is a measurement/test correction, not a claim of a production speedup.

The v1.3 [reference Windows measurements](https://github.com/AY2627S1-CS2103T-W09-2/tp/issues/80#issuecomment-6191265092) remain valid historical evidence: two cold 500-result observations exceeded one second with forced CSS/snapshot work included. These macOS numbers do not prove that screenshot overhead explained those misses. Hardware, source and measurement method differ. Rerun the ordinary method on the reference configuration and final #102 candidate; report any remaining miss to #94 for an explicit decision.

## Verification and remaining work

- `./gradlew check coverage`: 466 tests, zero failures/errors/skips, Checkstyle and coverage passed using Zulu JavaFX-enabled JDK 25.0.3. This JDK was used for the unit suite, not the six packaged-JAR timing processes.
- `ContactSearchAcceptance`: 43 assertions passed.
- `ProfileViewAcceptance`: 261 assertions passed, including result selection, details, Tab/Shift+Tab and arrow navigation.
- All six timing processes verified result counts, sorted order, unique/ambiguous selection, rendered cells and feedback; a search save attempt would fail the run.
- Yujie's independent method/results review, final-candidate/reference-platform acceptance and post-merge live-guide verification remain pending. #98 stays open for that handoff.
