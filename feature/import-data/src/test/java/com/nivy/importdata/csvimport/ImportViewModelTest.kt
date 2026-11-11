package com.nivy.importdata.csvimport

import android.content.Context
import android.net.Uri
import android.os.Looper
import com.nivy.data.backup.BackupDataUseCase
import com.nivy.data.backup.ImportResult
import com.nivy.domain.RootScreen
import com.nivy.navigation.ImportScreen
import com.nivy.navigation.Navigation
import com.nivy.onboarding.viewmodel.OnboardingViewModel
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    private val backup = mockk<Uri>()
    private val restored = ImportResult(
        rowsFound = 10,
        transactionsImported = 10,
        accountsImported = 2,
        categoriesImported = 3,
        failedRows = persistentListOf(),
    )
    private val backupDataUseCase = mockk<BackupDataUseCase> {
        coEvery { importBackupFile(backup, any()) } returns restored
    }
    private val nav = mockk<Navigation>(relaxed = true)
    private val onboarding = mockk<OnboardingViewModel>(relaxed = true)

    // The screen that picks the file: it hands the view model the backup at once.
    private val screen = mockk<Context>(moreInterfaces = arrayOf(RootScreen::class)) {
        every { (this@mockk as RootScreen).openFile(any()) } answers { firstArg<(Uri) -> Unit>()(backup) }
    }

    private val viewModel = ImportViewModel(nav = nav, backupDataUseCase = backupDataUseCase)

    @Before
    fun setUp() {
        // LiveData takes its values on the main thread only: here that's the test's.
        mockkStatic(Looper::class)
        every { Looper.getMainLooper() } returns mockk { every { thread } returns Thread.currentThread() }
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Looper::class)
    }

    @Test
    fun `a picked backup is restored, and its result shown`() {
        viewModel.uploadFile(screen)

        viewModel.importStep.value shouldBe ImportStep.RESULT
        viewModel.importResult.value shouldBe restored
    }

    @Test
    fun `finishing a restore started from onboarding tells it the wallet came in, and leaves the screen`() {
        viewModel.uploadFile(screen)

        viewModel.finish(ImportScreen(launchedFromOnboarding = true), onboarding)

        verify(exactly = 1) { onboarding.importFinished(success = true) }
        verify(exactly = 1) { nav.back() }
        viewModel.importStep.value shouldBe ImportStep.IMPORT_FROM
    }

    @Test
    fun `a restore that brought in less than half the rows isn't a success for onboarding`() {
        // 1 of 10 rows: the result page says it failed
        coEvery { backupDataUseCase.importBackupFile(backup, any()) } returns restored.copy(transactionsImported = 1)
        viewModel.uploadFile(screen)

        viewModel.finish(ImportScreen(launchedFromOnboarding = true), onboarding)

        verify(exactly = 1) { onboarding.importFinished(success = false) }
    }

    @Test
    fun `finishing a restore started from the settings leaves the screen, and tells onboarding nothing`() {
        viewModel.uploadFile(screen)

        viewModel.finish(ImportScreen(launchedFromOnboarding = false), onboarding)

        verify(exactly = 0) { onboarding.importFinished(any()) }
        verify(exactly = 1) { nav.back() }
    }

    @Test
    fun `skipping from onboarding moves it on without a restore`() {
        viewModel.skip(ImportScreen(launchedFromOnboarding = true), onboarding)

        verify(exactly = 1) { onboarding.importSkip() }
        verify(exactly = 0) { onboarding.importFinished(any()) }
        verify(exactly = 1) { nav.back() }
    }
}
