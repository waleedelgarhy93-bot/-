package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.data.model.AttendanceEntity
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
        MonthlyRecordEntity::class,
        AttendanceEntity::class
    ],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }

        /** v2: daily attendance table (old data is kept as is). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`studentId` INTEGER NOT NULL, `groupId` INTEGER NOT NULL, `dateKey` TEXT NOT NULL, " +
                        "`state` TEXT NOT NULL, FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_studentId_dateKey` ON `attendance` (`studentId`, `dateKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_groupId` ON `attendance` (`groupId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_dateKey` ON `attendance` (`dateKey`)")
            }
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
                    arrayOf<Any>(name, order)
                )
            }
        }
    }
}
