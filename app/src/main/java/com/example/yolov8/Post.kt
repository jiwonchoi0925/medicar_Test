package com.example.yolov8

import java.io.Serializable

data class Post(
    val id: Long,
    val title: String,
    val content: String,
    val imagePath: String // 이미지 경로
): Serializable
