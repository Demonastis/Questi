package com.example.quizcard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import android.os.Environment



class CreateQuizActivity : AppCompatActivity() {


    private val questionBank = mutableListOf<Question>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QuizAdapter

    private val PICK_JSON_FILE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_quiz)

        recyclerView = findViewById(R.id.recyclerView)
        adapter = QuizAdapter(questionBank)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Top buttons
        findViewById<Button>(R.id.addQuestionButton).setOnClickListener {
            questionBank.add(Question("", ""))
            adapter.notifyItemInserted(questionBank.size - 1)
        }

        findViewById<Button>(R.id.selectQuizButton).setOnClickListener {
            openFilePicker()
        }

        findViewById<Button>(R.id.saveQuizButton).setOnClickListener {
            saveQuizAs()
        }

    }

    private val openJsonFile =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    val type = object : TypeToken<List<Question>>() {}.type
                    questionBank.clear()
                    questionBank.addAll(Gson().fromJson(json, type))
                    adapter.notifyDataSetChanged()
                }
            }
        }

    private fun openFilePicker() {
        openJsonFile.launch(arrayOf("application/json"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    val type = object : TypeToken<List<Question>>() {}.type
                    questionBank.clear()
                    questionBank.addAll(Gson().fromJson(json, type))
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
    
    private fun saveQuizAs() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Save Quiz As")
            .setMessage("Enter file name (without .json)")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val fileName = input.text.toString().trim()
                if (fileName.isNotEmpty()) {
                    val json = Gson().toJson(questionBank)

                    // 📂 Get the public Documents folder
                    val documentsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    val quizDir = File(documentsDir, "QuizCard")
                    if (!quizDir.exists()) quizDir.mkdirs()

                    val file = File(quizDir, "$fileName.json")
                    file.writeText(json)

                    Toast.makeText(
                        this,
                        "Saved to Documents/QuizCard/$fileName.json",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
