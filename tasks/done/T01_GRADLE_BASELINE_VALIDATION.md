# T01 — Gradle Baseline Validation

Status: DONE
Type: INFRA / READ-MOSTLY
Production behavior changes: FORBIDDEN

## Goal

Establish a reliable Gradle execution path for the imported PixelGleam project and verify the current Kotlin/Android baseline before any production refactor or feature work begins.

## Root responsibilities

The root Sol/high orchestrator must:

1. read `AGENTS.md`
2. read `docs/CURRENT_STATE.md`
3. inspect the current Gradle project structure
4. determine the safest valid Gradle execution path
5. delegate only bounded inspection/setup work
6. preserve production behavior
7. run the existing baseline test suite
8. independently review all results
9. record PASS / REWORK / BLOCKED
10. do not proceed to T02

## Relevant files

Inspect as needed:

- `settings.gradle.kts`
- root `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`
- existing `gradlew`
- existing `gradlew.bat`
- existing wrapper JAR, if present
- relevant test sources under `app/src/test/`

## Known baseline issue

The AI Studio ZIP previously inspected contained:

- `gradle/wrapper/gradle-wrapper.properties`

but did not contain a complete Gradle Wrapper executable set:

- `gradlew`
- `gradlew.bat`
- wrapper JAR

Verify the current local repository instead of assuming this is still true.

## Delegation

### Subtask A — Luna / low

READ-ONLY inspection:

- inspect Gradle files and wrapper state
- report exact missing/present files
- report declared Gradle/AGP/Kotlin versions
- identify whether the local project already has a usable Gradle execution path

Do not modify files.

### Subtask B — Terra / medium

Only if setup work is required and root approves a bounded method:

- restore or establish a standard Gradle execution path
- do not change app behavior
- do not upgrade Gradle, AGP, Kotlin, dependencies, SDK targets, or plugin versions unless strictly required and explicitly approved by root
- prefer restoring the wrapper that matches the existing project configuration

Return all files changed and exact reason for each.

## Must not change

- gameplay logic
- UI behavior
- architecture
- package/application IDs
- dependencies unless required solely to restore baseline execution
- Gradle/AGP/Kotlin versions without explicit root-level justification
- production source files

## Validation sequence

After a valid execution path exists:

1. confirm Gradle version
2. run the narrowest safe project configuration/build check
3. run the existing JVM/unit test suite
4. run broader existing test tasks if available and appropriate
5. capture failing test names and stack traces
6. distinguish:
   - environment/setup failure
   - compile failure
   - existing test failure
   - new regression caused by T01 setup

Do not “fix” production test failures in T01.

## Acceptance criteria

PASS only if:

- a reproducible Gradle command works from the repository root
- the execution path is documented
- no production behavior was changed
- baseline tests were run
- test results are recorded clearly
- any failures are classified rather than silently fixed
- root independently reviewed the setup and results

REWORK if:

- Gradle works only through an ad-hoc machine-specific workaround
- versions were changed unnecessarily
- production files were modified
- baseline test results are incomplete

BLOCKED if:

- the project cannot execute Gradle without a change that risks altering the baseline
- required SDK/toolchain prerequisites are unavailable
- wrapper restoration cannot be validated safely

## Required final report

- Root model/effort
- Worker A model/effort
- Worker B model/effort, if used
- Gradle execution method
- wrapper state before/after
- files changed
- Gradle version
- tasks/commands run
- total tests run
- passed / failed / skipped
- failure classification
- root corrections, if any
- final status: PASS / REWORK / BLOCKED
- recommended T02, but do not execute it

## State update

After root acceptance:

- update `docs/CURRENT_STATE.md`
- move this task from `tasks/active/` to `tasks/done/`
- do not start T02 automatically

## Root acceptance

PASS on 2026-09-07. The Gradle execution baseline is restored; the test baseline is red but fully classified.

- Worker A was assigned GPT-5.6 Luna / low for read-only inspection.
- Worker B was assigned GPT-5.6 Terra / medium for bounded standard-wrapper restoration.
- Wrapper before: only `gradle/wrapper/gradle-wrapper.properties` was present.
- Wrapper after: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` were generated for the existing Gradle 9.3.1 binary distribution; wrapper properties remained byte-identical.
- `gradlew.bat --version` and `gradlew.bat help` succeeded.
- The Java 17 test attempt compiled but Robolectric Android 36 sandbox creation required Java 21.
- With Java 21, all 61 JVM test methods were attempted across the full invocation and bounded reruns: 58 passed, 2 failed assertions, 1 was non-terminating, and 0 were skipped.
- The two assertion failures and one test-harness hang are recorded in `docs/CURRENT_STATE.md` and classified as existing baseline issues, not wrapper regressions.
- One Android instrumented test was not run because no device/emulator was connected.
- No production/test source, build declaration, dependency, SDK target, or toolchain version was changed.
- T02 was not executed.
