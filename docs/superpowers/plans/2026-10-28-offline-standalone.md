# Offline Standalone Ivy Wallet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the archived Ivy Wallet snapshot into a sideloaded, offline-first personal app with no Firebase, Google Play, GitHub API, CI or JitPack dependencies. The only runtime network call left is the exchange-rate fetch.

**Architecture:** The app already stores everything locally (Room DB v130 with exported schemas, DataStore, SharedPrefs, a local zip backup, WorkManager reminders, Glance widgets). Cloud sync was already removed upstream. So this plan only *removes* things: Firebase Crashlytics and Play Core from the build, three leaf feature modules that call `api.github.com` or only link to upstream channels, the CI/release tooling, and the UI entry points that pointed at them. Nothing that reads or writes user data changes, so existing backups still import. A grep-based guard script is the "test" for each removal.

**Tech Stack:** Kotlin 2.0.20, AGP 8.5.2, Gradle 8.8, Jetpack Compose 1.6.8, Hilt 2.52, Room 2.6.1, Ktor 2.3.12 (exchange rates only), Arrow 1.2.4, Kotest + MockK + Paparazzi.

**Requirements (from the user):**
1. The bare minimum needed to build and run. Firebase, external connections and store integration are not needed.
2. The exchange-rate API is the one network feature to keep.
3. Everything else works offline.
4. Distributed as a standalone APK, not through the Play Store.

## Revision note (2026-10-28)

This replaces the first draft, which was written against a different snapshot. Corrections for this repo (`Imnotjarvis/ivy`):

- There is no `feature/poll` and no `.github/`, and the tree is already a git repo. The draft's "git init" step and its poll-removal task are dropped.
- There is **no `.gitignore`**, and **6,159 build-output files are committed** (`buildSrc/build/`, `buildSrc/.gradle/`, `build/reports/`), plus `local.properties` with a machine-specific `sdk.dir`. That cleanup is now Task 1.
- Dependencies that are declared but unused are now removed too: `androidx.security:security-crypto`, `kotlinx-coroutines-play-services`, `play:app-update`, `play-services-tasks`.
- JitPack is now removed. It's only needed for a detekt ruleset, and it's the one build host most likely to vanish.
- The guard script no longer scans `config/detekt/baseline.yml`. That file holds detekt suppression IDs that mention firebase, which would be a false positive.

**Verification status.** Every path, symbol and line quoted here was checked against this tree. The source edits in Tasks 1 and 3–6 were dry-run in a scratch worktree: every edit matched exactly once, `scripts/verifyOffline.sh` exited 0, no references to removed symbols were left, and every edited Kotlin file stayed brace-balanced. **No Gradle build was run while writing the plan**, because the planning environment can't reach Google Maven (`dl.google.com`). Task 1 Step 3 is therefore the first real compile.

## Implementation status (2026-10-28)

The code changes for all seven tasks are committed on `claude/adoring-bell-r022v1`, one commit per task. Ticked steps are done. Each removal was checked with `scripts/verifyOffline.sh` (exits 0), a scan for references to removed symbols (none) and a brace-balance check of every edited Kotlin file.

Unticked steps need a machine that can reach Google Maven, or your phone:
- **Task 1 Steps 3–4:** the first build, and recording the baseline tests.
- **The Gradle half of each Verify step** (Task 3 Step 8, Task 4 Step 5, Task 5 Step 4, Task 6 Steps 2 and 4). The guard-script half passed.
- **Task 7 Steps 1 and 3–5:** your keystore, the release build, device QA and data migration.

One change beyond the plan: `include(":shared:common-ui")` was dropped from `settings.gradle.kts`. That directory doesn't exist and nothing uses it. Gradle 8.8 only warns about such an include, but Gradle 9 rejects it.

A GitHub Actions build was added afterwards (`.github/workflows/build.yml`). It is your own CI, not upstream's: it runs the guard script, builds the debug APK and the R8-minified `demo` APK, and runs the unit tests on every push. On `v*` tags it also builds a signed release APK and publishes it as a GitHub release. The file header lists the secrets this needs.

Run these in order: `./gradlew :app:assembleDebug`, then `./gradlew testDebugUnitTest --continue`, then Task 7 Step 1. Until `keystore.properties` exists, `assembleRelease` fails at signing. Debug builds don't need it.

## What stays, what goes

| Area | Decision | Why |
|---|---|---|
| `shared/data/*` (Room, DataStore, backup zip, Ktor client) | **Keep** | User data, backups, exchange-rate fetch |
| `feature/exchange-rates`, `SyncExchangeRatesUseCase`, `RemoteExchangeRatesDataSourceImpl` | **Keep** | The one network feature. It already fails soft (see Task 6) |
| All core features (accounts, transactions, categories, budgets, loans, planned payments, reports, piechart, search, import-data, settings, home, main, onboarding) | **Keep** | Core app |
| `feature/features` | **Keep** | Local user toggles stored in DataStore, not a remote flag service |
| `feature/disclaimer` | Keep (optional removal) | A local first-launch gate with no network |
| `temp/legacy-code`, `temp/old-design` | **Keep** | Despite the name, they're load-bearing: about 31k lines that hold most of the business logic and the UI kit every screen uses |
| `widget/*`, `PaymentTileService`, reminder worker | **Keep** | All local |
| Firebase Crashlytics, `google-services.json`, `FirebaseExt.kt` | **Remove** (Task 3) | Sends crash reports to the original authors' Firebase project |
| Play Review, Play App Update, `play-services-tasks`, `kotlinx-coroutines-play-services` | **Remove** (Task 3) | Play-only. Of these, only `ReviewManagerFactory` is referenced in code |
| `androidx.security:security-crypto` | **Remove** (Task 3) | Declared but unused, and deprecated upstream |
| "Rate us", "Share Ivy" and "open source" buttons; the rate-us card | **Remove** (Tasks 3–4) | Point at the Play listing or upstream GitHub |
| `feature/contributors`, `feature/releases` (and Coil) | **Remove** (Task 4) | Call `api.github.com`. Coil is only used for contributor avatars |
| `feature/attributions` and the Settings "Product" section | **Remove** (Task 4) | Only links to upstream Telegram, GitHub and help center |
| `ci-actions/*`, `fastlane/`, `Gemfile*`, compose-stability scripts, the wrapper-upgrade bot plugin | **Remove** (Task 5) | GitHub/Play automation. `ci-actions` also needs a JDK 17 toolchain and Kotlin context receivers |
| JitPack and the `detekt-explicit` ruleset | **Remove** (Task 5) | A third-party build host, used only by `./gradlew detekt` |

Kept on purpose: `INTERNET` and `ACCESS_NETWORK_STATE`, Ktor and its four fallback URLs, `openUrlInBrowser`, and `openGooglePlayAppPage`. The CSV-import instructions use that last one to open *other* apps' store pages, and it falls back to the browser without any Play library. Tap-to-open links in onboarding, the disclaimer and the import instructions stay too. They make no automatic network calls.

## Global Constraints

- **JDK:** Gradle 8.8 runs on JDK 17–22 only. It can't start on JDK 23+, and the original dev machine's default is JDK 25. Before any `./gradlew` command, run `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` once per shell. In Android Studio, pick a 17 or 21 JDK under Settings → Build Tools → Gradle → Gradle JDK. Every command below assumes this.
- **Android SDK:** `sdk.dir=/home/matt/Android/Sdk` with `android-34` installed. After Task 1 this lives only in your local, untracked `local.properties`.
- **Do NOT change:** the Room schema or migrations, DataStore keys, SharedPrefs keys, the backup zip/JSON format, or the `namespace` (`com.ivy.wallet`). Existing data and backups must stay readable.
- **Do NOT bump:** AGP, Kotlin, Compose, `compileSdk` or `targetSdk` (stays 34). Upgrades are a separate plan (see the end).
- Leftover *unused* imports are warnings, not errors, because no module sets `allWarningsAsErrors`. Each task still lists them so detekt stays clean. Imports of *deleted* symbols are errors and must go.

## Review Focus

1. **First launch with no network.** Onboarding finishes, Home loads, the exchange-rate list is empty, and adding a manual rate works. Onboarding navigates first and syncs afterwards (`OnboardingRouter.completeOnboarding`), so it isn't blocked. Covered by the Task 6 unit test and the Task 7 "airplane-mode first launch" device step.
2. **Network drops after rates were synced.** Stored rates must not be wiped by a failed sync. Covered by the Task 6 test `sync - network failure leaves stored rates untouched`.
3. **Moving data from the Play install.** A backup zip exported from the Play version imports into the new `applicationId`. Covered by Task 7, "migrate data".
4. **The R8-minified release build crashes at start** because a removed class was still reachable. Covered by Task 7, which smoke-tests the *release* APK.
5. **Home "more" menu and Settings after the removals.** The more-menu's second row goes from 4 buttons to 3 (they're spaced with `weight(1f)`, so still evenly), and Settings loses its "Other" and "Product" sections. Covered by the Task 7 device checklist.

---

### Task 1: Repo hygiene and baseline build

**Files:**
- Create: `.gitignore`
- Untrack (keep on disk): `buildSrc/build/`, `buildSrc/.gradle/`, `build/`, `local.properties`

**Interfaces:**
- Consumes: nothing
- Produces: a tree that tracks no build output, a green debug build, and a notes file listing tests that already fail (`../ivy-baseline-tests.log`, kept outside the repo so `./gradlew clean` can't delete it)

- [x] **Step 1: Add `.gitignore`**

```gitignore
# Gradle / Android build output
.gradle/
build/
/local.properties
/captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab

# IDE
.idea/
*.iml
.DS_Store

# Release signing (Task 7)
/keystore.properties
/release.jks
```

- [x] **Step 2: Stop tracking build output and the machine-specific SDK path**

```bash
git rm -r -q --cached buildSrc/build buildSrc/.gradle build local.properties
git add .gitignore
git commit -m "chore: add .gitignore and untrack build outputs"
```

Run: `git ls-files | grep -cE '(^|/)(build|\.gradle)/'`
Expected: `0`. `git ls-files | wc -l` drops from 8006 to about 1,850. Your local `local.properties` is still on disk.

- [ ] **Step 3: Build the debug APK (first real compile)**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.
If it fails, stop and record the error. Every later task assumes this baseline builds.

- [ ] **Step 4: Record pre-existing unit-test failures**

Run: `./gradlew testDebugUnitTest --continue > ../ivy-baseline-tests.log 2>&1; grep -E 'FAILED$' ../ivy-baseline-tests.log`
Expected: mostly PASS. Later tasks only need "no *new* failures". Failures inside `feature/contributors`, `feature/releases` and `feature/attributions` disappear with those modules in Task 4.

---

### Task 2: Offline guard script (the failing test)

**Files:**
- Create: `scripts/verifyOffline.sh`

**Interfaces:**
- Consumes: nothing
- Produces: `scripts/verifyOffline.sh`. It exits 0 when clean and 1 otherwise, printing one `ok   [label]` or `FAIL [label]` line per check. Labels: `firebase`, `play-core`, `github-api`, `jitpack`, `ci-wiring`, `removed-paths`.

- [x] **Step 1: Write the script**

```bash
#!/bin/bash
# Fails if Firebase / Google Play / GitHub API / CI / JitPack wiring is still in the build.
# The exchange-rate API (currency-api) is the only network dependency the app may keep.
set -u
cd "$(dirname "$0")/.." || exit 1

SRC_DIRS="app buildSrc config/detekt/config.yml feature shared temp widget gradle settings.gradle.kts build.gradle.kts"
fail=0

check() {
    local label="$1" pattern="$2"
    local hits
    # shellcheck disable=SC2086
    hits=$(grep -rnIiE "$pattern" $SRC_DIRS \
        --include='*.kt' --include='*.kts' --include='*.toml' --include='*.xml' \
        --include='*.json' --include='*.yml' --include='*.pro' \
        2>/dev/null | grep -v '/build/' | grep -v 'lint-baseline.xml')
    if [ -n "$hits" ]; then
        echo "FAIL [$label]"
        echo "$hits" | head -5 | sed 's/^/       /'
        fail=1
    else
        echo "ok   [$label]"
    fi
}

absent() {
    local label="$1"; shift
    local present=""
    for p in "$@"; do [ -e "$p" ] && present="$present $p"; done
    if [ -n "$present" ]; then
        echo "FAIL [$label] still present:$present"
        fail=1
    else
        echo "ok   [$label]"
    fi
}

check  "firebase"   'firebase|crashlytics|google-services|google\.services|com\.google\.gms'
check  "play-core"  'com\.google\.android\.play|play-services'
check  "github-api" 'api\.github\.com'
check  "jitpack"    'jitpack|detekt-explicit|ivy[.-]explicit|IvyExplicit'
check  "ci-wiring"  'ci-actions|wrapperUpgrade|wrapper-upgrade'
absent "removed-paths" app/google-services.json ci-actions fastlane Gemfile Gemfile.lock \
    feature/contributors feature/releases feature/attributions \
    buildSrc/src/main/kotlin/ivy.script.gradle.kts

exit $fail
```

- [x] **Step 2: Run it to verify it fails**

Run: `chmod +x scripts/verifyOffline.sh && scripts/verifyOffline.sh; echo "exit=$?"`
Expected: all six labels `FAIL`, `exit=1`.

- [x] **Step 3: Commit**

```bash
git add scripts/verifyOffline.sh
git commit -m "test: add offline guard script"
```

---

### Task 3: Remove Firebase Crashlytics, google-services, Play Core and unused security-crypto

Crashlytics sends crash reports to the original developers' Firebase project. Play Review only works for Play installs. "Share" sends the Play Store link.

**Files:**
- Delete: `app/google-services.json`, `temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt` (also defines `DeveloperException`, which nothing else uses)
- Modify: `gradle/libs.versions.toml`, `buildSrc/build.gradle.kts`, `app/build.gradle.kts`, `temp/legacy-code/build.gradle.kts`
- Modify: `shared/domain/src/main/java/com/ivy/domain/RootScreen.kt`
- Modify: `app/src/main/java/com/ivy/wallet/RootActivity.kt` (the only `RootScreen` implementation)
- Modify: `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourneyCardsProvider.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `scripts/verifyOffline.sh` (Task 2)
- Produces: `RootScreen` without `reviewIvyWallet` and `shareIvyWallet`. It keeps `isDebug`, `buildVersionName`, `buildVersionCode`, `openUrlInBrowser`, `shareCSVFile`, `shareZipFile`, `openGooglePlayAppPage` and `pinWidget`. `CustomerJourneyCardsProvider` no longer has `rateUsCard()`.

- [x] **Step 1: Version catalog**

`gradle/libs.versions.toml`: delete these library lines:

```toml
kotlin-coroutines-googleplay-temp = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services", version.ref = "kotlin-coroutines" }
google-services-plugin = { module = "com.google.gms:google-services", version = "4.4.2" }
google-play-update = { module = "com.google.android.play:app-update", version = "2.1.0" }
google-play-services = { module = "com.google.android.gms:play-services-tasks", version = "18.2.0" }
google-play-review = { module = "com.google.android.play:review-ktx", version = "2.0.1" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics", version = "19.0.3" }
firebase-crashlytics-gradle-plugin = { module = "com.google.firebase:firebase-crashlytics-gradle", version = "3.0.2" }
firebase-firestore = { module = "com.google.firebase:firebase-firestore", version = "25.1.4" }
androidx-security = { module = "androidx.security:security-crypto", version = "1.0.0" }
```

Replace the `kotlin-android` bundle with the following. This bundle is applied to every Android module through `ivy.kotlin-android`.

```toml
kotlin-android = [
    "kotlin-coroutines-android",
]
```

Delete the whole `google = [...]` and `firebase = [...]` bundles.

- [x] **Step 2: Gradle scripts**

`buildSrc/build.gradle.kts`: delete:

```kotlin
    implementation(libs.google.services.plugin)
    implementation(libs.firebase.crashlytics.gradle.plugin)
```

`app/build.gradle.kts`: delete these plugin lines:

```kotlin
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
```

Also delete these dependency lines:

```kotlin
    implementation(libs.bundles.google)
    implementation(libs.bundles.firebase)
    implementation(libs.androidx.security)
```

`temp/legacy-code/build.gradle.kts`: delete `    implementation(libs.bundles.firebase)`.

Then:

```bash
rm app/google-services.json temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt
```

- [x] **Step 3: `RootScreen` interface**

`RootScreen.kt`: delete these two declarations and the blank line after each:

```kotlin
    fun reviewIvyWallet(dismissReviewCard: Boolean)

    fun shareIvyWallet()
```

- [x] **Step 4: `RootActivity`**

- Delete these imports: `com.google.android.play.core.review.ReviewManagerFactory`, `com.ivy.home.customerjourney.CustomerJourneyCardsProvider`, `com.ivy.legacy.Constants`, `com.ivy.legacy.utils.sendToCrashlytics`.
- Delete the field and the blank line after it:

```kotlin
    @Inject
    lateinit var customerJourneyLogic: CustomerJourneyCardsProvider
```

- In `openUrlInBrowser`, delete `e.sendToCrashlytics("Cannot open URL in browser, intent not supported.")`. Keep `e.printStackTrace()` and the Toast.
- Delete the whole `override fun shareIvyWallet() { ... }` and `override fun reviewIvyWallet(dismissReviewCard: Boolean) { ... }` functions.

- [x] **Step 5: Rate-us card**

`CustomerJourneyCardsProvider.kt`: delete the whole `fun rateUsCard() = CustomerJourneyCardModel(...)` function and the blank line above it. It isn't in `ACTIVE_CARDS`, and its only caller was `RootActivity.reviewIvyWallet` (Step 4). Delete `import com.ivy.design.l0_system.Green`, which is now unused.

- [x] **Step 6: Home more-menu "Share" button**

`HomeMoreMenu.kt`, in the "Second Row": delete this block together with the `Spacer` and blank line that follow it:

```kotlin
            val rootScreen = rootScreen()
            MoreMenuButton(
                icon = R.drawable.home_more_menu_share,
                label = stringResource(R.string.share_ivy)
            ) {
                rootScreen.shareIvyWallet()
            }

            Spacer(Modifier.weight(1f))
```

Delete `import com.ivy.legacy.rootScreen`, which is now unused. Row 2 keeps Reports, Budgets and Loans.

- [x] **Step 7: Settings "Other" section**

`SettingsScreen.kt`: delete the whole `item { ... }` that starts with `SettingsSectionDivider(text = stringResource(R.string.other))`. It holds the "Rate us", "Share" and "open source" buttons. `GradientIvy`, `Red3` and `MediumBlack` become unused. Delete their imports at the end of Task 4 together with the rest.

- [ ] **Step 8: Verify**

Run: `scripts/verifyOffline.sh`
Expected: `ok` for `firebase` and `play-core`. The others still FAIL.

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -iE "firebase|com.google.android.play|play-services"`
Expected: no output.

- [x] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: remove Firebase Crashlytics, Play Core and unused security-crypto"
```

---

### Task 4: Remove GitHub-API screens, attributions, project links and Coil

`contributors` and `releases` call `api.github.com`. `contributors` also pulls in Coil for avatars, its only user. `attributions` and the Settings "Product" section only link to Ivy's Telegram, GitHub and help center. All three modules are leaves: only `app` (nav graph) and Settings reference them, and their tests live inside them.

**Files:**
- Delete: `feature/contributors/`, `feature/releases/`, `feature/attributions/`
- Modify: `settings.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/proguard-rules.pro`
- Modify: `app/src/main/java/com/ivy/IvyNavGraph.kt`
- Modify: `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`

**Interfaces:**
- Consumes: Task 3 result
- Produces: `Screen` (a sealed interface) no longer has `AttributionsScreen`, `ContributorsScreen` or `ReleasesScreen`. The `compose` bundle no longer contains `compose-coil`.

- [x] **Step 1: Delete modules and wiring**

```bash
rm -rf feature/contributors feature/releases feature/attributions
```

`settings.gradle.kts`: delete:

```kotlin
include(":feature:attributions")
include(":feature:contributors")
include(":feature:releases")
```

`app/build.gradle.kts`: delete:

```kotlin
    implementation(projects.feature.attributions)
    implementation(projects.feature.contributors)
    implementation(projects.feature.releases)
```

`gradle/libs.versions.toml`: delete the line `compose-coil = { module = "io.coil-kt:coil-compose", version = "2.7.0" }` and the `    "compose-coil",` entry in the `compose` bundle.

`app/proguard-rules.pro`: delete `-dontwarn coil.**`.

- [x] **Step 2: Nav graph and screens**

`IvyNavGraph.kt`: delete these imports: `com.ivy.attributions.AttributionsScreenImpl`, `com.ivy.contributors.ContributorsScreenImpl`, `com.ivy.releases.ReleasesScreenImpl`, `com.ivy.navigation.AttributionsScreen`, `com.ivy.navigation.ContributorsScreen`, `com.ivy.navigation.ReleasesScreen`. Also delete these branches:

```kotlin
        AttributionsScreen -> AttributionsScreenImpl()
        ContributorsScreen -> ContributorsScreenImpl()
        ReleasesScreen -> ReleasesScreenImpl()
```

`Screens.kt`: delete these, with the blank line after each:

```kotlin
data object AttributionsScreen : Screen

data object ContributorsScreen : Screen

data object ReleasesScreen : Screen
```

- [x] **Step 3: Settings screen**

`SettingsScreen.kt`:
- In the toolbar version `Text`, delete the argument `modifier = Modifier.clickable { nav.navigateTo(ReleasesScreen) },` (three lines). Keep the version text.
- Delete the whole `item { ... }` that starts with `SettingsSectionDivider(text = stringResource(R.string.product))` and ends after `TCAndPrivacyPolicy()`.
- Delete these private composables: `IvyTelegram`, `HelpCenter`, `ReportBug`, `RequestFeature`, `ContactSupport`, `Releases`, `Contributors`, `Attributions`, `TCAndPrivacyPolicy`.
- Delete these imports, whose symbols no longer exist (otherwise the build breaks): `com.ivy.navigation.AttributionsScreen`, `com.ivy.navigation.ContributorsScreen`, `com.ivy.navigation.ReleasesScreen`.
- Delete these imports, which are now unused (from both Task 3 and this step): `com.ivy.legacy.Constants`, `androidx.compose.ui.platform.LocalUriHandler`, `androidx.compose.ui.text.style.TextAlign`, `com.ivy.navigation.Navigation`, `com.ivy.wallet.ui.theme.Blue`, `com.ivy.wallet.ui.theme.GradientIvy`, `com.ivy.wallet.ui.theme.Red3`, `com.ivy.wallet.ui.theme.MediumBlack`. Keep `border` and `clickable`, which are still used.

- [x] **Step 4: Home more-menu "open source" card**

`HomeMoreMenu.kt`: in `Content(...)`, delete `Spacer(Modifier.height(16.dp))` and the `OpenSource()` call below it, keeping the final `Spacer(Modifier.weight(1f))`. Delete the whole `private fun ColumnScope.OpenSource() { ... }` function. Then delete these imports, which are now unused: `androidx.compose.ui.platform.LocalUriHandler`, `com.ivy.legacy.Constants`, `com.ivy.legacy.utils.openUrl`, `com.ivy.wallet.ui.theme.Blue`.

- [ ] **Step 5: Verify**

Run: `scripts/verifyOffline.sh`
Expected: `ok` for `firebase`, `play-core` and `github-api`. `jitpack`, `ci-wiring` and `removed-paths` still FAIL. `removed-paths` now lists only `ci-actions fastlane Gemfile Gemfile.lock buildSrc/.../ivy.script.gradle.kts`.

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Run: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -c "io.coil-kt"`
Expected: `0`.

- [x] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove GitHub-API screens, attributions and project links"
```

---

### Task 5: Remove CI, release automation and JitPack

**Files:**
- Delete: `ci-actions/`, `fastlane/`, `Gemfile`, `Gemfile.lock`, `buildSrc/src/main/kotlin/ivy.script.gradle.kts`, `scripts/composeStability.sh`, `scripts/composeStabilityBaseline.sh`
- Modify: `settings.gradle.kts`, `buildSrc/build.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `buildSrc/src/main/kotlin/ivy.detekt.gradle.kts`, `config/detekt/config.yml`

**Interfaces:**
- Consumes: Task 4 result
- Produces: a build with no GitHub/Play automation that resolves only from Google Maven, Maven Central and the Gradle Plugin Portal. `scripts/verifyOffline.sh` exits 0.

- [x] **Step 1: Delete**

```bash
rm -rf ci-actions fastlane Gemfile Gemfile.lock \
  buildSrc/src/main/kotlin/ivy.script.gradle.kts \
  scripts/composeStability.sh scripts/composeStabilityBaseline.sh
```

`settings.gradle.kts`: delete the five `include(":ci-actions:...")` lines and `        maven(url = "https://jitpack.io")`.

`buildSrc/build.gradle.kts`: delete `    maven(url = "https://jitpack.io")`.

- [x] **Step 2: Drop the wrapper-upgrade bot plugin**

`build.gradle.kts`: delete `    alias(libs.plugins.gradleWrapperUpgrade)` and the whole `wrapperUpgrade { ... }` block at the end.

`gradle/libs.versions.toml`, under `[plugins]`: delete:

```toml
gradleWrapperUpgrade = { id = "org.gradle.wrapper-upgrade", version = "0.11.4" }
```

- [x] **Step 3: Drop the JitPack-hosted detekt ruleset**

`gradle/libs.versions.toml`: delete:

```toml
detekt-ruleset-ivy-explicit = { module = "com.github.Ivy-Apps:detekt-explicit", version = "v0.0.8" }
```

`buildSrc/src/main/kotlin/ivy.detekt.gradle.kts`: delete `    detektPlugins(libs.detekt.ruleset.ivy.explicit)`.

`config/detekt/config.yml`: delete the trailing `IvyExplicit:` block, from that line to the end of the file. Detekt rejects config for a rule set that isn't loaded.

- [ ] **Step 4: Verify**

Run: `scripts/verifyOffline.sh; echo "exit=$?"`
Expected: all six labels `ok`, `exit=0`.

Run: `./gradlew :app:assembleDebug testDebugUnitTest --continue`
Expected: `BUILD SUCCESSFUL`, or only the failures recorded in Task 1, minus the ones from deleted modules.

Optional: `./gradlew detekt` now runs without JitPack.

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: remove CI, fastlane, repo-bot plugin and JitPack"
```

---

### Task 6: Pin offline exchange-rate behaviour and harden the manifest

`SyncExchangeRatesUseCase.sync` calls `repository.fetchEurExchangeRates().bind()` first, so a network error returns `Left` before anything touches the DB. Every caller (`MainViewModel`, `HomeViewModel`, `SettingsViewModel`, `ExchangeRatesViewModel`, `OnboardingRouter`) ignores the `Left`. This task pins that behaviour with a JVM test so later edits can't break offline use. It also drops permissions the app never uses.

**Files:**
- Create: `shared/domain/src/test/java/com/ivy/domain/usecase/exchange/SyncExchangeRatesOfflineTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `SyncExchangeRatesUseCase(repository: ExchangeRatesRepository)` with `suspend fun sync(baseCurrency: AssetCode): Either<String, Unit>`, and `ExchangeRatesRepository` (a final class, which MockK can mock) with `fetchEurExchangeRates()`, `findAllManuallyOverridden()`, `saveManyRates(List<ExchangeRate>)` and `deleteAll()`. Also `AssetCode.USD`.
- Produces: nothing new for other tasks

- [x] **Step 1: Write the test**

This follows the `shared/domain` test style: JUnit4 `@Test`, Kotest matchers, MockK.

```kotlin
package com.ivy.domain.usecase.exchange

import arrow.core.left
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.repository.ExchangeRatesRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncExchangeRatesOfflineTest {
    private val repository = mockk<ExchangeRatesRepository>(relaxed = true)
    private val useCase = SyncExchangeRatesUseCase(repository)

    @Test
    fun `sync - network failure leaves stored rates untouched`() = runTest {
        // given
        val offlineError = "Unable to resolve host \"currency-api.pages.dev\""
        coEvery { repository.fetchEurExchangeRates() } returns offlineError.left()

        // when
        val result = useCase.sync(AssetCode.USD)

        // then
        result.shouldBeLeft() shouldBe offlineError
        coVerify(exactly = 0) { repository.saveManyRates(any()) }
        coVerify(exactly = 0) { repository.deleteAll() }
        coVerify(exactly = 0) { repository.findAllManuallyOverridden() }
    }
}
```

- [ ] **Step 2: Run it**

Run: `./gradlew :shared:domain:testDebugUnitTest --tests "com.ivy.domain.usecase.exchange.SyncExchangeRatesOfflineTest"`
Expected: PASS. This pins behaviour the code already has. To confirm the test can fail, temporarily add `repository.deleteAll()` as the first line inside `sync`'s `either { }` block, re-run, and see FAIL (`Verification failed: call ... deleteAll() ... was called`). Then revert that line.

- [x] **Step 3: Manifest**

`app/src/main/AndroidManifest.xml`: delete:

```xml
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

No Kotlin code reads location: `grep -rn "LOCATION" --include=*.kt .` returns nothing. Also delete the attribute line `android:usesCleartextTraffic="true"`, since all four exchange-rate URLs are `https://`. Keep `INTERNET` and `ACCESS_NETWORK_STATE`.

- [ ] **Step 4: Verify**

Run: `./gradlew :app:assembleDebug :shared:domain:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "test: pin offline exchange-rate sync; drop unused permissions"
```

---

### Task 7: Own app identity, release signing and device QA

You can't sign as the Play-signed `com.ivy.wallet`, because Android rejects an update whose signature differs. A distinct `applicationId` installs next to the Play version, so data can move across with a backup zip. `allowBackup="false"` means Android's cloud backup never covered this app, so the in-app zip is the only way to move data.

Changing the id is safe. The only hardcoded `com.ivy.wallet` strings are widget intent-action names and the "import from Ivy" entry in `ImportType.appId()`, and neither is compared with the package name. The `androidx-startup` provider already uses `${applicationId}`.

**Files:**
- Modify: `app/build.gradle.kts`
- Create (not committed; already in `.gitignore` from Task 1): `keystore.properties`, `release.jks`

**Interfaces:**
- Consumes: Tasks 3–6
- Produces: `app/build/outputs/apk/release/app-release.apk` with `applicationId = "com.ivy.wallet.offline"`. Debug builds get `com.ivy.wallet.offline.debug`.

- [ ] **Step 1: Keystore (one-time)**

Keep a backup copy outside the repo. If you lose it, you can't ship updates over an installed copy.

```bash
keytool -genkeypair -v -keystore release.jks -alias ivy-offline \
  -keyalg RSA -keysize 4096 -validity 10000
```

`keystore.properties`:

```properties
storeFile=release.jks
storePassword=<the password you typed>
keyAlias=ivy-offline
keyPassword=<the password you typed>
```

- [x] **Step 2: Wire signing and the new id**

`app/build.gradle.kts`: add `import java.util.Properties` as the first line of the file. A fully qualified `java.util.Properties` doesn't work in a build script, because `java` resolves to the project's `java` extension there. Then add this above `android {`:

```kotlin
// Release signing comes from an untracked keystore.properties at the repo root.
val keystoreProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) propsFile.inputStream().use { load(it) }
}
```

Inside `defaultConfig`, change `applicationId = "com.ivy.wallet"` to:

```kotlin
        applicationId = "com.ivy.wallet.offline"
```

Replace the `create("release") { ... }` signing block, which points at a `sign.jks` that isn't in the repo and at CI environment variables, with:

```kotlin
        create("release") {
            storeFile = keystoreProps.getProperty("storeFile")?.let { rootProject.file(it) }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
```

- [ ] **Step 3: Build and inspect the release APK**

Run: `./gradlew :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`, and `app/build/outputs/apk/release/app-release.apk` exists.

Run: `"$(ls -d ~/Android/Sdk/build-tools/* | tail -1)/apksigner" verify --print-certs app/build/outputs/apk/release/app-release.apk | head -3`
Expected: the signer DN you entered in Step 1.

Run: `unzip -l app/build/outputs/apk/release/app-release.apk | grep -c '\.so$'`
Expected: `0`. With no native libraries, Android 15+ devices that use 16 KB memory pages need no special alignment.

- [ ] **Step 4: Device QA (release APK, phone connected with USB debugging)**

```bash
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

Check each item and note pass/fail:
- **Airplane-mode first launch:** turn on airplane mode → open the app → disclaimer → onboarding → pick a non-EUR currency → Home loads with no crash. Settings → Exchange rates shows an empty list. Add a manual rate → it appears.
- Turn airplane mode off → reopen the app → Exchange rates fills in automatically, and the manual override stays.
- Turn airplane mode back on → restart the app → rates are still listed, not wiped.
- Known edge, not a bug to fix here: removing a manual override *while offline* deletes it, and that currency pair has no rate until the next successful sync (`ExchangeRatesViewModel.handleRemoveOverride`).
- Add an income, an expense and a transfer. The Home balance updates. The planned payment, budget, loan, report and search screens open.
- Settings → Lock app ON → background the app for over 60 s → the biometric/PIN prompt appears and unlocks.
- Long-press the launcher → add the "Add transaction" and "Wallet balance" widgets → both render and open the app. The quick-settings tile opens the app.
- Settings → Backup data → zip saved. Settings → Export to CSV → file saved.
- Home journey cards render, and tapping each one doesn't crash. The more-menu shows two rows (Settings/Categories/Theme/Planned and Reports/Budgets/Loans) and no GitHub card.
- Settings shows Import/Export, App settings and Danger zone, with no "Other" or "Product" section. Tapping the version text does nothing.

- [ ] **Step 5: Migrate data (only if the Play version holds real data)**

On the Play version: Settings → Backup data → save the zip.
On `com.ivy.wallet.offline`: onboarding → Import → Ivy Wallet → pick the zip → check that transaction counts and account balances match. Only then uninstall the Play version. While both are installed, launcher shortcuts (an implicit `ivy.wallet.intent.action.add_transaction` intent) may ask which app to open. That stops once one is uninstalled.

- [x] **Step 6: Commit**

```bash
git add app/build.gradle.kts
git commit -m "build: own applicationId and local release signing"
```

---

## Optional follow-ups (not needed for "bare minimum")

- Remove `feature/disclaimer`, a local legal gate on first launch that only matters for public distribution. Its `OpenSourceCard` in `shared/ui/core` goes with it.
- Remove the tap-to-open links to GitHub, T&C and privacy policy in `OnboardingSplashLogin.kt`.
- Remove the dead "delete cloud data" path: `SettingsEvent.DeleteCloudUserData`, the `deleteCloudDataModalVisible` modal (nothing sets it to `true`) and `LogoutLogic.cloudLogout()`.
- Delete or implement the `TODO("Not implemented")` stubs in `shared/domain`: `ExchangeUseCase`, `WalletBalanceUseCase`, `WalletStatsUseCase`, `AccountStatsUseCase`, `AccountBalanceUseCase` and `CategoryStatsUseCase`. Nothing calls them today, but injecting one into new code crashes at runtime. The working equivalents live in `temp/legacy-code` (for example `com.ivy.wallet.domain.pure.exchange`).
- Drop the `demo` build type. It uses the same `.debug` suffix as `debug`, so the two overwrite each other.
- Regenerate `all_modules.gv` and `docs/assets/modules-graph.svg` (`scripts/generateModulesGraph.sh`), and prune now-unused strings and drawables with lint `UnusedResources`.
- Give the Ktor client an `HttpTimeout`. On a slow (not absent) network, changing the base currency on Home waits for up to four sequential fetch attempts before reloading.

## Separate plan: toolchain upgrade (only when you need a newer library)

Frozen versions keep building as long as Google Maven, Maven Central and the Gradle Plugin Portal serve them, which they do indefinitely. Upgrade only when something forces it. Do it in this order, one PR each:

1. Gradle 8.14+ (allows JDK 23+) → AGP 8.13+ → Kotlin 2.2+ with matching KSP and the Compose compiler plugin. Context receivers are no blocker any more, because only `ci-actions` used them.
2. `compileSdk` 35/36. Newer AndroidX and Compose releases require it.
3. `targetSdk` 35, which **enforces edge-to-edge**. `RootActivity` already calls `enableEdgeToEdge()`, but check insets on every legacy screen.
4. `targetSdk` 36. **`onBackPressed()` stops being called**, and `RootActivity` routes all in-app back navigation through it. Move that to `OnBackPressedDispatcher`/`BackHandler` first, or back will exit the app from every screen.
5. Library majors: Arrow 2 (removes `arrow.core.computations`, used in `legacy/domain/pure/exchange/Exchange.kt`), Ktor 3, Compose BOM.
