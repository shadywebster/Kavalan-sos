package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        EmergencyContact::class,
        SafetyTip::class,
        AudioRecord::class,
        PoliceStation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun safetyTipDao(): SafetyTipDao
    abstract fun audioRecordDao(): AudioRecordDao
    abstract fun policeStationDao(): PoliceStationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kavalan_safety_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultTips(database.safetyTipDao())
                    populateDefaultStations(database.policeStationDao())
                }
            }
        }

        private suspend fun populateDefaultTips(safetyTipDao: SafetyTipDao) {
            val tips = listOf(
                SafetyTip(
                    title = "Target Vulnerable Points",
                    category = "Physical Defense",
                    content = "In a close combat situation, your primary goal is to incapacitate the attacker and escape. Aim strictly for target zones where force causes involuntary biological reactions.",
                    steps = "Eyes: Gouge or poke to disrupt vision;Nose: Strike upward with the heel of your palm;Throat: Punch or chop to restrict breathing;Groin: Kick or knee hard;Knees: Kick sideways to break balance"
                ),
                SafetyTip(
                    title = "The Power of Voice",
                    category = "Emergency Action",
                    content = "Your voice is a powerful psychological and tactical tool. Assailants count on silence or panic. Shock them and summon bystanders.",
                    steps = "Shout 'FIRE!' instead of 'HELP!': People are statistically far more responsive to fire warnings;Use authoritative, loud commands: Shout 'BACK OFF!' or 'STOP!' in a deep chest voice;Inhale deeply into your stomach before screaming to project maximum decibels"
                ),
                SafetyTip(
                    title = "Palm Heel Strike",
                    category = "Physical Defense",
                    content = "Striking with a closed fist carries a high risk of fracturing your fingers on bone. A palm heel strike is easier, safer, and highly devastating.",
                    steps = "Align your wrist directly under your hand;Thrust your hand outward, leading with the thick, hard base of your palm;Target the chin, nose, or solar plexus;Push fully through the target as if hitting through their head"
                ),
                SafetyTip(
                    title = "Makeshift Tools",
                    category = "Physical Defense",
                    content = "Common everyday items can immediately transform into potent defensive shields or force multipliers when cornered.",
                    steps = "Keys: Hold them between your index and middle finger, locked in a tight fist;Umbrella/Water Bottle: Use to strike or keep distance between you and the threat;Sand/Dirt/Debris: Throw directly at the attacker's eyes to buy seconds to run;Flashlight: Aim a bright beam directly into their eyes to temporarily blind them"
                ),
                SafetyTip(
                    title = "Digital Safeguards",
                    category = "Digital Safety",
                    content = "Modern smartphones have built-in emergency options. Configure these to streamline communication with authorities without looking at your screen.",
                    steps = "Configure Emergency SOS (5-clicks of Power button on Android);Enable continuous real-time location sharing via Google Maps with family;Keep emergency contacts saved under custom tags like 'ICE' (In Case of Emergency)"
                ),
                SafetyTip(
                    title = "Your Legal Rights in India",
                    category = "Rights & Law",
                    content = "Knowing your legal protection empowers you. Indian law provides specific safeguards for women contacting law enforcement.",
                    steps = "Zero FIR: A woman can file an FIR at ANY police station, regardless of where the incident occurred;Sunset Rule: A woman cannot be arrested after sunset and before sunrise except in extraordinary circumstances by a lady officer;Online Complaints: You can file an anonymous complaint on the National Cyber Crime portal"
                )
            )
            safetyTipDao.insertAllTips(tips)
        }

        private suspend fun populateDefaultStations(policeStationDao: PoliceStationDao) {
            val stations = listOf(
                PoliceStation(
                    name = "AWPS Thousand Lights (Women)",
                    district = "Chennai",
                    address = "Greams Road, Thousand Lights, Chennai - 600006",
                    phoneNumber = "04423452585",
                    latitude = 13.0562,
                    longitude = 80.2514
                ),
                PoliceStation(
                    name = "AWPS Mylapore (Women)",
                    district = "Chennai",
                    address = "Kutchery Road, Mylapore, Chennai - 600004",
                    phoneNumber = "04423452595",
                    latitude = 13.0330,
                    longitude = 80.2676
                ),
                PoliceStation(
                    name = "AWPS T. Nagar (Women)",
                    district = "Chennai",
                    address = "Madhavan Park, T. Nagar, Chennai - 600017",
                    phoneNumber = "04423452605",
                    latitude = 13.0401,
                    longitude = 80.2337
                ),
                PoliceStation(
                    name = "AWPS Guindy (Women)",
                    district = "Chennai",
                    address = "Race Course Road, Guindy, Chennai - 600032",
                    phoneNumber = "04423452615",
                    latitude = 13.0067,
                    longitude = 80.2206
                ),
                PoliceStation(
                    name = "AWPS Central Coimbatore (Women)",
                    district = "Coimbatore",
                    address = "State Bank Road, Gopalapuram, Coimbatore - 641018",
                    phoneNumber = "04222300959",
                    latitude = 11.0018,
                    longitude = 76.9682
                ),
                PoliceStation(
                    name = "AWPS Madurai Town (Women)",
                    district = "Madurai",
                    address = "Crime Branch Complex, Madurai Town, Madurai - 625001",
                    phoneNumber = "04522346303",
                    latitude = 9.9179,
                    longitude = 78.1196
                ),
                PoliceStation(
                    name = "AWPS Cantonment Trichy (Women)",
                    district = "Trichy",
                    address = "Cantonment, Trichy - 620001",
                    phoneNumber = "04312415411",
                    latitude = 10.8049,
                    longitude = 78.6856
                )
            )
            policeStationDao.insertAllStations(stations)
        }
    }
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY name ASC")
    fun getAllContacts(): kotlinx.coroutines.flow.Flow<List<EmergencyContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContact)

    @Update
    suspend fun updateContact(contact: EmergencyContact)

    @Delete
    suspend fun deleteContact(contact: EmergencyContact)
}

@Dao
interface SafetyTipDao {
    @Query("SELECT * FROM safety_tips ORDER BY id ASC")
    fun getAllTips(): kotlinx.coroutines.flow.Flow<List<SafetyTip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTip(tip: SafetyTip)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTips(tips: List<SafetyTip>)
}

@Dao
interface AudioRecordDao {
    @Query("SELECT * FROM audio_records ORDER BY timestamp DESC")
    fun getAllRecords(): kotlinx.coroutines.flow.Flow<List<AudioRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AudioRecord)

    @Delete
    suspend fun deleteRecord(record: AudioRecord)
}

@Dao
interface PoliceStationDao {
    @Query("SELECT * FROM police_stations ORDER BY name ASC")
    fun getAllStations(): kotlinx.coroutines.flow.Flow<List<PoliceStation>>

    @Query("SELECT * FROM police_stations WHERE district = :district ORDER BY name ASC")
    fun getStationsByDistrict(district: String): kotlinx.coroutines.flow.Flow<List<PoliceStation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: PoliceStation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStations(stations: List<PoliceStation>)
}
