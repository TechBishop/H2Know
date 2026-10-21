package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watched_systems")
data class WatchedSystemEntity(
    @PrimaryKey val pwsid: String,
    val systemName: String,
    val zip: String,
    val city: String,
    val state: String,
    val lastCheckedScore: Int,
    val fiveYearDeltaPts: Int,
    val alertOnNewViolations: Boolean = true,
    val alertOnDroughtShifts: Boolean = true,
    val alertOnCommunityTickets: Boolean = true,
    val savedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "community_tickets")
data class CommunityTicketEntity(
    @PrimaryKey val id: String,
    val pwsid: String,
    val systemName: String,
    val zip: String,
    val category: String,
    val title: String,
    val description: String,
    val status: String,
    val severity: String,
    val createdAt: String,
    val linkedContaminantCode: String?,
    val linkedContaminantName: String?,
    val affectedCount: Int,
    val notesPipeDelimited: String
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkTheme: Boolean = true,
    val householdProfileKey: String = "standard",
    val hasSeenAuthPrompt: Boolean = false
)

@Entity(tableName = "home_samples")
data class HomeSampleEntity(
    @PrimaryKey val id: String,
    val pwsid: String,
    val contaminantCode: String,
    val contaminantName: String,
    val unit: String,
    val measuredValue: Double,
    val notes: String,
    val testedAt: String
)

@Entity(tableName = "filter_tracker")
data class FilterTrackerEntity(
    @PrimaryKey val id: Int = 1,
    val filterName: String = "Activated Carbon + Lead Block (NSF-53)",
    val certificationBadge: String = "NSF-53 / NSF-42",
    val installedEpochMs: Long = System.currentTimeMillis() - (28L * 24L * 3600L * 1000L),
    val cycleDays: Int = 90,
    val householdSize: Int = 3
)

@Dao
interface H2KnowDao {
    @Query("SELECT * FROM watched_systems ORDER BY savedAtEpochMs DESC")
    fun observeWatchedSystems(): Flow<List<WatchedSystemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWatchedSystem(entity: WatchedSystemEntity)

    @Query("DELETE FROM watched_systems WHERE pwsid = :pwsid")
    suspend fun deleteWatchedSystem(pwsid: String)

    @Query("SELECT * FROM community_tickets WHERE pwsid = :pwsid OR zip = :zip ORDER BY createdAt DESC")
    fun observeTicketsForSystemOrZip(pwsid: String, zip: String): Flow<List<CommunityTicketEntity>>

    @Query("SELECT * FROM community_tickets ORDER BY createdAt DESC")
    fun observeAllCommunityTickets(): Flow<List<CommunityTicketEntity>>

    @Query("SELECT COUNT(*) FROM community_tickets")
    suspend fun getCommunityTicketCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommunityTicket(ticket: CommunityTicketEntity)

    @Query("SELECT * FROM community_tickets WHERE id = :id LIMIT 1")
    suspend fun getTicketById(id: String): CommunityTicketEntity?

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun observeUserPreferences(): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesOnce(): UserPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(prefs: UserPreferenceEntity)

    @Query("SELECT * FROM home_samples ORDER BY testedAt DESC")
    fun observeAllHomeSamples(): Flow<List<HomeSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeSample(sample: HomeSampleEntity)

    @Query("DELETE FROM home_samples WHERE id = :id")
    suspend fun deleteHomeSample(id: String)

    @Query("SELECT * FROM filter_tracker WHERE id = 1 LIMIT 1")
    fun observeFilterTracker(): Flow<FilterTrackerEntity?>

    @Query("SELECT * FROM filter_tracker WHERE id = 1 LIMIT 1")
    suspend fun getFilterTrackerOnce(): FilterTrackerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFilterTracker(tracker: FilterTrackerEntity)
}

@Database(
    entities = [
        WatchedSystemEntity::class,
        CommunityTicketEntity::class,
        UserPreferenceEntity::class,
        HomeSampleEntity::class,
        FilterTrackerEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class H2KnowDatabase : RoomDatabase() {
    abstract fun dao(): H2KnowDao

    companion object {
        @Volatile
        private var INSTANCE: H2KnowDatabase? = null

        fun getInstance(context: Context): H2KnowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    H2KnowDatabase::class.java,
                    "h2know_water_intelligence.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
