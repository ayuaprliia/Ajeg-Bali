package com.example.ajegbali

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk RecyclerView chat yang menampilkan pesan user dan bot
 * dengan layout berbeda (bubble kanan untuk user, kiri untuk bot).
 */
class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_BOT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
        val timeText = timeFormat.format(Date(message.timestamp))

        when (holder) {
            is UserViewHolder -> {
                holder.tvMessage.text = message.text
                holder.tvTime.text = timeText
            }
            is BotViewHolder -> {
                holder.tvMessage.text = message.text
                holder.tvTime.text = timeText
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvUserMessage)
        val tvTime: TextView = view.findViewById(R.id.tvUserTime)
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvBotMessage)
        val tvTime: TextView = view.findViewById(R.id.tvBotTime)
    }
}
