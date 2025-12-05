package com.example.sotugyo_kenkyu.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupAi: Group = view.findViewById(R.id.groupAi)
        val aiMessage: TextView = view.findViewById(R.id.textAiMessage)
        val aiTime: TextView = view.findViewById(R.id.textAiTime)

        val groupUser: Group = view.findViewById(R.id.groupUser)
        val userMessage: TextView = view.findViewById(R.id.textUserMessage)
        val userTime: TextView = view.findViewById(R.id.textUserTime)

        val dateHeader: TextView = view.findViewById(R.id.textDateHeader)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        holder.dateHeader.visibility = View.GONE

        if (message.isUser) {
            holder.groupAi.visibility = View.GONE
            holder.groupUser.visibility = View.VISIBLE
            holder.userMessage.text = message.message
            holder.userTime.text = currentTime
        } else {
            holder.groupUser.visibility = View.GONE
            holder.groupAi.visibility = View.VISIBLE
            holder.aiMessage.text = message.message
            holder.aiTime.text = currentTime
        }
    }

    override fun getItemCount() = messages.size
}