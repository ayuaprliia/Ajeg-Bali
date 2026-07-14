package com.example.ajegbali.feature.chatbot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ajegbali.R
import com.example.ajegbali.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk RecyclerView chat yang menampilkan pesan user dan bot
 * dengan layout berbeda (bubble kanan untuk user, kiri untuk bot).
 *
 * Fitur:
 * - Link YouTube di pesan bot bisa diklik → buka di aplikasi YouTube
 * - Long press pada pesan bot → copy teks ke clipboard
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

                // Buat URL (link YouTube dll) bisa diklik → buka di browser/app YouTube
                Linkify.addLinks(holder.tvMessage, Linkify.WEB_URLS)
                holder.tvMessage.movementMethod = LinkMovementMethod.getInstance()
                holder.tvMessage.setLinkTextColor(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_blue_dark)
                )

                // Long press → copy teks ke clipboard
                holder.tvMessage.setOnLongClickListener { view ->
                    val context = view.context
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Pesan Chatbot", message.text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Teks berhasil disalin 📋", Toast.LENGTH_SHORT).show()
                    true
                }
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
