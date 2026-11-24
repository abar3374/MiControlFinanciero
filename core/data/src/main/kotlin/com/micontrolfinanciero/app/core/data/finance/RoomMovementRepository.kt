package com.micontrolfinanciero.app.core.data.finance

import androidx.room.withTransaction
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.BudgetDao
import com.micontrolfinanciero.app.core.database.dao.CategoryDao
import com.micontrolfinanciero.app.core.database.dao.ExpenseDao
import com.micontrolfinanciero.app.core.database.dao.IncomeDao
import com.micontrolfinanciero.app.core.database.dao.MovementDao
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseEntity
import com.micontrolfinanciero.app.core.database.entity.IncomeEntity
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementFilter
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.model.BudgetProgress
import com.micontrolfinanciero.app.core.model.CategoryTotal
import com.micontrolfinanciero.app.core.model.Expense
import com.micontrolfinanciero.app.core.model.Income
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.MovementItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMovementRepository @Inject constructor(
    private val db: FinanceDatabase,
    private val expenseDao: ExpenseDao,
    private val incomeDao: IncomeDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val movementDao: MovementDao,
    private val auditDao: AuditDao,
) : MovementRepository {

    // ------------------------------------------------------------------ Gastos

    override suspend fun getExpense(id: Long): Expense? = expenseDao.get(id)?.toModel()

    override suspend fun saveExpense(expense: Expense): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = if (expense.id != 0L) expenseDao.get(expense.id) else null
        val entity = ExpenseEntity(
            id = existing?.id ?: 0L,
            concept = expense.concept,
            place = expense.place,
            amountCents = expense.amount.cents,
            occurredAt = expense.occurredAt,
            categoryId = expense.categoryId,
            budgetId = expense.budgetId,
            nature = expense.nature.name,
            paymentMethod = expense.paymentMethod.name,
            description = expense.description,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (existing == null) {
            val id = expenseDao.insert(entity)
            audit("expense", id, "CREATE", entity.auditText(), now)
            id
        } else {
            expenseDao.update(entity)
            audit("expense", entity.id, "UPDATE", "antes: ${existing.auditText()} | después: ${entity.auditText()}", now)
            entity.id
        }
    }

    override suspend fun deleteExpense(id: Long) {
        db.withTransaction {
            val existing = expenseDao.get(id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            expenseDao.markVoided(id, now)
            audit("expense", id, "DELETE", existing.auditText(), now)
        }
    }

    // ------------------------------------------------------------------ Ingresos

    override suspend fun getIncome(id: Long): Income? = incomeDao.get(id)?.toModel()

    override suspend fun saveIncome(income: Income): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = if (income.id != 0L) incomeDao.get(income.id) else null
        val entity = IncomeEntity(
            id = existing?.id ?: 0L,
            source = income.source,
            amountCents = income.amount.cents,
            receivedAt = income.receivedAt,
            incomeType = income.type.name,
            destination = income.destination.name,
            notes = income.notes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (existing == null) {
            val id = incomeDao.insert(entity)
            audit("income", id, "CREATE", entity.auditText(), now)
            id
        } else {
            incomeDao.update(entity)
            audit("income", entity.id, "UPDATE", "antes: ${existing.auditText()} | después: ${entity.auditText()}", now)
            entity.id
        }
    }

    override suspend fun deleteIncome(id: Long) {
        db.withTransaction {
            val existing = incomeDao.get(id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            incomeDao.markVoided(id, now)
            audit("income", id, "DELETE", existing.auditText(), now)
        }
    }

    // ------------------------------------------------------------------ Consultas

    override fun observeMovements(filter: MovementFilter): Flow<List<MovementItem>> =
        movementDao.observe(
            start = filter.range.startMillis,
            end = filter.range.endMillis,
            query = filter.query.trim(),
            includeExpenses = filter.includeExpenses,
            includeIncomes = filter.includeIncomes,
            limit = filter.limit,
        ).map { rows -> rows.map { it.toModel() } }

    override fun observeRecent(limit: Int): Flow<List<MovementItem>> =
        movementDao.observe(
            start = Long.MIN_VALUE,
            end = Long.MAX_VALUE,
            query = "",
            includeExpenses = true,
            includeIncomes = true,
            limit = limit,
        ).map { rows -> rows.map { it.toModel() } }

    override fun observeMonthSummary(range: MonthRange): Flow<MonthSummary> {
        val lastDay: LocalDate = range.month.atEndOfMonth()
        val totals = combine(
            expenseDao.observeTotal(range.startMillis, range.endMillis),
            incomeDao.observeTotal(range.startMillis, range.endMillis),
            expenseDao.observeTotalsByBudget(range.startMillis, range.endMillis),
            expenseDao.observeTotalsByCategory(range.startMillis, range.endMillis),
        ) { spent, income, byBudget, byCategory -> Totals(spent, income, byBudget.associate { it.groupId to it.totalCents }, byCategory) }

        return combine(totals, budgetDao.observeAll(), categoryDao.observeActive()) { t, budgets, categories ->
            val categoryNames = categories.associate { it.id to it.name }
            // Rubros que cuentan este mes: activos y que ya habían empezado.
            val monthBudgets = budgets.filter { it.active && it.startEpochDay <= lastDay.toEpochDay() }
            val progress = monthBudgets.map { entity ->
                BudgetProgress(
                    budget = entity.toModel(),
                    categoryName = categoryNames[entity.categoryId].orEmpty(),
                    spent = Money(t.byBudget[entity.id] ?: 0L),
                )
            }
            MonthSummary(
                month = range.month,
                budgetTotal = Money.sum(progress.map { it.budget.amount }),
                spent = Money(t.spent),
                spentInBudgets = Money.sum(progress.map { it.spent }),
                income = Money(t.income),
                budgets = progress,
                byCategory = t.byCategory.map { CategoryTotal(it.groupId, it.groupName, Money(it.totalCents)) },
            )
        }
    }

    private suspend fun audit(entity: String, id: Long, action: String, details: String, at: Long) {
        auditDao.insert(AuditLogEntity(entity = entity, entityId = id, action = action, details = details, at = at))
    }

    private data class Totals(
        val spent: Long,
        val income: Long,
        val byBudget: Map<Long, Long>,
        val byCategory: List<com.micontrolfinanciero.app.core.database.entity.GroupTotal>,
    )
}
