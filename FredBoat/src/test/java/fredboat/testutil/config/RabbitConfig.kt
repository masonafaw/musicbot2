package fredboat.testutil.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.SentinelHello
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.net.InetAddress
import java.util.*

@TestConfiguration
class RabbitConfig {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)
    }

    @Bean
    fun sentinelId(): String {
        val rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        val id = "${InetAddress.getLocalHost().hostName}-$rand"
        log.info("Unique identifier for this session: $id")
        return id
    }

    @Bean
    fun jsonMessageConverter(): MessageConverter {
        // We must register this Kotlin module to get deserialization to work with data classes
        return Jackson2JsonMessageConverter(ObjectMapper().registerKotlinModule())
    }

    @Bean
    fun eventQueue() = Queue(SentinelExchanges.EVENTS, false)

    @Bean
    fun requestExchange() = DirectExchange(SentinelExchanges.REQUESTS)

    @Bean
    fun requestQueue() = Queue(SentinelExchanges.REQUESTS, false)

    @Bean
    fun requestBinding(
            @Qualifier("requestExchange") requestExchange: DirectExchange,
            @Qualifier("requestQueue") requestQueue: Queue,
            @Qualifier("sentinelId") key: String
    ): Binding {
        log.info("BINDING")
        return BindingBuilder.bind(requestQueue).to(requestExchange).with(key)
    }

    @Component
    class HelloSender(
            private val rabbitTemplate: RabbitTemplate,
            @Qualifier("sentinelId")
            private val key: String) {
        fun send() {
            rabbitTemplate.convertAndSend(SentinelExchanges.EVENTS, SentinelHello(
                    0,
                    1,
                    1,
                    key
            ))
        }
    }
}