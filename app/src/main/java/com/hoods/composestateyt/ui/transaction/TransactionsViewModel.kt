package com.hoods.composestateyt.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoods.composestateyt.data.Transaction
import com.hoods.composestateyt.data.TransactionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.filterNot

sealed interface UiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : UiEvent
    data object NavigateToExport : UiEvent
}

class TransactionsViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    //TODO PROBLEM 3: ViewModel coordinates UI actions
    private val eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadInitialData()
    }

    //Hoisting UI element state to ViewModel when business logic needs it
    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(_uiState.value.searchDebounceMillis)
            loadInitialData()
        }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadInitialData()
    }

    fun onTypeFilterChanged(type: String) {
        _uiState.update { it.copy(typeFilter = type) }
        loadInitialData()
    }

    fun onPeriodFilterChanged(period: String) {
        _uiState.update { it.copy(periodFilter = period) }
        loadInitialData()
    }
    fun onDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                val deleted = repository.deleteTransaction(transaction.id)
                if (deleted != null) {
                    val updatedItems = _uiState.value.items.filterNot { it.id == transaction.id }
                    _uiState.update {
                        it.copy(
                            items = updatedItems,
                            isDeleting = false,
                            lastDeletedId = transaction.id,
                            lastDeletedTransaction = transaction,
                            totalCount = (it.totalCount - 1).coerceAtLeast(0),
                        )
                    }
                    loadSummary()
                    eventChannel.send(UiEvent.ShowSnackbar("Deleted ${transaction.title}", "Undo"))
                } else {
                    _uiState.update { it.copy(isDeleting = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, errorMessage = e.message ?: "Failed to delete") }
                eventChannel.send(UiEvent.ShowSnackbar("Failed to delete transaction", null))
            }
        }
    }

    fun onUndoDelete() {
        val toRestore = _uiState.value.lastDeletedTransaction ?: return
        viewModelScope.launch {
            try {
                repository.restoreTransaction(toRestore)
                _uiState.update {
                    it.copy(
                        lastDeletedTransaction = null,
                        lastDeletedId = null,
                    )
                }
                loadInitialData()
                eventChannel.send(UiEvent.ShowSnackbar("Restored ${toRestore.title}", null))
            } catch (e: Exception) {
                eventChannel.send(UiEvent.ShowSnackbar("Failed to restore transaction", null))
            }
        }
    }

    fun onLoadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore || current.isInitialLoading) return

        viewModelScope.launch {
            val nextPage = current.page + 1
            _uiState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
            try {
                val result = repository.getTransactions(
                    query = current.query,
                    category = current.selectedCategory,
                    type = current.typeFilter,
                    period = current.periodFilter,
                    page = nextPage,
                    pageSize = current.pageSize,
                )
                _uiState.update {
                    it.copy(
                        items = it.items + result.items,
                        page = nextPage,
                        hasMore = result.hasMore,
                        totalCount = result.totalCount,
                        isLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, loadMoreError = e.message ?: "Load more failed") }
                eventChannel.send(UiEvent.ShowSnackbar("Failed to load more items", "Retry"))
            }
        }
    }

    fun onRetry() {
        if (_uiState.value.loadMoreError != null) {
            onLoadMore()
        } else {
            loadInitialData()
        }
    }

    fun onExportClicked() {
        viewModelScope.launch {
            eventChannel.send(UiEvent.NavigateToExport)
        }
    }


    private fun loadInitialData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val current = _uiState.value
            _uiState.update {
                it.copy(
                    isInitialLoading = true,
                    errorMessage = null,
                    page = 0,
                )
            }
            try {
                val result = repository.getTransactions(
                    query = current.query,
                    category = current.selectedCategory,
                    type = current.typeFilter,
                    period = current.periodFilter,
                    page = 0,
                    pageSize = current.pageSize,
                )
                _uiState.update {
                    it.copy(
                        items = result.items,
                        totalCount = result.totalCount,
                        hasMore = result.hasMore,
                        isInitialLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        errorMessage = e.message ?: "Network error occurred",
                    )
                }
            }
            loadSummary()
        }
    }

    private suspend fun loadSummary() {
        val current = _uiState.value
        _uiState.update { it.copy(isSummaryLoading = true, summaryError = null) }
        try {
            val summary = repository.getSummary(
                query = current.query,
                category = current.selectedCategory,
                type = current.typeFilter,
                period = current.periodFilter,
            )
            _uiState.update {
                it.copy(
                    totalIncome = summary.totalIncome,
                    totalExpenses = summary.totalExpenses,
                    balance = summary.balance,
                    isSummaryLoading = false,
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSummaryLoading = false,
                    summaryError = e.message ?: "Failed to load summary",
                )
            }
        }
    }
}

class TransactionsViewModelFactory(
    private val repository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransactionsViewModel(repository) as T
    }
}
