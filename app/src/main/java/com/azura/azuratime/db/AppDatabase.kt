package com.azura.azuratime.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.azura.azuratime.db.PhoneIdEntity
import com.azura.azuratime.db.PhoneIdDao

@Database(
    entities = [
        ClassOption::class,
        SubClassOption::class,
        GradeOption::class,
        SubGradeOption::class,
        ProgramOption::class,
        RoleOption::class,
        FaceEntity::class,
        CheckInEntity::class, // Use new entity
        UserEntity::class, // Added UserEntity for authentication
        PhoneIdEntity::class // RE-ENABLED for PhoneIdDao
    ],
    version = 9, // Bump version for schema change
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun faceDao(): FaceDao
    abstract fun classOptionDao(): ClassOptionDao
    abstract fun subClassOptionDao(): SubClassOptionDao
    abstract fun gradeOptionDao(): GradeOptionDao
    abstract fun subGradeOptionDao(): SubGradeOptionDao
    abstract fun programOptionDao(): ProgramOptionDao
    abstract fun roleOptionDao(): RoleOptionDao
    abstract fun checkInDao(): CheckInDao // Use new DAO
    abstract fun userDao(): UserDao // Added UserDao
    abstract fun phoneIdDao(): PhoneIdDao // RE-ENABLED for PhoneIdDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "face_db"
                )
                .fallbackToDestructiveMigration() // For simplicity, recreate DB on schema change 
                .build().also { INSTANCE = it }
            }
    }
}
