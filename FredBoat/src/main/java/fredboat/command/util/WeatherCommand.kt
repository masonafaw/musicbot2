package fredboat.command.util

import com.fredboat.sentinel.entities.embed
import com.fredboat.sentinel.entities.footer
import fredboat.command.info.HelpCommand
import fredboat.commandmeta.abs.Command
import fredboat.commandmeta.abs.CommandContext
import fredboat.commandmeta.abs.IUtilCommand
import fredboat.messaging.internal.Context
import fredboat.util.rest.APILimitException
import fredboat.util.rest.Weather
import fredboat.util.rest.models.weather.RetrievedWeather
import java.text.MessageFormat

class WeatherCommand(private val weather: Weather, name: String, vararg aliases: String)
    : Command(name, *aliases), IUtilCommand {

    override suspend fun invoke(context: CommandContext) {

        context.sendTyping()
        if (!context.hasArguments()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val query = context.rawArgs
        val alphabeticalQuery = query.replace("[^A-Za-z]".toRegex(), "")

        if (alphabeticalQuery.isEmpty()) {
            HelpCommand.sendFormattedCommandHelp(context)
            return
        }

        val currentWeather: RetrievedWeather
        try {
            currentWeather = weather.getCurrentWeatherByCity(alphabeticalQuery)
        } catch (e: APILimitException) {
            context.reply(context.i18n("tryLater"))
            return
        }

        if (!currentWeather.isError) {
            val embed = embed {
                title = MessageFormat.format(
                        LOCATION_WEATHER_STRING_FORMAT,
                        currentWeather.location,
                        currentWeather.temperature
                )
                description = currentWeather.weatherDescription
                if (currentWeather.thumbnailUrl.isNotEmpty()) thumbnail = currentWeather.thumbnailUrl
                footer {
                    text = currentWeather.dataProviderString
                    iconUrl = currentWeather.dataProviderIcon
                }
            }


            context.reply(embed)
        } else {
            when (currentWeather.errorType()) {
                RetrievedWeather.ErrorCode.LOCATION_NOT_FOUND -> context.reply(context.i18nFormat("weatherLocationNotFound",
                        "`$query`"))

                else -> context.reply(context.i18nFormat("weatherError",
                        "`" + query.toUpperCase()) + "`")
            }
        }
    }

    override fun help(context: Context): String {
        return HELP_STRING_FORMAT + context.i18n("helpWeatherCommand")
    }

    companion object {
        private const val LOCATION_WEATHER_STRING_FORMAT = "{0} - {1}"
        private const val HELP_STRING_FORMAT = "{0}{1} <location>\n#"
    }
}
