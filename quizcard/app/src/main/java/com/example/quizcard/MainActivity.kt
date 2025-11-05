package com.example.quizcard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.DONUT)
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // --------------------------
    // Constants / Config
    // --------------------------
    private lateinit var soundPool: SoundPool
    private var right: Int = 0
    private var wrong: Int = 0
    private val PICK_JSON_FILE = 1001
    private var allPossibleAnswers = listOf(
        "Paris", "London", "Berlin", "Madrid",
        "Mars", "Earth", "Venus", "Jupiter",
        "Atlantic", "Pacific", "Indian", "Arctic",
        "3", "4", "5", "Blue Whale", "Portuguese", "7", "Oxygen", "Egypt"
    )
    private lateinit var tts: TextToSpeech

    // --------------------------
    // Views
    // --------------------------
    private lateinit var questionText: TextView

    private lateinit var number: TextView
    private lateinit var buttons: List<Button>

    // --------------------------
    // Quiz State
    // --------------------------
    private var currentQuestionIndex = 0
    private var questionBank: List<Question> = defaultQuestionBank()
    private var shuffledQuestions: List<Question> = listOf()

    // --------------------------
    // Data Classes
    // --------------------------
    data class Question(
        val text: String,
        val correctAnswer: String
    )

    // --------------------------
    // Lifecycle
    // --------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)
        // 1️⃣ Initialize views
        questionText = findViewById(R.id.questionText)
        buttons = listOf(
            findViewById(R.id.choiceA),
            findViewById(R.id.choiceB),
            findViewById(R.id.choiceC),
            findViewById(R.id.choiceD)
        )

        // 2️⃣ Load quiz files
        val quizFiles = getQuizFiles()
        if (quizFiles.isEmpty()) {
            Toast.makeText(this, "No quizzes found! Using default quiz.", Toast.LENGTH_LONG).show()
            updatePossibleAnswersFromQuestions(questionBank)
            shuffledQuestions = questionBank.shuffled()
            showNextQuestion()
        } else {

            showQuizSelectionDialog(quizFiles)
        }
        number = findViewById(R.id.textView)
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()

        // Load your sound effect from res/raw
        right = soundPool.load(this, R.raw.rightanswer, 1)
        wrong = soundPool.load(this, R.raw.wrong, 2)
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            tts.setSpeechRate(1.75f)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            }
//            val voices = tts.voices
//            val availableVoices = voices.filter { !it.isNetworkConnectionRequired } // only downloaded voices
//
//            val voiceNames = availableVoices.map { "${it.name} (${it.locale.displayName})" }.toTypedArray()
//
//            AlertDialog.Builder(this)
//                .setTitle("Select TTS Voice")
//                .setItems(voiceNames) { _, which ->
//                    val selectedVoice = availableVoices[which]
//                    tts.voice = selectedVoice
//                    Toast.makeText(this, "Selected: ${selectedVoice.name}", Toast.LENGTH_SHORT).show()
//                }
//                .show()

        }
    }

    // --------------------------
    // Quiz File Handling
    // --------------------------
    private fun getQuizFiles(): List<File> {
        val appDir = File(getExternalFilesDir(null), "quizzes")
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val quizDir = File(documentsDir, "QuizCard")

        val allFiles = mutableListOf<File>()
        if (appDir.exists()) allFiles += appDir.listFiles { it.extension == "json" } ?: emptyArray()
        if (quizDir.exists()) allFiles += quizDir.listFiles { it.extension == "json" } ?: emptyArray()

        return allFiles
    }

    private fun updatePossibleAnswersFromQuestions(questions: List<Question>) {
        allPossibleAnswers = questions
            .map { it.correctAnswer.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
    fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    private fun loadQuestionsFromFile(file: File): List<Question> {
        val json = file.bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<Question>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun showQuizSelectionDialog(quizFiles: List<File>) {
        val fileNames = quizFiles.map { it.nameWithoutExtension }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select a Quiz")
            .setItems(fileNames) { _, which ->
                val selectedFile = quizFiles[which]
                questionBank = loadQuestionsFromFile(selectedFile)
                updatePossibleAnswersFromQuestions(questionBank)
                shuffledQuestions = questionBank.shuffled()
                showNextQuestion()
            }
            .show()
    }

    // --------------------------
    // File Picker
    // --------------------------
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, PICK_JSON_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // ✅ Case 1: File picker result
        if (requestCode == PICK_JSON_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().readText()
                    val type = object : TypeToken<List<Question>>() {}.type
                    questionBank = Gson().fromJson(json, type)

                    // 🔁 Refresh possible answers for this quiz
                    updatePossibleAnswersFromQuestions(questionBank)

                    // 🔁 Reset quiz state
                    shuffledQuestions = questionBank.shuffled()
                    currentQuestionIndex = 0
                    showNextQuestion()
                }
            }
        }

        // ✅ Case 2: Returning from the QuestionNavigatorActivity
        if (requestCode == REQUEST_NAVIGATOR && resultCode == RESULT_OK) {
            val selectedIndex = data?.getIntExtra("selectedQuestionIndex", -1) ?: -1
            if (selectedIndex >= 0) {
                currentQuestionIndex = selectedIndex
                showNextQuestion()
            }
        }

    }
    var REQUEST_NAVIGATOR = 2001;


    fun onQuestionNavigatorClicked(view: View) {
        val intent = Intent(this, QuestionNavigatorActivity::class.java)
        val json = Gson().toJson(shuffledQuestions)
        intent.putExtra("questionData", json)
        startActivityForResult(intent, REQUEST_NAVIGATOR)
    }





    fun onSelectQuizFileClicked(view: View) {
        openFilePicker()
    }

    // --------------------------
    // Quiz Logic
    // --------------------------
    private fun getChoicesForQuestion(question: Question): List<String> {
        val wrongAnswers = allPossibleAnswers
            .filter { it != question.correctAnswer }
            .shuffled()
            .take(3)
        return (wrongAnswers + question.correctAnswer).shuffled()
    }

    private fun showNextQuestion() {
        if (shuffledQuestions.isEmpty()) return

        if (currentQuestionIndex >= shuffledQuestions.size) {
            Toast.makeText(this, "Quiz complete!", Toast.LENGTH_LONG).show()
            currentQuestionIndex = 0
            shuffledQuestions = questionBank.shuffled()
        }

        val q = shuffledQuestions[currentQuestionIndex]
        questionText.text = q.text
//        val n = "${currentQuestionIndex+1}/${shuffledQuestions.size}"
//        number.text = n;
        val choices = getChoicesForQuestion(q)
        for (i in buttons.indices) {
            buttons[i].text = choices[i]
        }
    }
    val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    fun onChoiceClicked(view: View) {
        val clickedButton = view as Button
        val chosenAnswer = clickedButton.text.toString()
        val currentQuestion = shuffledQuestions[currentQuestionIndex]
        val isCorrect = chosenAnswer == currentQuestion.correctAnswer


        if (isCorrect) {
//            Toast.makeText(this, "✅ Correct!", Toast.LENGTH_SHORT).show()
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            playSound(right)
            currentQuestionIndex++
            showNextQuestion()
        } else {
//            val toast = Toast.makeText(this,"❌ Wrong! Correct answer: ${currentQuestion.correctAnswer}",Toast.LENGTH_SHORT)
//            toast.show()
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 200)
            playSound(wrong)
//            Handler(Looper.getMainLooper()).postDelayed({ toast.cancel() }, 1000)
        }

    }

    fun onCreateQuizClicked(view: View) {
        startActivity(Intent(this, CreateQuizActivity::class.java))
    }

    fun onReadClicked(view: View) {
        val currentQuestion = shuffledQuestions[currentQuestionIndex]
        speakText(currentQuestion.text)
    }



    // --------------------------
    // Default Quiz
    // --------------------------
    private fun defaultQuestionBank(): List<Question> = listOf(
        Question("What is the capital of France?", "Paris"),
        Question("Which planet is known as the Red Planet?", "Mars"),
        Question("2 + 2 equals?", "4"),
        Question("Which ocean is the largest?", "Pacific"),
        Question("What is the largest mammal?", "Blue Whale"),
        Question("Which language is primarily spoken in Brazil?", "Portuguese"),
        Question("How many continents are there?", "7"),
        Question("Which element has the chemical symbol O?", "Oxygen"),
        Question("What is the square root of 16?", "4"),
        Question("Which country is famous for the pyramids?", "Egypt")
    )

    private fun playSound(soundId: Int) {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
    }

}
