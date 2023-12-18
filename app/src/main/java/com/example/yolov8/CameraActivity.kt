@file:Suppress("DEPRECATION")

package com.example.yolov8

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var rectView: RectView
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var session: OrtSession

    //녹화관련 변수들
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var displayMetrics: DisplayMetrics
    private var mediaProjection: MediaProjection? = null
    private var isRecording = false
    private var outputFile: File? = null


    private val dataProcess = DataProcess(context = this)


    companion object {
        const val PERMISSION = 1
        const val PERMISSION_WRITE_EXTERNAL = 2
        private const val REQUEST_CODE_SCREEN_RECORD = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera)

        previewView = findViewById(R.id.previewView)
        rectView = findViewById(R.id.rectView)
        projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        load()
        setCamera()


        // 버튼 클릭 이벤트
        val startRecordingButton: Button = findViewById(R.id.recBtn)
        startRecordingButton.setOnClickListener {
            if (isRecording) {
                stopScreenRecording()
            } else {
                val serviceIntent = Intent(this,Foreground::class.java)
                ContextCompat.startForegroundService(this,serviceIntent)
                startScreenRecording()

            }
        }



        val backButton: Button = findViewById(R.id.backBtn)
        backButton.setOnClickListener {
            finish()
        }

        val captureButton: Button = findViewById(R.id.captureButton)
        captureButton.setOnClickListener {
            capture()
        }
    }

    private fun setCamera() {
        val processCameraProvider = ProcessCameraProvider.getInstance(this).get()

        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9).build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val analysis = ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()

        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) {
            imageProcess(it)
            it.close()
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
    }

    private fun imageProcess(imageProxy: ImageProxy) {
        val bitmap = dataProcess.imageToBitmap(imageProxy)
        val floatBuffer = dataProcess.bitmapToFloatBuffer(bitmap)
        val inputName = session.inputNames.iterator().next()
        val shape = longArrayOf(
            DataProcess.BATCH_SIZE.toLong(),
            DataProcess.PIXEL_SIZE.toLong(),
            DataProcess.INPUT_SIZE.toLong(),
            DataProcess.INPUT_SIZE.toLong()
        )
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
        val resultTensor = session.run(Collections.singletonMap(inputName, inputTensor))
        val outputs = resultTensor.get(0).value as Array<*>
        val results = dataProcess.outputsToNPMSPredictions(outputs)

        rectView.transformRect(results)
        rectView.invalidate()
    }

    private fun load() {
        dataProcess.loadModel()
        dataProcess.loadLabel()

        ortEnvironment = OrtEnvironment.getEnvironment()
        session = ortEnvironment.createSession(
            this.filesDir.absolutePath.toString() + "/" + DataProcess.FILE_NAME,
            OrtSession.SessionOptions()
        )

        rectView.setClassLabel(dataProcess.classes)
    }

    private fun capture() {
        val rootView = window.decorView
        val screenShot = screenShot(rootView)

        screenShot?.let {
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(it)))
        }
    }

    private fun screenShot(view: View): File? {
        view.isDrawingCacheEnabled = true
        val screenBitmap = view.drawingCache
        val filename = "screenshot.png"
        val file =
            File(Environment.getExternalStorageDirectory().toString() + "/Pictures", filename)

        return try {
            val os = FileOutputStream(file)
            screenBitmap.compress(Bitmap.CompressFormat.PNG, 90, os)
            os.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            view.isDrawingCacheEnabled = false
        }
    }

    //화면 녹화 시작 메소드
    private fun startScreenRecording() {
        try {
            //화면 캡쳐 인텐트 생성
            val screenCaptureIntent = projectionManager.createScreenCaptureIntent()
            if (screenCaptureIntent != null) {
                startActivityForResult(screenCaptureIntent, REQUEST_CODE_SCREEN_RECORD) //활동 시작 요청, REQUEST_CODE_SCREEN_RECORD 이거로 인텐트 시작
            } else { // 실패시 오류코드
                showToast("Failed to create screen capture intent.")
            }
        } catch (e: SecurityException) { // 보안 예외 처리 ( 지피티
            e.printStackTrace()
            showToast("SecurityException: ${e.message}")
        }
    }
    //종료 메소드
    private fun stopScreenRecording() {
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.release()

        mediaProjection?.stop()
        isRecording = false

        // 파일이 정상적으로 생성되었는지 확인
        if (outputFile != null && outputFile!!.exists()) {
            showToast("Recording saved: ${outputFile!!.absolutePath}")
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outputFile)))
        } else {
            showToast("Error saving recording.")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SCREEN_RECORD && resultCode == RESULT_OK) { // 화면 캡처 액티비티에서 정상적으로 시작되었는지 확인
            if (data != null) { //데이터가 존재하는지 확인하고 없다면 밑의 엘스부분에서 토스트메시지작성
                mediaProjection = projectionManager.getMediaProjection(resultCode, data) // 미디어 프로젝션 설정

                val outputDir =
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "ScreenRecordings")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                val outputFileName = "screen_record_${System.currentTimeMillis()}.mp4"
                outputFile = File(outputDir, outputFileName)

                mediaRecorder = MediaRecorder().apply {
                    setVideoSource(MediaRecorder.VideoSource.SURFACE)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                    setVideoEncodingBitRate(1024 * 1024)  // 비트레이트 (bps)
                    setVideoFrameRate(30)                // 프레임레이트 (fps)

                    // 해상도를 1280x720 (720p)로 설정
                    setVideoSize(1280, 720)

                    setOutputFile(outputFile!!.absolutePath)

                    try {
                        prepare()
                        start()
                        showToast("Recording started!")
                        isRecording = true
                    } catch (e: IOException) {
                        e.printStackTrace()
                        showToast("Failed to prepare MediaRecorder.")
                    }
                }
            } else {
                showToast("Failed to get screen capture data.")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
