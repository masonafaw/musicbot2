package fredboat.testutil.config

import fredboat.config.ApplicationInfo
import fredboat.sentinel.RawUser
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
open class TestConfig {

    @Bean
    @Primary
    open fun applicationInfo() = ApplicationInfo(
            168672778860494849,
            false,
            "The best bot",
            "bdc4465f37fde2d04335d388076ece26",
            "FredBoatβ",
            81011298891993088,
            false
    )

    @Bean
    @Primary
    open fun selfUser() = RawUser(
            152691313123393536,
            "FredBoatβ",
            "5143",
            true
    )

}