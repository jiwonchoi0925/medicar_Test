package com.example.yolov8

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "YourDatabaseName"
        private const val DATABASE_VERSION = 1

        // 테이블 이름과 컬럼 이름 정의
        const val TABLE_NAME = "posts"
        const val COLUMN_ID = "id"
        const val COLUMN_TITLE = "title"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_IMAGE_PATH = "image_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 테이블 생성 SQL 쿼리 작성
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_TITLE TEXT," +
                "$COLUMN_CONTENT TEXT," +
                "$COLUMN_IMAGE_PATH TEXT);"

        // 쿼리 실행
        db.execSQL(createTableQuery)
    }
    fun deletePost(postId: Long): Boolean {
        val db = writableDatabase
        val deleteResult = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(postId.toString()))
        db.close()
        return deleteResult > 0
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 데이터베이스 업그레이드 시 필요한 작업 수행
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }



    // 추가적인 메서드가 필요할 경우 여기에 작성
}
