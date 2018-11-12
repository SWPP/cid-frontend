package com.cid.bot

import android.app.Activity
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.cid.bot.databinding.ActivityProfileBinding
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_profile.*

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding

    private lateinit var muser: Muser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)

        setResult(Activity.RESULT_OK)

        bTchangePassword.setOnClickListener {
            val layout = layoutInflater.inflate(R.layout.dialog_change_password, null)

            val dialog = AlertDialog.Builder(this)
                    .setTitle("Change Password")
                    .setMessage("Please input your current password and new password")
                    .setView(layout)
                    .setPositiveButton("Change", null)
                    .setNegativeButton("Cancel", null)
                    .show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { _ ->
                val currentPassword = layout.findViewById<EditText>(R.id.eTcurrentPassword).text.toString()
                val newPassword = layout.findViewById<EditText>(R.id.eTnewPassword).text.toString()
                val newPasswordConfirm = layout.findViewById<EditText>(R.id.eTnewPasswordConfirm).text.toString()
                if (newPassword != newPasswordConfirm) {
                    Toast.makeText(this, "New Passwords are not identical.", Toast.LENGTH_SHORT).show()
                } else {
                    tryChangePassword(currentPassword, newPassword)
                    dialog.dismiss()
                }
            }
        }

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

        tryLoadInfo()
    }

    private fun refresh(muser: Muser) {
        this.muser = muser
        eTbirthdate.setText(muser.birthdate)
        sPgender.setSelection(muser.gender)
    }

    private var loadInfoTask: Disposable? = null
    private fun tryLoadInfo() {
        if (loadInfoTask != null) return

        loadInfoTask = NetworkManager.call(API.loadMyInfo(), {
            refresh(it)
            sCautoSignIn.isChecked = getSharedPreferences(getString(R.string.pref_name_sign), 0).getBoolean(getString(R.string.pref_key_auto_sign_in), false)
            if (!sCautoSignIn.isChecked) sCautoSignIn.isEnabled = false
        }, {
            Toast.makeText(this, "Could not load profile temporarily. Please try later.", Toast.LENGTH_SHORT).show()
            finish()
        }, {
            loadInfoTask = null
        })
    }

    private var saveInfoTask: Disposable? = null
    private fun trySaveInfo() {
        if (saveInfoTask != null) return

        val gender = sPgender.selectedItemPosition
        val birthdate = eTbirthdate.text.toString().let {
            if (it.isNotEmpty()) it else null
        }

        val muser = muser.copy(
                gender = gender,
                birthdate = birthdate
        )

        saveInfoTask = NetworkManager.call(API.saveMyInfo(muser), {
            Toast.makeText(this, "Your profile has been modified successfully.", Toast.LENGTH_SHORT).show();
            refresh(it)
            with (getSharedPreferences(getString(R.string.pref_name_sign), 0).edit()) {
                putBoolean(getString(R.string.pref_key_auto_sign_in), sCautoSignIn.isChecked)
                if (sCautoSignIn.isChecked)
                    putString(getString(R.string.pref_key_token), NetworkManager.authToken)
                else
                    sCautoSignIn.isEnabled = false
                apply()
            }
        }, {
            Toast.makeText(this, "Please try later.", Toast.LENGTH_SHORT).show()
        }, {
            saveInfoTask = null
        })
    }

    private var changePasswordTask: Disposable? = null
    private fun tryChangePassword(oldPassword: String, newPassword: String) {
        if (changePasswordTask != null) return

        changePasswordTask = NetworkManager.call(API.changePassword(oldPassword, newPassword), {
            Toast.makeText(this, "Your password has been changed successfully.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }, {
            Toast.makeText(this, if ("error" in it) it["error"] else "Changing password did not finish successfully. Please try again.", Toast.LENGTH_SHORT).show()
        }, {
            changePasswordTask = null
        })
    }

    private var withdrawTask: Disposable? = null
    private fun tryWithdraw(username: String, password: String) {
        if (withdrawTask != null) return

        withdrawTask = NetworkManager.call(API.withdraw(username, password), {
            Toast.makeText(this, "Your membership has been removed successfully.", Toast.LENGTH_SHORT).show()
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
                trySaveInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        loadInfoTask?.dispose()
        saveInfoTask?.dispose()
        changePasswordTask?.dispose()
        withdrawTask?.dispose()
        super.onDestroy()
    }
}
