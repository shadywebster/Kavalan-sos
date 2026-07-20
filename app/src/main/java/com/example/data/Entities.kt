package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val relationship: String
)

@Entity(tableName = "safety_tips")
data class SafetyTip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // e.g., "Physical Defense", "Digital Safety", "Rights & Law"
    val steps: String // Semi-colon separated steps
)

@Entity(tableName = "audio_records")
data class AudioRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val fileName: String,
    val timestamp: Long,
    val durationSeconds: Int
)

@Entity(tableName = "police_stations")
data class PoliceStation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val district: String,
    val address: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double
)
