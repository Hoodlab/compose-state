package com.hoods.composestateyt.data

interface TransactionRepository {
    suspend fun getTransactions(
        query: String = "",
        category: String = "All",
        type: String = "All",
        period: String = "All",
        page: Int = 0,
        pageSize: Int = 20,
    ): TransactionPage

    suspend fun deleteTransaction(id: Int): Transaction?

    suspend fun restoreTransaction(transaction: Transaction)

    suspend fun getSummary(
        query: String = "",
        category: String = "All",
        type: String = "All",
        period: String = "All",
    ): TransactionSummary

    fun setFailNextCall(fail: Boolean)
}

data class DateRange(
    val startInclusive: String,
    val endInclusive: String,
)

data class TransactionSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val balance: Double,
)

data class TransactionPage(
    val items: List<Transaction>,
    val totalCount: Int,
    val hasMore: Boolean,
)
