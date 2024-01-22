package com.solartweaks.engine

import com.solartweaks.engine.bridge.overloadableExactAccessor
import java.util.*

internal fun initBufferUtil() = Unit

// Since it depends which version of netty is used per minecraft version,
// we use accessors... just to be sure
private val unpooledAccess by overloadableExactAccessor<Unpooled>("io/netty/buffer/Unpooled")

interface Unpooled {
    fun buffer(): ByteBuf
    fun buffer(cap: Int): ByteBuf
    fun buffer(cap: Int, maxCap: Int): ByteBuf
    fun copiedBuffer(bytes: ByteArray): ByteBuf
    fun wrappedBuffer(bytes: ByteArray): ByteBuf

    companion object : Unpooled by unpooledAccess
}

private val byteBufAccess by emptyAccessor<_, ByteBuf.Static> { "io/netty/buffer/ByteBuf" }

interface ByteBuf : InstanceAccessor {
    interface Static : StaticAccessor<ByteBuf>
    companion object : Static by byteBufAccess.static
}