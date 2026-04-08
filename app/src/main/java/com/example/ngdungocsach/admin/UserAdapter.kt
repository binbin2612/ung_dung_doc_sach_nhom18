package com.example.ngdungocsach.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.google.android.material.button.MaterialButton

class UserAdapter(
    private val userList: List<Triple<Int, String, String>>,
    private val onDeleteClick: (Int, String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val (id, username, role) = userList[position]
        holder.tvUsername.text = username
        holder.tvRole.text = "Vai trò: ${role.replaceFirstChar { it.uppercase() }}"
        
        holder.btnDelete.setOnClickListener {
            onDeleteClick(id, username)
        }
    }

    override fun getItemCount(): Int = userList.size
}
