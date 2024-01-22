package com.solartweaks.engine.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * Utility that ensures that asm can find inheritance info when writing a class.
 */
class LoaderClassWriter(
    reader: ClassReader? = null,
    flags: Int = 0,
    private val loader: ClassLoader,
) : ClassWriter(reader, flags) {
    override fun getClassLoader() = loader
    private fun String.load() = loader.getResourceAsStream("$this.class")?.readBytes()?.asClassNode(0)

    override fun getCommonSuperClass(type1: String, type2: String): String {
        return when {
            type1 == "java/lang/Object" || type2 == "java/lang/Object" -> "java/lang/Object"
            type1 == type2 -> type1
            else -> {
                val node1 = type1.load() ?: return super.getCommonSuperClass(type1, type2)
                val node2 = type2.load() ?: return super.getCommonSuperClass(type1, type2)

                when {
                    node1.isInterface || node2.isInterface -> "java/lang/Object"
                    else -> {
                        node1.getAllParents().intersect(node2.getAllParents().toSet())
                            .firstOrNull() ?: return super.getCommonSuperClass(type1, type2)
                    }
                }
            }
        }
    }

    private fun ClassNode.getAllParents() = listOf(name) + getParents()
    private fun ClassNode.getParents(): List<String> = when (name) {
        "java/lang/Object" -> emptyList()
        else -> listOf(superName) + (superName.load()?.getParents() ?: emptyList())
    }
}