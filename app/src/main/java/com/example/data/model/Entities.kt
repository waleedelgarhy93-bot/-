package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class StudentStatus {
    ACTIVE,
    SUSPENDED
}

enum class SessionState {
    UNMARKED,
    PRESENT,
    ABSENT,
    EXCUSED;

    fun next(): SessionState = when (this) {
        UNMARKED -> PRESENT
        PRESENT -> ABSENT
        ABSENT -> EXCUSED
        EXCUSED -> UNMARKED
    }
}

@JsonClass(generateAdapter = true)
data class SessionEntry(
    val state: SessionState = SessionState.UNMARKED,
    val dateMillis: Long? = null
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "centers")
data class CenterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String
)

@JsonClass(generateAdapter = true)
@Entity(tableName = "grade_stages")
data class GradeStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "study_groups",
    foreignKeys = [
        ForeignKey(
            entity = CenterEntity::class,
            parentColumns = ["id"],
            childColumns = ["centerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GradeStageEntity::class,
            parentColumns = ["id"],
            childColumns = ["gradeStageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["centerId"]),
        Index(value = ["gradeStageId"])
    ]
)
data class StudyGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val centerId: Long,
    val gradeStageId: Long,
    val groupName: String,
    val scheduleDaysAndTime: String,
    val sessionsPerMonth: Int = 8,
    val monthlyFee: Double
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = StudyGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["siblingGroupId"])
    ]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val groupId: Long,
    val fullName: String,
    val studentPhone: String = "",
    val parentPhone: String = "",
    val notes: String = "",
    val status: StudentStatus = StudentStatus.ACTIVE,
    val siblingGroupId: String? = null,
    val siblingDiscountAmount: Double = 0.0
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "monthly_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "monthYear"], unique = true),
        Index(value = ["groupId"]),
        Index(value = ["monthYear"])
    ]
)
data class MonthlyRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val studentId: Long,
    val groupId: Long,
    val monthYear: String,
    val isPaid: Boolean = false,
    val expectedAmount: Double,
    val paidAmount: Double = 0.0,
    val paymentDate: Long? = null,
    val sessionStatuses: List<SessionEntry> = List(8) { SessionEntry() }
)

/** One student's attendance on one day. [state] is PRESENT or ABSENT (no row = not marked). */
@JsonClass(generateAdapter = true)
@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["studentId", "dateKey"], unique = true),
        Index(value = ["groupId"]),
        Index(value = ["dateKey"])
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val studentId: Long,
    val groupId: Long,
    /** yyyy-MM-dd */
    val dateKey: String,
    val state: String
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = 2,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val centers: List<CenterEntity>,
    val gradeStages: List<GradeStageEntity>,
    val studyGroups: List<StudyGroupEntity>,
    val students: List<StudentEntity>,
    val monthlyRecords: List<MonthlyRecordEntity>,
    val attendance: List<AttendanceEntity> = emptyList()
)
