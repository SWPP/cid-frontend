package com.cid.bot

import android.animation.ValueAnimator
import android.app.Activity
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_sign.*
import kotlin.math.abs
import kotlin.math.max

class SignActivity : AppCompatActivity() {
    enum class Mode(val value: Float, val string: String) {
        SIGN_IN(0f, "Sign In"), SIGN_UP(1f, "Sign Up")
    }
    private var mode = Mode.SIGN_IN
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)

        setResult(Activity.RESULT_CANCELED)

        pref = getSharedPreferences(getString(R.string.pref_name_sign), 0)
        eTusername.setText(pref.getString(getString(R.string.pref_key_username), ""))
        eTpassword.setText(pref.getString(getString(R.string.pref_key_password), ""))
        if (pref.getBoolean(getString(R.string.pref_key_auto_sign_in), false)) {
            trySignIn()
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

    private fun trySignIn() {
        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()

        NetworkManager.call(API.signIn(username, password), {
            val token = it["token"].asString
            NetworkManager.authToken = token

            setResult(RESULT_OK)
            finish()
        }, {
            Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show()
        })
    }

    private fun trySignUp() {
        // TODO: Implement
    }

    override fun onBackPressed() {
        when (mode) {
            Mode.SIGN_UP -> changeMode(Mode.SIGN_IN)
            else -> super.onBackPressed()
        }
    }
}
