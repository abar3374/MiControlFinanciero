package com.micontrolfinanciero.app.feature.reports

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.micontrolfinanciero.app.core.domain.finance.CategoryRepository
import com.micontrolfinanciero.app.core.domain.finance.DEFAULT_ZONE
import com.micontrolfinanciero.app.core.domain.finance.MonthRange
import com.micontrolfinanciero.app.core.domain.finance.MovementRepository
import com.micontrolfinanciero.app.core.domain.finance.ReportRepository
import com.micontrolfinanciero.app.core.domain.profile.UserProfileRepository
import com.micontrolfinanciero.app.core.domain.report.Report
import com.micontrolfinanciero.app.core.domain.report.ReportCalculator
import com.micontrolfinanciero.app.core.domain.report.ReportFilters
import com.micontrolfinanciero.app.core.domain.report.ReportPeriod
import com.micontrolfinanciero.app.core.domain.report.ReportPeriodType
import com.micontrolfinanciero.app.core.model.ExpenseCategory
import com.micontrolfinanciero.app.core.model.MonthSummary
import com.micontrolfinanciero.app.core.model.UserProfile
import com.micontrolfinanciero.app.core.reports.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ReportsState(
    val loading: Boolean = true,
    val period: ReportPeriod = ReportPeriod.of(ReportPeriodType.MONTH, LocalDate.now(DEFAULT_ZONE)),
    val filters: ReportFilters = ReportFilters(),
    val report: Report? = null,
    /** Presupuesto vs gasto real (solo en reportes mensuales). */
    val monthSummary: MonthSummary? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val profile: UserProfile = UserProfile.EMPTY,
)

enum class ExportFormat(val mime: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    reportRepository: ReportRepository,
    movementRepository: MovementRepository,
    categoryRepository: CategoryRepository,
    profileRepository: UserProfileRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val period = MutableStateFlow(ReportPeriod.of(ReportPeriodType.MONTH, LocalDate.now(DEFAULT_ZONE)))
    private val filters = MutableStateFlow(ReportFilters())

    private val data = period.flatMapLatest { p ->
        val start = ReportCalculator.dataStart(p).atStartOfDay(DEFAULT_ZONE).toInstant().toEpochMilli()
        reportRepository.observeData(start, p.endMillis())
    }

    private val summary = period.flatMapLatest { p ->
        if (p.type == ReportPeriodType.MONTH) movementRepository.observeMonthSummary(MonthRange.of(YearMonth.from(p.start))) else flowOf(null)
    }

    val state: StateFlow<ReportsState> = combine(
        combine(period, filters, data) { p, f, d -> Triple(p, f, d) },
        summary,
        categoryRepository.observeAllCategories(),
        profileRepository.observeProfile(),
    ) { (p, f, d), s, cats, profile ->
        ReportsState(
            loading = false,
            period = p,
            filters = f,
            report = ReportCalculator.build(p, f, d.first, d.second),
            monthSummary = s,
            categories = cats,
            profile = profile,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun setType(type: ReportPeriodType) { period.value = ReportPeriod.of(type, period.value.start) }
    fun previous() { period.value = period.value.previous() }
    fun next() { period.value = period.value.next() }
    fun setFilters(value: ReportFilters) { filters.value = value }
    fun clearFilters() { filters.value = ReportFilters() }
    fun onMessageShown() { _message.value = null }

    fun suggestedFileName(format: ExportFormat): String {
        val label = period.value.label().lowercase().replace(Regex("[^a-z0-9áéíóúñ]+"), "-").trim('-')
        return "finance-reporte-$label.${format.extension}"
    }

    fun filtersText(s: ReportsState = state.value): String {
        val f = s.filters
        if (!f.isActive) return "Sin filtros"
        return buildList {
            f.categoryId?.let { id -> add("Categoría: " + (s.categories.firstOrNull { it.id == id }?.name ?: "?")) }
            f.nature?.let { add("Clasificación: ${it.label}") }
            f.method?.let { add("Método: ${it.label}") }
            f.fixed?.let { add(if (it) "Solo gastos fijos" else "Solo gastos ocasionales") }
        }.joinToString(" · ")
    }

    /** Escribe el reporte en el archivo que el usuario eligió con el selector de Android. */
    fun export(format: ExportFormat, uri: Uri) {
        val s = state.value
        val report = s.report ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        when (format) {
                            ExportFormat.PDF -> ReportExporter.writePdf(report, filtersText(s), s.profile.fullName, out)
                            ExportFormat.EXCEL -> ReportExporter.writeXlsx(report, filtersText(s), out)
                        }
                    } ?: error("No se pudo abrir el archivo")
                }
            }.onSuccess {
                _message.value = "Reporte guardado (${format.extension.uppercase()}). Recuerda: el archivo no está cifrado."
            }.onFailure {
                _message.value = "No se pudo exportar el reporte."
            }
        }
    }
}
