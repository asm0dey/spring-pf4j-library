package fb2

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.w3c.dom.Node

@Suppress("unused")
@Serializable
class Xmlns {
    @ProtoNumber(1)
    var name: String? = null
        protected set

    @ProtoNumber(2)
    var value: String? = null
        protected set

    constructor()
    internal constructor(node: Node) {
        name = node.nodeName
        value = node.nodeValue
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Xmlns

        if (name != other.name) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

}
