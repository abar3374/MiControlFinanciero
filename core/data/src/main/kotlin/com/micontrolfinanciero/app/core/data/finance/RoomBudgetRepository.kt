package com.micontrolfinanciero.app.core.data.finance

import androidx.room.withTransaction
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.BudgetDao
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.BudgetEntity
import com.micontrolfinanciero.app.core.domain.finance.BudgetRepository
import com.micontrolfinanciero.app.core.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBudgetRepository @Inject constructor(
    private val db: FinanceDatabase,
    private val dao: BudgetDao,
    private val auditDao: AuditDao,
) : BudgetRepository {

    override fun observeBudgets(): Flow<List<Budget>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getBudget(id: Long): Budget? = dao.get(id)?.toModel()

    override suspend fun saveBudget(budget: Budget): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = if (budget.id != 0L) dao.get(budget.id) else null
        val entity = BudgetEntity(
            id = existing?.id ?: 0L,
            name = budget.name,
            description = budget.description,
            categoryId = budget.categoryId,
            amountCents = budget.amount.cents,
            frequencyType = budget.frequencyType.name,
            nature = budget.nature.name,
            startEpochDay = budget.startDate.toEpochDay(),
            active = budget.active,
            notes = budget.notes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        if (existing == null) {
            val id = dao.insert(entity)
            auditDao.insert(AuditLogEntity(entity = "budget", entityId = id, action = "CREATE", details = entity.auditText(), at = now))
            id
        } else {
            dao.update(entity)
            auditDao.insert(
                AuditLogEntity(
                    entity = "budget",
                    entityId = entity.id,
                    action = "UPDATE",
                    details = "antes: ${existing.auditText()} | después: ${entity.auditText()}",
                    at = now,
                ),
            )
            entity.id
        }
    }

    override suspend fun deleteBudget(id: Long) {
        db.withTransaction {
            val existing = dao.get(id) ?: return@withTransaction
            dao.delete(id)
            auditDao.insert(
                AuditLogEntity(entity = "budget", entityId = id, action = "DELETE", details = existing.auditText(), at = System.currentTimeMillis()),
            )
        }
    }
}
