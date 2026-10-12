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
import com.hoods.composestateyt.ui.transaction.TransactionsRoute
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
                TransactionsRoute(
                    viewModel = viewModel,
                    onExport = {
                        Toast.makeText(this, "Export destination navigated", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}
