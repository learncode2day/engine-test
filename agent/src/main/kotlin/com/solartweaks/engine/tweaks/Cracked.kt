package com.solartweaks.engine.tweaks

import com.solartweaks.engine.*
import com.solartweaks.engine.bridge.*
import com.solartweaks.engine.util.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.IFEQ
import org.objectweb.asm.Type
import java.net.URL
import java.util.*
import java.util.function.Consumer
import javax.imageio.ImageIO

internal fun initCracked() {
    withModule<AllowCrackedAccounts> {
        // Implement skin
        findMinecraftClass {
            methods {
                named("bridge\$setLocationOfCapeOverride")
                "ctor" {
                    method.isConstructor()
                    transform {
                        methodExit {
                            val label = Label()
                            invokeMethod(::isCurrentlyCracked)
                            visitJumpInsn(IFEQ, label)

                            loadThis()
                            loadConstant(crackedSkin)
                            invokeMethod(::updateSkin)
                            visitLabel(label)
                        }
                    }
                }
            }
        }

        // Detect save method
        val jsonHandler = findLunarClass {
            node.isInterface()
            strings has "Loaded File: [%s]"
            methods {
                "save" { method returns Type.VOID_TYPE }
            }
        }

        val save by jsonHandler.methods

        findLunarClass {
            strings has "launcher_accounts.json"

            methods {
                // Override account loading
                // (skip real accounts in launcher_accounts.json, load fake)
                "load" {
                    strings has "Double Account, getting the newer one so we're skipping this one."
                    transform {
                        overwrite {
                            val currentMethod = owner.methodByFinder { method calls { method named "get" } }
                                ?: error("Current account method not found")

                            val mapMethod = owner.methodByFinder {
                                method.isPublic()
                                method returns asmTypeOf<Map<*, *>>()
                            } ?: error("Account map not found")

                            val updateCurrent = owner.methodByFinder { strings hasPartial "setCurrentAccount" }
                            // Legacy support
                                ?: owner.methodByFinder { strings hasPartial "Able to login" }
                                ?: error("Update current not found")

                            // Make sure the account type is loaded for the finder
                            loadConstant(currentMethod.method.returnType.className)
                            visitMethodInsn(
                                Opcodes.INVOKESTATIC,
                                "java/lang/Class",
                                "forName",
                                "(Ljava/lang/String;)Ljava/lang/Class;",
                                false
                            )
                            pop()

                            loadThis()
                            invokeMethod(mapMethod)

                            // Add username -> account to map
                            getCompanion<LunarAccount>()
                            getProperty(::crackedAccountImpl)
                            invokeMethod(LunarAccount.Static::cast)
                            getProperty(LunarAccount::username)

                            getProperty(::crackedAccountImpl)
                            visitMethodInsn(
                                Opcodes.INVOKEINTERFACE,
                                "java/util/Map",
                                "put",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                                true
                            )

                            pop()

                            // Set current account in lunar to the cracked account
                            loadThis()
                            getProperty(::crackedAccountImpl)
                            cast(updateCurrent.method.arguments.first())
                            invokeMethod(updateCurrent)
                            if (updateCurrent.method.returnType != Type.VOID_TYPE) pop()

                            returnMethod()
                        }
                    }
                }
            }

            transformMethods {
                replaceCall(
                    matcher = { it.isSimilar(save(), matchOwner = false) },
                    replacement = { pop() }
                )
            }
        }

        findLunarClass {
            methods {
                "loginForWS" {
                    strings hasPartial "Failed to establish connection with the auth server, account null."
                    transform {
                        methodEnter {
                            // Make sure cracked accounts do not have to authenticate for the websocket
                            val label = Label()
                            invokeMethod(::isCurrentlyCracked)
                            visitJumpInsn(Opcodes.IFEQ, label)

                            load(1, Opcodes.ALOAD)
                            concat {
                                appendString("crackedUser:")
                                appendString {
                                    getCompanion<LunarAccount>()
                                    getProperty(::crackedAccountImpl)
                                    invokeMethod(LunarAccount.Static::cast)
                                    getProperty(LunarAccount::username)
                                }
                            }

                            invokeMethod(Consumer<*>::accept)
                            returnMethod()

                            visitLabel(label)
                        }
                    }
                }
            }
        }
    }
}

private val lunarProfile = findLunarClass {
    methods {
        named("toString") { strings hasPartial "MinecraftProfile(" }
        "create" {
            method.isConstructor()
            arguments count 2
        }
    }

    fields {
        named("id")
        named("name")
    }
}

private val lunarProfileAccess by accessor<_, LunarProfile.Static>(lunarProfile) {
    if (!lunarProfile.hasValue) {
        // Legacy support
        val profile by lunarAccount.fields
        getAppClass(profile().field.type.className)
    }
}

interface LunarProfile : InstanceAccessor {
    val id: String
    val name: String

    interface Static : StaticAccessor<LunarProfile> {
        fun create(id: String, name: String): LunarProfile
    }

    companion object : Static by lunarProfileAccess.static
}

private val lunarAccount = stringDataFinder("Account") { isLunarClass() }
private val lunarAccountAccess by accessor<_, LunarAccount.Static>(lunarAccount)

interface LunarAccount : InstanceAccessor {
    var accessToken: String
    var refreshToken: String
    var profile: LunarProfile
    var remoteId: String
    var type: AccountType
    var username: String

    interface Static : StaticAccessor<LunarAccount>
    companion object : Static by lunarAccountAccess.static
}

private val accountType = enumFinder<AccountType.Static> {
    methods { named("getFormatted") }
}

private val accountTypeAccess by accessor<_, AccountType.Static>(accountType) {
    if (!accountType.hasValue) {
        // Legacy support
        val type by lunarAccount.fields
        getAppClass(type().field.type.className)
    }
}

interface AccountType : InstanceAccessor {
    val formatted: String

    interface Static : StaticAccessor<AccountType> {
        val MOJANG: AccountType
        val XBOX: AccountType
    }

    companion object : Static by accountTypeAccess.static
}

private val legacyAccountManager = findLunarClass {
    strings has "launcher_accounts.json"
    methods {
        "legacyLogin" { strings hasPartial "Able to login" }
    }
}

private val legacyLogin by legacyAccountManager.methods

val crackedAccountImpl: Any by lazy {
    generateDefaultClass(
        name = "CrackedAccount",
        extends = lunarAccount().name,
        defaultConstructor = false,
    ) {
        generateMethod("<init>", "()V") {
            loadThis()
            loadConstant(UUID.randomUUID().toString().replace("-", ""))
            visitMethodInsn(Opcodes.INVOKESPECIAL, lunarAccount().name, "<init>", "(Ljava/lang/String;)V", false)
            loadThis()
            invokeMethod(::updateAccount)
            returnMethod()
        }

        val isModern = lunarAccount().methods.any { it.arguments.firstOrNull() == asmTypeOf<Consumer<*>>() }
        lunarAccount().methods.filter { it.isAbstract }.forEach { toImpl ->
            generateMethod(toImpl.name, toImpl.desc) {
                when (toImpl.returnType) {
                    Type.BOOLEAN_TYPE -> {
                        if (!isModern && legacyLogin().method.callsNamed(toImpl.name)) {
                            getCompanion<LunarAccount>()
                            getProperty(::crackedAccountImpl)
                            invokeMethod(LunarAccount::cast)
                            invokeMethod(LunarAccount::updateSession)
                        }

                        loadConstant(true)
                    }

                    Type.LONG_TYPE -> loadConstant(Long.MAX_VALUE)
                    Type.INT_TYPE -> loadConstant((System.currentTimeMillis() / 1000L).toInt())
                    Type.VOID_TYPE -> if (isModern) {
                        load(1, Opcodes.ALOAD)
                        loadConstant(true)
                        box(Type.BOOLEAN_TYPE)
                        invokeMethod(Consumer<*>::accept)
                    }

                    else -> error("OUPS wrong impl")
                }

                returnMethod(toImpl.returnType.getOpcode(Opcodes.IRETURN))
            }
        }
    }.instance<Any>()
}

private fun LunarAccount.updateProfile(
    username: String,
    // notchian servers use this in offline mode, should we implement failsafe tho?
    uuid: UUID = UUID.nameUUIDFromBytes("OfflinePlayer:$username".encodeToByteArray())
) {
    profile = LunarProfile.create(uuid.toString().replace("-", ""), username)
    remoteId = uuid.toString()
    this.username = username
}

fun LunarAccount.updateSession() {
    Minecraft.session = Initializer.initSession(username, profile.id, accessToken, "msa")
}

fun updateAccount(delegate: Any) = with(LunarAccount.cast(delegate)) {
    val (crackedUsername) = getModule<AllowCrackedAccounts>()
    updateProfile(crackedUsername)

    accessToken = "0"
    refreshToken = "0"
    type = AccountType.XBOX
}

fun isCurrentlyCracked() = isModuleEnabled<AllowCrackedAccounts>() &&
        Minecraft.session.profile.id?.toString()?.replace("-", "") == LunarAccount.cast(crackedAccountImpl).profile.id

internal fun updateCrackedUsername(username: String) = with(LunarAccount.cast(crackedAccountImpl)) {
    updateProfile(username)
    updateSession()
}

private val skinHttp = HttpClient(CIO) { install(ContentNegotiation) { json(json) } }
private const val uuidURL = "https://api.mojang.com/users/profiles/minecraft"
private const val profileURL = "https://sessionserver.mojang.com/session/minecraft/profile"
private val skinScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun updateSkin(pb: Any, username: String) {
    skinScope.launch {
        val (_, uuid) = skinHttp.get("$uuidURL/$username").body<UUIDResponse>()
        val (_, _, properties) = skinHttp.get("$profileURL/$uuid").body<ProfileResponse>()

        val decoded = Base64.getDecoder().decode(properties.first { it.name == "textures" }.value).decodeToString()
        val textures = json.decodeFromString<TexturesProperty>(decoded)
        val skinProperty = textures.textures["SKIN"] ?: error("no skin D:")
        val image = ImageIO.read(URL(skinProperty.url))

        Minecraft.submit {
            val texture = Initializer.initCopiedDynamicTexture(image)
            val resource = ResourceLocation.create("randomSkin$username")
            val type = skinProperty.metadata?.get("model") ?: "default"

            Minecraft.textureManager.loadTexture(resource, TextureHolder.castAccessor(texture))
            RenderablePlayer.cast(pb).setSkinLocationOverride(resource, type)
        }
    }
}

@Serializable
private data class UUIDResponse(val name: String, val id: String)

@Serializable
private data class ProfileResponse(
    val id: String,
    val name: String,
    val properties: List<ProfileReponseProperty>
)

@Serializable
private data class ProfileReponseProperty(
    val name: String,
    val value: String,
    val signature: String? = null
)

@Serializable
private data class TexturesProperty(
    val timestamp: Long,
    val profileId: String,
    val profileName: String,
    val signatureRequired: Boolean = false,
    val textures: Map<String, TextureEntry>
)

@Serializable
private data class TextureEntry(val url: String, val metadata: Map<String, String>? = null)