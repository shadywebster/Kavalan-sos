package com.example.data

import kotlinx.coroutines.flow.Flow

class KavalanRepository(
    private val contactDao: EmergencyContactDao,
    private val safetyTipDao: SafetyTipDao,
    private val audioRecordDao: AudioRecordDao,
    private val policeStationDao: PoliceStationDao
) {
    val allContacts: Flow<List<EmergencyContact>> = contactDao.getAllContacts()
    val allTips: Flow<List<SafetyTip>> = safetyTipDao.getAllTips()
    val allRecords: Flow<List<AudioRecord>> = audioRecordDao.getAllRecords()
    val allStations: Flow<List<PoliceStation>> = policeStationDao.getAllStations()

    suspend fun insertContact(contact: EmergencyContact) {
        contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: EmergencyContact) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: EmergencyContact) {
        contactDao.deleteContact(contact)
    }

    suspend fun insertRecord(record: AudioRecord) {
        audioRecordDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: AudioRecord) {
        audioRecordDao.deleteRecord(record)
    }

    fun getStationsByDistrict(district: String): Flow<List<PoliceStation>> {
        return policeStationDao.getStationsByDistrict(district)
    }
}
