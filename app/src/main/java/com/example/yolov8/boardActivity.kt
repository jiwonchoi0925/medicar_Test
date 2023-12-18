package com.example.yolov8

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import androidx.core.net.toUri

// ...

private fun convertImagePathToUri(imagePath: String): Uri {
    return imagePath.toUri()
}


class boardActivity:AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_post)

        recyclerView = findViewById(R.id.rv_post)
        adapter = PostAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadDataFromDatabase()

        val write=findViewById<Button>(R.id.createPost)
        write.setOnClickListener{
            val goWrite=Intent(this,writePost::class.java)
            startActivity(goWrite)
        }

        val backButton= findViewById<Button>(R.id.home_Button)
        backButton.setOnClickListener {
            finish() // 현재 액티비티를 종료하고 이전 액티비티로 돌아갑니다.
        }
    }

    private fun loadDataFromDatabase() {

        // 이 부분에서 데이터베이스에서 데이터를 가져와서 adapter에 설정하는 작업을 수행
        GlobalScope.launch(Dispatchers.Main) {
            val posts = fetchDataFromDatabase() // fetchDataFromDatabase는 데이터베이스에서 데이터를 가져오는 함수로 직접 구현이 필요

            // 어댑터에 데이터 설정
            adapter.setData(posts)
        }
    }

    private fun fetchDataFromDatabase(): List<Post> {
        // 여기에 데이터베이스에서 데이터를 가져오는 코드를 추가
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_NAME, null, null, null, null, null, null)
        cursor.moveToFirst()
        val posts = mutableListOf<Post>()
        while (!cursor.isAfterLast) {
            val idIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)
            val titleIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TITLE)
            val contentIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CONTENT)
            val imagePathIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IMAGE_PATH)

            val id = cursor.getLong(idIndex)
            val title = cursor.getString(titleIndex)
            val content = cursor.getString(contentIndex)
            //val imagePath = cursor.getString(imagePathIndex)
            val imagePath = if (cursor.isNull(imagePathIndex)) {
                // imagePath가 null일 때의 처리를 여기에 작성하세요.
                // 예를 들면, 기본 이미지의 경로를 설정할 수 있습니다.
                "default_image_path"
            } else {
                cursor.getString(imagePathIndex)
            }

            val post = Post(id, title, content, imagePath)
            posts.add(post)
            cursor.moveToNext()
        }
        cursor.close()
        db.close()
        return posts
    }

}
