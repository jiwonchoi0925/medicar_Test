package com.example.yolov8

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import android.provider.MediaStore
import android.widget.EditText
import android.content.ContentValues
import android.os.Build
import android.provider.BaseColumns
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class writePost : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1 // 갤러리에서 이미지 선택을 위한 요청 코드

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var imageView: ImageView
    private lateinit var buttonUpload: Button
    private lateinit var createPostButton: Button
    private lateinit var adapter: PostAdapter
    private var selectedImagePath: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.write_post)

        dbHelper = DatabaseHelper(this)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        // 레이아웃에서 뷰들을 찾음
        imageView = findViewById(R.id.imageView2)
        buttonUpload = findViewById(R.id.buttonUploadFile)
        createPostButton = findViewById(R.id.createPost)
    //
        val post = intent.getSerializableExtra("post")
        if (post != null) {
            val postObj = post as Post
            editTextTitle.setText(postObj.title)
            editTextContent.setText(postObj.content)
            Glide.with(this)
                .load(postObj.imagePath)
                .into(imageView)
        } else {
            // "post" 값이 null일 때의 처리를 여기에 작성하세요.
        }

        // 버튼 클릭 시 갤러리 열도록 함
        buttonUpload.setOnClickListener {
            openGallery()
        }

        val backButton= findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // 현재 액티비티를 종료하고 이전 액티비티로 돌아갑니다.
        }

        createPostButton.setOnClickListener {
            val post = intent.getSerializableExtra("post") as? Post
            savePostToDatabase(post)
            loadDataFromDatabase()
            finish()
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
            val imagePath = cursor.getString(imagePathIndex)

            val post = Post(id, title, content, imagePath)
            posts.add(post)
            cursor.moveToNext()
        }
        cursor.close()
        db.close()
        return posts
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }


    // 갤러리에서 이미지 선택 후 돌아왔을 때 실행되는 콜백 메서드
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!

            // 선택한 이미지를 ImageView에 표시하거나 업로드 등의 작업 수행
            selectedImagePath = getImagePath(imageUri)
            imageView.setImageURI(imageUri)

            // 선택한 이미지의 실제 경로를 가져오려면 다음과 같이 처리할 수 있습니다.
            //val imagePath: String = getImagePath(imageUri)
        }

    }

    // 선택한 이미지의 실제 경로를 가져오는 메서드 (참고용, 실제로 사용 시 더 많은 처리가 필요할 수 있음)
    private fun getImagePath(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val columnIndex: Int = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA) ?: 0
        val imagePath: String = cursor?.getString(columnIndex) ?: ""
        cursor?.close()
        return imagePath
    }


    private fun savePostToDatabase(post: Post? = null) {

        val title = editTextTitle.text.toString()
        val content = editTextContent.text.toString()

        if (title.isNotEmpty() && content.isNotEmpty()) {
            val values = ContentValues()
            values.put("title", title)
            values.put("content", content)
            //values.put("image_path", selectedImagePath)

            // selectedImagePath가 null이 아니면, 즉 새로운 이미지가 선택되었다면 그 값을 사용
            // 그렇지 않고 post의 imagePath가 존재한다면 기존의 imagePath를 사용
            if (selectedImagePath != null) {
                values.put("image_path", selectedImagePath)
            } else if (post?.imagePath != null) {
                values.put("image_path", post.imagePath)
            }

            val db = dbHelper.writableDatabase
            if (post?.id == null) {
                // 게시글이 새로 생성되는 경우, 데이터베이스에 새로운 행을 추가합니다.
                val newRowId = db.insert(DatabaseHelper.TABLE_NAME, null, values)

                // newRowId를 사용하여 새로 추가된 행의 ID를 확인할 수 있음
                if (newRowId != -1L) {
                    Toast.makeText(this, "데이터가 성공적으로 저장되었습니다. ID: $newRowId", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 기존의 게시글이 수정되는 경우, 데이터베이스에서 해당 행을 업데이트합니다.
                val selection = "${DatabaseHelper.COLUMN_ID} LIKE ?"
                val selectionArgs = arrayOf(post.id.toString())
                val count = db.update(DatabaseHelper.TABLE_NAME, values, selection, selectionArgs)

                if (count > 0) {
                    Toast.makeText(this, "게시글이 성공적으로 수정되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            // TODO: 다른 후속 작업 수행 또는 UI 업데이트 등
        } else {
            // 사용자에게 제목과 내용을 모두 입력하라는 메시지 표시 또는 다른 처리
        }
    }
}
///sdcard/DCIM/Screenshots/Screenshot_20230923-170832_One UI Home.jpg