package com.micontrolfinanciero.app.core.data.finance

import com.micontrolfinanciero.app.core.database.dao.ReportDao
import com.micontrolfinanciero.app.core.domain.finance.ReportRepository
import com.micontrolfinanciero.app.core.domain.report.ReportExpense
import com.micontrolfinanciero.app.core.domain.report.ReportIncome
import com.micontrolfinanciero.app.core.model.ExpenseNature
import com.micontrolfinanciero.app.core.model.IncomeType
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReportRepository @Inject constructor(private val dao: ReportDao) : ReportRepository {

    override fun observeData(startMillis: Long, endMillis: Long): Flow<Pair<List<ReportExpense>, List<ReportIncome>>> =
        combine(dao.observeExpenses(startMillis, endMillis), dao.observeIncomes(startMillis, endMillis)) { expenses, incomes ->
            expenses.map {
                ReportExpense(
                    id = it.id,
                    concept = it.concept,
                    amount = Money(it.amountCents),
                    at = it.occurredAt,
                    categoryId = it.categoryId,
                    categoryName = it.categoryName,
                    nature = enumOr(it.nature, ExpenseNature.OTHER),
                    method = enumOr(it.paymentMethod, PaymentMethod.OTHER),
                    isFixed = it.isFixed,
                )
            } to incomes.map {
                ReportIncome(it.id, it.source, Money(it.amountCents), it.receivedAt, it.incomeType == IncomeType.FIXED.name)
            }
        }
}
