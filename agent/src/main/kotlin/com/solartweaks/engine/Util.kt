package com.solartweaks.engine

import com.solartweaks.engine.bridge.BridgeManager
import com.solartweaks.engine.util.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.lang.invoke.MethodType
import java.lang.invoke.StringConcatFactory
import java.lang.reflect.Method

fun MethodsContext.named(name: String, block: MethodContext.() -> Unit = {}) = name {
    method named name
    block()
}

fun FieldsContext.named(name: String, block: FieldContext.() -> Unit = {}) = name {
    node named name
    block()
}

fun MethodsContext.namedTransform(name: String, block: MethodTransformContext.() -> Unit) = name {
    method named name
    transform(block)
}

fun MethodsContext.clinit(block: MethodContext.() -> Unit) = "clinit" {
    method.isStaticInit()
    block()
}

fun MethodsContext.clinitTransform(block: MethodTransformContext.() -> Unit) = "clinit" {
    method.isStaticInit()
    transform(block)
}

fun ClassContext.isMinecraftClass() = node match { it.name.startsWith(minecraftPackage) }
fun ClassContext.isOptifineClass() = node match { it.name.startsWith(optifinePackage) }
fun ClassContext.isLunarClass() = node match { it.name.startsWith(lunarPackage) || it.name.startsWith(oldLunarPackage) }

fun optifineClassName(name: String, subpackage: String) = when (BridgeManager.minecraftVersion.id) {
    "v1_7" -> optifinePackage
    else -> "$optifinePackage$subpackage/"
} + name

fun loadOptifineClass(name: String, subpackage: String): Class<*> =
    mainLoader.loadClass(optifineClassName(name, subpackage).replace('/', '.'))

private fun ClassLoader.loadInternal(name: String) = loadClass(name.replace('/', '.'))

fun MethodData.asMethod(loader: ClassLoader = mainLoader): Method = loader.loadInternal(owner.name).getDeclaredMethod(
    method.name,
    *MethodType.fromMethodDescriptorString(method.desc, loader).parameterArray()
).also { it.isAccessible = true }

fun MethodData.tryInvoke(receiver: Any? = null, vararg params: Any?, method: Method = asMethod()) =
    method(receiver, *params)

fun String.splitSingle(part: String) = substringBefore(part) to substringAfter(part)

fun ClassFinder.useStringInfo() = onFound { node ->
    val toString = node.methodByName("toString") ?: return@onFound
    val indy = toString.instructions.filterIsInstance<InvokeDynamicInsnNode>().lastOrNull() ?: return@onFound
    if (
        indy.bsm.owner != internalNameOf<StringConcatFactory>() ||
        indy.bsm.name != "makeConcatWithConstants"
    ) return@onFound

    val concat = indy.bsmArgs.firstOrNull() as? String ?: return@onFound
    val parts = concat.substringAfter('(').substringBeforeLast(')').split(", ")
    val names = parts.map { it.substringBefore('=') }
    val getters = toString.instructions.filter { it.opcode == Opcodes.GETFIELD || it.opcode == Opcodes.INVOKEVIRTUAL }

    if (names.size != getters.size) {
        println("Failed finding toString info for ${node.name}, sizes do not match")
        return@onFound
    }

    getters.forEachIndexed { idx, insn ->
        val fieldInsn = when (insn) {
            is FieldInsnNode -> insn
            is MethodInsnNode -> {
                if (insn.owner != node.name) return@forEachIndexed

                val getter = node.methodByInvoke(insn) ?: error("Should never happen")
                getter.instructions.filterIsInstance<FieldInsnNode>().singleOrNull() ?: return@forEachIndexed
            }

            else -> error("toString references in a weird way")
        }

        val fieldData = FieldData(node, node.fieldByName(fieldInsn.name) ?: error("Field not in <this>"))
        fields.contents[names[idx]] = BoxElementFinder(fieldData)
    }
}

fun stringDataFinder(
    name: String,
    extra: ClassContext.() -> Unit = {},
) = finders.findClass {
    methods {
        named("toString") { strings hasPartial "$name(" }
    }

    extra()
}.also { it.useStringInfo() }

fun ClassFinder.addBuilderFields(normalClass: ClassFinder) = onFound {
    fields.contents.forEach { (name, field) ->
        normalClass.fields.contents[name] = LazyElementFinder {
            normalClass().fieldData.first { it.field.name == field().field.name }
        }
    }
}

inline fun findLunarClass(crossinline block: ClassContext.() -> Unit) = finders.findClass {
    isLunarClass()
    block()
}

inline fun findMinecraftClass(crossinline block: ClassContext.() -> Unit) = finders.findClass {
    isMinecraftClass()
    block()
}

inline fun findNamedClass(name: String, crossinline block: ClassContext.() -> Unit) = finders.findClass {
    node named name.replace('.', '/')
    block()
}

fun ClassContext.constantReplacement(from: Any, to: Any) = methods {
    unnamedMethod {
        constants has from
        transform { replaceConstant(from, to) }
    }
}

fun ClassContext.constantReplacements(map: Map<Any, Any>) = methods {
    map.forEach { (from, to) ->
        unnamedMethod {
            constants has from
            transform { replaceConstant(from, to) }
        }
    }
}

fun ClassContext.constantReplacements(vararg pairs: Pair<Any, Any>) = constantReplacements(pairs.toMap())