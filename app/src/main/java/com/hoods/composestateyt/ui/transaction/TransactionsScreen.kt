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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoods.composestateyt.data.Transaction

@Composable
fun TransactionsRoute(
    viewModel: TransactionsViewModel,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val screenState = rememberTransactionsScreenState()

    LaunchedEffect(uiState.query, uiState.selectedCategory, uiState.typeFilter, uiState.periodFilter) {
        screenState.scrollToTop()
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = screenState.snackbarHostState.showSnackbar(event.message, event.actionLabel, duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndoDelete()
                }
                is UiEvent.NavigateToExport -> onExport()
            }
        }
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
        onExportClicked = viewModel::onExportClicked,
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
                Button(onClick = { screenState.openFilterSheet() }) { Text("Filters") }
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
                LazyColumn(state = screenState.lazyListState, modifier = Modifier.weight(1f)) {
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
                            modifier = Modifier.clickable { screenState.selectTransaction(item.id) }
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

    if (screenState.isFilterSheetOpen) {
        ModalBottomSheet(onDismissRequest = { screenState.closeFilterSheet() }) {
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
                Button(onClick = { screenState.closeFilterSheet() }, Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }

    if (screenState.isDetailDialogOpen) {
        val selectedTx = uiState.items.find { it.id == screenState.selectedTransactionId }
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
                        onClick = {
                            onDeleteTransaction(selectedTx)
                            screenState.dismissDetailDialog()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { screenState.dismissDetailDialog() }) { Text("Close") } }
            )
        }
    }
}

