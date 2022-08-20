package xyz.tolvanen.weargram.ui.login

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import xyz.tolvanen.weargram.client.Authenticator
import xyz.tolvanen.weargram.client.Authorization
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(private val authenticator: Authenticator) : ViewModel() {

    val loginState = mutableStateOf<LoginState>(LoginState.SetNumber())

    val qrCode = mutableStateOf<Bitmap?>(null)

    init {
        authenticator.authorizationState.onEach {
            Log.d("kek", "here with $it")
            when (it) {
                Authorization.UNAUTHORIZED -> {
                    loginState.value = LoginState.Loading
                }
                Authorization.WAIT_NUMBER, Authorization.INVALID_NUMBER -> {
                    loginState.value = LoginState.SetNumber()
                }
                Authorization.WAIT_CODE, Authorization.INVALID_CODE -> {
                    loginState.value = LoginState.SetCode()
                }
                Authorization.WAIT_PASSWORD, Authorization.INVALID_PASSWORD -> {
                    loginState.value = LoginState.SetPassword()
                }
                Authorization.AUTHORIZED -> {
                    loginState.value = LoginState.Authorized
                }
            }
        }.launchIn(viewModelScope)

        authenticator.tokenState.onEach {
            it?.also { generateQrCode(it) }
        }.launchIn(viewModelScope)

    }

    private fun generateQrCode(token: String) {
        val size = 512f
        val mat = Encoder.encode(token, ErrorCorrectionLevel.L).matrix
        val bmpSize = ((size * (mat.height + 2)) / mat.height).toInt()
        val offset = (bmpSize - size) / 2
        val bmp = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.RGB_565)
        for (i in 0 until bmpSize) {
            val iMat = (i - offset) * mat.width / size
            for (j in 0 until bmpSize) {
                val jMat = (j - offset) * mat.height / size

                val pixelColor =
                    (if (iMat >= 0 && iMat < mat.width
                        && jMat >= 0 && jMat < mat.height
                        && mat[iMat.toInt(), jMat.toInt()] != 0.toByte()
                    )
                        Color.BLACK
                    else Color.WHITE)

                bmp.setPixel(i, j, pixelColor)
            }
        }

        qrCode.value = bmp
    }

    fun setNumber(number: String) {
        authenticator.setPhoneNumber(number)
        loginState.value = LoginState.Loading
    }

    fun setCode(code: String) {
        authenticator.setCode(code)
        loginState.value = LoginState.Loading
    }

    fun setPassword(password: String) {
        authenticator.setPassword(password)
        loginState.value = LoginState.Loading
    }
}

