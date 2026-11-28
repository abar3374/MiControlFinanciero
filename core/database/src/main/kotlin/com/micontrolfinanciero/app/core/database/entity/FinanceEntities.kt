package com.micontrolfinanciero.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Tablas de la versión 2 de la base de datos (Fase 4).
 * Dinero: amount_cents (Long, centavos). Fechas de movimientos: milisegundos UTC.
 * Enumeraciones: se guarda el nombre interno en inglés (por ejemplo "CASH").
 */

@Entity(
    tableName = "expense_category",
    indices = [Index(value = ["name"], unique = true)],
)
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "nature") val nature: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "archived") val archived: Boolean = false,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "budget",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("category_id")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "frequency_type") val frequencyType: String,
    @ColumnInfo(name = "nature") val nature: String,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "expense",
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
    indices = [Index("occurred_at"), Index("category_id"), Index("budget_id")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "concept") val concept: String,
    @ColumnInfo(name = "place") val place: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "budget_id") val budgetId: Long?,
    @ColumnInfo(name = "nature") val nature: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "description") val description: String,
    /** Borrado lógico: el gasto deja de contar pero su historia se conserva. */
    @ColumnInfo(name = "voided") val voided: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "income",
    indices = [Index("received_at")],
)
data class IncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "income_type") val incomeType: String,
    @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "notes") val notes: String,
    @ColumnInfo(name = "voided") val voided: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/** Registro de operaciones importantes (sección 13, punto 14). */
@Entity(
    tableName = "audit_log",
    indices = [Index("at")],
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Tabla afectada: expense, income, budget, expense_category. */
    @ColumnInfo(name = "entity") val entity: String,
    @ColumnInfo(name = "entity_id") val entityId: Long,
    /** CREATE, UPDATE o DELETE. */
    @ColumnInfo(name = "action") val action: String,
    /** Valores antes y después, en texto. */
    @ColumnInfo(name = "details") val details: String,
    @ColumnInfo(name = "at") val at: Long,
)

/** Fila del historial (gasto o ingreso) armada por una consulta UNION. */
data class MovementRow(
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subtitle") val subtitle: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "at") val at: Long,
    @ColumnInfo(name = "detail") val detail: String,
)

/** Total por rubro o por categoría. */
data class GroupTotal(
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "group_name") val groupName: String,
    @ColumnInfo(name = "total_cents") val totalCents: Long,
)

/** Gasto con los datos de reportes (categoría, y si es fijo por pago fijo o rubro fijo). */
data class ReportExpenseRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "concept") val concept: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "nature") val nature: String,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    @ColumnInfo(name = "is_fixed") val isFixed: Boolean,
)

data class ReportIncomeRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "income_type") val incomeType: String,
)
