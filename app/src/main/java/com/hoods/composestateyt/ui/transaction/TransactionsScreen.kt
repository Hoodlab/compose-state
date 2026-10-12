package com.hoods.composestateyt.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hoods.composestateyt.data.Transaction
import kotlinx.coroutines.flow.Flow

//TODO PROBLEM 4: screen reads the whole state
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    uiState: TransactionsUiState,
    events: Flow<UiEvent>,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onTypeFilterChanged: (String) -> Unit,
    onPeriodFilterChanged: (String) -> Unit,
    onFilterSheetVisibilityChanged: (Boolean) -> Unit,
    onTransactionClicked: (Transaction) -> Unit,
    onDismissDetailDialog: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onUndoDelete: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onScrollToTop: () -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
    onExportClicked: () -> Unit,
    onExport: () -> Unit,
) {
    //TODO PROBLEM 2: two writers for one concept
    var localFilterSheetOpen by rememberSaveable { mutableStateOf(uiState.isFilterSheetOpen) }
    LaunchedEffect(uiState.isFilterSheetOpen) {
        if (localFilterSheetOpen != uiState.isFilterSheetOpen) localFilterSheetOpen = uiState.isFilterSheetOpen
    }
    LaunchedEffect(localFilterSheetOpen) {
        if (uiState.isFilterSheetOpen != localFilterSheetOpen) onFilterSheetVisibilityChanged(localFilterSheetOpen)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(event.message, event.actionLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) onUndoDelete()
                }
                is UiEvent.ScrollToTop -> listState.animateScrollToItem(0)
                is UiEvent.NavigateToExport -> onExport()
            }
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onScrollPositionChanged(listState.firstVisibleItemIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions Dashboard") },
                actions = { TextButton(onClick = onExportClicked) { Text("Export") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.showScrollToTopButton) {
                FloatingActionButton(onClick = { onScrollToTop() }) {
                    Text("▲", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Income", style = MaterialTheme.typography.labelSmall); Text("$${"%.2f".format(uiState.totalIncome)}", color = Color(0xFF2E7D32)) }
                    Column { Text("Expenses", style = MaterialTheme.typography.labelSmall); Text("$${"%.2f".format(uiState.totalExpenses)}", color = Color(0xFFC62828)) }
                    Column { Text("Balance", style = MaterialTheme.typography.labelSmall); Text("$${"%.2f".format(uiState.balance)}", style = MaterialTheme.typography.titleMedium) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = uiState.query, onValueChange = onQueryChanged, placeholder = { Text("Search transactions...") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button(onClick = { localFilterSheetOpen = true }) { Text("Filters") }
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.isInitialLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.errorMessage != null) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("Retry") }
                }
            } else if (uiState.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No transactions found.") }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    items(uiState.items, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.title) },
                            supportingContent = { Text("${item.category} • ${item.date}") },
                            trailingContent = {
                                Text(
                                    "${if (item.amount >= 0) "+" else ""}$${"%.2f".format(item.amount)}",
                                    color = if (item.amount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            },
                            modifier = Modifier.clickable { onTransactionClicked(item) }
                        )
                        HorizontalDivider()
                    }
                    if (uiState.hasMore) {
                        item {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                if (uiState.isLoadingMore) CircularProgressIndicator()
                                else if (uiState.loadMoreError != null) Button(onClick = onRetry) { Text("Retry loading more") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (localFilterSheetOpen) {
        ModalBottomSheet(onDismissRequest = { localFilterSheetOpen = false }) {
            Column(Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Groceries", "Income", "Transport").forEach { cat ->
                        FilterChip(selected = uiState.selectedCategory == cat, onClick = { onCategorySelected(cat) }, label = { Text(cat) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "Income", "Expense").forEach { type ->
                        FilterChip(selected = uiState.typeFilter == type, onClick = { onTypeFilterChanged(type) }, label = { Text(type) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Period", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("All", "This Week", "This Month").forEach { period ->
                        FilterChip(selected = uiState.periodFilter == period, onClick = { onPeriodFilterChanged(period) }, label = { Text(period) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { localFilterSheetOpen = false }, Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }

    if (uiState.isDetailDialogOpen && uiState.selectedTransaction != null) {
        val tx = uiState.selectedTransaction
        AlertDialog(
            onDismissRequest = onDismissDetailDialog,
            title = { Text(tx.title) },
            text = {
                Column {
                    Text("Category: ${tx.category}")
                    Text("Amount: $${"%.2f".format(tx.amount)}")
                    Text("Date: ${tx.date}")
                    Text("Type: ${tx.type}")
                }
            },
            confirmButton = { Button(onClick = { onDeleteTransaction(tx) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismissDetailDialog) { Text("Close") } }
        )
    }
}
