package com.cid.bot

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.cid.bot.databinding.ActivityProfileBinding
import kotlinx.android.synthetic.main.activity_profile.*
import javax.inject.Inject

class ProfileActivity : BaseDaggerActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: ProfileViewModel
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Binding */
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ProfileViewModel::class.java)
        binding.viewModel = viewModel
        binding.executePendingBindings()

        /* Set Result for finish */
        setResult(Activity.RESULT_OK)

        /* Listeners */
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

    private fun tryLoadInfo() {
        viewModel.loadMuser()
        viewModel.loadMuserConfig()
    }

    private fun trySaveInfo() {
        val gender = sPgender.selectedItemPosition
        val birthdate = eTbirthdate.text.toString().let {
            if (it.isNotEmpty()) it else null
        }

        val muser = viewModel.muser.get()?.copy(
                gender = gender,
                birthdate = birthdate
        )
        val muserConfig = viewModel.muserConfig.get()?.copy(
                autoSignIn = sCautoSignIn.isChecked
        )
        if (muser == null || muserConfig == null) {
            tryLoadInfo()
            Toast.makeText(this, "Try later.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveMuser(muser, HObserver(onError = {
            val rest = binding.root.applyErrors(it)
            Toast.makeText(this, rest.simple(), Toast.LENGTH_LONG).show()
        }, onSuccess = {
            viewModel.saveMuserConfig(muserConfig, CObserver(onError = {
                Toast.makeText(this, it.simple(), Toast.LENGTH_LONG).show()
            }, onFinish = {
                Toast.makeText(this, "Your profile has been modified successfully.", Toast.LENGTH_SHORT).show()
            }))
        }))
    }

    private fun tryChangePassword(oldPassword: String, newPassword: String) {
        register(net.api.changePassword(oldPassword, newPassword), {
            viewModel.invalidateMuserConfig()
            Toast.makeText(this, "Your password has been changed successfully.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }, {
            Toast.makeText(this, if ("error" in it) it["error"] else "Changing password did not finish successfully. Please try again.", Toast.LENGTH_SHORT).show()
        })
    }

    private fun tryWithdraw(username: String, password: String) {
        register(net.api.withdraw(username, password), {
            viewModel.invalidateMuserConfig()
            Toast.makeText(this, "Your membership has been removed successfully.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }, {
            Toast.makeText(this, "Withdrawal did not finish successfully. Please try again.", Toast.LENGTH_SHORT).show()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mIrefresh -> {
                tryLoadInfo()
            }
            R.id.mIsave -> {
                trySaveInfo()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
