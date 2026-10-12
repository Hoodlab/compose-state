package com.hoods.composestateyt.data

data class Transaction(
    val id: Int,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val type: String = if (amount >= 0) "Income" else "Expense",
)
