package com.solartweaks.engine.tweaks

import com.solartweaks.engine.*
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.util.*
import java.util.*
import kotlin.jvm.optionals.getOrNull

// doesn't work on legacy branch
internal fun initCustomCosmetics() = Unit

private val getCosmeticsManagerMethod by lunarMain.methods.late { method returns cosmeticsManager() }
internal val cachedCosmeticsManager by lazy {
    CosmeticsManager.cast(getCosmeticsManagerMethod().tryInvoke(cachedLunarMain))
}

private val cosmeticsManager = findLunarClass {
    strings has "Failed to call loadable render target texture"
    methods {
        "loadCosmetics" { strings has "Could not load dev cosmetics file" }
    }
}.apply {
    onFound {
        val loadCosmetics by methods
        val name = loadCosmetics.assume().method
            .references.first { it.desc == "Ljava/util/Map;" }.name

        fields.contents["cosmeticsMap"] = BoxElementFinder(this().fieldData.first { it.field.name == name })
    }
}

private val cosmeticsManagerAccess by accessor<_, CosmeticsManager.Static>(cosmeticsManager)

interface CosmeticsManager : InstanceAccessor {
    val cosmeticsMap: MutableMap<Int, Any>

    interface Static : StaticAccessor<CosmeticsManager>
    companion object : Static by cosmeticsManagerAccess.static
}

fun CosmeticsManager.addCosmetic(entry: CosmeticIndexEntry) {
    cosmeticsMap[entry.id] = entry.delegate
}

internal val cosmeticIndexEntry = stringDataFinder("CosmeticIndexEntry") {
    methods {
        "construct" {
            method.isConstructor()
            arguments count 7
        }
    }
}

private val cosmeticIndexEntryAccess by accessor<_, CosmeticIndexEntry.Static>(cosmeticIndexEntry)

interface CosmeticIndexEntry : InstanceAccessor {
    val id: Int
    val resource: String
    val name: String
    val animated: Boolean
    val isGeckoLibCosmetic: Boolean
    val category: Optional<Any>
    val indexType: String

    interface Static : StaticAccessor<CosmeticIndexEntry> {
        fun construct(
            id: Int,
            resource: String,
            name: String,
            animated: Boolean,
            geckoLib: Boolean,
            category: Optional<Any>,
            indexType: String
        ): CosmeticIndexEntry
    }

    companion object : Static by cosmeticIndexEntryAccess.static
}

val CosmeticIndexEntry.actualCategory get() = category.map { CosmeticType.cast(it) }.getOrNull()

fun CosmeticIndexEntry.Static.construct(
    id: Int,
    resource: String,
    name: String,
    animated: Boolean,
    geckoLib: Boolean,
    category: CosmeticType?,
    indexType: String
) = construct(id, resource, name, animated, geckoLib, Optional.ofNullable(category?.delegate), indexType)

private val cosmeticTypeAccess by enumAccessor<_, CosmeticType.Static>()

interface CosmeticType : InstanceAccessor {
    interface Static : StaticAccessor<CosmeticType> {
        val CLOAK: CosmeticType
        val HAT: CosmeticType
        val BODYWEAR: CosmeticType
        val NECKWEAR: CosmeticType
        val BELTS: CosmeticType
        val MASK: CosmeticType
        val BANDANNA: CosmeticType
        val GLASSES: CosmeticType
        val PET: CosmeticType
        val WINGS: CosmeticType
        val BACKPACK: CosmeticType
        val SHOES: CosmeticType

        fun valueOf(name: String): CosmeticType
    }

    companion object : Static by cosmeticTypeAccess.static
}