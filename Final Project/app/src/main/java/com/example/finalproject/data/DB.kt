package com.example.finalproject.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
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

@Entity (
    tableName = "Stands",
    indices = [Index(value = ["name"], unique = true)]
)
data class Stand(@PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val cord: LatLng,
    val sitCount: Int,
    val healthStatus: HealthStatus,
    val userName: String
)

@Dao
interface HuntHealthDAO {
    @Insert
    suspend fun addStand(stand: Stand)

    @Delete
    suspend fun removeStand(stand: Stand)

    @Update
    suspend fun changeStandName(stand: Stand)

    @Query("SELECT * FROM Stands")
    fun getStands(): Flow<List<Stand>>
}

class Converters {
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

@Database(entities = [Stand::class],
    version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class HuntHealthDB: RoomDatabase() {
    abstract fun getInstance(): HuntHealthDAO
}
