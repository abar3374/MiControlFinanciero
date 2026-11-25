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
