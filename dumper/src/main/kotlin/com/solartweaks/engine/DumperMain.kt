package com.solartweaks.engine

import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.nio.file.Paths
import java.security.ProtectionDomain
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writeBytes

fun agentmain(arg: String?, inst: Instrumentation) {
    val outputPath = arg ?: ""
    val outputFile = Paths.get(outputPath)

    println("[Dumper] Writing classes to $outputFile")
    require(outputFile.isDirectory()) { "output is not a directory!" }

    inst.addTransformer(object : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain?,
            classfileBuffer: ByteArray
        ): ByteArray? {
            "$className.class".split('/')
                .fold(outputFile) { acc, curr -> acc.resolve(curr) }
                .also { it.parent.createDirectories() }
                .writeBytes(classfileBuffer)

            return null
        }
    })
}

fun premain(arg: String?, inst: Instrumentation) = agentmain(arg, inst)