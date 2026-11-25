package com.micontrolfinanciero.app.feature.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.FinanceValidator
import com.micontrolfinanciero.app.core.domain.finance.FixedPaymentRepository
import com.micontrolfinanciero.app.core.model.Money
import com.micontrolfinanciero.app.core.model.MoneyFormat
import com.micontrolfinanciero.app.core.model.MonthlyPayment
import com.micontrolfinanciero.app.core.model.PaymentMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.inject.Inject

data class MonthlyPaymentsState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(DEFAULT_ZONE),
    val items: List<MonthlyPayment> = emptyList(),
) {
    val total: Money get() = Money.sum(items.map { it.record?.amount ?: it.payment.amount })
    val paid: Money get() = Money.sum(items.mapNotNull { it.record?.amount })
    val pending: Money get() = Money.sum(items.filter { !it.isPaid }.map { it.payment.amount })
    val pendingCount: Int get() = items.count { !it.isPaid }
}

/** Diálogo para registrar un pago: el valor y el método se pueden ajustar. */
data class PayDialogState(
    val item: MonthlyPayment,
    val amountDigits: String,
    val method: PaymentMethod,
    val error: String? = null,
    val saving: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthlyPaymentsViewModel @Inject constructor(
    private val repository: FixedPaymentRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now(DEFAULT_ZONE))

    val state: StateFlow<MonthlyPaymentsState> = combine(
        month,
        month.flatMapLatest { m -> repository.observeMonth(m) { LocalDate.now(DEFAULT_ZONE) } },
    ) { m, items -> MonthlyPaymentsState(loading = false, month = m, items = items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyPaymentsState())

    private val _pay = MutableStateFlow<PayDialogState?>(null)
    val pay: StateFlow<PayDialogState?> = _pay

    private val _confirmUndo = MutableStateFlow<MonthlyPayment?>(null)
    val confirmUndo: StateFlow<MonthlyPayment?> = _confirmUndo

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun previousMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    fun openPay(item: MonthlyPayment) {
        _pay.value = PayDialogState(
            item = item,
            amountDigits = (item.payment.amount.cents / 100).toString(),
            method = item.payment.paymentMethod,
        )
    }

    fun onPayAmount(digits: String) = _pay.update { it?.copy(amountDigits = digits, error = null) }
    fun onPayMethod(method: PaymentMethod) = _pay.update { it?.copy(method = method) }
    fun closePay() { _pay.value = null }

    fun confirmPay() {
        val dialog = _pay.value ?: return
        val units = FinanceValidator.digitsOnly(dialog.amountDigits).toLongOrNull()
        if (units == null || units <= 0L) {
            _pay.update { it?.copy(error = "Escribe el valor pagado.") }
            return
        }
        _pay.update { it?.copy(saving = true) }
        viewModelScope.launch {
            val item = dialog.item
            // Si se paga un mes que no es el actual, el gasto queda con fecha dentro de ese mes.
            val now = ZonedDateTime.now(DEFAULT_ZONE)
            val paidAt = if (YearMonth.from(now) == item.month) {
                now.toInstant().toEpochMilli()
            } else {
                item.dueDate.atTime(12, 0).atZone(DEFAULT_ZONE).toInstant().toEpochMilli()
            }
            runCatching {
                repository.markPaid(item.payment.id, item.month, Money.ofUnits(units), paidAt, dialog.method)
            }.onSuccess {
                _pay.value = null
                val amount = MoneyFormat.COLOMBIA.format(Money.ofUnits(units))
                _message.value = when {
                    item.settlesCredit -> "¡${item.payment.name} quedó saldado! Desde el próximo mes ya no aparece."
                    item.installmentNumber != null ->
                        "Pagaste la cuota ${item.installmentNumber} de ${item.payment.totalInstallments} de ${item.payment.name} ($amount)."
                    else -> "Pago registrado: ${item.payment.name} · $amount. Se agregó a tus gastos."
                }
            }.onFailure { e ->
                _pay.update { it?.copy(saving = false, error = e.message ?: "No se pudo registrar el pago.") }
            }
        }
    }

    fun askUndo(item: MonthlyPayment) { _confirmUndo.value = item }
    fun cancelUndo() { _confirmUndo.value = null }

    fun undo() {
        val item = _confirmUndo.value ?: return
        _confirmUndo.value = null
        viewModelScope.launch {
            repository.undoPaid(item.payment.id, item.month)
            _message.value = "Pago deshecho: ${item.payment.name} vuelve a quedar pendiente."
        }
    }

    fun onMessageShown() { _message.value = null }
}
