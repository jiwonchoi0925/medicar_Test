package com.example.yolov8
// PostAdapter.kt

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.widget.ImageView
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast




class PostAdapter : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts: List<Post> = emptyList()

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder 내부에서 아이템 뷰의 각 요소에 대한 참조 정의
        val imageView: ImageView = itemView.findViewById(R.id.carPhoto)
        val titleTextView: TextView = itemView.findViewById(R.id.title)
        val contentTextView: TextView = itemView.findViewById(R.id.content)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton) // 삭제 버튼 추가

        init {
            itemView.setOnClickListener {
                val context = it.context
                val intent = Intent(context, writePost::class.java)
                val post = posts[adapterPosition]
                intent.putExtra("post", post)
                context.startActivity(intent)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        // 새로운 뷰 홀더 생성
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rv_post, parent, false)
        return PostViewHolder(view)


    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        // 뷰 홀더가 바인딩될 때 호출
        val currentPost = posts[position]

        // 이미지 로딩 (Glide 사용)
        Glide.with(holder.imageView.context)
            .load(currentPost.imagePath)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.imageView)


        // 기타 데이터를 뷰에 설정
        holder.titleTextView.text = currentPost.title
        holder.contentTextView.text = currentPost.content

        /*holder.deleteButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                // 삭제할 아이템의 포지션을 가져와서 해당 포스트의 ID를 얻음
                val postId = posts[position].id

                // 데이터베이스에서 해당 포스트 삭제
                val dbHelper = DatabaseHelper(holder.itemView.context)
                val isDeleted = dbHelper.deletePost(postId)
                dbHelper.close()

                if (isDeleted) {
                    // 어댑터에서 아이템 삭제
                    deleteItem(position)
                } else {
                    // 삭제 실패 처리
                    Toast.makeText(holder.itemView.context, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }*/
        holder.deleteButton.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                // 삭제할 아이템의 포지션을 가져와서 해당 포스트의 ID를 얻음
                val postId = posts[position].id

                // 데이터베이스에서 해당 포스트 삭제
                val dbHelper = DatabaseHelper(holder.itemView.context)


                val alertDialogBuilder = AlertDialog.Builder(holder.itemView.context)
                alertDialogBuilder.setTitle("삭제 확인")
                    .setMessage("정말로 삭제하시겠습니까?")
                    .setPositiveButton("예") { _, _ ->
                        val isDeleted = dbHelper.deletePost(postId)
                        dbHelper.close()
                        if (isDeleted) {
                            // 어댑터에서 아이템 삭제
                            deleteItem(position)
                        } else {
                            // 삭제 실패 처리
                            Toast.makeText(holder.itemView.context, "삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("아니오") { _, _ ->
                        Toast.makeText(holder.itemView.context, "삭제 취소" , Toast.LENGTH_SHORT).show()
                    }
                    .show()
            }
        }
    }

    override fun getItemCount(): Int {
        // 데이터 세트의 크기 반환
        return posts.size
    }

    fun setData(newPosts: List<Post>) {
        // 어댑터의 데이터 갱신
        posts = newPosts
        notifyDataSetChanged()
    }
    fun deleteItem(position: Int) {
        posts = posts.toMutableList().apply {
            removeAt(position)
        }
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }
}
