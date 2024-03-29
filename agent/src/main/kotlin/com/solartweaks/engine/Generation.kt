package com.solartweaks.engine

import com.solartweaks.engine.util.CompoundLoader
import com.solartweaks.engine.util.generateClass
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writeBytes

// Everything related to class generation
// Prefix for every classname that gets generated
const val generatedPrefix = "com/solartweaks/engine/generated"
val combinedAppLoader by lazy { CompoundLoader(listOf(mainLoader, LoaderDummy::class.java.classLoader)) }

/**
 * Utility to get a class by [name] with app loader
 */
fun getAppClass(name: String): Class<*> = combinedAppLoader.loadClass(name)

// ClassLoader used for loading generated classes
object GenerationLoader : ClassLoader(combinedAppLoader) {
    private val dumpPath: Path? = System.getProperty("solar.dump")
        ?.let { Paths.get(it).also { f -> f.createDirectories() } }
        ?.takeIf { it.isDirectory() }

    fun createClass(name: String, bytes: ByteArray): Class<*> {
        // Dump if property set
        if (dumpPath != null) {
            "${name.replace('.', '/')}.class".split('/').fold(dumpPath) { acc, curr -> acc.resolve(curr) }
                .also { it.parent.createDirectories() }
                .writeBytes(bytes)
        }

        return defineClass(name, bytes, 0, bytes.size)
    }
}

// Increment-on-get counter to generate unique names for unnamed classes
var unnamedCounter = 0
    get() = field++

// Utility that acts as a shortcut for generateClass
inline fun generateDefaultClass(
    name: String = "Unnamed$unnamedCounter",
    extends: String = "java/lang/Object",
    implements: List<String> = listOf(),
    defaultConstructor: Boolean = true,
    access: Int = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
    debug: Boolean = false,
    generator: ClassVisitor.() -> Unit
) = generateClass(
    name = "$generatedPrefix/$name",
    extends, implements, defaultConstructor, access,
    loader = { bytes, cName -> GenerationLoader.createClass(cName, bytes) },
    debug = debug,
    generator = generator
)

// Utility to create an instance of a given class
// (useful when class extends an interface)
// Assumes there is a no-arg constructor
inline fun <reified T> Class<*>.instance() = getConstructor().newInstance() as T

private object LoaderDummy