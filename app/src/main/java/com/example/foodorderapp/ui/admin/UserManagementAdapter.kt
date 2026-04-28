package com.example.foodorderapp.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.foodorderapp.R
import com.example.foodorderapp.databinding.ItemUserBinding

class UserManagementAdapter(
    private var users: List<Map<String, Any>>,
    private val onUserClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: Map<String, Any>) {
            val context = binding.root.context

            // Name (utamakan storeName untuk seller)
            val name = user["storeName"] as? String
                ?: user["name"] as? String ?: "Unknown"
            val email = user["email"] as? String ?: ""
            val role = user["role"] as? String ?: "buyer"
            val isActive = user["isActive"] as? Boolean ?: true

            binding.tvUserName.text = name
            binding.tvUserEmail.text = email

            // Avatar initial
            binding.tvUserInitial.text = name.firstOrNull()?.uppercase() ?: "U"

            // Role badge
            binding.tvUserRole.apply {
                when (role) {
                    "buyer" -> {
                        text = context.getString(R.string.users_role_buyer)
                        setBackgroundResource(R.drawable.bg_role_buyer)
                        setTextColor(android.graphics.Color.parseColor("#7E4710"))
                    }
                    "seller" -> {
                        text = context.getString(R.string.users_role_seller)
                        setBackgroundResource(R.drawable.bg_role_seller)
                        setTextColor(android.graphics.Color.parseColor("#1565C0"))
                    }
                    "admin" -> {
                        text = context.getString(R.string.users_role_admin)
                        setBackgroundResource(R.drawable.bg_role_admin)
                        setTextColor(ContextCompat.getColor(context, R.color.primary))
                    }
                    else -> {
                        text = role.replaceFirstChar { it.uppercase() }
                        setBackgroundResource(R.drawable.bg_role_buyer)
                    }
                }
            }

            // Suspended badge
            binding.tvSuspendedBadge.visibility = if (!isActive) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Click
            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<Map<String, Any>>) {
        users = newUsers
        notifyDataSetChanged()
    }
}