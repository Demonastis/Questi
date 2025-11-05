package com.example.quizcard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView

class QuizAdapter(private val questions: MutableList<Question>) :
    RecyclerView.Adapter<QuizAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: EditText = view.findViewById(R.id.questionText)
        val answerText: EditText = view.findViewById(R.id.answerText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val q = questions[position]

        // Set text safely
        holder.questionText.setText(q.text)
        holder.answerText.setText(q.correctAnswer)

        // Avoid multiple listeners: clear then add
        holder.questionText.doAfterTextChanged { editable ->
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                questions[pos].text = editable.toString()
            }
        }

        holder.answerText.doAfterTextChanged { editable ->
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                questions[pos].correctAnswer = editable.toString()
            }
        }

        // Safe delete handler
        holder.deleteButton.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                questions.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    override fun getItemCount(): Int = questions.size
}
