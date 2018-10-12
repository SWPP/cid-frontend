package com.cid.bot

import android.animation.ValueAnimator
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.widget.LinearLayout
import android.widget.Toast
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_sign.*
import kotlin.math.abs
import kotlin.math.max

class SignActivity : AppCompatActivity() {
    enum class Mode(val value: Float, val string: String) {
        SIGN_IN(0f, "Sign In"), SIGN_UP(1f, "Sign Up")
    }
    private var mode = Mode.SIGN_IN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)

        supportActionBar?.title = Mode.SIGN_IN.string
        setResult(Activity.RESULT_CANCELED)
        NetworkManager.authToken = null

        with (getSharedPreferences(getString(R.string.pref_name_sign), 0)) {
            cBautoSignIn.isChecked = getBoolean(getString(R.string.pref_key_auto_sign_in), false)
            cBsaveUsername.isChecked = getBoolean(getString(R.string.pref_key_save_username), false)
            if (cBsaveUsername.isChecked)
                eTusername.setText(getString(getString(R.string.pref_key_username), ""))
        }

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

    private var signInTask: Disposable? = null
    private fun trySignIn() {
        if (signInTask != null) return

        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()

        signInTask = NetworkManager.call(API.signIn(username, password), {
            val token = it["token"].asString
            NetworkManager.authToken = token
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
            setResult(RESULT_OK)
            finish()
        }, {
            Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show()
        }, {
            signInTask = null
        })
    }

    private var signUpTask: Disposable? = null
    private fun trySignUp() {
        if (signUpTask != null) return

        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()
        val passwordConfirm = eTpasswordConfirm.text.toString()

        if (password != passwordConfirm) {
            Toast.makeText(this, "Passwords are not identical.", Toast.LENGTH_SHORT).show()
            return
        }

        signUpTask = NetworkManager.call(API.signUp(username, password), {
            Toast.makeText(this, "You have been signed up for our membership.\nPlease sign in to use our service.", Toast.LENGTH_LONG).show()
            eTusername.setText("")
            eTpassword.setText("")
            eTpasswordConfirm.setText("")
            changeMode(Mode.SIGN_IN)
        }, {
            Toast.makeText(this, if ("error" in it) it["error"] else "Please try again.", Toast.LENGTH_SHORT).show()
        }, {
            signUpTask = null
        })
    }

    override fun onBackPressed() {
        when (mode) {
            Mode.SIGN_UP -> changeMode(Mode.SIGN_IN)
            else -> super.onBackPressed()
        }
    }

    override fun onDestroy() {
        signInTask?.dispose()
        signUpTask?.dispose()
        super.onDestroy()
    }
}
