package com.micontrolfinanciero.app.core.data.settings

import androidx.room.withTransaction
import com.micontrolfinanciero.app.core.database.FinanceDatabase
import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.database.dao.SettingsDao
import com.micontrolfinanciero.app.core.database.entity.AppSettingEntity
import com.micontrolfinanciero.app.core.database.entity.AuditLogEntity
import com.micontrolfinanciero.app.core.domain.finance.SettingsRepository
import com.micontrolfinanciero.app.core.model.AppSettings
import com.micontrolfinanciero.app.core.model.UpdateChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Ajustes guardados como pares clave-valor en la tabla app_setting (base cifrada). */
@Singleton
class RoomSettingsRepository @Inject constructor(
    private val db: FinanceDatabase,
    private val dao: SettingsDao,
    private val auditDao: AuditDao,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dao.observeAll().map { rows -> fromMap(rows.associate { it.key to it.value }) }.distinctUntilChanged()

    override suspend fun update(change: (AppSettings) -> AppSettings) {
        db.withTransaction {
            val current = observeSettings().first()
            val next = change(current)
            if (next == current) return@withTransaction
            toMap(next).forEach { (k, v) -> dao.put(AppSettingEntity(k, v)) }
            auditDao.insert(
                AuditLogEntity(entity = "app_setting", entityId = 0, action = "UPDATE", details = describeChanges(current, next), at = System.currentTimeMillis()),
            )
        }
    }

    private fun describeChanges(a: AppSettings, b: AppSettings): String = buildList {
        if (a.remindersEnabled != b.remindersEnabled) add("recordatorios=${b.remindersEnabled}")
        if (a.reminderDays != b.reminderDays) add("dias=${b.reminderDays.sorted()}")
        if (a.customReminderDays != b.customReminderDays) add("personalizado=${b.customReminderDays}")
        if (a.reminderHour != b.reminderHour) add("hora=${b.reminderHour}")
        if (a.autoLockMinutes != b.autoLockMinutes) add("bloqueo_min=${b.autoLockMinutes}")
        if (a.secureScreen != b.secureScreen) add("pantalla_segura=${b.secureScreen}")
        if (a.updateChannel != b.updateChannel) add("canal=${b.updateChannel}")
    }.joinToString("; ")

    private companion object {
        const val REMINDERS = "reminders_enabled"
        const val DAYS = "reminder_days"
        const val CUSTOM = "reminder_custom_days"
        const val HOUR = "reminder_hour"
        const val LOCK = "auto_lock_minutes"
        const val SECURE = "secure_screen"
        const val CHANNEL = "update_channel"

        fun fromMap(m: Map<String, String>): AppSettings {
            val d = AppSettings()
            return AppSettings(
                remindersEnabled = m[REMINDERS]?.toBooleanStrictOrNull() ?: d.remindersEnabled,
                reminderDays = m[DAYS]?.let { s -> s.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..60 }.toSet() } ?: d.reminderDays,
                customReminderDays = m[CUSTOM]?.toIntOrNull()?.takeIf { it in 0..60 },
                reminderHour = m[HOUR]?.toIntOrNull()?.takeIf { it in 0..23 } ?: d.reminderHour,
                autoLockMinutes = m[LOCK]?.toIntOrNull()?.takeIf { it in 0..60 } ?: d.autoLockMinutes,
                secureScreen = m[SECURE]?.toBooleanStrictOrNull() ?: d.secureScreen,
                updateChannel = UpdateChannel.entries.firstOrNull { it.name == m[CHANNEL] } ?: d.updateChannel,
            )
        }

        fun toMap(s: AppSettings): Map<String, String> = mapOf(
            REMINDERS to s.remindersEnabled.toString(),
            DAYS to s.reminderDays.sorted().joinToString(","),
            CUSTOM to (s.customReminderDays?.toString() ?: ""),
            HOUR to s.reminderHour.toString(),
            LOCK to s.autoLockMinutes.toString(),
            SECURE to s.secureScreen.toString(),
            CHANNEL to s.updateChannel.name,
        )
    }
}
