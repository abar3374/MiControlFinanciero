package com.micontrolfinanciero.app.core.data.finance

import com.micontrolfinanciero.app.core.database.entity.BudgetEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseCategoryEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseEntity
import com.micontrolfinanciero.app.core.database.entity.IncomeEntity
import com.micontrolfinanciero.app.core.database.entity.MovementRow
import com.micontrolfinanciero.app.core.model.Budget
import com.micontrolfinanciero.app.core.model.Expense
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.ExpenseFrequencyType
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.Income
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.IncomeType
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MovementItem
import com.micontrolfinanciero.app.core.model.MovementKind
import com.micontrolfinanciero.app.core.model.PaymentMethod
import java.time.LocalDate

/** Convierte texto guardado a enumeración sin fallar si el valor es desconocido. */
internal inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

internal fun ExpenseCategoryEntity.toModel() = ExpenseCategory(
    id = id,
    name = name,
    nature = enumOr(nature, ExpenseNature.OTHER),
    isDefault = isDefault,
    archived = archived,
)

internal fun BudgetEntity.toModel() = Budget(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    amount = Money(amountCents),
    frequencyType = enumOr(frequencyType, ExpenseFrequencyType.OCCASIONAL),
    nature = enumOr(nature, ExpenseNature.OTHER),
    startDate = LocalDate.ofEpochDay(startEpochDay),
    active = active,
    notes = notes,
)

internal fun ExpenseEntity.toModel() = Expense(
    id = id,
    concept = concept,
    place = place,
    amount = Money(amountCents),
    occurredAt = occurredAt,
    categoryId = categoryId,
    budgetId = budgetId,
    nature = enumOr(nature, ExpenseNature.OTHER),
    paymentMethod = enumOr(paymentMethod, PaymentMethod.OTHER),
    description = description,
)

internal fun IncomeEntity.toModel() = Income(
    id = id,
    source = source,
    amount = Money(amountCents),
    receivedAt = receivedAt,
    type = enumOr(incomeType, IncomeType.VARIABLE),
    destination = enumOr(destination, IncomeDestination.OTHER),
    notes = notes,
)

internal fun MovementRow.toModel(): MovementItem {
    val kind = enumOr(kind, MovementKind.EXPENSE)
    return MovementItem(
        kind = kind,
        id = id,
        title = title,
        subtitle = if (kind == MovementKind.INCOME) enumOr(subtitle, IncomeType.VARIABLE).label else subtitle,
        amount = Money(amountCents),
        at = at,
        detail = detail,
    )
}

/** Texto corto para la auditoría (no incluye nada que no esté ya en la base cifrada). */
internal fun ExpenseEntity.auditText() =
    "concepto=$concept; valor_centavos=$amountCents; fecha=$occurredAt; categoria=$categoryId; rubro=$budgetId; metodo=$paymentMethod"

internal fun IncomeEntity.auditText() =
    "origen=$source; valor_centavos=$amountCents; fecha=$receivedAt; tipo=$incomeType; destino=$destination"

internal fun BudgetEntity.auditText() =
    "nombre=$name; valor_centavos=$amountCents; categoria=$categoryId; activo=$active"
