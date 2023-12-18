package com.example.yolov8

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setPermissions()

        // 카메라 버튼을 찾고 클릭 리스너 설정
        val cameraButton = findViewById<Button>(R.id.camera_on)
        cameraButton.setOnClickListener {
            // 카메라 액티비티 시작
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        val boardButton = findViewById<Button>(R.id.board)
        boardButton.setOnClickListener {
            val goBoard = Intent(this, boardActivity::class.java)
            startActivity(goBoard)
        }
    }

    private fun setPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MEDIA_CONTENT_CONTROL,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )

        // 권한이 필요한 경우 권한 요청
        val permissionsToRequestFiltered = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequestFiltered.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequestFiltered.toTypedArray(),
                CameraActivity.PERMISSION
            )
        }
    }
}
