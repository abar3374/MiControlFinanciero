package com.micontrolfinanciero.app.core.data.backup

import com.micontrolfinanciero.app.core.database.dao.AuditDao
import com.micontrolfinanciero.app.core.domain.backup.AuditEntry
import com.micontrolfinanciero.app.core.domain.backup.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAuditRepository @Inject constructor(private val dao: AuditDao) : AuditRepository {
    override fun observeLatest(limit: Int): Flow<List<AuditEntry>> =
        dao.observeLatest(limit).map { list -> list.map { AuditEntry(it.id, it.entity, it.entityId, it.action, it.details, it.at) } }
}
