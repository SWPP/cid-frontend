package com.cid.bot

import android.animation.ValueAnimator
import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.LinearLayout
import com.cid.bot.data.MuserConfig
import com.cid.bot.databinding.ActivitySignBinding
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_sign.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class SignActivity : BaseDaggerActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: SignViewModel
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
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SignViewModel::class.java)
        binding.viewModel = viewModel
        binding.executePendingBindings()

        /* Configure */
        supportActionBar?.title = Mode.SIGN_IN.string
        setResult(Activity.RESULT_CANCELED)

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

        viewModel.invalidateMuserConfig()
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
            toastShort("Try later.")
            return
        }

        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()

        register(net.api.signIn(username, password, pushToken), {
            val token = it["token"].asString
            viewModel.saveMuserConfig(MuserConfig(
                    autoSignIn = cBautoSignIn.isChecked,
                    token = token,
                    saveUsername = cBsaveUsername.isChecked,
                    username = if (cBsaveUsername.isChecked) username else null
            ), CObserver(onFinish = {
                setResult(RESULT_OK)
                finish()
            }))
        }, {
            toastLong(it.zip())
        })
    }

    private fun trySignUp() {
        binding.root.resetErrors()

        val username = eTusername.text.toString()
        val password = eTpassword.text.toString()
        val passwordConfirm = eTpasswordConfirm.text.toString()

        if (password != passwordConfirm) {
            val error = "Passwords are not identical."
            binding.root.applyErrors(mapOf("password" to error, "password_confirm" to error))
            return
        }

        register(net.api.signUp(username, password), {
            toastLong("You have been signed up for our membership.\nPlease sign in to use our service.")
            eTusername.setText("")
            eTpassword.setText("")
            eTpasswordConfirm.setText("")
            changeMode(Mode.SIGN_IN)
        }, {
            val rest = binding.root.applyErrors(it)
            toastLong(rest.simple())
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
