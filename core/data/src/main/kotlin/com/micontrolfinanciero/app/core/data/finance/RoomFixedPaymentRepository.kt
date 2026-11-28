package com.micontrolfinanciero.app.core.data.finance

import androidx.room.withTransaction
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.BudgetDao
import com.micontrolfinanciero.app.core.database.dao.CategoryDao
import com.micontrolfinanciero.app.core.database.dao.ExpenseDao
import com.micontrolfinanciero.app.core.database.dao.ExpectedIncomeDao
import com.micontrolfinanciero.app.core.database.dao.FixedPaymentDao
import com.micontrolfinanciero.app.core.database.dao.IncomeDao
import com.micontrolfinanciero.app.core.database.entity.ExpectedIncomeEntity
import com.micontrolfinanciero.app.core.database.entity.ExpectedIncomeRecordEntity
import com.micontrolfinanciero.app.core.database.entity.IncomeEntity
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.database.entity.ExpenseEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentEntity
import com.micontrolfinanciero.app.core.database.entity.FixedPaymentRecordEntity
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentSchedule
import com.micontrolfinanciero.app.core.model.ExpectedIncome
import com.micontrolfinanciero.app.core.model.ExpectedIncomeRecord
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.IncomeDestination
import com.micontrolfinanciero.app.core.model.IncomeType
import com.micontrolfinanciero.app.core.model.MonthlyIncome
import com.micontrolfinanciero.app.core.model.FixedPayment
import com.micontrolfinanciero.app.core.model.FixedPaymentRecord
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.PaymentMethod
import com.micontrolfinanciero.app.core.model.defaultClassification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

internal fun YearMonth.toPeriod(): Int = year * 100 + monthValue
internal fun Int.toYearMonth(): YearMonth = YearMonth.of(this / 100, this % 100)

@Singleton
class RoomFixedPaymentRepository @Inject constructor(
    private val db: FinanceDatabase,
    private val dao: FixedPaymentDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val auditDao: AuditDao,
    private val expectedIncomeDao: ExpectedIncomeDao,
    private val incomeDao: IncomeDao,
) : FixedPaymentRepository {

    private val monthName = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"))

    override fun observeFixedPayments(): Flow<List<FixedPayment>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getFixedPayment(id: Long): FixedPayment? = dao.get(id)?.toModel()

    override suspend fun saveFixedPayment(payment: FixedPayment): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = if (payment.id != 0L) dao.get(payment.id) else null
        val entity = FixedPaymentEntity(
            id = existing?.id ?: 0L,
            name = payment.name,
            categoryId = payment.categoryId,
            budgetId = payment.budgetId,
            amountCents = payment.amount.cents,
            dueDay = payment.dueDay,
            totalInstallments = payment.totalInstallments,
            initialPaidInstallments = payment.initialPaidInstallments,
            startPeriod = payment.startMonth.toPeriod(),
            paymentMethod = payment.paymentMethod.name,
            notes = payment.notes,
            active = payment.active,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            intervalMonths = payment.intervalMonths,
            entity = payment.entity,
            interestRateBp = payment.interestRateBp,
            originalAmountCents = payment.originalAmount?.cents,
        )
        if (existing == null) {
            val id = dao.insert(entity)
            audit("fixed_payment", id, "CREATE", entity.auditText(), now)
            id
        } else {
            dao.update(entity)
            audit("fixed_payment", entity.id, "UPDATE", "antes: ${existing.auditText()} | después: ${entity.auditText()}", now)
            entity.id
        }
    }

    override suspend fun deactivate(id: Long) {
        db.withTransaction {
            val existing = dao.get(id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            dao.update(existing.copy(active = false, updatedAt = now))
            audit("fixed_payment", id, "DELETE", "desactivado: ${existing.auditText()}", now)
        }
    }

    override fun observeRecords(): Flow<List<FixedPaymentRecord>> = dao.observeRecords().map { rows ->
        rows.map {
            FixedPaymentRecord(
                fixedPaymentId = it.fixedPaymentId,
                month = it.period.toYearMonth(),
                expenseId = it.expenseId,
                amount = Money(it.amountCents),
                paidAt = it.occurredAt,
            )
        }
    }

    override fun observeMonth(month: YearMonth, today: () -> LocalDate): Flow<List<MonthlyPayment>> =
        combine(observeFixedPayments(), observeRecords()) { payments, records ->
            FixedPaymentSchedule.forMonth(payments, records, month, today())
        }

    override suspend fun markPaid(
        paymentId: Long,
        month: YearMonth,
        amount: Money,
        paidAt: Long,
        method: PaymentMethod,
    ): Long = db.withTransaction {
        val payment = dao.get(paymentId) ?: throw IllegalArgumentException("El pago fijo no existe")
        val period = month.toPeriod()
        // Si había un registro viejo cuyo gasto fue anulado, se limpia antes de volver a pagar.
        dao.getRecord(paymentId, period)?.let { old ->
            val oldExpense = expenseDao.get(old.expenseId)
            if (oldExpense != null) throw IllegalStateException("Este pago ya está registrado en ese mes")
            dao.deleteRecord(old.id)
        }
        val now = System.currentTimeMillis()
        val model = payment.toModel()
        val records = dao.observeRecordsOnce()
        val installment = FixedPaymentSchedule.statusOf(model, month, records, LocalDate.now()).installmentNumber
        val category = categoryDao.get(payment.categoryId)
        val nature = category?.let { enumOr(it.nature, ExpenseNature.OTHER).defaultClassification() } ?: ExpenseNature.OBLIGATION
        val budgetId = payment.budgetId?.takeIf { id -> budgetDao.get(id)?.active == true }
        val description = buildString {
            append("Pago de ").append(month.format(monthName))
            if (installment != null && payment.totalInstallments != null) {
                append(" · cuota ").append(installment).append(" de ").append(payment.totalInstallments)
            }
        }
        val expense = ExpenseEntity(
            concept = payment.name,
            place = "",
            amountCents = amount.cents,
            occurredAt = paidAt,
            categoryId = payment.categoryId,
            budgetId = budgetId,
            nature = nature.name,
            paymentMethod = method.name,
            description = description,
            createdAt = now,
            updatedAt = now,
        )
        val expenseId = expenseDao.insert(expense)
        dao.insertRecord(FixedPaymentRecordEntity(fixedPaymentId = paymentId, period = period, expenseId = expenseId, createdAt = now))
        audit("expense", expenseId, "CREATE", expense.auditText(), now)
        audit("fixed_payment", paymentId, "UPDATE", "pagado $period con gasto $expenseId", now)
        expenseId
    }

    override suspend fun undoPaid(paymentId: Long, month: YearMonth) {
        db.withTransaction {
            val record = dao.getRecord(paymentId, month.toPeriod()) ?: return@withTransaction
            val now = System.currentTimeMillis()
            expenseDao.get(record.expenseId)?.let { expense ->
                expenseDao.markVoided(expense.id, now)
                audit("expense", expense.id, "DELETE", "deshacer pago del mes: ${expense.auditText()}", now)
            }
            dao.deleteRecord(record.id)
            audit("fixed_payment", paymentId, "UPDATE", "pago de ${month.toPeriod()} deshecho", now)
        }
    }

    // ------------------------------------------------------------------ Ingresos esperados

    override fun observeExpectedIncomes(): Flow<List<ExpectedIncome>> =
        expectedIncomeDao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getExpectedIncome(id: Long): ExpectedIncome? = expectedIncomeDao.get(id)?.toModel()

    override suspend fun saveExpectedIncome(income: ExpectedIncome): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val existing = if (income.id != 0L) expectedIncomeDao.get(income.id) else null
        val entity = ExpectedIncomeEntity(
            id = existing?.id ?: 0L,
            source = income.source,
            amountCents = income.amount.cents,
            day = income.day,
            intervalMonths = income.intervalMonths,
            destination = income.destination.name,
            startPeriod = income.startMonth.toPeriod(),
            notes = income.notes,
            active = income.active,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val details = "origen=${entity.source}; valor_centavos=${entity.amountCents}; dia=${entity.day}"
        if (existing == null) {
            val id = expectedIncomeDao.insert(entity)
            audit("expected_income", id, "CREATE", details, now)
            id
        } else {
            expectedIncomeDao.update(entity)
            audit("expected_income", entity.id, "UPDATE", details, now)
            entity.id
        }
    }

    override suspend fun deactivateExpectedIncome(id: Long) {
        db.withTransaction {
            val existing = expectedIncomeDao.get(id) ?: return@withTransaction
            val now = System.currentTimeMillis()
            expectedIncomeDao.update(existing.copy(active = false, updatedAt = now))
            audit("expected_income", id, "DELETE", "desactivado: ${existing.source}", now)
        }
    }

    private fun observeIncomeRecords(): Flow<List<ExpectedIncomeRecord>> = expectedIncomeDao.observeRecords().map { rows ->
        rows.map { ExpectedIncomeRecord(it.expectedIncomeId, it.period.toYearMonth(), it.incomeId, Money(it.amountCents), it.receivedAt) }
    }

    override fun observeIncomeMonth(month: YearMonth, today: () -> LocalDate): Flow<List<MonthlyIncome>> =
        combine(observeExpectedIncomes(), observeIncomeRecords()) { incomes, records ->
            FixedPaymentSchedule.incomesForMonth(incomes, records, month, today())
        }

    override suspend fun markIncomeReceived(
        expectedId: Long,
        month: YearMonth,
        amount: Money,
        receivedAt: Long,
        destination: IncomeDestination,
    ): Long = db.withTransaction {
        val expected = expectedIncomeDao.get(expectedId) ?: throw IllegalArgumentException("El ingreso esperado no existe")
        val period = month.toPeriod()
        expectedIncomeDao.getRecord(expectedId, period)?.let { old ->
            if (incomeDao.get(old.incomeId) != null) throw IllegalStateException("Este ingreso ya está confirmado en ese mes")
            expectedIncomeDao.deleteRecord(old.id)
        }
        val now = System.currentTimeMillis()
        val income = IncomeEntity(
            source = expected.source,
            amountCents = amount.cents,
            receivedAt = receivedAt,
            incomeType = IncomeType.FIXED.name,
            destination = destination.name,
            notes = "Ingreso fijo de ${month.format(monthName)}",
            createdAt = now,
            updatedAt = now,
        )
        val incomeId = incomeDao.insert(income)
        expectedIncomeDao.insertRecord(ExpectedIncomeRecordEntity(expectedIncomeId = expectedId, period = period, incomeId = incomeId, createdAt = now))
        audit("income", incomeId, "CREATE", income.auditText(), now)
        incomeId
    }

    override suspend fun undoIncomeReceived(expectedId: Long, month: YearMonth) {
        db.withTransaction {
            val record = expectedIncomeDao.getRecord(expectedId, month.toPeriod()) ?: return@withTransaction
            val now = System.currentTimeMillis()
            incomeDao.get(record.incomeId)?.let { income ->
                incomeDao.markVoided(income.id, now)
                audit("income", income.id, "DELETE", "deshacer ingreso esperado: ${income.auditText()}", now)
            }
            expectedIncomeDao.deleteRecord(record.id)
        }
    }

    private fun ExpectedIncomeEntity.toModel() = ExpectedIncome(
        id = id,
        source = source,
        amount = Money(amountCents),
        day = day,
        intervalMonths = intervalMonths,
        destination = enumOr(destination, IncomeDestination.ACCOUNT),
        startMonth = startPeriod.toYearMonth(),
        notes = notes,
        active = active,
    )

    private suspend fun FixedPaymentDao.observeRecordsOnce(): List<FixedPaymentRecord> =
        recordsSnapshot().map {
            FixedPaymentRecord(it.fixedPaymentId, it.period.toYearMonth(), it.expenseId, Money(it.amountCents), it.occurredAt)
        }

    private suspend fun audit(entity: String, id: Long, action: String, details: String, at: Long) {
        auditDao.insert(AuditLogEntity(entity = entity, entityId = id, action = action, details = details, at = at))
    }

    private fun FixedPaymentEntity.toModel() = FixedPayment(
        id = id,
        name = name,
        categoryId = categoryId,
        budgetId = budgetId,
        amount = Money(amountCents),
        dueDay = dueDay,
        totalInstallments = totalInstallments,
        initialPaidInstallments = initialPaidInstallments,
        startMonth = startPeriod.toYearMonth(),
        paymentMethod = enumOr(paymentMethod, PaymentMethod.OTHER),
        notes = notes,
        active = active,
        intervalMonths = intervalMonths,
        entity = entity,
        interestRateBp = interestRateBp,
        originalAmount = originalAmountCents?.let { Money(it) },
    )

    private fun FixedPaymentEntity.auditText() =
        "nombre=$name; valor_centavos=$amountCents; dia=$dueDay; cuotas=$totalInstallments; pagadas_antes=$initialPaidInstallments; activo=$active"
}
