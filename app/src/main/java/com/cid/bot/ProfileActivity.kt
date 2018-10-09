package com.cid.bot

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bTwithdraw.setOnClickListener {
            val layout = layoutInflater.inflate(R.layout.dialog_withdraw, null)

            AlertDialog.Builder(this)
                    .setTitle("Withdraw")
                    .setMessage("Please input your username and password.")
                    .setView(layout)
                    .setPositiveButton("Withdraw") { _, _ ->
                        val username = layout.findViewById<EditText>(R.id.eTusername).text.toString()
                        val password = layout.findViewById<EditText>(R.id.eTpassword).text.toString()
                        tryWithdraw(username, password)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    private var withdrawTask: Disposable? = null
    private fun tryWithdraw(username: String, password: String) {
        if (withdrawTask != null) return

        withdrawTask = NetworkManager.call(API.withdraw(username, password), {
            Toast.makeText(this, "Your membership has been removed.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }, {
            Toast.makeText(this, "Withdrawal did not finish successfully. Please try again.", Toast.LENGTH_SHORT).show()
        }, {
            withdrawTask = null
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mIsave -> {
                // TODO: save user profile
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
