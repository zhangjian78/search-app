package jerry.gadgets

object HttpCookieParser {

    fun parseCookieString(input: String) : Map<String, String> {
        val kvs = input.splitToSequence(";")
        val out = linkedMapOf<String, String>()

        kvs.forEach {
            val kv = it.splitToSequence("=")
            if (kv.count() <= 0)
                return@forEach

            val kvi = kv.iterator()
            val k = kvi.next().trim()
            var v = ""
            if (kvi.hasNext()) {
                v = kvi.next().trim()
            }

            out[k] = v
        }

        return out
    }
}