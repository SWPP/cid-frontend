package com.cid.bot

import android.animation.ValueAnimator
import android.app.Activity
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.cid.bot.data.Muser
import com.cid.bot.data.MuserConfig
import com.cid.bot.databinding.ActivitySignBinding
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_sign.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class SignActivity : BaseDaggerActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var binding: ActivitySignBinding

    enum class Mode(val value: Float, val string: String) {
        SIGN_IN(0f, "Sign In"), SIGN_UP(1f, "Sign Up")
    }
    private var mode = Mode.SIGN_IN
    private var pushToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Binding */
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign)
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SignViewModel::class.java)
        binding.viewModel = viewModel
        binding.executePendingBindings()
//        setContentView(R.layout.activity_sign)

        /* Configure */
        supportActionBar?.title = Mode.SIGN_IN.string
        setResult(Activity.RESULT_CANCELED)
//        NetworkManager.authToken = null

/*
        with (getSharedPreferences(getString(R.string.pref_name_sign), 0)) {
            cBautoSignIn.isChecked = getBoolean(getString(R.string.pref_key_auto_sign_in), false)
            cBsaveUsername.isChecked = getBoolean(getString(R.string.pref_key_save_username), false)
            if (cBsaveUsername.isChecked)
                eTusername.setText(getString(getString(R.string.pref_key_username), ""))
        }
*/

        /* Listeners */
        bTsignIn.setOnClickListener {
            if (mode == Mode.SIGN_IN)
                trySignIn()
            else
                changeMode(Mode.SIGN_IN)
        }

        bTsignUp.setOnClickListener {
            if (mode == Mode.SIGN_UP)
                trySignUp()
            else
                changeMode(Mode.SIGN_UP)
        }

        getPushToken()
    }

    private var modeAnim: ValueAnimator? = null
    private var modeValue = 0f   /* 0: SIGN_IN, 1: SIGN_UP */
    private fun changeMode(mode: Mode) {
        this.mode = mode
        val animTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        supportActionBar?.title = mode.string

        if (modeAnim?.isRunning == true) modeAnim?.cancel()

        ValueAnimator.ofFloat(modeValue, mode.value).apply {
            duration = (animTime * abs(mode.value - modeValue)).toLong()

            addUpdateListener {
                val value = it.animatedValue as Float
                modeValue = value

                with (tILpasswordConfirm.layoutParams as ConstraintLayout.LayoutParams) {
                    height = max(1, (value * tILpassword.height).toInt())
                    tILpasswordConfirm.layoutParams = this
                }

                with (bTsignUp.layoutParams as LinearLayout.LayoutParams) {
                    weight = 1 + value
                    bTsignUp.layoutParams = this
                }

                with (bTsignIn.layoutParams as LinearLayout.LayoutParams) {
                    weight = 1 - value
                    bTsignIn.layoutParams = this
                }
            }

            modeAnim = this
        }.start()
    }

    private fun trySignIn() {
        val pushToken = pushToken
        if (pushToken == null) {
            Toast.makeText(this, "Try later.", Toast.LENGTH_SHORT).show()
            return
        }

        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()

        register(API.signIn(username, password, pushToken), {
            val token = it["token"].asString
            NetworkManager.authToken = token
            binding.viewModel?.saveMuserConfig(MuserConfig(
                    autoSignIn = cBautoSignIn.isChecked,
                    token = if (cBautoSignIn.isChecked) token else null,
                    saveUsername = cBsaveUsername.isChecked,
                    username = if (cBsaveUsername.isChecked) username else null
            ))
/*
            with (getSharedPreferences(getString(R.string.pref_name_sign), 0).edit()) {
                putBoolean(getString(R.string.pref_key_auto_sign_in), cBautoSignIn.isChecked)
                if (cBautoSignIn.isChecked) {
                    putString(getString(R.string.pref_key_token), token)
                }
                putBoolean(getString(R.string.pref_key_save_username), cBsaveUsername.isChecked)
                if (cBsaveUsername.isChecked) {
                    putString(getString(R.string.pref_key_username), username)
                }
                apply()
            }
*/
            setResult(RESULT_OK)
            finish()
        }, {
            Toast.makeText(this, it.zip(), Toast.LENGTH_SHORT).show()
        })
    }

    private fun trySignUp() {
        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()
        val passwordConfirm = eTpasswordConfirm.text.toString()

        if (password != passwordConfirm) {
            val error = "Passwords are not identical."
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            binding.root.applyErrors(mapOf("password" to error, "password_confirm" to error))
            return
        }

        register(API.signUp(username, password), {
            Toast.makeText(this, "You have been signed up for our membership.\nPlease sign in to use our service.", Toast.LENGTH_LONG).show()
            eTusername.setText("")
            eTpassword.setText("")
            eTpasswordConfirm.setText("")
            changeMode(Mode.SIGN_IN)
        }, {
            val rest = binding.root.applyErrors(it)
            Toast.makeText(this, "Error occurred. ${rest.zip()}", Toast.LENGTH_LONG).show()
        })
    }

    private fun getPushToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
            pushToken = it.token
            if (pushToken == null)
                getPushToken()
        }
    }

    override fun onBackPressed() {
        when (mode) {
            Mode.SIGN_UP -> changeMode(Mode.SIGN_IN)
            else -> super.onBackPressed()
        }
    }
}
