package com.hoods.composestateyt.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoods.composestateyt.data.Transaction
import kotlin.compareTo

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState = rememberTransactionsScreenState()

    LaunchedEffect(
        uiState.filters.query,
        uiState.filters.selectedCategory,
        uiState.filters.typeFilter,
        uiState.filters.periodFilter
    ) {
        screenState.scrollToTop()
    }
    val currentOnUndoDelete by rememberUpdatedState(viewModel::onUndoDelete)
    val currentOnMessageShown by rememberUpdatedState(viewModel::onMessageShown)

    LaunchedEffect(uiState.userMessage?.id) {
        val message = uiState.userMessage ?: return@LaunchedEffect
        val result = screenState.snackbarHostState.showSnackbar(
            message = message.text,
            actionLabel = if (message.undo != null) "Undo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) currentOnUndoDelete()
        currentOnMessageShown(message.id)
    }

    TransactionsScreen(
        uiState = uiState,
        screenState = screenState,
        onQueryChanged = viewModel::onQueryChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onTypeFilterChanged = viewModel::onTypeFilterChanged,
        onPeriodFilterChanged = viewModel::onPeriodFilterChanged,
        onDeleteTransaction = viewModel::onDeleteTransaction,
        onLoadMore = viewModel::onLoadMore,
        onRetry = viewModel::onRetry,
        onExportClicked = onExport,
        modifier = modifier,
    )
}

//TODO FIX 4: screen reads the whole state
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    screenState: TransactionsScreenState,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onTypeFilterChanged: (String) -> Unit,
    onPeriodFilterChanged: (String) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onExportClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    //TODO FIX 2: two writers for one concept
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Transactions Dashboard") },
                actions = { TextButton(onClick = onExportClicked) { Text("Export") } }
            )
        },
        snackbarHost = { SnackbarHost(screenState.snackbarHostState) },
        floatingActionButton = {
            if (screenState.showScrollToTopButton) {
                FloatingActionButton(onClick = { screenState.scrollToTop() }) {
                    Text("▲", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp)) {
            HeaderSection(state = uiState.header)
            SearchAndFilters(
                state = uiState.filters,
                onQueryChanged = onQueryChanged,
                onOpenFilterSheet = { screenState.openFilterSheet() })
            Spacer(Modifier.height(8.dp))
            TransactionList(
                state = uiState.list,
                lazyListState = screenState.lazyListState,
                onTransactionClicked = { screenState.selectTransaction(it.id) },
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (screenState.isFilterSheetOpen) {
        ModalBottomSheet(onDismissRequest = { screenState.closeFilterSheet() }) {
            Column(Modifier
                .padding(16.dp)
                .fillMaxWidth()) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "All",
                        "Groceries",
                        "Income",
                        "Transport"
                    ).forEach {
                        FilterChip(
                            selected = uiState.filters.selectedCategory == it,
                            onClick = { onCategorySelected(it) },
                            label = { Text(it) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "All",
                        "Income",
                        "Expense"
                    ).forEach {
                        FilterChip(
                            selected = uiState.filters.typeFilter == it,
                            onClick = { onTypeFilterChanged(it) },
                            label = { Text(it) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Period", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "All",
                        "This Week",
                        "This Month"
                    ).forEach {
                        FilterChip(
                            selected = uiState.filters.periodFilter == it,
                            onClick = { onPeriodFilterChanged(it) },
                            label = { Text(it) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { screenState.closeFilterSheet() },
                    Modifier.align(Alignment.End)
                ) { Text("Done") }
            }
        }
    }

    if (screenState.isDetailDialogOpen) {
        val content = uiState.list as? ListState.Content
        val selectedTx = content?.items?.find { it.id == screenState.selectedTransactionId }
        if (selectedTx != null) {
            AlertDialog(
                onDismissRequest = { screenState.dismissDetailDialog() },
                title = { Text(selectedTx.title) },
                text = {
                    Column {
                        Text("Category: ${selectedTx.category}")
                        Text("Amount: $${"%.2f".format(selectedTx.amount)}")
                        Text("Date: ${selectedTx.date}")
                        Text("Type: ${selectedTx.type}")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onDeleteTransaction(selectedTx); screenState.dismissDetailDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { screenState.dismissDetailDialog() }) {
                        Text(
                            "Close"
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun HeaderSection(state: HeaderState, modifier: Modifier = Modifier) {
    Card(modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Income",
                    style = MaterialTheme.typography.labelSmall
                ); Text("$${"%.2f".format(state.totalIncome)}", color = Color(0xFF2E7D32))
            }
            Column {
                Text(
                    "Expenses",
                    style = MaterialTheme.typography.labelSmall
                ); Text("$${"%.2f".format(state.totalExpenses)}", color = Color(0xFFC62828))
            }
            Column {
                Text(
                    "Balance",
                    style = MaterialTheme.typography.labelSmall
                ); Text(
                "$${"%.2f".format(state.balance)}",
                style = MaterialTheme.typography.titleMedium
            )
            }
        }
    }
}

@Composable
fun SearchAndFilters(
    state: FiltersState,
    onQueryChanged: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search transactions...") },
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onOpenFilterSheet) { Text("Filters") }
    }
}

@Composable
fun TransactionList(
    state: ListState, lazyListState: LazyListState,
    onTransactionClicked: (Transaction) -> Unit,
    onLoadMore: () -> Unit, onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is ListState.Loading -> Box(
                Modifier.fillMaxSize(),
                Alignment.Center
            ) { CircularProgressIndicator() }

            is ListState.Error -> Column(
                Modifier.fillMaxSize(),
                Arrangement.Center,
                Alignment.CenterHorizontally
            ) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Retry") }
            }

            is ListState.Content -> {
                if (state.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No transactions found.") }
                } else {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                        items(state.items, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = { Text("${item.category} • ${item.date}") },
                                trailingContent = {
                                    Text(
                                        "${if (item.amount >= 0) "+" else ""}$${"%.2f".format(item.amount)}",
                                        color = if (item.amount >= 0) Color(0xFF2E7D32) else Color(
                                            0xFFC62828
                                        )
                                    )
                                },
                                modifier = Modifier.clickable { onTransactionClicked(item) }
                            )
                            HorizontalDivider()
                        }
                        if (state.hasMore) {
                            item {
                                LaunchedEffect(Unit) { onLoadMore() }
                                Box(Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp), Alignment.Center) {
                                    if (state.isLoadingMore) CircularProgressIndicator()
                                    else if (state.loadMoreFailed) Button(onClick = onRetry) {
                                        Text(
                                            "Retry loading more"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

