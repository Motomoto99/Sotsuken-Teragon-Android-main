package com.example.sotugyo_kenkyu.ai

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userMessage: TextView = view.findViewById(R.id.textUserMessage)
        val aiMessage: TextView = view.findViewById(R.id.textAiMessage)
        val userContainer: View = view.findViewById(R.id.layoutUser)
        val aiContainer: View = view.findViewById(R.id.layoutAi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.userContainer.visibility = View.VISIBLE
            holder.aiContainer.visibility = View.GONE
            holder.userMessage.text = message.message
        } else {
            holder.userContainer.visibility = View.GONE
            holder.aiContainer.visibility = View.VISIBLE
            holder.aiMessage.text = message.message
        }
    }

    override fun getItemCount() = messages.size
}