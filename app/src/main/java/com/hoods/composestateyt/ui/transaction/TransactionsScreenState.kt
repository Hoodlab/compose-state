package com.hoods.composestateyt.ui.transaction

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
//Todo Solution 1 state holder for UI states
@Composable
fun rememberTransactionsScreenState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    lazyListState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
): TransactionsScreenState = remember(coroutineScope, lazyListState, snackbarHostState) {
    TransactionsScreenState(coroutineScope, lazyListState, snackbarHostState)
}

class TransactionsScreenState(
    val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState = LazyListState(),
    val snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    var isFilterSheetOpen by mutableStateOf(false)
    var selectedTransactionId by mutableStateOf<Int?>(null)

    val isDetailDialogOpen: Boolean get() = selectedTransactionId != null
    val showScrollToTopButton: Boolean by derivedStateOf { lazyListState.firstVisibleItemIndex > 3 }

    fun openFilterSheet() { isFilterSheetOpen = true }
    fun closeFilterSheet() { isFilterSheetOpen = false }
    fun selectTransaction(id: Int?) { selectedTransactionId = id }
    fun dismissDetailDialog() { selectedTransactionId = null }
    fun scrollToTop() { coroutineScope.launch { lazyListState.animateScrollToItem(0) } }
}
