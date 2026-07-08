package com.example.upipaymenttracker

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionModel>> = transactionDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetModel>> = budgetDao.getAllBudgets()

    suspend fun insert(transaction: TransactionModel) {
        transactionDao.insert(transaction)
    }

    suspend fun setBudget(budget: BudgetModel) {
        budgetDao.insertBudget(budget)
    }
}