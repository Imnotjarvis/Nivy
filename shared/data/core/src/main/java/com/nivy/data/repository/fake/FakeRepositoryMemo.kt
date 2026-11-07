package com.nivy.data.repository.fake

import com.nivy.base.TestDispatchersProvider
import com.nivy.data.DataObserver
import kotlinx.coroutines.CoroutineScope
import com.nivy.data.repository.RepositoryMemoFactory
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
fun fakeRepositoryMemoFactory(
    dataObserver: DataObserver = DataObserver(),
): RepositoryMemoFactory = RepositoryMemoFactory(
    dataObserver = dataObserver,
    dispatchers = TestDispatchersProvider,
    appScope = CoroutineScope(TestDispatchersProvider.main),
)
