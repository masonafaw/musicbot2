package fredboat.testutil.extensions

import com.palantir.docker.compose.connection.Container
import com.palantir.docker.compose.connection.State
import com.palantir.docker.compose.connection.waiting.SuccessOrFailure
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import fredboat.testutil.extensions.DockerExtension.Companion.docker
import fredboat.util.rest.Http
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.sql.DriverManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object DockerHealthChecks {

    private val log: Logger = LoggerFactory.getLogger(DockerHealthChecks::class.java)

    fun checkPostgres(c: Container) = wrap(c) { _ ->
        DriverManager.getConnection("jdbc:postgresql://localhost/fredboat_cache", "postgres", "").use { conn ->
            conn.createStatement().use { statement ->
                val resultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname='fredboat_cache';")
                resultSet.next()
                val result = resultSet.getString(1)
                if(result.equals("1", ignoreCase = true)) {
                    SuccessOrFailure.success()
                } else {
                    SuccessOrFailure.failure("Unexpected result $result")
                }
            }
        }
    }

    fun checkQuarterdeck(c: Container) = wrap(c) { container ->
        if (container.state() == State.DOWN) container.up()

        val versions = Http(Http.DEFAULT_BUILDER)["http://localhost:4269/info/api/versions"]
                .basicAuth("test", "test")
                .asString()
        log.info("Quarterdeck versions supported: $versions")
        SuccessOrFailure.success()
    }

    fun checkRabbitMq(c: Container) = wrap(c) { _ ->
        val factory = ConnectionFactory()
        var conn: Connection? = null
        try {
            conn = factory.newConnection()
        } finally {
            conn?.close()
        }
        SuccessOrFailure.success()
    }

    fun checkLavalink(c: Container) = wrap(c) { _ ->
        val headers = mapOf(
                "Authorization" to "youshallnotpass",
                "Num-Shards" to "1",
                "User-Id" to "1337"
        )

        val sock = TestSocket(URI.create("ws://localhost:2333"), headers)
        sock.connect() // async
        SuccessOrFailure.fromBoolean(
                sock.latch.await(10, TimeUnit.SECONDS),
                "Lavalink timed out"
        )
    }

    private inline fun wrap(container: Container, func: (Container) -> SuccessOrFailure): SuccessOrFailure {
        try {
            val id = docker.dockerCompose().id(container)
            if (!id.isPresent) {
                return SuccessOrFailure.failure("no id on container")
            }
            return func(container)
        } catch (e: Exception) {
            log.error("Failed health check for ${container.containerName}: ${e.message}")
            return SuccessOrFailure.fromException(e)
        }
    }

    private class TestSocket(uri: URI, headers: Map<String, String>) : WebSocketClient(uri, headers) {

        val latch = CountDownLatch(1)

        override fun onOpen(handshakedata: ServerHandshake) {
            log.info("Connected to Lavalink")
            latch.countDown()
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            if (remote) log.error("Lavalink closed connected. Code: $code, reason: $reason")
        }

        override fun onMessage(message: String) {}

        override fun onError(ex: java.lang.Exception) {
            log.error("Error in socket to Lavalink", ex)
        }

    }

}