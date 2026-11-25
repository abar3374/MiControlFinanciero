package com.micontrolfinanciero.app.core.data.finance

import androidx.room.withTransaction
import com.micontrolfinanciero.app.core.database.FinanceDatabase
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
    private val db: FinanceDatabase,
    private val dao: CategoryDao,
    private val auditDao: AuditDao,
) : CategoryRepository {

    private val seedLock = Mutex()

    override fun observeCategories(): Flow<List<ExpenseCategory>> =
        dao.observeActive()
            .onStart { ensureDefaults() }
            .map { list -> list.map { it.toModel() } }

    override fun observeAllCategories(): Flow<List<ExpenseCategory>> =
        dao.observeAll()
            .onStart { ensureDefaults() }
            .map { list -> list.map { it.toModel() } }

    override suspend fun addCategory(name: String, nature: ExpenseNature): Long = db.withTransaction {
        val clean = FinanceValidator.clean(name)
        val now = System.currentTimeMillis()
        val existing = dao.findByName(clean)
        when {
            existing == null -> {
                val id = dao.insert(
                    ExpenseCategoryEntity(
                        name = clean,
                        nature = nature.name,
                        isDefault = false,
                        sortOrder = dao.maxSortOrder() + 1,
                        createdAt = now,
                    ),
                )
                audit(id, "CREATE", "nombre=$clean", now)
                id
            }
            existing.archived -> {
                // Ya existía oculta: se vuelve a mostrar en vez de duplicarla.
                dao.setArchived(existing.id, false)
                audit(existing.id, "UPDATE", "mostrar de nuevo: ${existing.name}", now)
                existing.id
            }
            else -> throw IllegalArgumentException("Ya existe una categoría con ese nombre.")
        }
    }

    override suspend fun renameCategory(id: Long, name: String) {
        db.withTransaction {
            val clean = FinanceValidator.clean(name)
            val current = dao.get(id) ?: return@withTransaction
            val other = dao.findByName(clean)
            if (other != null && other.id != id) throw IllegalArgumentException("Ya existe una categoría con ese nombre.")
            dao.rename(id, clean)
            audit(id, "UPDATE", "nombre: ${current.name} -> $clean", System.currentTimeMillis())
        }
    }

    override suspend fun setCategoryHidden(id: Long, hidden: Boolean) {
        db.withTransaction {
            val current = dao.get(id) ?: return@withTransaction
            dao.setArchived(id, hidden)
            audit(id, "UPDATE", (if (hidden) "ocultar: " else "mostrar: ") + current.name, System.currentTimeMillis())
        }
    }

    private suspend fun audit(id: Long, action: String, details: String, at: Long) {
        auditDao.insert(AuditLogEntity(entity = "expense_category", entityId = id, action = action, details = details, at = at))
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
