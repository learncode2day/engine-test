@file:Suppress("unused")

package com.solartweaks.engine.bridge

import com.solartweaks.engine.*
import com.solartweaks.engine.tweaks.withModule
import com.solartweaks.engine.util.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.security.PublicKey
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.LongConsumer
import kotlin.jvm.optionals.getOrNull

fun initBridge() = Unit

val tessellatorAccess by bridgeAccessor<_, TessellatorBridge.Static>()

interface TessellatorBridge : InstanceAccessor {
    fun begin(mode: Int, hasTexture: Boolean, hasColor: Boolean)
    fun pos(x: Float, y: Float, z: Float)
    fun normal(x: Float, y: Float, z: Float)
    fun color(r: Float, g: Float, b: Float, a: Float)
    fun uv(u: Float, v: Float)
    fun lightmap(light: Int)
    fun endVertex()
    fun end()
    fun isDrawing(): Boolean
    fun setTranslation(x: Double, y: Double, z: Double)

    interface Static : StaticAccessor<TessellatorBridge>
    companion object : Static by tessellatorAccess.static
}

val initializerBridge = bridgeFinder<InitializerBridge> {
    methods {
        "initSizedDynamicTexture" {
            allowMissing = true
            method named "initDynamicTexture"
            arguments count 2
        }

        "initCopiedDynamicTexture" {
            allowMissing = true
            method named "initDynamicTexture"
            arguments count 1
        }

        "openURLString" {
            allowMissing = true
            method named "bridge\$openURL"
            arguments[0] = asmTypeOf<String>()
        }
    }
}

val initializerAccess by accessor<_, InitializerBridge.Static>(initializerBridge, allowNotImplemented = true)

interface InitializerBridge : InstanceAccessor {
    val minecraftVersion: Any
    fun initKeyBinding(desc: String, code: Int, category: String): KeyBinding
    fun initBlockPos(x: Int, y: Int, z: Int): BlockPosition
    fun initTextComponent(text: String): ChatComponent //
    fun initThreadDownloadImageData(downloadTo: File, url: String, resource: ResourceLocation, buffered: Boolean): ThreadDownloadImageData
    fun initSession(username: String, playerID: String, token: String, sessionType: String): SessionBridge
    fun initCustomScreen(impl: LunarCustomScreen): ScreenBridge
    fun initTessellator(): TessellatorBridge
    fun initSizedDynamicTexture(width: Int, height: Int): NativeImage
    fun initCopiedDynamicTexture(copiedFrom: BufferedImage): NativeImage
    fun initPacketBuffer(buf: ByteBuf): PacketBuffer
    fun initCustomPayload(channel: String, buffer: PacketBuffer): Any

    // TODO
    fun getBlockFromItem(item: Any): Any
    fun initOldServerPinger(): Any
    fun initGuiSelectWorld(parent: ScreenBridge?): ScreenBridge
    fun initGuiMultiplayer(parent: ScreenBridge?): ScreenBridge
    fun initGuiOptions(parent: ScreenBridge?): ScreenBridge
    fun initLanguage(parent: ScreenBridge?): ScreenBridge
    fun initOpenLink(parent: ScreenBridge?, message: String, link: URI, trusted: Boolean): ScreenBridge
    fun initChatClickEvent(action: Any, param: String): Any
    fun initChatHoverEvent(action: Any, component: ChatComponent): Any
    fun initItemStack(item: ItemBridge): ItemStack
    fun initEmptyItemStack(): ItemStack
    val bossStatus: List<Any>
    fun initScoreboard(): Scoreboard
    fun initDummyScoreObjective(scoreboard: Scoreboard, name: String): ScoreboardObjective
    fun initAABB(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): AxisAlignedBB
    fun initLCBorder(id: String, display: String, progress: Int): FakeBorder
    fun initCustomTexture(texture: Any): Any
    fun initTickingTexture(texture: Any): Any
    fun initDummyPlayer(): LocalPlayer
    fun initMovementInput(settings: ClientSettings): MovementInput
    fun initServerData(name: String, ip: String, isLan: Boolean): ServerData
    val geometryTessellator: GeomTesselator
    fun readImage(input: InputStream): BufferedImage
    fun initFrameBuffer(width: Int, height: Int, depth: Boolean): FrameBuffer
    fun initOffscreenRenderTarget(width: Int, height: Int, texture: Int): FrameBuffer
    fun loadShaderGroup(buffer: FrameBuffer, location: ResourceLocation): ShaderGroup
    fun initBlockRenderBatch(): BlockRenderBatch
    fun openURL(url: URI): Boolean
    fun openURLString(url: String): Boolean
    fun openFileForEditing(file: File): Boolean
    fun reloadVanillaKeybindings() //
    fun initNbtTagCompound(): NbtTagCompound //

    interface Static : StaticAccessor<InitializerBridge>
    companion object : Static by initializerAccess.static
}

val tdidBridge by bridgeAccessor<_, ThreadDownloadImageData.Static>()

interface ThreadDownloadImageData : InstanceAccessor {
    val isUploaded: Boolean
    val file: File
    fun requestContent(): CompletableFuture<Any>

    interface Static : StaticAccessor<ThreadDownloadImageData>
    companion object : Static by tdidBridge.static
}

fun InitializerBridge.initCustomScreen(screen: CustomScreen) = initCustomScreen(LunarCustomScreen.create(screen))

val itemBridgeAccess by bridgeAccessor<_, ItemBridge.Static>()

interface ItemBridge : InstanceAccessor {
    fun isItemPotion(): Boolean
    fun isItemSoup(): Boolean
    fun isItemSkull(): Boolean
    fun hasEffect(stack: ItemStack): Boolean
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun getColorFromItemStack(stack: ItemStack, durability: Int): Integer
    fun shouldRotateAroundWhenRendering(): Boolean
    fun getSprite(stack: ItemStack, durability: Int): Optional<Any>
    fun requiresMultipleRenderPasses(): Boolean
    fun isItemBlock(): Boolean
    val registryName: String
    fun isRepairable(stack: ItemStack, by: ItemStack): Boolean
    val food: Optional<Any>

    interface Static : StaticAccessor<ItemBridge>
    companion object : Static by itemBridgeAccess.static
}

val initCopiedDynamicTexture by initializerBridge.methods
val nativeImageAccess by emptyAccessor<_, NativeImage.Static> { initCopiedDynamicTexture().method.returnType.internalName }

interface NativeImage : InstanceAccessor {
    interface Static : StaticAccessor<NativeImage>
    companion object : Static by nativeImageAccess.static
}

val blockAccess by bridgeAccessor<_, BlockBridge.Static>()

interface BlockBridge : InstanceAccessor {
    fun isSkull(): Boolean
    fun isFoliage(): Boolean
    fun getStack(item: BlockPosition): ItemStack
    fun isWater(): Boolean
    fun isAir(): Boolean

    interface Static : StaticAccessor<BlockBridge>
    companion object : Static by blockAccess.static
}

val nbtTagCompoundAccess by bridgeAccessor<_, NbtTagCompound.Static>()

interface NbtTagCompound : InstanceAccessor {
    fun getLong(index: String): Long
    fun getByte(index: String): Byte
    fun getInteger(index: String): Int
    fun getString(index: String): String
    fun getFloat(index: String): Float
    fun getDouble(index: String): Double
    fun getShort(index: String): Short
    fun getBoolean(index: String): Boolean
    fun getCompoundTag(index: String): NbtTagCompound
    fun putString(index: String, value: String)

    interface Static : StaticAccessor<NbtTagCompound>
    companion object : Static by nbtTagCompoundAccess.static
}

val blockRenderBatchAccess by bridgeAccessor<_, BlockRenderBatch.Static>()

interface BlockRenderBatch : InstanceAccessor {
    fun renderBlock(block: BlockBridge, x: Int, z: Int)
    fun flush()

    interface Static : StaticAccessor<BlockRenderBatch>
    companion object : Static by blockRenderBatchAccess.static
}

val shaderGroupAccess by bridgeAccessor<_, ShaderGroup.Static>()

interface ShaderGroup : InstanceAccessor {
    fun listShaders(): List<Any>
    val shaderGroupName: String
    fun process(partialTicks: Float)
    fun close()

    interface Static : StaticAccessor<ShaderGroup>
    companion object : Static by shaderGroupAccess.static
}

val ShaderGroup.actualShaders get() = listShaders().map { ShaderInstance.cast(it) }

val shaderInstanceAccess by bridgeAccessor<_, ShaderInstance.Static>()

interface ShaderInstance : InstanceAccessor {
    val shaderManager: ShaderManager

    interface Static : StaticAccessor<ShaderInstance>
    companion object : Static by shaderInstanceAccess.static
}

val shaderManagerAccess by bridgeAccessor<_, ShaderManager.Static>()

interface ShaderManager : InstanceAccessor {
    fun getShaderUniform(name: String): ShaderUniform

    interface Static : StaticAccessor<ShaderManager>
    companion object : Static by shaderManagerAccess.static
}

val shaderUniformAccess by bridgeAccessor<_, ShaderUniform.Static> {
    methods {
        "set1f" { arguments count 1 }
        "set2f" { arguments count 2 }
        "set3f" { arguments count 3 }
    }
}

interface ShaderUniform : InstanceAccessor {
    fun set1f(f: Float)
    fun set2f(f1: Float, f2: Float)
    fun set3f(f1: Float, f2: Float, f3: Float)

    interface Static : StaticAccessor<ShaderUniform>
    companion object : Static by shaderUniformAccess.static
}

val resourceLocationAccess by bridgeAccessor<_, ResourceLocation.Static> {
    methods {
        named("create") { arguments count 1 }
        "createWithDomain" {
            method named "create"
            arguments count 2
        }
    }
}

interface ResourceLocation : InstanceAccessor {
    val domain: String
    val path: String

    interface Static : StaticAccessor<ResourceLocation> {
        fun createWithDomain(domain: String, path: String): ResourceLocation
        fun create(path: String): ResourceLocation
    }

    companion object : Static by resourceLocationAccess.static
}

val frameBufferAccess by bridgeAccessor<_, FrameBuffer.Static>()

interface FrameBuffer : InstanceAccessor {
    fun framebufferWidth(): Int
    fun framebufferHeight(): Int
    fun framebufferTextureWidth(): Int
    fun framebufferTextureHeight(): Int
    val framebufferTexture: Int
    fun unbindFrameBuffer()
    fun frameBufferRender(x: Int, y: Int)
    fun bindWrite(binds: Boolean)
    fun delete()

    interface Static : StaticAccessor<FrameBuffer>
    companion object : Static by frameBufferAccess.static
}

val geomTesselatorAccess by bridgeAccessor<_, GeomTesselator.Static>()

interface GeomTesselator : InstanceAccessor {
    fun setTranslation(x: Double, y: Double, z: Double)
    fun begin(mode: Int)
    fun draw()
    fun setDelta(delta: Double)
    fun drawCuboid(renderer: Any, x: Int, y: Int)
    fun drawLines(renderer: Any, x: Int, y: Int)
    fun isDrawing(): Boolean

    interface Static : StaticAccessor<GeomTesselator>
    companion object : Static by geomTesselatorAccess.static
}

val movementInputAccess by bridgeAccessor<_, MovementInput.Static>()

interface MovementInput : InstanceAccessor {
    val strafeSpeed: Float
    val forwardSpeed: Float
    fun isSneaking(): Boolean
    fun isJumping(): Boolean
    fun setMoveForward(moveForward: Float)
    fun setMoveStrafe(strafe: Float)
    fun setJump(jump: Boolean)
    fun setSneak(sneak: Boolean)
    fun up(): Boolean
    fun down(): Boolean
    fun left(): Boolean
    fun right(): Boolean
    fun setUp(up: Boolean)
    fun setDown(down: Boolean)
    fun setLeft(left: Boolean)
    fun setRight(right: Boolean)

    interface Static : StaticAccessor<MovementInput>
    companion object : Static by movementInputAccess.static
}

val keyBindingAccess by bridgeAccessor<_, KeyBinding.Static>()

interface KeyBinding : InstanceAccessor {
    var key: KeyType
    fun isKeyDown(): Boolean
    val keyName: String
    val keyDescription: String
    fun setKeyBindState(held: Boolean)
    val clashesWith: List<Any>
    val category: String

    interface Static : StaticAccessor<KeyBinding>
    companion object : Static by keyBindingAccess.static
}

val keyTypeFinder = enumFinder<KeyType.Static> { isLunarClass() }
val keyTypeAccessor by accessor<_, KeyType.Static>(keyTypeFinder)

interface KeyType : InstanceAccessor {
    interface Static : StaticAccessor<KeyType> {
        val KEY_NONE: KeyType
        val KEY_0: KeyType
        val KEY_1: KeyType
        val KEY_2: KeyType
        val KEY_3: KeyType
        val KEY_4: KeyType
        val KEY_5: KeyType
        val KEY_6: KeyType
        val KEY_7: KeyType
        val KEY_8: KeyType
        val KEY_9: KeyType
        val KEY_A: KeyType
        val KEY_B: KeyType
        val KEY_C: KeyType
        val KEY_D: KeyType
        val KEY_E: KeyType
        val KEY_F: KeyType
        val KEY_G: KeyType
        val KEY_H: KeyType
        val KEY_I: KeyType
        val KEY_J: KeyType
        val KEY_K: KeyType
        val KEY_L: KeyType
        val KEY_M: KeyType
        val KEY_N: KeyType
        val KEY_O: KeyType
        val KEY_P: KeyType
        val KEY_Q: KeyType
        val KEY_R: KeyType
        val KEY_S: KeyType
        val KEY_T: KeyType
        val KEY_U: KeyType
        val KEY_V: KeyType
        val KEY_W: KeyType
        val KEY_X: KeyType
        val KEY_Y: KeyType
        val KEY_Z: KeyType
        val KEY_RSHIFT: KeyType
        val KEY_LSHIFT: KeyType
        val KEY_FUNCTION: KeyType
        val KEY_LCONTROL: KeyType
        val KEY_RCONTROL: KeyType
        val KEY_LCOMMAND: KeyType
        val KEY_RCOMMAND: KeyType
        val KEY_LWINDOWS: KeyType
        val KEY_RWINDOWS: KeyType
        val KEY_CLEAR: KeyType
        val KEY_LMENU: KeyType
        val KEY_RMENU: KeyType
        val KEY_RETURN: KeyType
        val KEY_ESCAPE: KeyType
        val KEY_GRAVE: KeyType
        val KEY_TAB: KeyType
        val KEY_UP: KeyType
        val KEY_BACK: KeyType
        val KEY_LEFT: KeyType
        val KEY_RIGHT: KeyType
        val KEY_HOME: KeyType
        val KEY_PGUP: KeyType
        val KEY_PGDOWN: KeyType
        val KEY_PG: KeyType
        val KEY_END: KeyType
        val KEY_DELETE: KeyType
        val KEY_F1: KeyType
        val KEY_F2: KeyType
        val KEY_F3: KeyType
        val KEY_F4: KeyType
        val KEY_F5: KeyType
        val KEY_F6: KeyType
        val KEY_F7: KeyType
        val KEY_F8: KeyType
        val KEY_F9: KeyType
        val KEY_F10: KeyType
        val KEY_F11: KeyType
        val KEY_F12: KeyType
        val KEY_DOWN: KeyType
        val KEY_SPACE: KeyType
        val KEY_MOUSE1: KeyType
        val KEY_MOUSE2: KeyType
        val KEY_MOUSE3: KeyType
        val KEY_MOUSE4: KeyType
        val KEY_MOUSE5: KeyType
        val KEY_MOUSE6: KeyType
        val KEY_MOUSE7: KeyType
        val KEY_MOUSE8: KeyType
        val KEY_MOUSE9: KeyType
        val KEY_MOUSE10: KeyType
        val KEY_MOUSE11: KeyType
        val KEY_MOUSE12: KeyType
        val KEY_MOUSE13: KeyType
        val KEY_MOUSE14: KeyType
        val KEY_MOUSE15: KeyType
        val KEY_MOUSE16: KeyType
        val KEY_MINUS: KeyType
        val KEY_EQUALS: KeyType
        val KEY_LBRACKET: KeyType
        val KEY_RBRACKET: KeyType
        val KEY_SEMICOLON: KeyType
        val KEY_APOSTROPHE: KeyType
        val KEY_BACKSLASH: KeyType
        val KEY_COMMA: KeyType
        val KEY_PERIOD: KeyType
        val KEY_SLASH: KeyType
        val KEY_MULTIPLY: KeyType
        val KEY_CAPITAL: KeyType
        val KEY_NUMLOCK: KeyType
        val KEY_SCROLL: KeyType
        val KEY_PRINTSC: KeyType
        val KEY_NUMPAD7: KeyType
        val KEY_NUMPAD8: KeyType
        val KEY_NUMPAD9: KeyType
        val KEY_SUBTRACT: KeyType
        val KEY_NUMPAD4: KeyType
        val KEY_NUMPAD5: KeyType
        val KEY_NUMPAD6: KeyType
        val KEY_ADD: KeyType
        val KEY_NUMPAD1: KeyType
        val KEY_NUMPAD2: KeyType
        val KEY_NUMPAD3: KeyType
        val KEY_NUMPAD0: KeyType
        val KEY_DECIMAL: KeyType
        val KEY_F13: KeyType
        val KEY_F14: KeyType
        val KEY_F15: KeyType
        val KEY_F16: KeyType
        val KEY_F17: KeyType
        val KEY_F18: KeyType
        val KEY_F19: KeyType
        val KEY_NUMPADEQUALS: KeyType
        val KEY_NUMPADENTER: KeyType
        val KEY_DIVIDE: KeyType
        val KEY_PAUSE: KeyType
        val KEY_INSERT: KeyType
    }

    companion object : Static by keyTypeAccessor.static
}

val KeyBinding.actualClashesWith get() = clashesWith.map(KeyBinding::cast)

val clientSettingsAccess by bridgeAccessor<_, ClientSettings.Static>()

interface ClientSettings : InstanceAccessor {
    fun setThirdPersonView(index: Int)
    val thirdPersonView: Int
    val screenshotKey: Any
    fun keyBindForward(): KeyBinding
    fun keyBindLeft(): KeyBinding
    fun keyBindBack(): KeyBinding
    fun keyBindRight(): KeyBinding
    fun keyBindJump(): KeyBinding
    fun keyBindAttack(): KeyBinding
    fun keyBindUseItem(): KeyBinding
    fun keyBindSprint(): KeyBinding
    fun keyBindSneak(): KeyBinding
    fun keyBindPlayerList(): KeyBinding
    fun keyBindTogglePerspective(): KeyBinding
    val keyBindings: Array<KeyBinding>
    fun setKeyBinds(binds: Array<KeyBinding>)
    fun isFancyGraphics(): Boolean
    val renderDistance: Int
    fun setGamma(gamma: Float)
    fun setGammaOverride(gammaOverride: Float)
    fun removeGammaOverride()
    fun isSettingGamma(): Boolean
    fun postSetGammaByUser()
    fun showDebugInfo(): Boolean
    val modelParts: Set<Any>
    fun isHideGui(): Boolean
    val chatScale: Float
    fun setOptionFloatValue(optionIndex: Int, value: Float)
    fun setFancyGraphics(fancyGraphics: Boolean)
    fun setKeyBindState(key: KeyType, enabled: Boolean)
    fun unpressAllKeys()
    val resourcePacks: List<Any>
    var smoothCamera: Boolean
    fun updateVSync()
    var toggleSprint: Boolean
    var toggleSneak: Boolean
    val zoomKey: Optional<Any>
    var frameRateLimit: Int
    fun setVBO(useVBOS: Boolean)
    val panoramaSpeed: Double

    interface Static : StaticAccessor<ClientSettings>
    companion object : Static by clientSettingsAccess.static
}

val InitializerBridge.actualBossStatus get() = bossStatus.map(BossStatus::cast)

val fakeBorderAccess by bridgeAccessor<_, FakeBorder.Static>()

interface FakeBorder : InstanceAccessor {
    val id: String
    fun update(x: Double, y: Double, z: Double, arg3: Double, arg4: Int)
    fun setCancelEntry(cancelEntry: Boolean)
    fun setCancelExit(cancelExit: Boolean)
    fun setColor(color: Int)
    fun shouldRender(): Boolean
    fun renderBox(partialTicks: Float)
    fun isCancelEntry(): Boolean
    fun contains(x: Double, z: Double): Boolean
    fun isCancelExit(): Boolean
    val walls: List<Any>
    fun setCanShrink(canShrink: Boolean)
    fun setCenter(x: Double, z: Double)
    fun setStaticBounds(bounds: AxisAlignedBB)
    fun setTransition(speed: Double)
    fun allowsInteraction(): Boolean
    val color: Int
    val world: String
    fun isWorldBorder(): Boolean

    interface Static : StaticAccessor<FakeBorder>
    companion object : Static by fakeBorderAccess.static
}

val FakeBorder.actualWalls get() = walls.map(AxisAlignedBB::cast)

val aabbAccess by bridgeAccessor<_, AxisAlignedBB.Static>()

interface AxisAlignedBB {
    val minX: Double
    val minY: Double
    val minZ: Double
    val maxX: Double
    val maxY: Double
    val maxZ: Double

    fun expand(dx: Double, dy: Double, dz: Double): AxisAlignedBB
    fun offset(dx: Double, dy: Double, dz: Double): AxisAlignedBB
    fun intersectsWith(other: AxisAlignedBB): Boolean

    interface Static : StaticAccessor<AxisAlignedBB>
    companion object : Static by aabbAccess.static
}

val AxisAlignedBB.width get() = maxX - minX
val AxisAlignedBB.height get() = maxY - minY

val chatColorAccess by enumAccessor<_, ChatColor.Static> {
    methods {
        named("getFormattingCode")
        named("isFancyStyling")
        named("isColor")
        named("getFriendlyName")
        named("getColorIndex")
        named("getLegacyColor")
    }
}

interface ChatColor : InstanceAccessor {
    val formattingCode: Char
    val isFancyStyling: Boolean
    val isColor: Boolean
    val friendlyName: String
    val colorIndex: Int
    val legacyColor: Int

    interface Static : StaticAccessor<ChatColor> {
        val BLACK: ChatColor
        val DARK_BLUE: ChatColor
        val DARK_GREEN: ChatColor
        val DARK_AQUA: ChatColor
        val DARK_RED: ChatColor
        val DARK_PURPLE: ChatColor
        val GOLD: ChatColor
        val GRAY: ChatColor
        val DARK_GRAY: ChatColor
        val BLUE: ChatColor
        val GREEN: ChatColor
        val AQUA: ChatColor
        val RED: ChatColor
        val LIGHT_PURPLE: ChatColor
        val YELLOW: ChatColor
        val WHITE: ChatColor
        val OBFUSCATED: ChatColor
        val BOLD: ChatColor
        val STRIKETHROUGH: ChatColor
        val UNDERLINE: ChatColor
        val ITALIC: ChatColor
        val RESET: ChatColor
    }

    companion object : Static by chatColorAccess.static
}

val scorePlayerTeamAccess by bridgeAccessor<_, ScorePlayerTeam.Static>()

interface ScorePlayerTeam : InstanceAccessor {
    val chatFormat: ChatColor
    fun formatString(str: String): ChatComponent

    interface Static : StaticAccessor<ScorePlayerTeam>
    companion object : Static by scorePlayerTeamAccess.static
}

val scoreboardAccess by bridgeAccessor<_, Scoreboard.Static>()

interface Scoreboard : InstanceAccessor {
    fun getPlayersTeam(player: String): ScorePlayerTeam
    fun getObjectiveInDisplaySlot(slot: Int): ScoreboardObjective
    fun getSortedScores(objective: ScoreboardObjective): Collection<Any>
    fun getValueFromObjective(player: String, objective: ScoreboardObjective): ScoreboardScore

    interface Static : StaticAccessor<Scoreboard>
    companion object : Static by scoreboardAccess.static
}

fun Scoreboard.getActualSortedScores(objective: ScoreboardObjective) =
    getSortedScores(objective).map(ScoreboardScore::cast)

val scoreAccess by bridgeAccessor<_, ScoreboardScore.Static>()

interface ScoreboardScore : InstanceAccessor {
    val playerName: String

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    val scorePoints: Integer
    fun setScorePoints(value: Int)

    interface Static : StaticAccessor<ScoreboardScore>
    companion object : Static by scoreAccess.static
}

val scoreboardObjectiveAccess by bridgeAccessor<_, ScoreboardObjective.Static>()

interface ScoreboardObjective : InstanceAccessor {
    val scoreboard: Scoreboard
    val displayName: ChatComponent
    fun setDisplayName(rawName: String)

    interface Static : StaticAccessor<ScoreboardObjective>
    companion object : Static by scoreboardObjectiveAccess.static
}

val itemStackAccess by bridgeAccessor<_, ItemStack.Static>()

interface ItemStack : InstanceAccessor {
    val displayName: String
    val rawDisplayName: String
    fun hasCustomLore(): Boolean
    val item: ItemBridge
    var stackSize: Int
    val maxDamage: Int
    var itemDamage: Int
    val isItemDamaged: Boolean
    val maxStackSize: Int
    val isItemStackDamageableNoUnbr: Boolean
    var repairCost: Int
    val isItemStackDamageable: Boolean
    fun hasDisplayName(): Boolean
    fun clearCustomName()
    fun setStackDisplayName(name: String)
    fun copy(): ItemStack
    fun setEnchantments(enchantments: Map<Any, Int>)
    val isEmpty: Boolean
    val itemUseAction: Any
    val maxItemUseDuration: Int
    fun areItemsEqual(comparedWith: ItemStack): Boolean
    val unlocalizedName: String
    fun getTooltip(player: LocalPlayer, flag: Boolean): List<String>

    interface Static : StaticAccessor<ItemStack>
    companion object : Static by itemStackAccess.static
}

val screenBridgeAccess by bridgeAccessor<_, ScreenBridge.Static>()

// Do not implement, there will be utils to implement this
interface ScreenBridge : InstanceAccessor {
    fun drawScreen(x: Int, y: Int, partialTicks: Float)
    fun setWorldAndResolution(client: ClientBridge, width: Int, height: Int)
    fun mouseClicked(x: Int, y: Int, buttonIndex: Int)
    fun mouseReleased(x: Int, y: Int, buttonIndex: Int)
    fun onGuiClosed()
    fun updateScreen()
    fun keyTyped(key: Char, keyEnum: Any)
    fun handleMouseInput()
    fun setClickedLinkURI(link: URI)
    fun isControlsGui(): Boolean
    fun allowUserInput(): Boolean
    fun drawScrollableHoveringText(renderer: Any, components: List<Any>, x: Int, y: Int)
    fun doesGuiPauseGame(): Boolean

    interface Static : StaticAccessor<ScreenBridge>
    companion object : Static by screenBridgeAccess.static
}

val bossStatusAccess by bridgeAccessor<_, BossStatus.Static>()

interface BossStatus : InstanceAccessor {
    var healthScale: Float
    val bossName: Optional<String>
    fun setBossName(name: String)
    var statusBarTime: Int
    fun hasColorModifier(): Boolean
    fun drawBar(renderer: Any, x: Float, y: Float)

    interface Static : StaticAccessor<BossStatus>
    companion object : Static by bossStatusAccess.static
}

val packetBufferAccess by bridgeAccessor<_, PacketBuffer.Static>()

interface PacketBuffer : InstanceAccessor {
    fun writeInt(int: Int)
    fun readInt(): Int
    fun writeString(str: String)
    fun readStringFromBuffer(maxLength: Int): String
    fun writeLong(long: Long)
    fun readLong(): Long
    fun readBoolean(): Boolean
    fun writeVarIntToBuffer(value: Int)
    fun readVarIntFromBuffer(): Int
    fun writeShort(shortAsInt: Int)
    fun readShort(): Short
    fun writeBytes(bytes: ByteArray)
    fun readBytes(target: ByteArray)
    fun readFloat(): Float
    fun writeBoolean(value: Boolean)
    fun readableBytes(): Int
    fun release(): Boolean
    fun writeFloat(value: Float)

    interface Static : StaticAccessor<PacketBuffer>
    companion object : Static by packetBufferAccess.static
}

fun PacketBuffer.readUUID() = UUID(readLong(), readLong())
fun PacketBuffer.writeUUID(uuid: UUID) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun PacketBuffer.toByteArray(): ByteArray {
    val bytes = ByteArray(readableBytes())
    readBytes(bytes)
    release()
    return bytes
}

val sessionBridgeAccess by bridgeAccessor<_, SessionBridge.Static>()

interface SessionBridge : InstanceAccessor {
    val playerID: String
    val username: String
    val token: String
    val profile: GameProfile

    interface Static : StaticAccessor<SessionBridge>
    companion object : Static by sessionBridgeAccess.static
}

val clientBridgeAccess by bridgeAccessor<_, ClientBridge.Static>()

interface ClientBridge : InstanceAccessor {
    val player: PlayerBridge?
    val world: WorldBridge?
    val currentServerData: ServerData?
    val currentScreen: ScreenBridge?
    val isWindowFocused: Boolean
    fun hasInGameFocus(): Boolean
    fun setDisplayTitle(title: String)
    val pointedEntity: Optional<Any>
    val mcDataDir: File
    fun displayWidth(): Int
    fun displayHeight(): Int
    fun unicode(): Boolean
    val guiScale: Int
    fun loadWorld(world: WorldBridge)
    fun refreshResources()
    fun shutdownMinecraftApplet()
    val gameSettings: ClientSettings
    val windowId: Optional<Long>
    val systemTime: Long
    val debugFPS: Int
    fun isFullScreen(): Boolean
    fun toggleFullscreen()
    fun submit(task: Runnable)
    fun isGamePaused(): Boolean
    fun isDisplayCreated(): Boolean
    fun isDisplayActive(): Boolean
    fun setRepeatEventsEnabled(repeatEvents: Boolean)
    fun connect(serverData: ServerData, parent: ScreenBridge?)
    fun isConnectedToRealms(): Boolean
    fun updateDisplay()
    fun renderChunkBorder(): Boolean
    fun setRenderChunkBorder(enabled: Boolean)
    fun setCurrentServer(server: ServerData?)
    val playerController: PlayerController?
    val netHandler: NetHandlerBridge
    val fontRenderer: FontRendererBridge
    val resourceManager: ResourceManager
    val mcDefaultResourcePack: ResourcePack
    val textureManager: TextureManagerBridge
    var session: SessionBridge
//    val profileProperties: Multimap<String, Any>
    val sessionService: Any
    val entityRenderer: EntityRenderer
    val renderItem: ItemRenderer
    val soundHandler: SoundHandler
    fun displayScreen(screen: ScreenBridge?)
    val renderManager: RenderManagerBridge
    val timer: TimerBridge
    val guiIngame: GuiIngame?
    val framebuffer: FrameBuffer
    val renderViewEntity: EntityBridge
    val renderGlobal: GlobalRenderer
    val textureMap: TextureMap
    val effectRenderer: EffectRenderer
    val objectMouseOver: MouseOverEntity?
    fun lastServerData(): ServerData?
    val selectedResourcePack: ResourcePack?
    val skinManager: SkinManager

    interface Static : StaticAccessor<ClientBridge>
    companion object : Static by clientBridgeAccess.static
}

object RenderManager : RenderManagerBridge by Minecraft.renderManager
object ClientTimer : TimerBridge by Minecraft.timer
object FontRenderer : FontRendererBridge by Minecraft.fontRenderer
object TextureManager : TextureManagerBridge by Minecraft.textureManager

val skinManagerAccess by bridgeAccessor<_, SkinManager.Static>()

interface SkinManager : InstanceAccessor {
    fun getInsecureSkinInformation(profile: GameProfile): Map<Any, Any>
    fun registerTexture(texture: Any, type: Any): ResourceLocation

    interface Static : StaticAccessor<SkinManager>
    companion object : Static by skinManagerAccess.static
}

val mouseOverEntityAccess by bridgeAccessor<_, MouseOverEntity.Static>()

interface MouseOverEntity : InstanceAccessor {
    val entityHit: EntityBridge
    val blockPosition: BlockPosition
    fun isTypeOfHit(type: Any): Boolean
    fun border()
    fun isBorder(): Boolean

    interface Static : StaticAccessor<MouseOverEntity>
    companion object : Static by mouseOverEntityAccess.static
}

val effectRendererAccess by bridgeAccessor<_, EffectRenderer.Static>()

interface EffectRenderer : InstanceAccessor {
    fun addEffect(effect: Any)
    fun emitParticleAtEntity(entity: EntityBridge, particle: Any)

    interface Static : StaticAccessor<EffectRenderer>
    companion object : Static by effectRendererAccess.static
}

val textureMapAccess by bridgeAccessor<_, TextureMap.Static>()

interface TextureMap : InstanceAccessor {
    val blockTextures: ResourceLocation
    val itemTextures: ResourceLocation
    fun getAtlasSprite(name: String): Any

    interface Static : StaticAccessor<TextureMap>
    companion object : Static by textureMapAccess.static
}

val globalRendererAccess by bridgeAccessor<_, GlobalRenderer.Static>()

interface GlobalRenderer : InstanceAccessor {
    val maximumRenderCount: Int
    val unculledRenderCount: Int
    fun setNeedsFullRenderChunkUpdate(enabled: Boolean)
    fun reloadChunks()

    interface Static : StaticAccessor<GlobalRenderer>
    companion object : Static by globalRendererAccess.static
}

val guiIngameAccess by bridgeAccessor<_, GuiIngame.Static>()

interface GuiIngame : InstanceAccessor {
    fun renderGameOverlay(partialTicks: Float)
    fun showCrosshair(): Boolean
    val chatGUI: ChatGui
    fun resetTitleTimer()
    val title: Any // from adventure
    fun titlesTimer(): Int
    val ticks: Int

    interface Static : StaticAccessor<GuiIngame>
    companion object : Static by guiIngameAccess.static
}

val chatGuiAccess by bridgeAccessor<_, ChatGui.Static>()

interface ChatGui : InstanceAccessor {
    fun printChatMessage(component: ChatComponent)
    fun sendMessageWithId(component: ChatComponent, id: Int)
    val chatOpen: Boolean
    fun deleteMessage(index: Int)
    val screenName: Optional<String>

    interface Static : StaticAccessor<ChatGui>
    companion object : Static by chatGuiAccess.static
}

val timerAccess by bridgeAccessor<_, TimerBridge.Static>()

interface TimerBridge : InstanceAccessor {
    fun partialTick(): Float
    fun advanceTime(by: Long)
    fun timer(): Float

    interface Static : StaticAccessor<TimerBridge>
    companion object : Static by timerAccess.static
}

val renderablePlayerAccess by bridgeAccessor<_, RenderablePlayer.Static>()

interface RenderablePlayer : InstanceAccessor {
    fun setLocationOfCape(loc: ResourceLocation)
    fun setLocationOfCapeOverride(loc: ResourceLocation)
    val skinType: String
    fun loadAndGetRealSkinType(): Optional<Any>
    val locationSkin: ResourceLocation
    fun getSwingProgress(partialTicks: Float): Float
    fun setSkinLocationOverride(loc: ResourceLocation, type: String)
    val locationSkinDefault: ResourceLocation
    fun isSkinTextureUploaded(): Boolean
    val teamName: Optional<String>

    interface Static : StaticAccessor<RenderablePlayer>
    companion object : Static by renderablePlayerAccess.static
}

val modelPartsAccess by bridgeAccessor<_, PlayerModelParts.Static>()

interface PlayerModelParts : InstanceAccessor {
    fun cloak(): PlayerModelPart
    fun leftSleeve(): PlayerModelPart
    fun rightSleeve(): PlayerModelPart
    fun leftPants(): PlayerModelPart
    fun rightPants(): PlayerModelPart
    fun jacket(): PlayerModelPart
    fun isSlim(): Boolean

    interface Static : StaticAccessor<PlayerModelParts>
    companion object : Static by modelPartsAccess.static
}

val modelPartAccess by bridgeAccessor<_, PlayerModelPart.Static>()

interface PlayerModelPart : InstanceAccessor {
    fun setTextureOffsetX(x: Int)
    fun setTextureOffsetY(y: Int)
    var rotateAngleX: Float
    var rotateAngleY: Float
    var rotateAngleZ: Float
    var rotatePointX: Float
    var rotatePointY: Float
    var rotatePointZ: Float
    fun isVisible(): Boolean
    fun setVisible(visible: Boolean)
    fun postRender(partialTicks: Float)
    fun render(partialTicks: Float, loc: ResourceLocation)

    interface Static : StaticAccessor<PlayerModelPart>
    companion object : Static by modelPartAccess.static
}

val playerRendererAccess by bridgeAccessor<_, PlayerRenderer.Static>()

interface PlayerRenderer : InstanceAccessor {
    val mainModel: PlayerModelParts
    fun renderEquippedItems(player: RenderablePlayer, partialTicks: Float)
    fun hasModernSkin(player: RenderablePlayer): Boolean
    fun setPlayerModelType(skinny: Boolean)

    interface Static : StaticAccessor<PlayerRenderer>
    companion object : Static by playerRendererAccess.static
}

val renderManagerAccess by bridgeAccessor<_, RenderManagerBridge.Static>()

interface RenderManagerBridge : InstanceAccessor {
    val renderPosX: Double
    val renderPosY: Double
    val renderPosZ: Double
    val playerViewX: Double
    val playerViewY: Double
    fun viewerPosX(): Double
    fun viewerPosY(): Double
    fun viewerPosZ(): Double
    fun defaultPlayerRenderer(): PlayerRenderer
    val skinMap: Map<String, Any>
    fun setTextureManager(manager: TextureManagerBridge)
    fun setLivingEntity(entity: LivingEntity)
    fun setOptions(options: ClientSettings)
    fun setRenderShadow(renderShadow: Boolean)
    fun setPlayerViewY(viewY: Float)
    fun renderEntityWithPosYaw(
        renderer: Any,
        entity: EntityBridge,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        partialTicks: Float
    )

    fun setDebugBoundingBox(enabled: Boolean)
    fun showDebugBoundingBox(): Boolean
    fun prepare(world: WorldBridge, entity: EntityBridge)
    fun onPlayerRenderersReloaded(onReloaded: Runnable)
    val camera: Optional<Any>

    interface Static : StaticAccessor<RenderManagerBridge>
    companion object : Static by renderManagerAccess.static
}

val RenderManagerBridge.actualCamera get() = camera.map { Camera.cast(it) }.getOrNull()
val RenderManagerBridge.actualSkinMap get() = skinMap.mapValues { (_, v) -> PlayerRenderer.cast(v) }

val cameraAccess by bridgeAccessor<_, Camera.Static>()

interface Camera : InstanceAccessor {
    val posX: Double
    val posY: Double
    val posZ: Double
    val yaw: Float
    val pitch: Float

    interface Static : StaticAccessor<Camera>
    companion object : Static by cameraAccess.static
}

val potionEffectAccess by bridgeAccessor<_, PotionEffect.Static>()

interface PotionEffect : InstanceAccessor {
    fun getIsPotionDurationMax(): Boolean
    val potionID: Int
    val duration: Float
    val amplifier: Int
    val effectName: String
    fun renderEffectIcon(x: Float, y: Float)

    interface Static : StaticAccessor<PotionEffect>
    companion object : Static by potionEffectAccess.static
}

val potionEffectTypeAccess by bridgeAccessor<_, PotionEffectType.Static>()

interface PotionEffectType : InstanceAccessor {
    fun isBadEffect(): Boolean
    fun hasStatusIcon(): Boolean
    val statusIconIndex: Int
    fun getID(): Int

    interface Static : StaticAccessor<PotionEffectType>
    companion object : Static by potionEffectTypeAccess.static
}

val livingEntityAccess by bridgeAccessor<_, LivingEntity.Static>()

interface LivingEntity : InstanceAccessor {
    val lastAttacker: Optional<Any>
    val lastAttackerTime: Int
    val hurtTime: Int
    val deathTime: Int
    fun isPotionActive(type: PotionEffectType): Boolean
    fun getActivePotionEffect(type: PotionEffectType): PotionEffect
    val activePotionEffects: List<Any>
    fun isOnLadder(): Boolean
    fun isInWater(): Boolean
    val armSwingAnimationEnd: Int
    var renderYawOffset: Float
    val previousRotationYawOffset: Float
    var rotationYawHead: Float
    var prevRotationYawHead: Float
    var prevRenderYawOffset: Float
    fun getEquipmentInSlot(slot: Int): ItemStack
    val health: Float
    fun isPlayerSleeping(): Boolean
    val moveForward: Float
    val heldItem: ItemStack
    fun shouldAlwaysRenderNametag(): Boolean
    fun setAlwaysRenderNameTag(alwaysRender: Boolean)
    val displayName: String
    fun isSwimming(): Boolean
    fun isElytraFlying(): Boolean
    val lastAttackedMillis: Long
    val lastHurtMillis: Long

    interface Static : StaticAccessor<LivingEntity>
    companion object : Static by livingEntityAccess.static
}

val LivingEntity.actualPotionEffects get() = activePotionEffects.map { PotionEffectType.cast(it) }
val LivingEntity.actualLastAttacker get() = lastAttacker.map { LivingEntity.cast(it) }.getOrNull()

val entityBridgeAccess by bridgeAccessor<_, EntityBridge.Static>()

interface EntityBridge : InstanceAccessor {
    fun isSneaking(): Boolean
    var posX: Double
    var posY: Double
    var posZ: Double
    val motionX: Double
    var motionY: Double
    val motionZ: Double
    var rotationYaw: Double
    var rotationPitch: Double
    val previousRotationYaw: Double
    val previousRotationPitch: Double
    val uniqueID: UUID
    val boundingBox: AxisAlignedBB
    val frustumBoundingBox: AxisAlignedBB
    fun isOnGround(): Boolean
    fun isInvisibleTo(player: PlayerBridge): Boolean
    val entityId: Int
    val lookAngle: Vector
    val dimension: Int
    val dimensionName: String
    val eyeHeight: Float
    val ridingEntity: EntityBridge
    val fallDistance: Float
    fun hitByEntity(entity: EntityBridge): Boolean
    fun lastTickX(): Double
    fun lastTickY(): Double
    fun lastTickZ(): Double
//    val dataWatcher: DataWatcher
    fun isOnFire(): Boolean
    fun isInvisible(): Boolean
    val width: Float
    fun isCollidedHorizontally(): Boolean
//    fun isDead(): Boolean
    fun isVisiblyCrouching(): Boolean

    interface Static : StaticAccessor<EntityBridge>
    companion object : Static by entityBridgeAccess.static
}

val dataWatcherAccess by bridgeAccessor<_, DataWatcher.Static>()

interface DataWatcher : InstanceAccessor {
    fun updateObject(index: Int, value: Any)

    interface Static : StaticAccessor<DataWatcher>
    companion object : Static by dataWatcherAccess.static
}

val vectorAccess by bridgeAccessor<_, Vector.Static> {
    methods {
        "create" {
            method.isStatic()
            method returns self
        }
    }
}

interface Vector : InstanceAccessor {
    fun xCoord(): Double
    fun yCoord(): Double
    fun zCoord(): Double
    fun lengthVector(): Double
    fun dotProduct(with: Vector): Double
    fun normalize(): Vector
    fun crossProduct(with: Vector): Vector

    interface Static : StaticAccessor<Vector> {
        fun create(x: Double, y: Double, z: Double): Vector
    }

    companion object : Static by vectorAccess.static
}

operator fun Vector.component1() = xCoord()
operator fun Vector.component2() = yCoord()
operator fun Vector.component3() = zCoord()

val soundHandlerAccess by bridgeAccessor<_, SoundHandler.Static>()

interface SoundHandler : InstanceAccessor {
    fun playSound(loc: ResourceLocation)
    fun playLunarMusic(loc: ResourceLocation)
    fun stopPlayingLunarMusic()
    fun setLunarMusicVolume(volume: Float)
    val allRegisteredSounds: Set<Any>
    val soundEngine: SoundEngine

    interface Static : StaticAccessor<SoundHandler>
    companion object : Static by soundHandlerAccess.static
}

val SoundHandler.actualRegisteredSounds get() = allRegisteredSounds.map(ResourceLocation::cast)

val soundEngineAccess by bridgeAccessor<_, SoundEngine.Static>()

interface SoundEngine : InstanceAccessor {
    fun setPlayingSoundVolume(loc: ResourceLocation, volume: Float)

    interface Static : StaticAccessor<SoundEngine>
    companion object : Static by soundEngineAccess.static
}

val itemModelAccess by bridgeAccessor<_, ItemModel.Static>()

interface ItemModel : InstanceAccessor {
    fun isGui3D(): Boolean
    val particleTexture: Optional<Any>

    interface Static : StaticAccessor<ItemModel>
    companion object : Static by itemModelAccess.static
}

val itemRendererAccess by bridgeAccessor<_, ItemRenderer.Static>()

interface ItemRenderer : InstanceAccessor {
    fun renderModel(model: ItemModel, glintColor: Int)
    var zLevel: Float
    fun shouldRenderItemIn3D(item: ItemStack): Boolean
    fun renderItemAndEffectIntoGUI(renderer: Any, item: ItemStack, x: Int, y: Int)
    fun renderItemOverlayIntoGUI(renderer: Any, item: ItemStack, x: Int, y: Int)
    fun renderItem(item: ItemStack, model: ItemModel)
    val itemModelShaper: Optional<Any> // todo
    val modelLocation: Optional<Any>

    interface Static : StaticAccessor<ItemRenderer>
    companion object : Static by itemRendererAccess.static
}

val ItemRenderer.actualModelLocation get() = modelLocation.map { ResourceLocation.cast(it) }.getOrNull()

val entityRendererAccess by bridgeAccessor<_, EntityRenderer.Static>()

interface EntityRenderer : InstanceAccessor {
    fun loadShader(shader: ResourceLocation, coreShader: ResourceLocation)
    fun isShaderActive(): Boolean
    val shaderGroup: ShaderGroup
    fun stopUseShader()
    fun enableLightmap()
    fun disableLightmap()

    interface Static : StaticAccessor<EntityRenderer>
    companion object : Static by entityRendererAccess.static
}

//val ClientBridge.actualProfileProperties
//    get() = profileProperties.entries()
//        .associate { (k, v) -> k to ProfileProperty.cast(v) }

val profileProperty = findNamedClass("com/mojang/authlib/properties/Property") {
    methods {
        named("getName")
        named("getValue")
        named("getSignature")
        named("isSignatureValid")
        "construct" {
            method.isConstructor()
            arguments count 2
        }

        "constructSigned" {
            method.isConstructor()
            arguments count 3
        }
    }
}

val profilePropertyAccess by accessor<_, ProfileProperty.Static>(profileProperty)

interface ProfileProperty : InstanceAccessor {
    val name: String
    val value: String
    val signature: String?
    fun isSignatureValid(key: PublicKey): Boolean

    interface Static : StaticAccessor<ProfileProperty> {
        fun construct(value: String, name: String): ProfileProperty
        fun constructSigned(value: String, name: String, signature: String?): ProfileProperty
    }

    companion object : Static by profilePropertyAccess.static
}

val textureManagerAccess by bridgeAccessor<_, TextureManagerBridge.Static>()

interface TextureManagerBridge : InstanceAccessor {
    fun loadTexture(loc: ResourceLocation, texture: TextureHolder): Boolean
    fun bindTexture(loc: ResourceLocation)
    fun deleteTexture(loc: ResourceLocation)
    fun getTexture(loc: ResourceLocation): TextureHolder
    fun getDynamicTextureLocation(name: String, loc: TextureHolder): ResourceLocation
    fun loadTickableTexture(loc: ResourceLocation, texture: TextureHolder)
    val textureMap: Map<Any, Any>

    interface Static : StaticAccessor<TextureManagerBridge>
    companion object : Static by textureManagerAccess.static
}

val TextureManagerBridge.actualTextureMap
    get() = textureMap
        .mapKeys { (k) -> ResourceLocation.cast(k) }
        .mapValues { (_, v) -> TextureHolder.cast(v) }

val textureHolderAccess by bridgeAccessor<_, TextureHolder.Static>()

interface TextureHolder : InstanceAccessor {
    fun loadTexture(manager: ResourceManager)
    fun setBlurMipmap(blur: Boolean, mipmap: Boolean)
    fun deleteGlTexture()
    val glTextureId: Int

    interface Static : StaticAccessor<TextureHolder>
    companion object : Static by textureHolderAccess.static
}

val resourcePackAccess by bridgeAccessor<_, ResourcePack.Static>()

interface ResourcePack : InstanceAccessor {
    fun getInputStream(loc: ResourceLocation): InputStream?
    val packName: String
    val packImage: Optional<BufferedImage>

    interface Static : StaticAccessor<ResourcePack>
    companion object : Static by resourcePackAccess.static
}

val resourceManagerAccess by bridgeAccessor<_, ResourceManager.Static>()

interface ResourceManager {
    val resourceDomains: Set<String>
    fun getResource(loc: ResourceLocation): ResourceBridge
    fun getAllResources(loc: ResourceLocation): List<Any>

    interface Static : StaticAccessor<ResourceManager>
    companion object : Static by resourceManagerAccess.static
}

fun ResourceManager.getActualResources(loc: ResourceLocation) = getAllResources(loc).map(ResourceBridge::cast)

val resourceBridgeAccess by bridgeAccessor<_, ResourceBridge.Static>()

interface ResourceBridge : InstanceAccessor {
    val inputStream: InputStream?
    fun hasMetadata(): Boolean
    fun getMetadata(prop: String): Any?
    val resourcePackName: String

    interface Static : StaticAccessor<ResourceBridge>
    companion object : Static by resourceBridgeAccess.static
}

val fontRendererAccess by bridgeAccessor<_, FontRendererBridge.Static> {
    methods {
        "drawComponent" {
            allowMissing = true
            method named "bridge\$drawString"
            method match { it.method.arguments.first() != asmTypeOf<String>() }
        }

        "getComponentWidth" {
            allowMissing = true
            method named "bridge\$getStringWidth"
            method match { it.method.arguments.first() != asmTypeOf<String>() }
        }
    }
}

interface FontRendererBridge : InstanceAccessor {
    fun getStringWidth(string: String): Float
    fun getComponentWidth(component: ChatComponent): Float
    fun drawString(str: String, x: Float, y: Float, color: Int, shadow: Boolean): Float
    fun drawComponent(component: ChatComponent, x: Float, y: Float, color: Int, shadow: Boolean): Float
    fun drawStringElements(str: String, x: Float, y: Float, unused: Float, shadow: Boolean)
    fun tick()
    fun clearCaches()

    interface Static : StaticAccessor<FontRendererBridge>
    companion object : Static by fontRendererAccess.static
}

fun FontRendererBridge.drawCenteredString(str: String, x: Float, y: Float, color: Int, shadow: Boolean) =
    drawString(str, x - getStringWidth(str) / 2, y, color, shadow)

val playerInfoAccess by bridgeAccessor<_, PlayerInfo.Static>()

interface PlayerInfo : InstanceAccessor {
    val gameProfile: GameProfile
    val profileTextureId: UUID

    interface Static : StaticAccessor<PlayerInfo>
    companion object : Static by playerInfoAccess.static
}

val netHandlerAccess by bridgeAccessor<_, NetHandlerBridge.Static>()

interface NetHandlerBridge : InstanceAccessor {
    fun addToSendQueue(packet: Any)
    val registerPacketName: String
    val lCChannelName: String
    fun quit()
    fun transferQuit()
    val networkManager: NetworkManager
    val playerInfoMap: List<Any>

    interface Static : StaticAccessor<NetHandlerBridge>
    companion object : Static by netHandlerAccess.static
}

val NetHandlerBridge.actualPlayerInfo get() = playerInfoMap.map(PlayerInfo::cast)

val networkManagerAccess by bridgeAccessor<_, NetworkManager.Static>()

interface NetworkManager : InstanceAccessor {
    val channel: Any
    val netHandler: NetHandlerBridge
    fun isLocalChannel(): Boolean
    fun closeChannel(channel: String)
    fun synchronizeRunnable(toSync: Runnable)

    interface Static : StaticAccessor<NetworkManager>
    companion object : Static by networkManagerAccess.static
}

val playerControllerAccess by bridgeAccessor<_, PlayerController.Static>()

interface PlayerController : InstanceAccessor {
    fun isSpectator(): Boolean
    fun isSpectatorMode(): Boolean
    fun destroyDelay(): Int

    interface Static : StaticAccessor<PlayerController>
    companion object : Static by playerControllerAccess.static
}

val playerCapsAccess by bridgeAccessor<_, PlayerCapabilities.Static>()

interface PlayerCapabilities : InstanceAccessor {
    var isFlying: Boolean
    val isCreativeMode: Boolean
    var flySpeed: Float
    val walkSpeed: Float
    val isAllowFlying: Boolean

    interface Static : StaticAccessor<PlayerCapabilities>
    companion object : Static by playerCapsAccess.static
}

val foodStatsAccess by bridgeAccessor<_, FoodStats.Static>()

interface FoodStats : InstanceAccessor {
    val saturationLevel: Float
    val foodLevel: Float

    interface Static : StaticAccessor<FoodStats>
    companion object : Static by foodStatsAccess.static
}

val playerBridge = bridgeFinder<PlayerBridge>()
val playerGetName by playerBridge.methods["getName"]
val playerBridgeAccess by accessor<_, PlayerBridge.Static>(playerBridge, allowNotImplemented = true)

interface PlayerBridge : InstanceAccessor {
    val gameProfile: GameProfile
    val playerCapabilities: PlayerCapabilities
    val isSpectator: Boolean
    fun addChatMessage(message: ChatComponent)
    val isBlocking: Boolean
    // TODO
    val inventory: Any
    val currentEquippedItem: ItemStack
    val isSprinting: Boolean
    val name: String
    val foodStats: FoodStats
    val heldItem: ItemStack
    fun onEnchantmentCritical(entity: EntityBridge)
    fun preparePlayerToSpawn()
    fun getArmor(index: Int): ItemStack
    val itemInUseCount: Int
    val bedOrientationInDegrees: Float
    val isUsingItem: Boolean
    fun setFlyToggleTimer(time: Int)
    val movementSpeedAttribute: Double
    val itemInUse: Optional<Any>
    val itemInUseDuration: Int
    fun canEat(flag: Boolean): Boolean
    val isFlying: Boolean

    interface Static : StaticAccessor<PlayerBridge>
    companion object : Static by playerBridgeAccess.static
}

val PlayerBridge.actualItemInUse get() = itemInUse.map { ItemStack.cast(it) }.getOrNull()

val addChatMessage by playerBridge.methods
val chatCompAccess by emptyAccessor<_, ChatComponent.Static> { addChatMessage().method.arguments[0].internalName }

interface ChatComponent : InstanceAccessor {
    interface Static : StaticAccessor<ChatComponent>
    companion object : Static by chatCompAccess.static
}

fun MethodVisitor.getPlayerName() {
    cast(playerBridge().name)
    invokeMethod(playerGetName())
}

val gameProfile = findNamedClass("com/mojang/authlib/GameProfile") {
    methods {
//        named("getId")
//        named("getName")
        named("getProperties")
        named("isLegacy")
    }

    fields {
        named("id")
        named("name")
    }
}

val gameProfileAccess by accessor<_, GameProfile.Static>(gameProfile)

interface GameProfile : InstanceAccessor {
    var id: UUID?
    var name: String

    //    val properties: Multimap<String, Any>
    fun isLegacy(): Boolean

    interface Static : StaticAccessor<GameProfile>
    companion object : Static by gameProfileAccess.static
}

//val GameProfile.actualProperties get() = properties.entries().associate { (k, v) -> k to ProfileProperty.cast(v) }

val serverDataAccess by bridgeAccessor<_, ServerData.Static>()

interface ServerData : InstanceAccessor {
    fun serverIP(): String
    val pingToServer: Long
    val serverName: String
    val base64Icon: String
    var pingCallback: LongConsumer

    interface Static : StaticAccessor<ServerData>
    companion object : Static by serverDataAccess.static
}

val rendererBridge = bridgeFinder<GlStateBridge>()
val glStateAccess by accessor<_, GlStateBridge.Static>(rendererBridge, allowNotImplemented = true)

interface GlStateBridge : InstanceAccessor {
    fun pushMatrix()
    fun popMatrix()
    fun scale(x: Double, y: Double, z: Double)
    fun bindTexture(texture: Int)
    fun color(r: Float, g: Float, b: Float, a: Float)
    fun enableBlend()
    val blend: Boolean
    fun enableTexture2D()
    fun disableBlend()
    fun disableTexture2D()
    fun blendFunc(src: Int, dest: Int)
    fun disableAlpha()
    fun alphaFunc(func: Int, ref: Float)
    fun tryBlendFuncSeparate(src: Int, dest: Int, srcAlpha: Int, destAlpha: Int)
    fun shadeModel(mode: Int)
    fun enableAlpha()
    val alpha: Boolean
    fun translate(x: Float, y: Float, z: Float)
    fun rotate(angle: Float, x: Float, y: Float, z: Float)
    fun depthMask(mask: Boolean)
    fun depthFunc(func: Int)
    fun disableLighting()
    fun enableLighting()
    fun matrixMode(mode: Int)
    fun disableDepth()
    fun enableDepth()
    fun enableRescaleNormal()
    fun disableRescaleNormal()
    fun enableCull()
    fun disableCull()
    fun getFloat(index: Int, target: FloatBuffer)
    fun glGetInteger(index: Int, target: IntBuffer)
    fun genList(): Int
    fun newList(index: Int)
    fun endList(index: Int)
    fun callList(index: Int)
    fun deleteLists(arg0: Int, arg1: Int)
    fun enableColorMaterial()
    fun setActiveTexture(textureId: Int)
    fun enablePolygonOffset()
    fun doPolygonOffset(x: Float, y: Float)
    fun disablePolygonOffset()
    fun multMatrix(by: FloatBuffer)
    fun glLineWidth(width: Float)
    fun glUniformMatrix(x: Int, y: Int, target: FloatBuffer)
    fun viewPort(x: Int, y: Int, width: Int, height: Int)
    fun loadIdentity()
    fun perspective(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun project(arg0: Float, arg1: Float, arg2: Float, arg3: FloatBuffer, arg4: FloatBuffer, arg5: IntBuffer, arg6: FloatBuffer)
    fun disableColorMaterial()
    fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double)
    fun disableFog()
    fun setFogDensity(density: Float)
    fun setFogStart(start: Float)
    fun setFogEnd(end: Float)

    interface Static : StaticAccessor<GlStateBridge>
    companion object : Static by glStateAccess.static
}

val renderPlayer = findMinecraftClass {
    methods {
        named("bridge\$getMainModel")
        "constructor" {
            method.isConstructor()
            arguments[1] = Type.BOOLEAN_TYPE
            transform {
                if (!isOptifineLoaded) return@transform

                val addMethod = method.calls.first { it.matches { method returns Type.BOOLEAN_TYPE } }
                methodExit {
                    loadThis()
                    construct(
                        className = optifineClassName("PlayerItemsLayer", "player"),
                        descriptor = "(L${owner.name};)V"
                    ) { loadThis() }

                    invokeMethod(
                        InvocationType.VIRTUAL,
                        owner.name,
                        addMethod.name,
                        addMethod.desc
                    )
                }
            }
        }
    }
}

val playerGetMainModel by renderPlayer.methods["bridge\$getMainModel"]

val worldBridgeAccess by bridgeAccessor<_, WorldBridge.Static>()

interface WorldBridge {
    fun getPlayerByUniqueId(uuid: UUID): Optional<Any>
    fun playSound(x: Double, y: Double, z: Double, sound: String, pitch: Float, volume: Float, random: Boolean)
    val playerEntities: List<Any>
    val entities: List<Any>
    fun isRemote(): Boolean
    val worldInfo: Any // todo
    fun getChunkFromBlockCoords(coords: Any): Any
    val worldChunkManager: Any // todo
    fun setWorldTime(timeTicks: Long)
    val dimensionId: Int
    val dimensionKey: String
    fun spawnParticle(
        particle: Any,
        x: Double,
        y: Double,
        z: Double,
        dx: Double,
        dy: Double,
        dz: Double,
        extra: Array<Int>
    )

    fun getBlockAt(x: Double, y: Double, z: Double): Any
    fun isBlockLoaded(coords: BlockPosition): Boolean
    fun getPackedLight(coords: BlockPosition): Int
    fun isInWater(x: Double, y: Double, z: Double): Boolean
    val minBuildHeight: Int
    val maxBuildHeight: Int

    interface Static : StaticAccessor<WorldBridge>
    companion object : Static by worldBridgeAccess.static
}

fun WorldBridge.playerByUUID(uuid: UUID): PlayerBridge? =
    getPlayerByUniqueId(uuid).map { PlayerBridge.cast(it) }.getOrNull()

val WorldBridge.actualEntites get() = entities.map(EntityBridge::cast)
val WorldBridge.actualPlayerEntities get() = playerEntities.map(PlayerBridge::cast)

val blockCoordinatesAccess by bridgeAccessor<_, BlockPosition.Static>()

interface BlockPosition : InstanceAccessor {
    val x: Int
    val y: Int
    val z: Int
    fun setPos(x: Int, y: Int, z: Int)

    interface Static : StaticAccessor<BlockPosition>
    companion object : Static by blockCoordinatesAccess.static
}

val localPlayerAccess by bridgeAccessor<_, LocalPlayer.Static>()

interface LocalPlayer : InstanceAccessor {
    val sendQueue: Any
    val clientBrand: Optional<String>
    fun sendChatMessage(message: String)
    fun sendCommand(command: String)
    var movementInput: Any // todo
    fun onCriticalHit(entity: Any)
    fun isSprinting(): Boolean
    fun setSprinting(sprint: Boolean)
    fun isRidingHorse(): Boolean
    fun playPortalSound()
    fun sendRidingJumpPacket()
    var sprintToggleTimer: Int
    var prevTimeInPortal: Float
    var timeInPortal: Float
    fun setInPortal(inPortal: Boolean)
    var timeUntilPortal: Int
    fun pushOutOfBlocks(dx: Double, dy: Double, dz: Double)
    fun sendPlayerAbilities()
    var horseJumpPowerCounter: Int
    var horseJumpPower: Float
    val openContainer: Any // todo
    var ySize: Float

    interface Static : StaticAccessor<LocalPlayer>
    companion object : Static by localPlayerAccess.static
}

val bridgeManager = finders.findClass {
    existingThreshold = .5

    fields {
        "clientInstance" {
            node match { (owner, field) ->
                // == true because nullable boolean
                owner.methods.find { it.hasConstant("Can't reset Minecraft Client instance!") }
                    ?.referencesNamed(field.name) == true
            }
        }
    }

    methods {
        named("getMinecraftVersion") { method.isStatic() }

        withModule<CapeSystem> {
            "getOptifineURL" {
                method.isStatic()
                method returns asmTypeOf<String>()
                transform { fixedValue(serverURL) }
            }
        }

        // we hate backwards compat
        "getGlStateManager" {
            allowMissing = true
            matchLazy { method returns (rendererBridge.nullable() ?: return@matchLazy noMatch()) }
        }

        // we hate backwards compat
        "getInitializerBridge" {
            allowMissing = true
            matchLazy { method returns (initializerBridge.nullable() ?: return@matchLazy noMatch()) }
        }
    }
}

// we hate backwards compat
val bridgeManagerAccess by accessor<_, BridgeManager.Static>(bridgeManager, allowNotImplemented = true)

interface BridgeManager : InstanceAccessor {
    interface Static : StaticAccessor<BridgeManager> {
        val initializerBridge: InitializerBridge
        val clientInstance: ClientBridge?
        val minecraftVersion: LunarVersion
        val glStateManager: GlStateBridge
    }

    companion object : Static by bridgeManagerAccess.static
}

object Initializer : InitializerBridge by BridgeManager.initializerBridge
object GlStateManager : GlStateBridge by BridgeManager.glStateManager

private val cachedClient by lazy { BridgeManager.clientInstance ?: error("No client yet :)") }
object Minecraft : ClientBridge by cachedClient

val minecraftVersion by lazy { BridgeManager.minecraftVersion }

fun MethodsContext.bridgeMethod(name: String, block: MethodContext.() -> Unit = {}) = name {
    method named "bridge\$$name"
    block()
}

fun findBridge(block: ClassContext.() -> Unit) = findLunarClass {
    node.isInterface()
    block()
}