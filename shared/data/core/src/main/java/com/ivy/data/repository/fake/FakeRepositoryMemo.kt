package com.ivy.data.repository.fake

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import kotlinx.coroutines.CoroutineScope
import com.ivy.data.repository.RepositoryMemoFactory
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
fun fakeRepositoryMemoFactory(
    dataObserver: DataObserver = DataObserver(),
): RepositoryMemoFactory = RepositoryMemoFactory(
    dataObserver = dataObserver,
    dispatchers = TestDispatchersProvider,
    appScope = CoroutineScope(TestDispatchersProvider.main),
)
