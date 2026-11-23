package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.MonthlyRecordEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudyGroupEntity

@Database(
    entities = [
        CenterEntity::class,
        GradeStageEntity::class,
        StudyGroupEntity::class,
        StudentEntity::class,
        MonthlyRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ledgerDao(): LedgerDao

    companion object {
        private const val DB_NAME = "center_ledger.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        prepopulateDefaults(db)
                    }
                })
                .build()
        }

        private fun prepopulateDefaults(db: SupportSQLiteDatabase) {
            val defaultCenters = listOf(
                "سنتر لوبيتال",
                "سنتر الروبي",
                "سنتر سمارو"
            )
            defaultCenters.forEach { name ->
                db.execSQL("INSERT INTO centers (name) VALUES (?)", arrayOf(name))
            }

            val defaultStages = listOf(
                "أولى إعدادي" to 1,
                "تانية إعدادي" to 2,
                "تالتة إعدادي" to 3,
                "أولى ثانوي" to 4,
                "تانية ثانوي" to 5,
                "تالتة ثانوي" to 6
            )
            defaultStages.forEach { (name, order) ->
                db.execSQL(
                    "INSERT INTO grade_stages (name, sortOrder) VALUES (?, ?)",
                    arrayOf(name, order)
                )
            }
        }
    }
}
