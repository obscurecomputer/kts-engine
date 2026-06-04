
import me.znotchill.blossom.instances.InstanceTemplate
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import me.znotchill.blossom.server.BlossomServer
import net.minestom.server.Auth
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent

object BaseGenerator : InstanceTemplate {
    override fun generate(unit: GenerationUnit) {
        unit.modifier().fill(
            unit.absoluteStart().withY(-64.0),
            unit.absoluteEnd().withY(0.0),
            Block.GREEN_CONCRETE
        )
    }
}

class Server : BlossomServer(
    auth = Auth.Online()
) {
    val mainInstance = BaseGenerator.new()

    override fun preLoad() {
        listener<AsyncPlayerConfigurationEvent> { event ->
            event.spawningInstance = mainInstance
            event.player.respawnPoint = Pos(0.0, 0.0, 0.0)
            event.player.permissionLevel = 4
        }
    }
}

Server().start()
