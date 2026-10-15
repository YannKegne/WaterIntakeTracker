package com.example.waterintaketracker

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var dao: WaterDao
    private val adapter = WaterAdapter()

    private lateinit var amountInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveButton: Button
    private lateinit var todayTotalText: TextView
    private lateinit var emptyText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = AppDatabase.getInstance(this).waterDao()

        amountInput = findViewById(R.id.amountInput)
        notesInput = findViewById(R.id.notesInput)
        saveButton = findViewById(R.id.saveButton)
        todayTotalText = findViewById(R.id.todayTotalText)
        emptyText = findViewById(R.id.emptyText)
        recyclerView = findViewById(R.id.entriesRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        saveButton.setOnClickListener { saveEntry() }

        observeDatabase()
    }

    private fun observeDatabase() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    dao.getAllEntries().collect { entries ->
                        adapter.submitList(entries) {
                            if (entries.isNotEmpty()) recyclerView.scrollToPosition(0)
                        }
                        emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    dao.getTotalSince(startOfToday()).collect { total ->
                        todayTotalText.text = getString(
                            R.string.today_total_format, WaterAdapter.formatOz(total)
                        )
                    }
                }
            }
        }
    }

    private fun saveEntry() {
        val amount = amountInput.text.toString().trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            amountInput.error = getString(R.string.error_invalid_amount)
            return
        }
        val notes = notesInput.text.toString().trim().ifBlank { null }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.insert(WaterEntry(amountOz = amount, notes = notes))
            }
            amountInput.text.clear()
            notesInput.text.clear()
            amountInput.clearFocus()
            notesInput.clearFocus()
            hideKeyboard()
            Toast.makeText(this@MainActivity, R.string.entry_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(saveButton.windowToken, 0)
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
