package com.hoods.composestateyt.ui.transaction

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsScreenStateTest {

    @Test
    fun filterSheetVisibility_updatesStateDirectly() {
        val testScope = TestScope(UnconfinedTestDispatcher())
        val screenState = TransactionsScreenState(coroutineScope = testScope)

        assertFalse(screenState.isFilterSheetOpen)

        screenState.openFilterSheet()
        assertTrue(screenState.isFilterSheetOpen)

        screenState.closeFilterSheet()
        assertFalse(screenState.isFilterSheetOpen)
    }

    @Test
    fun detailDialogSelection_togglesVisibilityAndId() {
        val testScope = TestScope(UnconfinedTestDispatcher())
        val screenState = TransactionsScreenState(coroutineScope = testScope)

        assertNull(screenState.selectedTransactionId)
        assertFalse(screenState.isDetailDialogOpen)

        screenState.selectTransaction(101)
        assertEquals(101, screenState.selectedTransactionId)
        assertTrue(screenState.isDetailDialogOpen)

        screenState.dismissDetailDialog()
        assertNull(screenState.selectedTransactionId)
        assertFalse(screenState.isDetailDialogOpen)
    }
}
