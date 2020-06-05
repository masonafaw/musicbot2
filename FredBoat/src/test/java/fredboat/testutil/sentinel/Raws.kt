package fredboat.testutil.sentinel

import com.fredboat.sentinel.entities.Ban
import fredboat.perms.Permission
import fredboat.sentinel.*

/**
 * Welcome to FredBoat Hangout! This is a small Discord server with 4 members including a bot!
 *
 * We recently pruned the server of some >100k members.
 *
 * Roster:
 *  * Fre_d: who happens to be the owner of this place
 *  * FredBoat: The bot
 *  * Napster: Admin wielding a banhammer
 *  * RealKC: Generic shitposter
 *
 *  Marisa was banned for theft
 */

/* Don't use immutable lists here. We want to be able to modify state directly */
@Suppress("MemberVisibilityCanBePrivate")
object Raws {

    val botAdminRole = RawRole(
            236842565855147826,
            "Bot Admin",
            0
    )

    val adminRole = RawRole(
            174824663258497024,
            "Admin",
            (Permission.BAN_MEMBERS + Permission.KICK_MEMBERS + Permission.MESSAGE_MANAGE).raw
    )

    val uberAdminRole = RawRole(
            318730262314834812,
            "Über Admin",
            (Permission.BAN_MEMBERS + Permission.KICK_MEMBERS + Permission.MESSAGE_MANAGE).raw
    )

    val owner = RawMember(
            81011298891993088,
            "Fre_d",
            "Fred",
            "0310",
            174820236481134592,
            false,
            mutableListOf(),
            null
    )

    val self = RawMember(
            152691313123393536,
            "FredBoat♪♪",
            "FredBoat",
            "7284",
            174820236481134592,
            true,
            mutableListOf(uberAdminRole.id),
            null
    )

    val napster = RawMember(
            166604053629894657,
            "Napster",
            "Napster",
            "0001",
            174820236481134592,
            false,
            mutableListOf(adminRole.id),
            null
    )

    val realkc = RawMember(
            165912402716524544,
            "RealKC (RealKingChuck | KC)™",
            "RealKC",
            "7788",
            174820236481134592,
            false,
            mutableListOf(),
            null
    )

    val generalChannel = RawTextChannel(
            174820236481134592,
            "general",
            (Permission.MESSAGE_READ + Permission.MESSAGE_WRITE).raw
    )

    val privateChannel = RawTextChannel(
            184358843206074368,
            "private",
            0
    )

    val musicChannel = RawVoiceChannel(
            226661001754443776,
            "Music",
            mutableListOf(),
            5,
            (Permission.VIEW_CHANNEL + Permission.VOICE_CONNECT + Permission.VOICE_SPEAK).raw
    )

    val guild = RawGuild(
            174820236481134592,
            "FredBoat Hangout",
            owner.id,
            mutableListOf(owner, self, napster, realkc),
            mutableListOf(generalChannel, privateChannel),
            mutableListOf(musicChannel),
            mutableListOf(botAdminRole, uberAdminRole, adminRole),
            voiceServerUpdate = null
    )

    val banList = mutableListOf(Ban(RawUser(
            id = 12345678901234,
            name = "Marisa Kirisame",
            discrim = "5678",
            bot = false
    ), "Theft"))
}