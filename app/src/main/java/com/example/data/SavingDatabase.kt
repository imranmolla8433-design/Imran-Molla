package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saving_payments", primaryKeys = ["userId", "dateIso"])
data class SavingPaymentEntity(
    val userId: String,
    val dateIso: String,
    val isPaid: Boolean,
    val transactionNote: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface SavingPaymentDao {
    @Query("SELECT * FROM saving_payments WHERE userId = :userId ORDER BY dateIso ASC")
    fun observeUserPayments(userId: String): Flow<List<SavingPaymentEntity>>

    @Query("SELECT * FROM saving_payments WHERE userId = :userId ORDER BY dateIso ASC")
    suspend fun getUserPaymentsOnce(userId: String): List<SavingPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPayment(payment: SavingPaymentEntity)

    @Query("DELETE FROM saving_payments WHERE userId = :userId AND dateIso = :dateIso")
    suspend fun deletePaymentByDate(userId: String, dateIso: String)

    @Query("DELETE FROM saving_payments WHERE userId = :userId")
    suspend fun clearUserPayments(userId: String)
}

@Database(
    entities = [SavingPaymentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SavingDatabase : RoomDatabase() {
    abstract fun savingPaymentDao(): SavingPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: SavingDatabase? = null

        fun getInstance(context: Context): SavingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SavingDatabase::class.java,
                    "my_saving_tracker_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
