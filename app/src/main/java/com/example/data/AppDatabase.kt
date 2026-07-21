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
                ),
                PoliceStation(
                    name = "AWPS Salem Town (Women)",
                    district = "Salem",
                    address = "AWPS Fort, Salem Town, Salem - 636001",
                    phoneNumber = "04272210352",
                    latitude = 11.6643,
                    longitude = 78.1460
                ),
                PoliceStation(
                    name = "AWPS Tirunelveli Town (Women)",
                    district = "Tirunelveli",
                    address = "Town Police Complex, Tirunelveli - 627006",
                    phoneNumber = "04622333010",
                    latitude = 8.7284,
                    longitude = 77.6891
                ),
                PoliceStation(
                    name = "AWPS Vellore (Women)",
                    district = "Vellore",
                    address = "Filterbed Road, Vellore - 632001",
                    phoneNumber = "04162220113",
                    latitude = 12.9165,
                    longitude = 79.1325
                ),
                PoliceStation(
                    name = "AWPS Thanjavur Town (Women)",
                    district = "Thanjavur",
                    address = "Near South Gate, Thanjavur - 613001",
                    phoneNumber = "04362230303",
                    latitude = 10.7870,
                    longitude = 79.1378
                ),
                PoliceStation(
                    name = "AWPS Dindigul Town (Women)",
                    district = "Dindigul",
                    address = "Near Bus Stand, Dindigul - 624001",
                    phoneNumber = "04512431405",
                    latitude = 10.3673,
                    longitude = 77.9803
                ),
                PoliceStation(
                    name = "AWPS Erode Town (Women)",
                    district = "Erode",
                    address = "AWPS Erode Town, Brough Road, Erode - 638001",
                    phoneNumber = "04242250100",
                    latitude = 11.3410,
                    longitude = 77.7172
                ),
                PoliceStation(
                    name = "AWPS Tiruppur Town (Women)",
                    district = "Tiruppur",
                    address = "AWPS Palladam Road, Tiruppur - 641601",
                    phoneNumber = "04212242100",
                    latitude = 11.1085,
                    longitude = 77.3411
                ),
                PoliceStation(
                    name = "AWPS Thoothukudi Central (Women)",
                    district = "Thoothukudi",
                    address = "AWPS Beach Road, Thoothukudi - 628001",
                    phoneNumber = "04612320100",
                    latitude = 8.8106,
                    longitude = 78.1348
                ),
                PoliceStation(
                    name = "AWPS Nagercoil Town (Women)",
                    district = "Kanyakumari",
                    address = "Court Road, Nagercoil, Kanyakumari - 629001",
                    phoneNumber = "04652220100",
                    latitude = 8.1833,
                    longitude = 77.4119
                ),
                PoliceStation(
                    name = "AWPS Kanchipuram (Women)",
                    district = "Kanchipuram",
                    address = "AWPS Taluk Office Complex, Kanchipuram - 631501",
                    phoneNumber = "04427222100",
                    latitude = 12.8342,
                    longitude = 79.7037
                ),
                PoliceStation(
                    name = "AWPS Cuddalore (Women)",
                    district = "Cuddalore",
                    address = "AWPS Semmandalam, Cuddalore - 607001",
                    phoneNumber = "04142230100",
                    latitude = 11.7480,
                    longitude = 79.7714
                ),
                PoliceStation(
                    name = "AWPS Dharmapuri (Women)",
                    district = "Dharmapuri",
                    address = "AWPS Netaji Bypass Road, Dharmapuri - 636701",
                    phoneNumber = "04342230100",
                    latitude = 12.1356,
                    longitude = 78.1581
                ),
                PoliceStation(
                    name = "AWPS Krishnagiri (Women)",
                    district = "Krishnagiri",
                    address = "AWPS Rayakottai Road, Krishnagiri - 635001",
                    phoneNumber = "04343230100",
                    latitude = 12.5266,
                    longitude = 78.2137
                ),
                PoliceStation(
                    name = "AWPS Pudukkottai (Women)",
                    district = "Pudukkottai",
                    address = "AWPS Alangudi Road, Pudukkottai - 622001",
                    phoneNumber = "04322230100",
                    latitude = 10.3797,
                    longitude = 78.8206
                ),
                PoliceStation(
                    name = "AWPS Sivakasi (Women)",
                    district = "Virudhunagar",
                    address = "AWPS Police Station Road, Sivakasi - 626123",
                    phoneNumber = "04562230100",
                    latitude = 9.4533,
                    longitude = 77.7915
                ),
                PoliceStation(
                    name = "AWPS Karur (Women)",
                    district = "Karur",
                    address = "AWPS Thanthonimalai, Karur - 639001",
                    phoneNumber = "04324230100",
                    latitude = 10.9602,
                    longitude = 78.0766
                ),
                PoliceStation(
                    name = "AWPS Namakkal (Women)",
                    district = "Namakkal",
                    address = "AWPS Mohanoor Road, Namakkal - 637001",
                    phoneNumber = "02862222100",
                    latitude = 11.2189,
                    longitude = 78.1672
                ),
                PoliceStation(
                    name = "AWPS Ooty Town (Women)",
                    district = "Nilgiris",
                    address = "AWPS Garden Road, Ooty - 643001",
                    phoneNumber = "04232440100",
                    latitude = 11.4102,
                    longitude = 76.6950
                ),
                PoliceStation(
                    name = "AWPS Ramanathapuram (Women)",
                    district = "Ramanathapuram",
                    address = "AWPS Kenikarai, Ramanathapuram - 623501",
                    phoneNumber = "04567230100",
                    latitude = 9.3639,
                    longitude = 78.8395
                ),
                PoliceStation(
                    name = "AWPS Sivaganga (Women)",
                    district = "Sivaganga",
                    address = "AWPS Near Collectorate, Sivaganga - 630561",
                    phoneNumber = "04575230100",
                    latitude = 9.8433,
                    longitude = 78.4806
                ),
                PoliceStation(
                    name = "AWPS Theni (Women)",
                    district = "Theni",
                    address = "AWPS Madurai Road, Theni - 625531",
                    phoneNumber = "04546230100",
                    latitude = 10.0104,
                    longitude = 77.4768
                ),
                PoliceStation(
                    name = "AWPS Tiruvallur (Women)",
                    district = "Tiruvallur",
                    address = "AWPS JN Road, Tiruvallur - 602001",
                    phoneNumber = "04427660100",
                    latitude = 13.1438,
                    longitude = 79.9079
                ),
                PoliceStation(
                    name = "AWPS Tiruvannamalai (Women)",
                    district = "Tiruvannamalai",
                    address = "AWPS Polur Road, Tiruvannamalai - 606601",
                    phoneNumber = "04175230100",
                    latitude = 12.2253,
                    longitude = 79.0747
                ),
                PoliceStation(
                    name = "AWPS Tiruvarur (Women)",
                    district = "Tiruvarur",
                    address = "AWPS Nethaji Road, Tiruvarur - 610001",
                    phoneNumber = "04366230100",
                    latitude = 10.7725,
                    longitude = 79.6361
                ),
                PoliceStation(
                    name = "AWPS Villupuram (Women)",
                    district = "Villupuram",
                    address = "AWPS Trichy Trunk Road, Villupuram - 605602",
                    phoneNumber = "04146230100",
                    latitude = 11.9401,
                    longitude = 79.4861
                ),
                PoliceStation(
                    name = "AWPS Nagapattinam (Women)",
                    district = "Nagapattinam",
                    address = "AWPS Public Office Road, Nagapattinam - 611001",
                    phoneNumber = "04365230100",
                    latitude = 10.7661,
                    longitude = 79.8433
                ),
                PoliceStation(
                    name = "AWPS Mayiladuthurai (Women)",
                    district = "Mayiladuthurai",
                    address = "AWPS Kachery Road, Mayiladuthurai - 609001",
                    phoneNumber = "04364230100",
                    latitude = 11.1018,
                    longitude = 79.6525
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
