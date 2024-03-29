package com.solartweaks.engine.util

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

// Everything related to class generation

/**
 * Generates a class with given [name] and loads it with the specified [loader] function
 *
 * @param [extends] internal name of the class to inherit from.
 * @param [implements] list of internal names of interfaces to inherit from/implement.
 * @param [defaultConstructor] determines if a simple ()V constructor with super() is generated.
 * be careful: if the class that gets extended doesn't have a ()V of its own, it'll fail.
 * @param [access] access flags for the new class.
 * @param [writerFlags] flags to pass to ClassWriter.
 * @param [version] Opcode that determines the class file version. Defaults to 1.8.
 * @param [loader] function that loads the generated class.
 * @param [debug] whether to print the output of the class generation.
 */
inline fun generateClass(
    name: String,
    extends: String = "java/lang/Object",
    implements: List<String> = listOf(),
    defaultConstructor: Boolean = true,
    access: Int = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL,
    writerFlags: Int = ClassWriter.COMPUTE_FRAMES,
    version: Int = Opcodes.V1_8,
    loader: (bytes: ByteArray, name: String) -> Class<*>,
    debug: Boolean = false,
    generator: ClassVisitor.() -> Unit
): Class<*> {
    val writer = ClassWriter(writerFlags)
    val visitor = if (debug) TraceClassVisitor(writer, PrintWriter(System.out)) else writer
    with(visitor) {
        visit(version, access, name, null, extends, implements.toTypedArray())

        if (defaultConstructor) {
            generateMethod("<init>", "()V") {
                loadThis()
                visitMethodInsn(INVOKESPECIAL, extends, "<init>", "()V", false)
                returnMethod()
            }
        }

        generator()
        visitEnd()
    }

    val bytes = writer.toByteArray()
    return loader(bytes, name.replace('/', '.'))
}

/**
 * Generates a method with given [name] and [descriptor] for this [ClassVisitor]
 *
 * @param [access] Opcodes that determine access for the new method
 * @param [maxStack] maximum stack size for the code block
 * @param [maxLocals] maximum local variable count (`this` counts!) for this method
 */
inline fun ClassVisitor.generateMethod(
    name: String,
    descriptor: String,
    access: Int = Opcodes.ACC_PUBLIC,
    maxStack: Int = 0,
    maxLocals: Int = 0,
    generator: MethodVisitor.() -> Unit
) = with(visitMethod(access, name, descriptor, null, null)) {
    visitCode()
    generator()
    visitMaxs(maxStack, maxLocals)
    visitEnd()
}