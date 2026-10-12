package com.hoods.composestateyt.data

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.math.abs
import kotlin.math.round

class FakeTransactionRepository(
    initialTransactions: List<Transaction> = generateSampleTransactions(),
    private val delayMillis: Long = 600L,
) : TransactionRepository {

    private val transactions = initialTransactions.toMutableList()
    private var failNextCall: Boolean = false
    private var callCount = 0

    override fun setFailNextCall(fail: Boolean) {
        failNextCall = fail
    }

    override suspend fun getTransactions(
        query: String,
        category: String,
        type: String,
        period: String,
        page: Int,
        pageSize: Int,
    ): TransactionPage {
        delay(delayMillis)
        callCount++
        if (failNextCall) {
            failNextCall = false
            throw IOException("Simulated network error")
        }

        val filtered = filterTransactions(query, category, type, period)
        val safePage = page.coerceAtLeast(0)
        val start = safePage * pageSize
        val items = filtered.drop(start).take(pageSize)
        val hasMore = start + items.size < filtered.size

        return TransactionPage(
            items = items,
            totalCount = filtered.size,
            hasMore = hasMore,
        )
    }

    override suspend fun deleteTransaction(id: Int): Transaction? {
        delay(delayMillis)
        val index = transactions.indexOfFirst { it.id == id }
        return if (index != -1) {
            transactions.removeAt(index)
        } else {
            null
        }
    }

    override suspend fun restoreTransaction(transaction: Transaction) {
        delay(delayMillis)
        if (transactions.none { it.id == transaction.id }) {
            transactions.add(0, transaction)
        }
    }

    override suspend fun getSummary(
        query: String,
        category: String,
        type: String,
        period: String,
    ): TransactionSummary {
        delay(delayMillis / 2)
        val filtered = filterTransactions(query, category, type, period)
        var income = 0.0
        var expenses = 0.0
        for (item in filtered) {
            if (item.amount >= 0) {
                income += item.amount
            } else {
                expenses += abs(item.amount)
            }
        }
        return TransactionSummary(
            totalIncome = income,
            totalExpenses = expenses,
            balance = income - expenses,
        )
    }

    private fun filterTransactions(
        query: String,
        category: String,
        type: String,
        period: String,
    ): List<Transaction> {
        return transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.title.contains(query, ignoreCase = true) ||
                tx.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || tx.category.equals(category, ignoreCase = true)

            val matchesType = when (type.lowercase()) {
                "income" -> tx.amount >= 0
                "expense" -> tx.amount < 0
                else -> true
            }

            val matchesPeriod = when (period.lowercase()) {
                "this week" -> tx.date >= "2026-10-05"
                "this month" -> tx.date >= "2026-10-01"
                else -> true
            }

            matchesQuery && matchesCategory && matchesType && matchesPeriod
        }
    }

    companion object {
        fun generateSampleTransactions(): List<Transaction> {
            val categories = listOf("Groceries", "Income", "Transport", "Utilities", "Food", "Health", "Entertainment", "Education")
            val titles = mapOf(
                "Groceries" to listOf("Supermarket", "Organic Market", "Bakery", "Corner Store"),
                "Income" to listOf("Salary", "Freelance Project", "Client Retainer", "Dividend"),
                "Transport" to listOf("Fuel Station", "Bus Ticket", "Metro Card", "Ride Share"),
                "Utilities" to listOf("Electric Bill", "Internet Service", "Water Bill", "Phone Plan"),
                "Food" to listOf("Restaurant", "Coffee Shop", "Pizza Delivery", "Lunch Bistro"),
                "Health" to listOf("Pharmacy", "Dental Clinic", "Gym Membership", "Doctor Visit"),
                "Entertainment" to listOf("Cinema Ticket", "Streaming Service", "Concert Pass", "Book Store"),
                "Education" to listOf("Online Course", "Technical Book", "Conference Pass", "Workshop")
            )

            val list = mutableListOf<Transaction>()
            for (i in 1..200) {
                val cat = categories[(i - 1) % categories.size]
                val titleOptions = titles[cat] ?: listOf("Item $i")
                val title = titleOptions[(i - 1) % titleOptions.size]
                val isIncome = cat == "Income"
                val amount = if (isIncome) {
                    200.0 + (i * 15.5) % 1500.0
                } else {
                    -(10.0 + (i * 7.25) % 120.0)
                }
                val roundedAmount = round(amount * 100.0) / 100.0
                val day = 1 + (i % 28)
                val dayStr = if (day < 10) "0$day" else "$day"
                val date = "2026-10-$dayStr"
                list.add(
                    Transaction(
                        id = i,
                        title = title,
                        category = cat,
                        amount = roundedAmount,
                        date = date,
                    )
                )
            }
            return list
        }
    }
}
