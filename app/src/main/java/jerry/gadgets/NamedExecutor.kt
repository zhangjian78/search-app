package jerry.gadgets

import java.util.concurrent.Executors


private val logger = Logger("NAMED_EXECUTOR")

object NamedExecutor {
    const val cocurrency = 4
    init{
        logger.debug {"created single thread executor and concurrent thread executor with concurrency of: $cocurrency"}
    }

    val singleThreadExecutor = Executors.newFixedThreadPool(1)

    val multiThreadExecutor = Executors.newFixedThreadPool(cocurrency)

}
