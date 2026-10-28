# Offline Standalone Ivy Wallet Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the archived Ivy Wallet repo into a sideloaded, offline-first personal app with no Firebase, Google Play, GitHub API or CI dependencies. The only network call left is the exchange-rate fetch.

**Architecture:** Delete the modules that only exist to talk to outside services (`feature:poll`, `feature:contributors`, `feature:releases`, `feature:attributions`, `ci-actions:*`). Strip Firebase and Play Core from the build. Cut the UI entry points that pointed at them. Keep everything that stores data locally (Room, DataStore, SharedPrefs, WorkManager reminders, widgets) exactly as it is, so existing backups still import. A grep-based guard script is the "test" for each removal.

**Tech Stack:** Kotlin 2.0.20, AGP 8.5.2, Gradle 8.8, Jetpack Compose 1.6.8, Hilt 2.52, Room 2.6.1, Ktor 2.3.12 (exchange rates only), Arrow 1.2.4, Kotest + MockK.

**Spec:** No separate spec. These requirements come from the user:
1. Bare minimum needed to build and run. Firebase, external connections and store integration are not needed.
2. The exchange-rate API is the one network feature to keep.
3. Everything else must work offline.
4. Distributed as a standalone APK, not through the Play Store.

## Global Constraints

- JDK: run Gradle with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`. The system default is JDK 25, which Gradle 8.8 cannot run on. Every `./gradlew` command below is written with this prefix.
- Android SDK: `/home/matt/Android/Sdk`. `android-34` is already installed.
- Do NOT change: the Room schema or migrations, DataStore keys, SharedPrefs keys, or the backup zip format. Existing user data and backups must stay readable.
- Do NOT bump AGP, Kotlin, Compose, `compileSdk` or `targetSdk` (stays 34) in this plan. Upgrades are separate work.
- Keep the `INTERNET` permission, Ktor, `RemoteExchangeRatesDataSourceImpl` and its four fallback URLs.
- Keep the `namespace` (`com.ivy.wallet`). Only `applicationId` changes, in Task 8.
- The repo is not a git repository yet. Task 1 creates it so every later removal can be reverted.

## Review Focus

1. **First launch with no network.** Onboarding finishes, the home screen loads, the exchange-rate list is empty, and adding a manual rate still works. Covered by the Task 7 unit test plus the Task 8 device step "airplane-mode first launch".
2. **Network drops after rates were synced.** Previously stored rates must not be wiped by a failed sync. Covered by the Task 7 unit test `sync - network failure leaves stored rates untouched`.
3. **Moving data from the Play Store install.** A backup zip exported from the Play version imports into the new `applicationId`. Covered by Task 8, step "migrate data".
4. **R8-minified release build crashes at start** because a removed class was still reachable. Covered by Task 8, which smoke-tests the *release* APK and not just debug.
5. **Home screen journey cards, app lock (biometric) and widgets after the removals.** Covered by the Task 8 device checklist.

---

### Task 1: Baseline: git, SDK path, first build

**Files:**
- Create: `local.properties`
- Create: `.git/` (via `git init`)

**Interfaces:**
- Consumes: nothing
- Produces: a committed baseline, and a recorded list of tests that already fail before any change (`/tmp/claude-1000/-home-matt-Documents-GitHub-ivy-wallet-main/27e36e21-271c-45f2-ae3e-01b02492d5d5/scratchpad/baseline-test-failures.txt` or any local notes file)

- [ ] **Step 1: Init git and commit untouched tree**

```bash
cd /home/matt/Documents/GitHub/ivy-wallet-main
git init
git add -A
git commit -m "chore: import ivy-wallet archived snapshot"
```

- [ ] **Step 2: Point Gradle at the SDK**

`local.properties` (already ignored by `.gitignore`):

```properties
sdk.dir=/home/matt/Android/Sdk
```

- [ ] **Step 3: Build the debug APK**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`, APK at `app/build/outputs/apk/debug/app-debug.apk`.
If it fails, stop and report the error. Every later task assumes this baseline builds.

- [ ] **Step 4: Run unit tests and record pre-existing failures**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew testDebugUnitTest --continue 2>&1 | tee /tmp/claude-1000/-home-matt-Documents-GitHub-ivy-wallet-main/27e36e21-271c-45f2-ae3e-01b02492d5d5/scratchpad/baseline-tests.log`
Expected: mostly PASS. Copy any `FAILED` test names into your notes. Later tasks only need "no *new* failures".

- [ ] **Step 5: Commit**

Nothing new is tracked (`local.properties` is gitignored), so there is nothing to commit. Move on.

---

### Task 2: Offline guard script (the failing test)

**Files:**
- Create: `scripts/verifyOffline.sh`

**Interfaces:**
- Consumes: nothing
- Produces: `scripts/verifyOffline.sh`. It exits 0 when clean and 1 when any check fails, printing one `ok   [label]` or `FAIL [label]` line per check. Labels: `firebase`, `play-core`, `github-api`, `poll-module`, `ci-actions`, `google-services.json`.

- [ ] **Step 1: Write the script**

```bash
#!/bin/bash
# Fails if any Firebase / Google Play / GitHub API / CI wiring is still in the build.
# The exchange-rate API (currency-api) is the only network dependency allowed.
set -u
cd "$(dirname "$0")/.." || exit 1

SRC_DIRS="app buildSrc feature shared temp widget gradle settings.gradle.kts build.gradle.kts"
fail=0

check() {
    local label="$1" pattern="$2"
    local hits
    # shellcheck disable=SC2086
    hits=$(grep -rnIiE "$pattern" $SRC_DIRS \
        --include='*.kt' --include='*.kts' --include='*.toml' --include='*.xml' --include='*.json' \
        2>/dev/null | grep -v '/build/')
    if [ -n "$hits" ]; then
        echo "FAIL [$label]"
        echo "$hits" | head -5 | sed 's/^/       /'
        fail=1
    else
        echo "ok   [$label]"
    fi
}

check "firebase"    'firebase|crashlytics|google-services|google\.services|com\.google\.gms'
check "play-core"   'com\.google\.android\.play|play-services'
check "github-api"  'api\.github\.com'
check "poll-module" 'feature:poll|feature\.poll|com\.ivy\.poll'
check "ci-actions"  'ci-actions'

if [ -e app/google-services.json ]; then
    echo "FAIL [google-services.json]"
    fail=1
else
    echo "ok   [google-services.json]"
fi

exit $fail
```

- [ ] **Step 2: Run it to verify it fails**

Run: `chmod +x scripts/verifyOffline.sh && scripts/verifyOffline.sh; echo "exit=$?"`
Expected: every label prints `FAIL`, and `exit=1`.

- [ ] **Step 3: Commit**

```bash
git add scripts/verifyOffline.sh
git commit -m "test: add offline guard script"
```

---

### Task 3: Remove the poll feature (Firestore)

The poll writes votes to Ivy's Firestore project, and its home card expired on 2025-07-28. Remove it first. It is the only Firestore user, so it has to go before the google-services plugin in Task 4.

**Files:**
- Delete: `feature/poll/` (both `impl` and `public`)
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `feature/home/build.gradle.kts`
- Modify: `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourneyCardModel.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourneyCardsProvider.kt`
- Modify: `app/src/main/java/com/ivy/IvyNavGraph.kt`
- Modify: `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt`

**Interfaces:**
- Consumes: `scripts/verifyOffline.sh` (Task 2)
- Produces: `CustomerJourneyDeps(timeProvider: TimeProvider)` with a single field. `CustomerJourneyCardsProvider` no longer takes `pollRepository`. `Screen` no longer has `PollScreen`.

- [ ] **Step 1: Delete module and includes**

```bash
rm -rf feature/poll
```

`settings.gradle.kts`: delete these two lines:

```kotlin
include(":feature:poll:impl")
include(":feature:poll:public")
```

`app/build.gradle.kts`: delete all three of these lines (`poll.impl` appears twice):

```kotlin
    implementation(projects.feature.poll.impl)
    implementation(projects.feature.poll.public)
```

`feature/home/build.gradle.kts`: delete:

```kotlin
    implementation(projects.feature.poll.public)
```

- [ ] **Step 2: Drop `pollRepository` from the card deps**

`CustomerJourneyCardModel.kt`: remove `import com.ivy.poll.data.PollRepository` and change the deps class to:

```kotlin
@Immutable
data class CustomerJourneyDeps(
    val timeProvider: TimeProvider,
)
```

- [ ] **Step 3: Remove the vote card from the provider**

`CustomerJourneyCardsProvider.kt`:
- Remove these imports: `com.ivy.navigation.Navigation`, `com.ivy.navigation.PollScreen`, `com.ivy.poll.data.PollRepository`, `com.ivy.poll.data.model.PollId`, `java.time.LocalDate`.
- Remove the constructor parameter `private val pollRepository: PollRepository,`.
- Replace the deps construction with:

```kotlin
    val deps = CustomerJourneyDeps(
      timeProvider = timeProvider,
    )
```

- Change `ACTIVE_CARDS` to:

```kotlin
    val ACTIVE_CARDS = listOf(
      adjustBalanceCard(),
      addPlannedPaymentCard(),
      didYouKnow_pinAddTransactionWidgetCard(),
      didYouKnow_expensesPieChart(),
    )
```

- Delete the whole `private fun voteCard() = CustomerJourneyCardModel(...)` function, including its `@Suppress(...)` annotation line.

- [ ] **Step 4: Remove the nav route**

`IvyNavGraph.kt`: delete `import com.ivy.navigation.PollScreen`, `import com.ivy.poll.impl.ui.PollScreen`, and the branch line `PollScreen -> PollScreen()`.

`Screens.kt`: delete:

```kotlin
data object PollScreen : Screen
```

- [ ] **Step 5: Verify**

Run: `scripts/verifyOffline.sh`
Expected: `ok   [poll-module]` (other labels still FAIL).

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove Firestore poll feature"
```

---

### Task 4: Remove Firebase Crashlytics, google-services and Play Core

Crashlytics sends crash reports to the original developers' Firebase project. Play Review only works for Play installs. "Share" sends a Play Store link.

**Files:**
- Delete: `app/google-services.json`
- Delete: `temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt`
- Modify: `gradle/libs.versions.toml`
- Modify: `buildSrc/build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `temp/legacy-code/build.gradle.kts`
- Modify: `shared/domain/src/main/java/com/ivy/domain/RootScreen.kt`
- Modify: `app/src/main/java/com/ivy/wallet/RootActivity.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/customerjourney/CustomerJourneyCardsProvider.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: Task 3 result (no Firestore users left)
- Produces: `RootScreen` without `reviewIvyWallet` and `shareIvyWallet`. It keeps `openUrlInBrowser`, `shareCSVFile`, `shareZipFile`, `openGooglePlayAppPage`, `pinWidget` and the build-info vals.

- [ ] **Step 1: Version catalog**

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
```

Replace the `kotlin-android` bundle with:

```toml
kotlin-android = [
    "kotlin-coroutines-android",
]
```

Delete the whole `google = [...]` and `firebase = [...]` bundles.

- [ ] **Step 2: Gradle scripts**

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
```

`temp/legacy-code/build.gradle.kts`: delete:

```kotlin
    implementation(libs.bundles.firebase)
```

Then:

```bash
rm app/google-services.json temp/legacy-code/src/main/java/com/ivy/legacy/utils/FirebaseExt.kt
```

- [ ] **Step 3: `RootScreen` interface**

`RootScreen.kt`: delete these two declarations and the blank line after each:

```kotlin
    fun reviewIvyWallet(dismissReviewCard: Boolean)

    fun shareIvyWallet()
```

- [ ] **Step 4: `RootActivity`**

- Delete these imports: `com.google.android.play.core.review.ReviewManagerFactory`, `com.ivy.home.customerjourney.CustomerJourneyCardsProvider`, `com.ivy.legacy.Constants`, `com.ivy.legacy.utils.sendToCrashlytics`.
- Delete the field:

```kotlin
    @Inject
    lateinit var customerJourneyLogic: CustomerJourneyCardsProvider
```

- In `openUrlInBrowser`, delete the line `e.sendToCrashlytics("Cannot open URL in browser, intent not supported.")`. Keep `e.printStackTrace()` and the Toast.
- Delete the whole `override fun shareIvyWallet() { ... }` function.
- Delete the whole `override fun reviewIvyWallet(dismissReviewCard: Boolean) { ... }` function.

- [ ] **Step 5: Rate-us card**

`CustomerJourneyCardsProvider.kt`: delete the whole `fun rateUsCard() = CustomerJourneyCardModel(...)` function. It is not in `ACTIVE_CARDS`. Its only caller was `RootActivity.reviewIvyWallet`, which Step 4 removed.

- [ ] **Step 6: Home more-menu share button**

`HomeMoreMenu.kt`, in the "Second Row": delete this block together with the `Spacer` that follows it:

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

If `import com.ivy.legacy.rootScreen` is now unused (`grep -c "rootScreen" feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt` returns 1), delete it.

- [ ] **Step 7: Settings "Other" section**

`SettingsScreen.kt`: delete the whole `item { ... }` that starts with `SettingsSectionDivider(text = stringResource(R.string.other))`. It contains three buttons: "Rate us", "Share" and "open source". Then drop the imports that are now unused:

```bash
f=feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt
for s in GradientIvy Red3 MediumBlack; do echo "$s $(grep -cw "$s" $f)"; done
```

Delete the `import com.ivy.wallet.ui.theme.<Symbol>` line for each symbol whose count is `1`.

- [ ] **Step 8: Verify**

Run: `scripts/verifyOffline.sh`
Expected: `ok` for `firebase`, `play-core`, `poll-module` and `google-services.json`. `github-api` and `ci-actions` still FAIL.

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -iE "firebase|com.google.android.play|play-services"`
Expected: no output.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: remove Firebase Crashlytics and Play Core"
```

---

### Task 5: Remove GitHub-API screens and outbound project links

`contributors` and `releases` call `api.github.com`. `contributors` also pulls in Coil to load avatars. `attributions` and the Settings "Product" section are only links to Ivy's Telegram, GitHub and help center.

**Files:**
- Delete: `feature/contributors/`, `feature/releases/`, `feature/attributions/`
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/src/main/java/com/ivy/IvyNavGraph.kt`
- Modify: `shared/ui/navigation/src/main/java/com/ivy/navigation/Screens.kt`
- Modify: `feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt`
- Modify: `feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt`

**Interfaces:**
- Consumes: Task 4 result
- Produces: `Screen` no longer has `AttributionsScreen`, `ContributorsScreen` or `ReleasesScreen`. The `compose` bundle no longer contains `compose-coil`.

- [ ] **Step 1: Delete modules and wiring**

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

`gradle/libs.versions.toml`: delete the library line `compose-coil = { module = "io.coil-kt:coil-compose", version = "2.7.0" }` and the `"compose-coil",` entry in the `compose` bundle.

- [ ] **Step 2: Nav graph and screens**

`IvyNavGraph.kt`: delete these imports: `com.ivy.attributions.AttributionsScreenImpl`, `com.ivy.contributors.ContributorsScreenImpl`, `com.ivy.releases.ReleasesScreenImpl`, `com.ivy.navigation.AttributionsScreen`, `com.ivy.navigation.ContributorsScreen`, `com.ivy.navigation.ReleasesScreen`. Delete these branches:

```kotlin
        AttributionsScreen -> AttributionsScreenImpl()
        ContributorsScreen -> ContributorsScreenImpl()
        ReleasesScreen -> ReleasesScreenImpl()
```

`Screens.kt`: delete:

```kotlin
data object AttributionsScreen : Screen

data object ContributorsScreen : Screen

data object ReleasesScreen : Screen
```

- [ ] **Step 3: Settings screen**

`SettingsScreen.kt`:
- In the toolbar version `Text`, delete the `modifier = Modifier.clickable { nav.navigateTo(ReleasesScreen) },` argument. Keep the text.
- Delete the whole `item { ... }` that starts with `SettingsSectionDivider(text = stringResource(R.string.product))`, ending after `TCAndPrivacyPolicy()`.
- Delete these private composables: `IvyTelegram`, `HelpCenter`, `ReportBug`, `RequestFeature`, `ContactSupport`, `Releases`, `Contributors`, `Attributions`, `TCAndPrivacyPolicy`.
- Remove the imports that are now unused:

```bash
f=feature/settings/src/main/java/com/ivy/settings/SettingsScreen.kt
for s in Constants LocalUriHandler ReleasesScreen ContributorsScreen AttributionsScreen Navigation Blue TextAlign border; do echo "$s $(grep -cw "$s" $f)"; done
```

Delete the import line for each symbol whose count is `1`. Keep any symbol with a count above 1.

- [ ] **Step 4: Home more-menu open-source card**

`HomeMoreMenu.kt`: delete the call `OpenSource()` and the `Spacer(Modifier.height(16.dp))` directly above it. Delete the whole `private fun ColumnScope.OpenSource() { ... }` function. Then check for imports that are now unused:

```bash
f=feature/home/src/main/java/com/ivy/home/HomeMoreMenu.kt
for s in Constants LocalUriHandler openUrl Blue; do echo "$s $(grep -cw "$s" $f)"; done
```

Delete the import line for each symbol whose count is `1`.

- [ ] **Step 5: Verify**

Run: `scripts/verifyOffline.sh`
Expected: every label `ok` except `ci-actions`.

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: remove GitHub-API screens and project links"
```

---

### Task 6: Remove CI, release automation and repo-bot infrastructure

**Files:**
- Delete: `ci-actions/`, `.github/`, `fastlane/`, `Gemfile`, `Gemfile.lock`, `buildSrc/src/main/kotlin/ivy.script.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: Task 5 result
- Produces: a build with no GitHub/Play automation. `scripts/verifyOffline.sh` exits 0.

- [ ] **Step 1: Delete**

```bash
rm -rf ci-actions .github fastlane Gemfile Gemfile.lock buildSrc/src/main/kotlin/ivy.script.gradle.kts
```

`settings.gradle.kts`: delete:

```kotlin
include(":ci-actions:base")
include(":ci-actions:compose-stability")
include(":ci-actions:issue-assign")
include(":ci-actions:issue-create-comment")
include(":ci-actions:pr-description-check")
```

- [ ] **Step 2: Drop the wrapper-upgrade bot plugin**

`build.gradle.kts`: delete `alias(libs.plugins.gradleWrapperUpgrade)` and the whole `wrapperUpgrade { ... }` block.

`gradle/libs.versions.toml`, under `[plugins]`: delete:

```toml
gradleWrapperUpgrade = { id = "org.gradle.wrapper-upgrade", version = "0.11.4" }
```

- [ ] **Step 3: Verify**

Run: `scripts/verifyOffline.sh; echo "exit=$?"`
Expected: all six labels `ok`, `exit=0`.

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug testDebugUnitTest --continue`
Expected: `BUILD SUCCESSFUL`, or only the failures already recorded in Task 1.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: remove CI, fastlane and repo-bot infrastructure"
```

---

### Task 7: Pin offline exchange-rate behaviour and harden the manifest

`SyncExchangeRatesUseCase` already returns `Left` on a network error without touching the DB. This task pins that behaviour with a JVM test so later edits can't break offline use. It also drops permissions the app never uses.

**Files:**
- Create: `shared/domain/src/test/java/com/ivy/domain/usecase/exchange/SyncExchangeRatesOfflineTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `SyncExchangeRatesUseCase(repository: ExchangeRatesRepository)` with `suspend fun sync(baseCurrency: AssetCode): Either<String, Unit>`. Consumes `ExchangeRatesRepository.fetchEurExchangeRates(): Either<String, List<ExchangeRate>>`, `saveManyRates(List<ExchangeRate>)` and `deleteAll()`. Consumes `AssetCode.unsafe(String)`.
- Produces: nothing new for other tasks

- [ ] **Step 1: Write the test**

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
        val result = useCase.sync(AssetCode.unsafe("USD"))

        // then
        result.shouldBeLeft() shouldBe offlineError
        coVerify(exactly = 0) { repository.saveManyRates(any()) }
        coVerify(exactly = 0) { repository.deleteAll() }
        coVerify(exactly = 0) { repository.findAllManuallyOverridden() }
    }
}
```

- [ ] **Step 2: Run it**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :shared:domain:testDebugUnitTest --tests "com.ivy.domain.usecase.exchange.SyncExchangeRatesOfflineTest"`
Expected: PASS. This pins behaviour the code already has. To confirm the test can fail, temporarily add `repository.deleteAll()` as the first line inside `sync`'s `either { }` block, re-run, see `FAIL` (`Verification failed: call ... deleteAll() ... was called`), then revert that line.

- [ ] **Step 3: Manifest**

`app/src/main/AndroidManifest.xml`: delete:

```xml
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

No Kotlin code reads location (`grep -rn "LOCATION" --include=*.kt .` returns nothing). Also delete the attribute line `android:usesCleartextTraffic="true"`. All four exchange-rate URLs are `https://`.

Keep `INTERNET` and `ACCESS_NETWORK_STATE`.

- [ ] **Step 4: Verify**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleDebug :shared:domain:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: pin offline exchange-rate sync; drop unused permissions"
```

---

### Task 8: Own app identity, release signing, device QA

A self-signed APK can't replace the Play-signed `com.ivy.wallet` because the signatures differ. A distinct `applicationId` installs next to the Play version, so data can be moved across with a backup zip.

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `.gitignore`
- Create (not committed): `keystore.properties`, `release.jks`

**Interfaces:**
- Consumes: Tasks 3–7
- Produces: `app/build/outputs/apk/release/app-release.apk` with `applicationId = "com.ivy.wallet.offline"`. Debug builds get `com.ivy.wallet.offline.debug`.

- [ ] **Step 1: Keystore (one-time, keep a backup copy outside the repo; losing it means you can't ship updates over an installed copy)**

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

`.gitignore`: append:

```gitignore
/keystore.properties
/release.jks
```

- [ ] **Step 2: Wire signing and the new id**

`app/build.gradle.kts`: above `android {`, add:

```kotlin
val keystoreProps = java.util.Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
```

Inside `defaultConfig`, change `applicationId = "com.ivy.wallet"` to:

```kotlin
        applicationId = "com.ivy.wallet.offline"
```

Replace the `create("release") { ... }` signing block with:

```kotlin
        create("release") {
            storeFile = keystoreProps.getProperty("storeFile")?.let(rootProject::file)
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
```

- [ ] **Step 3: Build release**

Run: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleRelease`
Expected: `BUILD SUCCESSFUL`, `app/build/outputs/apk/release/app-release.apk` exists.

Run: `~/Android/Sdk/build-tools/36.1.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk | head -3`
Expected: the signer DN you entered in Step 1.

- [ ] **Step 4: Device QA (release APK, phone connected with USB debugging)**

```bash
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

Check each item and note pass/fail:
- **Airplane-mode first launch:** turn on airplane mode → open app → disclaimer → onboarding → pick a non-EUR currency → home loads, no crash. Settings → Exchange rates shows an empty list. Add a manual rate → it appears.
- Turn airplane mode off → reopen the app → Exchange rates fills in automatically. The manual override stays.
- Turn airplane mode back on → restart the app → rates are still listed (not wiped).
- Add income, expense and transfer. Home balance updates. Planned payment, budget and loan screens open.
- Settings → Lock app ON → background the app for over 60 s → biometric/PIN prompt appears and unlocks.
- Long-press launcher → add the "Add transaction" and "Wallet balance" widgets → both render and open the app.
- Settings → Backup data → zip saved. Settings → Export to CSV → file saved.
- Home journey cards render, and tapping each one does not crash.

- [ ] **Step 5: Migrate data (only if the Play version holds real data)**

On the Play version: Settings → Backup data → save the zip.
On `com.ivy.wallet.offline`: onboarding → Import → Ivy Wallet → pick the zip → check that transaction counts and account balances match. Only then uninstall the Play version.

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts .gitignore
git commit -m "build: own applicationId and local release signing"
```

---

## Optional follow-ups (not needed for "bare minimum")

- Remove `feature/disclaimer` (a local legal gate on first launch; only matters for public distribution).
- Remove the onboarding splash links to GitHub, T&C and privacy policy (`OnboardingSplashLogin.kt`). They are tap-to-open only and make no automatic network calls.
- Remove `DeviceIdUseCase` (only the poll used it), the `SettingsEvent.DeleteCloudUserData` dead path, and unused string resources.
- Remove detekt's JitPack-hosted `detekt-explicit` ruleset and its `IvyExplicit:` block in `config/detekt/config.yml`, then the `jitpack.io` repositories, so the build has one fewer third-party host.
- Toolchain upgrade (AGP 8.10+, Kotlin 2.2+, Gradle 8.14+, compileSdk/targetSdk 35+) as its own plan. Raising targetSdk to 35 forces edge-to-edge, so expect UI inset fixes on legacy screens.
