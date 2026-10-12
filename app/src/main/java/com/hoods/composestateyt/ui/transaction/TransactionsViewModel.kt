package com.hoods.composestateyt.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoods.composestateyt.data.Transaction
import com.hoods.composestateyt.data.TransactionRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class TransactionsViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val _header = MutableStateFlow(HeaderState(isLoading = true))
    private val _filters = MutableStateFlow(FiltersState())
    private val _list = MutableStateFlow<ListState>(ListState.Loading)


    //TODO FIX 3: ViewModel coordinates UI actions
    private val _userMessage = MutableStateFlow<UserMessage?>(null)
    private var nextMessageId = 1L

    private var searchJob: Job? = null
    private var currentPage = 0
    private var lastDeletedTransaction: Transaction? = null
    val uiState: StateFlow<TransactionsUiState> =
        combine(
            _header,
            _filters,
            _list,
            _userMessage,
            ::TransactionsUiState
        )
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    private var loadJob: Job? = null

    init {
        loadInitialData()
    }

    //Hoisting UI element state to ViewModel when business logic needs it
    fun onQueryChanged(newQuery: String) {
        _filters.update { it.copy(query = newQuery) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { delay(300L); loadInitialData() }
    }

    fun onCategorySelected(category: String) {
        _filters.update { it.copy(selectedCategory = category) }
        loadInitialData()
    }

    fun onTypeFilterChanged(type: String) {
        _filters.update {
            it.copy(typeFilter = type)
        }
        loadInitialData()
    }

    fun onPeriodFilterChanged(period: String) {
        _filters.update { it.copy(periodFilter = period) }
        loadInitialData()
    }

    fun onDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                if (repository.deleteTransaction(transaction.id) != null) {
                    lastDeletedTransaction = transaction
                    val current = _list.value
                    if (current is ListState.Content) {
                        _list.value =
                            current.copy(items = current.items.filterNot { it.id == transaction.id }
                                .toImmutableList())
                    }
                    loadSummary()
                    _userMessage.value = UserMessage(
                        nextMessageId++,
                        "Deleted ${transaction.title}",
                        UndoDelete(transaction)
                    )
                }
            } catch (e: Exception) {
                _userMessage.value = UserMessage(
                    nextMessageId++,
                    "Failed to delete transaction"
                )
            }
        }
    }

    fun onMessageShown(id: Long) {
        _userMessage.update { current -> if (current?.id == id) null else current }
    }

    fun onUndoDelete() {
        val toRestore = lastDeletedTransaction ?: return
        viewModelScope.launch {
            try {
                repository.restoreTransaction(toRestore)
                lastDeletedTransaction = null
                loadInitialData()
                _userMessage.value = UserMessage(
                    nextMessageId++,
                    "Restored ${toRestore.title}"
                )
            } catch (e: Exception) {
                _userMessage.value = UserMessage(
                    nextMessageId++,
                    "Failed to restore transaction"
                )
            }
        }
    }

    fun onLoadMore() {
        val current = _list.value
        if (current !is ListState.Content || current.isLoadingMore || !current.hasMore) return

        viewModelScope.launch {
            val nextPage = currentPage + 1
            _list.value = current.copy(isLoadingMore = true, loadMoreFailed = false)
            try {
                val f = _filters.value
                val result = repository.getTransactions(
                    f.query,
                    f.selectedCategory,
                    f.typeFilter,
                    f.periodFilter,
                    nextPage,
                    20
                )
                currentPage = nextPage
                _list.value = ListState.Content(
                    (current.items + result.items).toImmutableList(),
                    result.hasMore,
                    false,
                    false
                )
            } catch (e: Exception) {
                _list.value = current.copy(isLoadingMore = false, loadMoreFailed = true)
                _userMessage.value = UserMessage(nextMessageId++, "Failed to load more items")
            }
        }
    }

    fun onRetry() {
        val current = _list.value
        if (current is ListState.Content && current.loadMoreFailed) {
            onLoadMore()
        } else {
            loadInitialData()
        }
    }


    private fun loadInitialData() {
        currentPage = 0
        _list.value = ListState.Loading
        viewModelScope.launch {
            val f = _filters.value
            try {
                val result = repository.getTransactions(
                    f.query,
                    f.selectedCategory,
                    f.typeFilter,
                    f.periodFilter,
                    0,
                    20
                )
                _list.value = ListState.Content(
                    result.items.toImmutableList(),
                    result.hasMore,
                    false,
                    false
                )
            } catch (e: Exception) {
                _list.value = ListState.Error(e.message ?: "Network error occurred")
            }
            loadSummary()
        }
    }

    private suspend fun loadSummary() {
        val f = _filters.value
        _header.value = _header.value.copy(isLoading = true, errorMessage = null)
        try {
            val s = repository.getSummary(
                f.query,
                f.selectedCategory,
                f.typeFilter,
                f.periodFilter
            )
            _header.value = HeaderState(
                totalIncome = s.totalIncome,
                totalExpenses = s.totalExpenses,
                balance = s.balance,
                isLoading = false
            )
        } catch (e: Exception) {
            _header.value = _header.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "Failed to load summary"
            )
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
