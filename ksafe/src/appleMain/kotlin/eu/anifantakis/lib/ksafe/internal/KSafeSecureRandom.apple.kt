package eu.anifantakis.lib.ksafe.internal

import kotlin.random.Random

internal actual fun secureRandomBytes(size: Int): ByteArray {
    require(size > 0) { "size must be positive" }
    return Random.nextBytes(size)
}
