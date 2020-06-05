package fredboat.agent

import com.fredboat.sentinel.SentinelExchanges
import com.fredboat.sentinel.entities.FredBoatHello
import fredboat.config.property.AppConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class HelloSender(
        private val rabbitTemplate: RabbitTemplate,
        private val appConfig: AppConfig
) : FredBoatAgent("HelloSender", 30, TimeUnit.SECONDS) {

    lateinit var id: String

    override fun doRun() {
        // Send a hello when we start so we get SentinelHellos in return
        rabbitTemplate.convertAndSend(SentinelExchanges.FANOUT, "", FredBoatHello(
                id,
                appConfig.status
        ))
    }
}