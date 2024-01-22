package com.solartweaks.engine

import com.solartweaks.engine.util.*
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.net.URLClassLoader
import java.security.ProtectionDomain

private val classLoaderTypes = listOf(internalNameOf<URLClassLoader>(), internalNameOf<ClassLoader>())
internal val systemClassExtension = mods.flatMap { it.entry.systemPackages }.toSet()

// Prevents external ClassLoaders from loading any of "our" classes through their loader
// (which in turn breaks state in ClassFinders for example)
fun Instrumentation.installAntiClassLoader(debug: Boolean = false) {
    addTransformer(object : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain,
            classfileBuffer: ByteArray
        ): ByteArray? {
//             Stop when the supplied class is a system class
//            val binaryName = className.replace('/', '.')
//            if (isSystemClass(binaryName) || systemClassExtension.any { binaryName.startsWith(it) }) return null
            // Only do this when this is a lunar class
            // (only genesis, ichor must be transformed, but not vpatcher)
            // (legacy mac compat)
            if (!className.startsWith(lunarPackage)) return null

            // Convert to ClassNode
            val node = classfileBuffer.asClassNode()

            // Skip when not a classloader
            if (node.superName !in classLoaderTypes) return null

            // Find the findClass and loadClass methods
            val classType = "L${internalNameOf<Class<*>>()};"
            val toTransform = listOf(
                "findClass" to "(Ljava/lang/String;)$classType",
                "loadClass" to "(Ljava/lang/String;Z)$classType"
            ).mapNotNull { (name, desc) -> node.methods.find { it.name == name && it.desc == desc } }

            // If no methods to transform, exit early
            if (toTransform.isEmpty()) {
                println("ClassLoader $className was found, but did not have {find,load}Class")
                return null
            }

            // Define transforms
            val transforms = toTransform.map { method ->
                MethodTransformContext(node, method).apply {
                    methodEnter {
                        val label = Label()

                        // Class name (binary)
                        load(1)

                        // Check if the class is a system class
                        invokeMethod(::isSystemClass)

                        // If they do NOT match, continue normal execution
                        visitJumpInsn(IFEQ, label)

                        if (debug) visitPrintln {
                            concat {
                                appendString("Preventing ")
                                appendString { load(1) }
                                appendString(" to get loaded by ${node.name}")
                            }
                        }

                        // Prevent loading with custom loader by calling super (which should work)
                        loadThis()

                        // Class name
                        load(1)

                        // `resolve`
                        if (method.name == "loadClass") load(2, ILOAD)

                        // Invoke super
                        invokeMethod(InvocationType.SPECIAL, node.superName, method.name, method.desc)

                        // Return the result from super
                        returnMethod(ARETURN)

                        // Create label to continue normal execution when prefix didn't match
                        visitLabel(label)
                    }
                }.asClassTransform()
            }

            // Transform the heck out of these!
            return node.transformDefault(transforms, classfileBuffer, loader, debug = debug)
        }
    })
}