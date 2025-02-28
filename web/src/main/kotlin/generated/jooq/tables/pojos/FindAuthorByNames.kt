/*
 * This file is generated by jOOQ.
 */
package generated.jooq.tables.pojos


import generated.jooq.tables.interfaces.IFindAuthorByNames

import javax.annotation.processing.Generated


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = [
        "https://www.jooq.org",
        "jOOQ version:3.19.16",
        "schema version:0"
    ],
    comments = "This class is generated by jOOQ"
)
@Suppress("UNCHECKED_CAST")
data class FindAuthorByNames(
    override val id: Long? = null
): IFindAuthorByNames {

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null)
            return false
        if (this::class != other::class)
            return false
        val o: FindAuthorByNames = other as FindAuthorByNames
        if (this.id == null) {
            if (o.id != null)
                return false
        }
        else if (this.id != o.id)
            return false
        return true
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + (if (this.id == null) 0 else this.id.hashCode())
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder("FindAuthorByNames (")

        sb.append(id)

        sb.append(")")
        return sb.toString()
    }
}
