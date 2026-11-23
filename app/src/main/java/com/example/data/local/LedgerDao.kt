package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CenterEntity
import com.example.data.model.GradeStageEntity
import com.example.data.model.MonthlyRecordEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentStatus
import com.example.data.model.StudyGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    // --- Centers ---
    @Query("SELECT * FROM centers ORDER BY id ASC")
    fun observeCenters(): Flow<List<CenterEntity>>

    @Query("SELECT * FROM centers ORDER BY id ASC")
    suspend fun getAllCenters(): List<CenterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenter(center: CenterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCenters(centers: List<CenterEntity>)

    @Update
    suspend fun updateCenter(center: CenterEntity)

    @Delete
    suspend fun deleteCenter(center: CenterEntity)

    @Query("DELETE FROM centers")
    suspend fun clearCenters()

    // --- Grade Stages ---
    @Query("SELECT * FROM grade_stages ORDER BY sortOrder ASC, id ASC")
    fun observeGradeStages(): Flow<List<GradeStageEntity>>

    @Query("SELECT * FROM grade_stages ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllGradeStages(): List<GradeStageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeStage(stage: GradeStageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeStages(stages: List<GradeStageEntity>)

    @Update
    suspend fun updateGradeStage(stage: GradeStageEntity)

    @Delete
    suspend fun deleteGradeStage(stage: GradeStageEntity)

    @Query("DELETE FROM grade_stages")
    suspend fun clearGradeStages()

    // --- Study Groups ---
    @Query("SELECT * FROM study_groups ORDER BY centerId ASC, gradeStageId ASC, id ASC")
    fun observeStudyGroups(): Flow<List<StudyGroupEntity>>

    @Query("SELECT * FROM study_groups WHERE id = :groupId LIMIT 1")
    fun observeStudyGroupById(groupId: Long): Flow<StudyGroupEntity?>

    @Query("SELECT * FROM study_groups WHERE id = :groupId LIMIT 1")
    suspend fun getStudyGroupById(groupId: Long): StudyGroupEntity?

    @Query("SELECT * FROM study_groups ORDER BY id ASC")
    suspend fun getAllStudyGroups(): List<StudyGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroup(group: StudyGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroups(groups: List<StudyGroupEntity>)

    @Update
    suspend fun updateStudyGroup(group: StudyGroupEntity)

    @Delete
    suspend fun deleteStudyGroup(group: StudyGroupEntity)

    @Query("DELETE FROM study_groups")
    suspend fun clearStudyGroups()

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY fullName ASC")
    fun observeAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE groupId = :groupId ORDER BY fullName ASC")
    fun observeStudentsByGroup(groupId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :studentId LIMIT 1")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE status = :status")
    suspend fun getStudentsByStatus(status: StudentStatus): List<StudentEntity>

    @Query("SELECT * FROM students WHERE siblingGroupId = :siblingGroupId ORDER BY fullName ASC")
    suspend fun getStudentsBySiblingGroup(siblingGroupId: String): List<StudentEntity>

    @Query("SELECT * FROM students ORDER BY id ASC")
    suspend fun getAllStudents(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun clearStudents()

    // --- Monthly Records ---
    @Query("SELECT * FROM monthly_records WHERE monthYear = :monthYear")
    fun observeMonthlyRecordsByMonth(monthYear: String): Flow<List<MonthlyRecordEntity>>

    @Query("SELECT * FROM monthly_records WHERE groupId = :groupId AND monthYear = :monthYear")
    fun observeMonthlyRecordsByGroupAndMonth(groupId: Long, monthYear: String): Flow<List<MonthlyRecordEntity>>

    @Query("SELECT * FROM monthly_records WHERE studentId = :studentId AND monthYear = :monthYear LIMIT 1")
    suspend fun getMonthlyRecord(studentId: Long, monthYear: String): MonthlyRecordEntity?

    @Query("SELECT * FROM monthly_records WHERE monthYear = :monthYear")
    suspend fun getMonthlyRecordsForMonth(monthYear: String): List<MonthlyRecordEntity>

    @Query("SELECT * FROM monthly_records ORDER BY id ASC")
    suspend fun getAllMonthlyRecords(): List<MonthlyRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMonthlyRecordIgnore(record: MonthlyRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyRecordsReplace(records: List<MonthlyRecordEntity>)

    @Update
    suspend fun updateMonthlyRecord(record: MonthlyRecordEntity)

    @Query("DELETE FROM monthly_records")
    suspend fun clearMonthlyRecords()
}
