package fredboat.command.moderation

import com.fredboat.sentinel.entities.ModRequestType
import fredboat.testutil.sentinel.Raws
import org.junit.jupiter.api.Test

class SoftbanCommandTest : AbstractModerationTest() {

    @Test
    fun testWorking() {
        testCommand(";;softban realkc", invoker = Raws.napster) {
            expect(ModRequestType.BAN, Raws.realkc)
            expect(ModRequestType.UNBAN, Raws.realkc)
        }
    }

    @Test
    fun testWorkingOwner() {
        testCommand(";;softban realkc", invoker = Raws.owner) {
            expect(ModRequestType.BAN, Raws.realkc)
            expect(ModRequestType.UNBAN, Raws.realkc)
        }
    }

    /*
    working
    workingOwner
    missingkick
    missingban
     */

}