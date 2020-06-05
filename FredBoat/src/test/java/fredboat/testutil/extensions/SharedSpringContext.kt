package fredboat.testutil.extensions

import fredboat.commandmeta.CommandInitializer
import fredboat.main.Launcher
import fredboat.sentinel.SentinelTracker
import fredboat.testutil.IntegrationTest
import fredboat.testutil.config.RabbitConfig
import fredboat.testutil.sentinel.CommandTester
import fredboat.testutil.sentinel.SentinelState
import fredboat.testutil.sentinel.delayUntil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.extension.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.support.AbstractApplicationContext
import java.lang.System.currentTimeMillis
import java.lang.Thread.sleep
import kotlin.system.exitProcess

class SharedSpringContext : ParameterResolver, BeforeAllCallback, AfterEachCallback {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SharedSpringContext::class.java)
        private var application: AbstractApplicationContext? = null
    }

    override fun beforeAll(context: ExtensionContext) {
        if (application != null) return // Don't start the application again

        log.info("Initializing test context")
        val job = GlobalScope.launch {
            try {
                Launcher.main(emptyArray())
            } catch (t: Throwable) {
                log.error("Failed initializing context", t)
                exitProcess(-1)
            }
        }
        val start = currentTimeMillis()
        var i = 0
        while (Launcher.instance == null) {
            sleep(1000)
            i++
            if (currentTimeMillis() - start > 3 * 60000) {
                log.error("Context initialization timed out")
                exitProcess(-1)
            }
        }
        log.info("Acquired Spring context after ${(currentTimeMillis() - start)/1000} seconds")
        application = Launcher.springContext
        try {
            // Context may or may not be refreshed yet. Refreshing too many times will throw an exception
            application!!.refresh()
        } catch (ignored: IllegalStateException) {}
        val helloSender = application!!.getBean(RabbitConfig.HelloSender::class.java)
        val tracker = application!!.getBean(SentinelTracker::class.java)
        delayUntil(timeout = 10000) {
            helloSender.send()
            tracker.getHello(0) != null
        }
        IntegrationTest.commandTester = application!!.getBean(CommandTester::class.java)

        delayUntil(timeout = 10000) { CommandInitializer.initialized }
        assertTrue("Command initialization timed out", CommandInitializer.initialized)

        log.info("Successfully initialized test context ${application!!.javaClass.simpleName}")
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return application!!.getBean(parameterContext.parameter.type) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return application!!.getBean(parameterContext.parameter.type)
    }

    override fun afterEach(context: ExtensionContext?) {
        SentinelState.reset()
    }

}