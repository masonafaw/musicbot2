package fredboat.testutil.util

import fredboat.db.api.GuildPermsService
import fredboat.db.transfer.GuildPermissions
import fredboat.sentinel.Guild
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.function.Function

@Service
@Primary
class MockGuildPermsService : GuildPermsService {

    final val default: (guild: Guild) -> GuildPermissions = { guild ->
        GuildPermissions().apply { id = guild.id.toString() }
    }
    var factory = default

    override fun fetchGuildPermissions(guild: Guild) = factory(guild)

    override fun transformGuildPerms(guild: Guild, transformation: Function<GuildPermissions, GuildPermissions>)
    = transformation.apply(factory(guild))

}