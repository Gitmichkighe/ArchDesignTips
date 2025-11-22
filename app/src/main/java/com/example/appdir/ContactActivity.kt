package com.example.appdir

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

data class ContactMessage(
    val name: String,
    val email: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ContactActivity : AppCompatActivity() {

    private lateinit var sendButton: Button
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var messageEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        sendButton = findViewById(R.id.sendButton)
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        messageEditText = findViewById(R.id.messageEditText)

        // Initially disable send button
        sendButton.isEnabled = false

        // Add text watchers to validate input
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        nameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        messageEditText.addTextChangedListener(textWatcher)

        sendButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()

            // Disable button while sending
            sendButton.isEnabled = false

            sendMessage(name, email, message) { success ->
                sendButton.isEnabled = true
                if (success) {
                    Toast.makeText(this, "Message sent successfully!", Toast.LENGTH_SHORT).show()
                    nameEditText.text.clear()
                    emailEditText.text.clear()
                    messageEditText.text.clear()
                } else {
                    Toast.makeText(this, "Failed to send message. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateSendButtonState() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val message = messageEditText.text.toString().trim()

        // Enable button only if all fields are valid
        sendButton.isEnabled = name.isNotEmpty() &&
                email.isNotEmpty() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                message.isNotEmpty()
    }

    private fun sendMessage(name: String, email: String, message: String, onComplete: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val contactMessage = ContactMessage(name, email, message)

        db.collection("messages")
            .add(contactMessage)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
