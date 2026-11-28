package com.micontrolfinanciero.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Upsert
import androidx.room.Query
import androidx.room.Update
import com.micontrolfinanciero.app.core.database.entity.AppSettingEntity
import com.micontrolfinanciero.app.core.database.entity.AttachmentEntity
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.ExpectedIncomeEntity
import com.micontrolfinanciero.app.core.database.entity.ExpectedIncomeRecordEntity
import com.micontrolfinanciero.app.core.database.entity.IncomeRecordWithIncome
import com.micontrolfinanciero.app.core.database.entity.NotificationLogEntity
import com.micontrolfinanciero.app.core.database.entity.ReportExpenseRow
import com.micontrolfinanciero.app.core.database.entity.ReportIncomeRow
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

    @Query("SELECT * FROM audit_log ORDER BY at DESC, id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<AuditLogEntity>>
}

@Dao
interface ExpectedIncomeDao {
    @Query("SELECT * FROM expected_income ORDER BY active DESC, day, source")
    fun observeAll(): Flow<List<ExpectedIncomeEntity>>

    @Query("SELECT * FROM expected_income WHERE id = :id")
    suspend fun get(id: Long): ExpectedIncomeEntity?

    @Insert
    suspend fun insert(entity: ExpectedIncomeEntity): Long

    @Update
    suspend fun update(entity: ExpectedIncomeEntity)

    @Query(
        """
        SELECT r.expected_income_id AS expected_income_id, r.period AS period, r.income_id AS income_id,
               i.amount_cents AS amount_cents, i.received_at AS received_at
        FROM expected_income_record r JOIN income i ON i.id = r.income_id
        WHERE i.voided = 0
        """,
    )
    fun observeRecords(): Flow<List<IncomeRecordWithIncome>>

    @Query("SELECT * FROM expected_income_record WHERE expected_income_id = :id AND period = :period")
    suspend fun getRecord(id: Long, period: Int): ExpectedIncomeRecordEntity?

    @Insert
    suspend fun insertRecord(entity: ExpectedIncomeRecordEntity): Long

    @Query("DELETE FROM expected_income_record WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Query("DELETE FROM expected_income_record WHERE income_id = :incomeId")
    suspend fun deleteRecordsForIncome(incomeId: Long)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachment WHERE expense_id = :expenseId ORDER BY created_at")
    fun observeForExpense(expenseId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachment WHERE id = :id")
    suspend fun get(id: Long): AttachmentEntity?

    @Insert
    suspend fun insert(entity: AttachmentEntity): Long

    @Query("DELETE FROM attachment WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_setting")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT value FROM app_setting WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun put(entity: AppSettingEntity)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT COUNT(*) FROM notification_log WHERE kind = :kind AND ref_id = :refId AND period = :period AND days_before = :daysBefore")
    suspend fun count(kind: String, refId: Long, period: Int, daysBefore: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: NotificationLogEntity): Long

    @Query("DELETE FROM notification_log WHERE sent_at < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface ReportDao {
    @Query(
        """
        SELECT e.id AS id, e.concept AS concept, e.amount_cents AS amount_cents, e.occurred_at AS occurred_at,
               e.category_id AS category_id, c.name AS category_name, e.nature AS nature,
               e.payment_method AS payment_method,
               (EXISTS (SELECT 1 FROM fixed_payment_record r WHERE r.expense_id = e.id)
                OR COALESCE(b.frequency_type, '') = 'FIXED') AS is_fixed
        FROM expense e
        JOIN expense_category c ON c.id = e.category_id
        LEFT JOIN budget b ON b.id = e.budget_id
        WHERE e.voided = 0 AND e.occurred_at >= :start AND e.occurred_at < :end
        ORDER BY e.occurred_at DESC
        """,
    )
    fun observeExpenses(start: Long, end: Long): Flow<List<ReportExpenseRow>>

    @Query(
        """
        SELECT id, source, amount_cents, received_at, income_type FROM income
        WHERE voided = 0 AND received_at >= :start AND received_at < :end
        ORDER BY received_at DESC
        """,
    )
    fun observeIncomes(start: Long, end: Long): Flow<List<ReportIncomeRow>>
}
