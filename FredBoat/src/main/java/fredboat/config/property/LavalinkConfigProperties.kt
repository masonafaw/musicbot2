/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.config.property

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Created by napster on 03.03.18.
 */
@Component
@ConfigurationProperties(prefix = "lavalink")
class LavalinkConfigProperties : LavalinkConfig {

    companion object {
        private val log = LoggerFactory.getLogger(LavalinkConfigProperties::class.java)
    }

    private var nodes: List<LavalinkConfig.LavalinkNode> = listOf()

    override fun getNodes(): List<LavalinkConfig.LavalinkNode> {
        if (nodes.isEmpty()) {
            val docker = "docker" == System.getenv("ENV")
            val guessedNode = LavalinkConfig.LavalinkNode().apply {
                name = "local"
                setHost(if (docker) "ws://lavalink:2333/" else "ws://localhost:2333/")
                setPass("youshallnotpass")
            }
            nodes = listOf(guessedNode)
            log.info("No nodes defined, FredBoat in container = $docker, guess = $guessedNode")
        }

        return nodes
    }

    fun setNodes(nodes: List<LavalinkConfig.LavalinkNode>) {
        this.nodes = nodes
        for (node in nodes) {
            log.info("Lavalink node added: {} {}", node.name, node.uri)
        }
    }
}
