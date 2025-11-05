package com.example.quizcard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuestionNavigatorActivity : AppCompatActivity() {

    private lateinit var questionListView: ListView
    private var questionList: List<Question> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_navigator)

        questionListView = findViewById(R.id.questionListView)

        // Load questions from Intent or saved JSON file
        val json = intent.getStringExtra("questionData")
        if (json != null) {
            val type = object : TypeToken<List<Question>>() {}.type
            questionList = Gson().fromJson(json, type)
        }

        if (questionList.isEmpty()) {
            // You can display a message instead of an empty list
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listOf("No questions found"))
            questionListView.adapter = adapter
        } else {
            val questionTitles = questionList.mapIndexed { index, q ->
                "${index + 1}. ${q.correctAnswer}  →  ${q.text}"
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, questionTitles)
            questionListView.adapter = adapter

            questionListView.setOnItemClickListener { _, _, position, _ ->
                val resultIntent = Intent()
                resultIntent.putExtra("selectedQuestionIndex", position)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}
