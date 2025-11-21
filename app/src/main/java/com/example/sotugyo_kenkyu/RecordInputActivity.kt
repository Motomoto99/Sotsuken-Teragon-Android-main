package com.example.sotugyo_kenkyu

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.util.Calendar
import java.util.Locale

class RecordInputActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_input)

        val header = findViewById<View>(R.id.header)
        val buttonCancel = findViewById<View>(R.id.buttonCancel)
        val buttonSave = findViewById<View>(R.id.buttonSave)

        val textDate = findViewById<TextView>(R.id.textDate)
        val containerDate = findViewById<View>(R.id.containerDate)

        val cardPhoto = findViewById<View>(R.id.cardPhoto)
        val containerRecipe = findViewById<View>(R.id.containerRecipe)

        // WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (12 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        // 初期値セット
        val calendar = Calendar.getInstance()
        updateDateText(textDate, calendar)

        buttonCancel.setOnClickListener { finish() }

        buttonSave.setOnClickListener {
            Toast.makeText(this, "記録を保存しました", Toast.LENGTH_SHORT).show()
            finish()
        }

        cardPhoto.setOnClickListener {
            Toast.makeText(this, "写真を追加", Toast.LENGTH_SHORT).show()
        }

        // 日付変更
        containerDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    updateDateText(textDate, calendar)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // レシピ選択
        containerRecipe.setOnClickListener {
            Toast.makeText(this, "レシピ選択画面を開く(未実装)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDateText(view: TextView, cal: Calendar) {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        view.text = String.format(Locale.getDefault(), "%d/%02d/%02d", year, month, day)
    }
}