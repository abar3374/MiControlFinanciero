package com.micontrolfinanciero.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Tablas de la versión 3 (0.4.0): pagos fijos mensuales y sus pagos por mes.
 * period = año * 100 + mes (por ejemplo 202611 = noviembre de 2026).
 */

@Entity(
    tableName = "fixed_payment",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("category_id"), Index("budget_id")],
)
data class FixedPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "budget_id") val budgetId: Long?,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "due_day") val dueDay: Int,
    /** null = se repite sin fin; número = crédito con cuotas. */
    @ColumnInfo(name = "total_installments") val totalInstallments: Int?,
    @ColumnInfo(name = "initial_paid_installments") val initialPaidInstallments: Int,
    @ColumnInfo(name = "start_period") val startPeriod: Int,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    // ---- v4 (0.5.0)
    @ColumnInfo(name = "interval_months", defaultValue = "1") val intervalMonths: Int = 1,
    @ColumnInfo(name = "entity", defaultValue = "") val entity: String = "",
    @ColumnInfo(name = "interest_rate_bp") val interestRateBp: Int? = null,
    @ColumnInfo(name = "original_amount_cents") val originalAmountCents: Long? = null,
)

@Entity(
    tableName = "fixed_payment_record",
    foreignKeys = [
        ForeignKey(
            entity = FixedPaymentEntity::class,
            parentColumns = ["id"],
            childColumns = ["fixed_payment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["fixed_payment_id", "period"], unique = true),
        Index("expense_id"),
    ],
)
data class FixedPaymentRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "fixed_payment_id") val fixedPaymentId: Long,
    @ColumnInfo(name = "period") val period: Int,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Pago registrado con el valor y la fecha reales del gasto enlazado. */
data class RecordWithExpense(
    @ColumnInfo(name = "fixed_payment_id") val fixedPaymentId: Long,
    @ColumnInfo(name = "period") val period: Int,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
)

/* ------------------------------------------------------------------ v4 (0.5.0) */

/** Ingreso fijo esperado (salario). */
@Entity(tableName = "expected_income")
data class ExpectedIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "day") val day: Int,
    @ColumnInfo(name = "interval_months") val intervalMonths: Int,
    @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "start_period") val startPeriod: Int,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "expected_income_record",
    foreignKeys = [
        ForeignKey(entity = ExpectedIncomeEntity::class, parentColumns = ["id"], childColumns = ["expected_income_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = IncomeEntity::class, parentColumns = ["id"], childColumns = ["income_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index(value = ["expected_income_id", "period"], unique = true), Index("income_id")],
)
data class ExpectedIncomeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "expected_income_id") val expectedIncomeId: Long,
    @ColumnInfo(name = "period") val period: Int,
    @ColumnInfo(name = "income_id") val incomeId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

data class IncomeRecordWithIncome(
    @ColumnInfo(name = "expected_income_id") val expectedIncomeId: Long,
    @ColumnInfo(name = "period") val period: Int,
    @ColumnInfo(name = "income_id") val incomeId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
)

/** Comprobante de un gasto. El archivo vive en filesDir/attachments. */
@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(entity = ExpenseEntity::class, parentColumns = ["id"], childColumns = ["expense_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("expense_id")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "expense_id") val expenseId: Long,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** Ajustes de la app como pares clave-valor (recordatorios, bloqueo, canal de actualizaciones…). */
@Entity(tableName = "app_setting")
data class AppSettingEntity(
    @PrimaryKey @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String,
)

/** Recordatorios ya enviados: evita notificaciones duplicadas. */
@Entity(
    tableName = "notification_log",
    indices = [Index(value = ["kind", "ref_id", "period", "days_before"], unique = true)],
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "ref_id") val refId: Long,
    @ColumnInfo(name = "period") val period: Int,
    @ColumnInfo(name = "days_before") val daysBefore: Int,
    @ColumnInfo(name = "sent_at") val sentAt: Long,
)
