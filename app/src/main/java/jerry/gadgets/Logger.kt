package jerry.gadgets

import android.util.Log

class Logger(val tag: String) {

    fun debug(s: String) {
        Log.d(tag, s)
    }
    fun info(s: String) {
        Log.i(tag, s)
    }

    fun warn(s: String) {
        Log.w(tag, s)
    }

    fun error(s: String) {
        Log.e(tag, s);
    }

    fun debug(msg: () -> Any?) {
        Log.d(tag, msg().toString())
    }
    fun info(msg: () -> Any?) {
        Log.i(tag, msg().toString())
    }
    fun warn(msg: () -> Any?) {
        Log.w(tag, msg().toString())
    }
    fun error(msg: () -> Any?) {
        Log.e(tag, msg().toString())
    }
}
