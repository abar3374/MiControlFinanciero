package com.micontrolfinanciero.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.BudgetEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseCategoryEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentRecordEntity
import com.micontrolfinanciero.app.core.database.entity.RecordWithExpense
import com.micontrolfinanciero.app.core.database.entity.GroupTotal
import com.micontrolfinanciero.app.core.database.entity.IncomeEntity
import com.micontrolfinanciero.app.core.database.entity.MovementRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM expense_category WHERE archived = 0 ORDER BY sort_order, name")
    fun observeActive(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_category ORDER BY archived, sort_order, name")
    fun observeAll(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_category WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): ExpenseCategoryEntity?

    @Query("SELECT * FROM expense_category WHERE id = :id")
    suspend fun get(id: Long): ExpenseCategoryEntity?

    @Query("UPDATE expense_category SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE expense_category SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("SELECT COUNT(*) FROM expense_category")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM expense_category")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(entity: ExpenseCategoryEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ExpenseCategoryEntity>)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget ORDER BY active DESC, name")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budget WHERE id = :id")
    suspend fun get(id: Long): BudgetEntity?

    @Insert
    suspend fun insert(entity: BudgetEntity): Long

    @Update
    suspend fun update(entity: BudgetEntity)

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense WHERE id = :id AND voided = 0")
    suspend fun get(id: Long): ExpenseEntity?

    @Insert
    suspend fun insert(entity: ExpenseEntity): Long

    @Update
    suspend fun update(entity: ExpenseEntity)

    @Query("UPDATE expense SET voided = 1, updated_at = :now WHERE id = :id")
    suspend fun markVoided(id: Long, now: Long)

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM expense
        WHERE voided = 0 AND occurred_at >= :start AND occurred_at < :end
        """,
    )
    fun observeTotal(start: Long, end: Long): Flow<Long>

    @Query(
        """
        SELECT e.budget_id AS group_id, b.name AS group_name, SUM(e.amount_cents) AS total_cents
        FROM expense e JOIN budget b ON b.id = e.budget_id
        WHERE e.voided = 0 AND e.occurred_at >= :start AND e.occurred_at < :end
        GROUP BY e.budget_id, b.name
        """,
    )
    fun observeTotalsByBudget(start: Long, end: Long): Flow<List<GroupTotal>>

    @Query(
        """
        SELECT e.category_id AS group_id, c.name AS group_name, SUM(e.amount_cents) AS total_cents
        FROM expense e JOIN expense_category c ON c.id = e.category_id
        WHERE e.voided = 0 AND e.occurred_at >= :start AND e.occurred_at < :end
        GROUP BY e.category_id, c.name
        ORDER BY total_cents DESC
        """,
    )
    fun observeTotalsByCategory(start: Long, end: Long): Flow<List<GroupTotal>>
}

@Dao
interface IncomeDao {
    @Query("SELECT * FROM income WHERE id = :id AND voided = 0")
    suspend fun get(id: Long): IncomeEntity?

    @Insert
    suspend fun insert(entity: IncomeEntity): Long

    @Update
    suspend fun update(entity: IncomeEntity)

    @Query("UPDATE income SET voided = 1, updated_at = :now WHERE id = :id")
    suspend fun markVoided(id: Long, now: Long)

    @Query(
        """
        SELECT COALESCE(SUM(amount_cents), 0) FROM income
        WHERE voided = 0 AND received_at >= :start AND received_at < :end
        """,
    )
    fun observeTotal(start: Long, end: Long): Flow<Long>
}

@Dao
interface MovementDao {
    /**
     * Gastos e ingresos juntos, del más reciente al más antiguo.
     * [query] busca en concepto, lugar y categoría (gastos) o en el origen (ingresos).
     */
    @Query(
        """
        SELECT * FROM (
            SELECT 'EXPENSE' AS kind, e.id AS id, e.concept AS title, c.name AS subtitle,
                   e.amount_cents AS amount_cents, e.occurred_at AS at, e.payment_method AS detail
            FROM expense e JOIN expense_category c ON c.id = e.category_id
            WHERE :includeExpenses = 1 AND e.voided = 0
              AND e.occurred_at >= :start AND e.occurred_at < :end
              AND (:query = '' OR e.concept LIKE '%' || :query || '%'
                   OR e.place LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
            UNION ALL
            SELECT 'INCOME' AS kind, i.id AS id, i.source AS title, i.income_type AS subtitle,
                   i.amount_cents AS amount_cents, i.received_at AS at, i.destination AS detail
            FROM income i
            WHERE :includeIncomes = 1 AND i.voided = 0
              AND i.received_at >= :start AND i.received_at < :end
              AND (:query = '' OR i.source LIKE '%' || :query || '%')
        )
        ORDER BY at DESC, id DESC
        LIMIT :limit
        """,
    )
    fun observe(
        start: Long,
        end: Long,
        query: String,
        includeExpenses: Boolean,
        includeIncomes: Boolean,
        limit: Int,
    ): Flow<List<MovementRow>>
}

@Dao
interface FixedPaymentDao {
    @Query("SELECT * FROM fixed_payment ORDER BY active DESC, due_day, name")
    fun observeAll(): Flow<List<FixedPaymentEntity>>

    @Query("SELECT * FROM fixed_payment WHERE id = :id")
    suspend fun get(id: Long): FixedPaymentEntity?

    @Insert
    suspend fun insert(entity: FixedPaymentEntity): Long

    @Update
    suspend fun update(entity: FixedPaymentEntity)

    /** Pagos registrados cuyo gasto sigue vigente (no anulado). */
    @Query(
        """
        SELECT r.fixed_payment_id AS fixed_payment_id, r.period AS period, r.expense_id AS expense_id,
               e.amount_cents AS amount_cents, e.occurred_at AS occurred_at
        FROM fixed_payment_record r JOIN expense e ON e.id = r.expense_id
        WHERE e.voided = 0
        """,
    )
    fun observeRecords(): Flow<List<RecordWithExpense>>

    @Query(
        """
        SELECT r.fixed_payment_id AS fixed_payment_id, r.period AS period, r.expense_id AS expense_id,
               e.amount_cents AS amount_cents, e.occurred_at AS occurred_at
        FROM fixed_payment_record r JOIN expense e ON e.id = r.expense_id
        WHERE e.voided = 0
        """,
    )
    suspend fun recordsSnapshot(): List<RecordWithExpense>

    @Query("SELECT * FROM fixed_payment_record WHERE fixed_payment_id = :paymentId AND period = :period")
    suspend fun getRecord(paymentId: Long, period: Int): FixedPaymentRecordEntity?

    @Insert
    suspend fun insertRecord(entity: FixedPaymentRecordEntity): Long

    @Query("DELETE FROM fixed_payment_record WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM fixed_payment_record WHERE expense_id = :expenseId")
    suspend fun deleteRecordsForExpense(expenseId: Long)
}

@Dao
interface AuditDao {
    @Insert
    suspend fun insert(entity: AuditLogEntity)
}
