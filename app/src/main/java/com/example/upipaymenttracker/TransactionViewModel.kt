package com.example.upipaymenttracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, RETRO_90S, NEON_NIGHT
}

data class Subscription(
    val name: String,
    val monthlyAmount: Double,
    val annualCost: Double
)

class TransactionViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("vyaay_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean("dynamic_color", true))
    val dynamicColor = _dynamicColor.asStateFlow()

    val allTransactions: StateFlow<List<TransactionModel>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetModel>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<TransactionModel>> = 
        combine(allTransactions, _searchQuery, _selectedCategory, _selectedDate) { transactions, query, category, date ->
            transactions.filter { transaction ->
                val transCal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                val matchesMonth = transCal.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                                 transCal.get(Calendar.YEAR) == date.get(Calendar.YEAR)
                
                val matchesQuery = query.isEmpty() || 
                                 transaction.category.contains(query, ignoreCase = true) || 
                                 transaction.amount.contains(query) ||
                                 transaction.note.contains(query, ignoreCase = true)
                
                val matchesCategory = category == "All" || transaction.category == category
                
                matchesMonth && matchesQuery && matchesCategory
            }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpending: StateFlow<Double> = filteredTransactions.map { transactions ->
        transactions.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: StateFlow<Map<String, Double>> = filteredTransactions.map { transactions ->
        transactions.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subscriptions: StateFlow<List<Subscription>> = allTransactions.map { transactions ->
        detectSubscriptions(transactions)
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun detectSubscriptions(transactions: List<TransactionModel>): List<Subscription> {
        val subKeywords = listOf(
            "netflix", "spotify", "youtube", "amazon", "prime", "disney", "gym", "hotstar", "apple", "google",
            "zee5", "sony liv", "voot", "jiocinema", "alt balaji", "gaana", "wynk", "audible", "canva", "adobe",
            "microsoft", "office 365", "icloud", "dropbox", "tinder", "bumble", "cult.fit"
        )
        return transactions.groupBy { it.note.lowercase() }
            .filter { (name, list) -> 
                name.isNotEmpty() && name != "unknown merchant" && 
                (subKeywords.any { name.contains(it) } || list.size >= 2)
            }
            .map { (name, list) ->
                val avgAmount = list.map { it.amount.toDoubleOrNull() ?: 0.0 }.average()
                Subscription(
                    name = name.replaceFirstChar { it.uppercase() },
                    monthlyAmount = avgAmount,
                    annualCost = avgAmount * 12
                )
            }
    }

    fun changeMonth(amount: Int) {
        val newDate = _selectedDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, amount)
        _selectedDate.value = newDate
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean("dynamic_color", enabled).apply()
    }

    fun insert(transaction: TransactionModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(transaction)
    }

    fun delete(transaction: TransactionModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(transaction)
    }

    fun update(transaction: TransactionModel) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(transaction)
    }

    fun setBudget(category: String, amount: Double) = viewModelScope.launch(Dispatchers.IO) {
        repository.setBudget(BudgetModel(category, amount))
    }
}

class TransactionViewModelFactory(
    private val application: Application,
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}