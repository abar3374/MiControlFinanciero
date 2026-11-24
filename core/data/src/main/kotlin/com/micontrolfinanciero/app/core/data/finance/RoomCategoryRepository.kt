package com.micontrolfinanciero.app.core.data.finance

import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.CategoryDao
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseCategoryEntity
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseNature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
    private val auditDao: AuditDao,
) : CategoryRepository {

    private val seedLock = Mutex()

    override fun observeCategories(): Flow<List<ExpenseCategory>> =
        dao.observeActive()
            .onStart { ensureDefaults() }
            .map { list -> list.map { it.toModel() } }

    override suspend fun addCategory(name: String, nature: ExpenseNature): Long {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            ExpenseCategoryEntity(
                name = FinanceValidator.clean(name),
                nature = nature.name,
                isDefault = false,
                sortOrder = dao.maxSortOrder() + 1,
                createdAt = now,
            ),
        )
        auditDao.insert(AuditLogEntity(entity = "expense_category", entityId = id, action = "CREATE", details = "nombre=$name", at = now))
        return id
    }

    /** La primera vez crea las categorías predeterminadas (se pueden agregar más). */
    private suspend fun ensureDefaults() = seedLock.withLock {
        if (dao.count() > 0) return@withLock
        val now = System.currentTimeMillis()
        dao.insertAll(
            DEFAULTS.mapIndexed { index, (name, nature) ->
                ExpenseCategoryEntity(name = name, nature = nature.name, isDefault = true, sortOrder = index, createdAt = now)
            },
        )
    }

    private companion object {
        val DEFAULTS = listOf(
            "Alimentación" to ExpenseNature.FOOD,
            "Transporte" to ExpenseNature.TRANSPORT,
            "Vivienda" to ExpenseNature.HOUSING,
            "Servicios públicos" to ExpenseNature.NEED,
            "Salud" to ExpenseNature.HEALTH,
            "Educación" to ExpenseNature.EDUCATION,
            "Ocio" to ExpenseNature.LEISURE,
            "Crédito y deudas" to ExpenseNature.CREDIT,
            "Ahorro" to ExpenseNature.SAVINGS,
            "Otros" to ExpenseNature.OTHER,
        )
    }
}
