package com.example.ngdungocsach.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.model.User
import com.google.android.material.button.MaterialButton

class UserAdapter(
    private val userList: List<User>,
    private val onDeleteClick: (String) -> Unit,
    private val onBlockClick: (User) -> Unit,
    private val onHideClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteUser)
        val btnBlock: MaterialButton = view.findViewById(R.id.btnBlockUser)
        val btnHide: MaterialButton = view.findViewById(R.id.btnHideUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.tvUsername.text = user.username
        holder.tvRole.text = "Vai trò: ${user.role.replaceFirstChar { it.uppercase() }}"

        // Cập nhật icon Block
        if (user.isBlocked) {
            holder.btnBlock.setIconResource(android.R.drawable.ic_lock_lock)
            holder.btnBlock.setIconTintResource(R.color.error)
        } else {
            holder.btnBlock.setIconResource(android.R.drawable.ic_lock_lock)
            holder.btnBlock.setIconTintResource(R.color.white)
        }

        // Cập nhật icon Hide
        if (user.isHidden) {
            holder.btnHide.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            holder.btnHide.setIconTintResource(R.color.error)
        } else {
            holder.btnHide.setIconResource(android.R.drawable.ic_menu_view)
            holder.btnHide.setIconTintResource(R.color.primary)
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(user.username) }
        holder.btnBlock.setOnClickListener { onBlockClick(user) }
        holder.btnHide.setOnClickListener { onHideClick(user) }

        // Vô hiệu hóa nút cho admin gốc
        if (user.username == "admin") {
            holder.btnDelete.isEnabled = false
            holder.btnBlock.isEnabled = false
            holder.btnHide.isEnabled = false
        } else {
            holder.btnDelete.isEnabled = true
            holder.btnBlock.isEnabled = true
            holder.btnHide.isEnabled = true
        }
    }

    override fun getItemCount(): Int = userList.size
}