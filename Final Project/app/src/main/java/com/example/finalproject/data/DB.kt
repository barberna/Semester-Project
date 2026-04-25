package com.example.finalproject.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.finalproject.HealthStatus
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Entity (
    tableName = "Stands",
    indices = [Index(value = ["name"], unique = true)]
)

data class Stand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
     val name: String,
     val cord: LatLng,
     val sitCount: Int,
     val healthStatus: HealthStatus
)

@Entity(
    tableName = "Sits",
    foreignKeys = [
        ForeignKey(
            entity = Stand::class,
            parentColumns = ["id"],
            childColumns = ["standId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Sit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val standId: Int,
    val standName: String,
    val date: LocalDateTime
)

@Dao
interface HuntHealthDAO {
    // Stands Table Calls
    @Insert
    suspend fun addStand(stand: Stand)

    @Delete
    suspend fun removeStand(stand: Stand)

    @Update
    suspend fun updateStand(stand: Stand)

    @Query("SELECT * FROM Stands")
    fun getStands(): Flow<List<Stand>>

    @Query("SELECT * FROM Stands WHERE id = :id")
    suspend fun getStandId(id: Int): Stand?

    // Sits Table Calls
    @Insert
    suspend fun addSitRecord(sit: Sit)

    @Query("UPDATE Sits SET standName = :newName WHERE standId = :id")
    suspend fun updateSitRecordName(id: Int, newName: String)

    @Query("SELECT COUNT(*) FROM Sits WHERE standId = :id AND date >= :tenDaysAgo")
    suspend fun getStandSitCount(id: Int, tenDaysAgo: LocalDateTime): Int

    @Query("SELECT * FROM Sits WHERE standId = :id ORDER BY date DESC LIMIT 1")
    suspend fun getLatestSitForStand(id: Int): Sit?

    @Query("SELECT * FROM Sits WHERE standId = :standId")
    fun getStandSits(standId: Int): Flow<List<Sit>>

    @Query("SELECT * FROM Sits")
    fun getAllSits(): Flow<List<Sit>>
}

class CordConverters {
    @TypeConverter
    fun fromLatLng(latLng: LatLng): String {
        return "${latLng.latitude},${latLng.longitude}"
    }

    @TypeConverter
    fun toLatLng(latLngString: String): LatLng {
        val parts = latLngString.split(",")
        return LatLng(parts[0].toDouble(), parts[1].toDouble())
    }

    @TypeConverter
    fun fromHealthStatus(status: HealthStatus): String {
        return status.name
    }

    @TypeConverter
    fun toHealthStatus(status: String): HealthStatus {
        return HealthStatus.valueOf(status)
    }
}

class DateConverters {
    @TypeConverter
    fun fromString(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToString(date: LocalDateTime?): String? {
        return date?.toString()
    }
}

@Database(entities = [Stand::class, Sit::class],
    version = 1, exportSchema = true)
@TypeConverters(CordConverters::class, DateConverters::class)
abstract class HuntHealthDB: RoomDatabase() {
    abstract fun getInstance(): HuntHealthDAO
}
