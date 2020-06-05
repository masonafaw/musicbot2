package fredboat.sentinel

class SentinelException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)