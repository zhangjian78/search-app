package jerry.gadgets.gsearch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.gson.Gson
import jerry.gadgets.Logger
import jerry.gadgets.R
import jerry.gadgets.checkPermissions
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException


private val logger = Logger("VOSK_EXT")

fun MainActivity.isLikelyTelevision(): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> true
        Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&
                @Suppress("DEPRECATION")
                packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) -> true
        packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST) &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_ETHERNET) &&
                !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) -> true
        else -> false
    }
}

fun MainActivity.initVoskEngine() {
    if (isLikelyTelevision()) {
        return
    }

    if (mVoskModelLoading) {
        return
    }
    mVoskModelLoading = true

    StorageService.unpack(this, "model-en-us", "model",
        { model: Model ->
            mVoskModel = model
            logger.warn { "vosk engine model loaded: $mVoskModel" }
            mVoskModelLoading = false
        }
    ) { exception: IOException ->
        mVoskEngineException = exception
        logger.warn { "can not load vosk engine: $exception" }
        logger.warn { exception }
        mVoskModelLoading = false
    }
}

fun MainActivity.hideVoiceInputOverlay(sendQuery:Boolean) {
    findViewById<ViewGroup>(R.id.voice_input_overlay).visibility = View.GONE
    mVoiceInputAni?.stop()

    toggleVoiceInputEngineState(true)
    logger.debug { "closing voice input overlay" }

    val query = mVoskResultView?.text ?: ""
    if (query.isBlank()) {
        return
    }

    if (sendQuery) {
        doQuery(query.toString())
    }
}


fun MainActivity.showVoiceInputOverlay() {

    val hasPermission = this.checkPermissions(
        listOf(
            Manifest.permission.RECORD_AUDIO
        )
    )
    logger.debug { "audio permission got? $hasPermission"  }
    if (!hasPermission) {
        return
    }

    val engineOk = toggleVoiceInputEngineState(false)
    logger.debug { "opening voice input overlay, engine ready? $engineOk" }

    if (engineOk) {
        findViewById<ViewGroup>(R.id.voice_input_overlay).visibility = View.VISIBLE
        mVoskResultView?.setText("")
        mVoiceInputAni?.start()
        return
    }

    Toast.makeText(this, "Voice engine is warming up", Toast.LENGTH_LONG).show()
    initVoskEngine()
}

fun MainActivity.toggleVoiceInputEngineState(close: Boolean) : Boolean {

    if (mVoskModel == null) {
        return false
    }

    if (mVoskSpeechService != null) {
        mVoskSpeechService?.stop()
        mVoskSpeechService = null
    }

    if (close) {
        return false
    }

    return try {
        val rec = Recognizer(mVoskModel, 16000.0f)
        mVoskSpeechService = SpeechService(rec, 16000.0f)
        mVoskSpeechService?.startListening(this)
        true
    } catch (e: IOException) {
        mVoskEngineException = e
        false
    }
}

class VoskResult {
    var text: String = ""
}

fun extractVoskEngineOutputText(output: String) : String {
    return try {
        val gson = Gson()
        val result = gson.fromJson(output, VoskResult::class.java)
        result.text
    } catch (e: Exception) {
        return e.toString()
    }
}
