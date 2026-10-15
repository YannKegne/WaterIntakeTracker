package com.example.waterintaketracker

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow


@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_oz") val amountOz: Double,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)


@Dao
interface WaterDao {


    @Query("SELECT * FROM water_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<WaterEntry>>


    @Query("SELECT COALESCE(SUM(amount_oz), 0) FROM water_entries WHERE timestamp >= :since")
    fun getTotalSince(since: Long): Flow<Double>

    @Insert
    suspend fun insert(entry: WaterEntry)

    @Query("DELETE FROM water_entries")
    suspend fun deleteAll()
}


@Database(entities = [WaterEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_intake_db"
                ).build().also { INSTANCE = it }
            }
    }
}
