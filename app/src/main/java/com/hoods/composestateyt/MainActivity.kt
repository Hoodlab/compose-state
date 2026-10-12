package com.hoods.composestateyt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hoods.composestateyt.data.FakeTransactionRepository
import com.hoods.composestateyt.ui.theme.ComposeStateYTTheme
import com.hoods.composestateyt.ui.transaction.TransactionsScreen
import com.hoods.composestateyt.ui.transaction.TransactionsViewModel
import com.hoods.composestateyt.ui.transaction.TransactionsViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: TransactionsViewModel by viewModels {
        TransactionsViewModelFactory(FakeTransactionRepository())
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeStateYTTheme {
                val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
                TransactionsScreen(
                    uiState = uiState,
                    events = viewModel.events,
                    onQueryChanged = viewModel::onQueryChanged,
                    onCategorySelected = viewModel::onCategorySelected,
                    onTypeFilterChanged = viewModel::onTypeFilterChanged,
                    onPeriodFilterChanged = viewModel::onPeriodFilterChanged,
                    onFilterSheetVisibilityChanged = viewModel::onFilterSheetVisibilityChanged,
                    onTransactionClicked = viewModel::onTransactionClicked,
                    onDismissDetailDialog = viewModel::onDismissDetailDialog,
                    onDeleteTransaction = viewModel::onDeleteTransaction,
                    onUndoDelete = viewModel::onUndoDelete,
                    onLoadMore = viewModel::onLoadMore,
                    onRetry = viewModel::onRetry,
                    onScrollToTop = viewModel::onScrollToTopClicked,
                    onScrollPositionChanged = viewModel::onScrollPositionChanged,
                    onExportClicked = viewModel::onExportClicked,
                    onExport = {
                        Toast.makeText(this, "Export destination navigated", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
