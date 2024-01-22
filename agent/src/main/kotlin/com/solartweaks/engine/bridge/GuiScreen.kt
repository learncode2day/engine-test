package com.solartweaks.engine.bridge

import com.solartweaks.engine.*
import com.solartweaks.engine.util.*
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import java.lang.reflect.Constructor

fun initGuiScreen() = Unit

// finder for the delegate implementation
val lunarCustomScreen = findBridge {
    methods {
        named("getWidth")
        // Apparently matching on getHeight breaks legacy support
//        named("getHeight")
        named("mouseClicked")
        named("mouseReleased")
        named("onGuiClosed")
        named("updateScreen")
        bridgeMethod("getScreenName")
        "draw" {
            arguments[1] = Type.INT_TYPE
            arguments[2] = Type.INT_TYPE
            arguments[3] = Type.FLOAT_TYPE
        }

        "resize" {
            arguments[1] = Type.INT_TYPE
            arguments[2] = Type.INT_TYPE
            arguments count 3
        }

        "onKey" { arguments[0] = Type.CHAR_TYPE }
        "unknown" {
            arguments[0] = Type.INT_TYPE
            arguments count 1
        }

        "unpressKeys" { method returns Type.BOOLEAN_TYPE }
    }
}

val lcCustomScreenAccess by accessor<_, LunarCustomScreen.Static>(lunarCustomScreen)

// accessor for lunar custom screens (wrapper type for type safety basically)
interface LunarCustomScreen : InstanceAccessor {
    interface Static : StaticAccessor<LunarCustomScreen>
    companion object : Static by lcCustomScreenAccess.static
}

// Delegate which allows you to create custom lc screens
val screenDelegate by lazy {
    val draw by lunarCustomScreen.methods
    val resize by lunarCustomScreen.methods
    val onKey by lunarCustomScreen.methods
    val unpressKeys by lunarCustomScreen.methods
    val unknown by lunarCustomScreen.methods

    generateDefaultClass(
        "LCScreenDelegate",
        implements = listOf(lunarCustomScreen().name),
        defaultConstructor = false,
        debug = true
    ) {
        fun MethodVisitor.screenDelegate() {
            loadThis()
            visitFieldInsn(
                GETFIELD,
                "$generatedPrefix/LCScreenDelegate",
                "delegate",
                "L${internalNameOf<CustomScreen>()};"
            )
        }

        visitField(ACC_PRIVATE or ACC_FINAL, "delegate", "L${internalNameOf<CustomScreen>()};", null, null)
        generateMethod("<init>", "(L${internalNameOf<CustomScreen>()};)V") {
            loadThis()
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

            loadThis()
            load(1)
            visitFieldInsn(
                PUTFIELD,
                "$generatedPrefix/LCScreenDelegate",
                "delegate",
                "L${internalNameOf<CustomScreen>()};"
            )

            returnMethod()
        }

        // appears to be unused but has to be implemented
        generateMethod("getWidth", "()I") {
            loadConstant(0)
            returnMethod(IRETURN)
        }

        // appears to be unused but has to be implemented
        generateMethod("getHeight", "()I") {
            loadConstant(0)
            returnMethod(IRETURN)
        }

        generateMethod(unpressKeys().method.name, "()Z") {
            screenDelegate()
            getProperty(CustomScreen::unpressKeys)
            returnMethod(IRETURN)
        }

        generateMethod("bridge\$getScreenName", "()Ljava/util/Optional;") {
            screenDelegate()
            getProperty(CustomScreen::screenName)
            visitMethodInsn(
                INVOKESTATIC,
                "java/util/Optional",
                "ofNullable",
                "(Ljava/lang/Object;)Ljava/util/Optional;",
                false
            )

            returnMethod(ARETURN)
        }

        generateMethod("mouseClicked", "(III)V") {
            screenDelegate()
            load(1, ILOAD)
            load(2, ILOAD)
            load(3, ILOAD)
            invokeMethod(CustomScreen::mouseClicked)
            returnMethod()
        }

        generateMethod("mouseReleased", "(III)V") {
            screenDelegate()
            load(1, ILOAD)
            load(2, ILOAD)
            load(3, ILOAD)
            invokeMethod(CustomScreen::mouseReleased)
            returnMethod()
        }

        generateMethod("onGuiClosed", "()V") {
            screenDelegate()
            invokeMethod(CustomScreen::onGuiClosed)
            returnMethod()
        }

        generateMethod("updateScreen", "()V") {
            screenDelegate()
            invokeMethod(CustomScreen::updateScreen)
            returnMethod()
        }

        generateMethod(draw().method.name, draw().method.desc) {
            screenDelegate()
            load(2, ILOAD)
            load(3, ILOAD)
            load(4, FLOAD)
            invokeMethod(CustomScreen::draw)
            returnMethod()
        }

        generateMethod(resize().method.name, resize().method.desc) {
            screenDelegate()
            load(2, ILOAD)
            load(3, ILOAD)
            invokeMethod(CustomScreen::updateSize)
            returnMethod()
        }

        generateMethod(onKey().method.name, onKey().method.desc) {
            screenDelegate()
            load(1, ILOAD)
            getCompanion<KeyType>()
            load(2)
            invokeMethod(KeyType.Static::cast)
            invokeMethod(CustomScreen::onKey)
            returnMethod()
        }

        generateMethod(unknown().method.name, "(I)V") { returnMethod() }
        generateMethod("initGui", "()V") {
            screenDelegate()
            @Suppress("DEPRECATION")
            invokeMethod(CustomScreen::initGui)
            returnMethod()
        }
    }
}

val screenDelegateCtor: Constructor<out Any> by lazy { screenDelegate.getConstructor(CustomScreen::class.java) }
fun LunarCustomScreen.Companion.create(impl: CustomScreen) =
    LunarCustomScreen.cast(screenDelegateCtor.newInstance(impl))

interface CustomScreen {
    val unpressKeys: Boolean get() = true
    val screenName: String? get() = null
    fun mouseClicked(x: Int, y: Int, button: Int) {}
    fun mouseReleased(x: Int, y: Int, button: Int) {}
    fun onGuiClosed() {}
    fun updateScreen() {}
    fun draw(mouseX: Int, mouseY: Int, delta: Float) {}
    fun updateSize(width: Int, height: Int) {}
    fun onKey(charTyped: Char, key: KeyType) {}

    // appears to be unused if you are not a custom screen, do not use
    @Deprecated("Appears to be unused in lunar code", replaceWith = ReplaceWith("this.resize()"))
    fun initGui() {
    }
}

fun CustomScreen.asScreenBridge() = Initializer.initCustomScreen(this)

class ElementExtension {
    var buttons = listOf<ButtonInfo>()
    var labels = listOf<LabelInfo>()

    fun draw(mouseX: Int, mouseY: Int) {
        buttons.forEach { it.draw(mouseX, mouseY) }
        labels.forEach { it.draw() }
    }

    private val buttonSound by lazy { ResourceLocation.create("gui.button.press") }

    fun mouseClicked(x: Int, y: Int) {
        val clickedButton = buttons.find { it.hovered(x.toFloat(), y.toFloat()) } ?: return
        clickedButton.handlers.forEach { it() }

        runCatching { Minecraft.soundHandler.playSound(buttonSound) }
            .onFailure { println("Was unable to play button click sound"); it.printStackTrace() }
    }

    inline fun setupButtons(block: MutableList<ButtonInfo>.() -> Unit) {
        buttons = buildList(block)
    }

    inline fun setupLabels(block: MutableList<LabelInfo>.() -> Unit) {
        labels = buildList(block)
    }
}

data class ButtonInfo(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float = 150f,
    val height: Float = 20f,
    val disabled: Boolean = false
) {
    val handlers = mutableListOf<() -> Unit>()
    fun handler(block: () -> Unit): ButtonInfo {
        handlers += block
        return this
    }
}

fun ButtonInfo.state(hovered: Boolean) = when {
    disabled -> 0
    hovered -> 2
    else -> 1
}

fun ButtonInfo.draw(hovered: Boolean) = DrawUtils.drawButton(text, x, y, width, height, state(hovered))
fun ButtonInfo.draw(mouseX: Int, mouseY: Int) = draw(hovered(mouseX.toFloat(), mouseY.toFloat()))
fun ButtonInfo.hovered(px: Float, py: Float) = px in x..x + width && py in y..y + height

data class LabelInfo(
    val text: String,
    val x: Float,
    val y: Float,
    val color: Int = 0xFFFFFF,
    val shadow: Boolean = true,
)

fun LabelInfo.draw() = FontRenderer.drawCenteredString(text, x, y, color, shadow)

object DrawUtils {
    fun drawRectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        r: Float = 1f,
        g: Float = 1f,
        b: Float = 1f,
        a: Float = 1f,
    ) = with(Initializer.initTessellator()) {
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        defaultBlendFunc()

        begin(GLConstants.GL_QUADS, hasTexture = false, hasColor = true)
        pos(x, y + height, 0f) // bl
        color(r, g, b, a)
        endVertex()

        pos(x + width, y + height, 0f) // br
        color(r, g, b, a)
        endVertex()

        pos(x + width, y, 0f) // tr
        color(r, g, b, a)
        endVertex()

        pos(x, y, 0f) // tl
        color(r, g, b, a)
        endVertex()
        end()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
    }

    fun drawTexturedRectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        u0: Float,
        u1: Float,
        v0: Float,
        v1: Float
    ) = with(Initializer.initTessellator()) {
        begin(GLConstants.GL_QUADS, hasTexture = true, hasColor = true)
        pos(x, y + height, 0f)
        uv(u0, v1)
        color(1f, 1f, 1f, 1f)
        endVertex()

        pos(x + width, y + height, 0f)
        uv(u1, v1)
        color(1f, 1f, 1f, 1f)
        endVertex()

        pos(x + width, y, 0f)
        uv(u1, v0)
        color(1f, 1f, 1f, 1f)
        endVertex()

        pos(x, y, 0f)
        uv(u0, v0)
        color(1f, 1f, 1f, 1f)
        endVertex()
        end()
    }

    private val widgetsLocation by lazy { ResourceLocation.create("textures/gui/widgets.png") }

    // 0 = disabled
    // 1 = enabled
    // 2 = hovered
    fun drawButton(text: String, x: Float, y: Float, width: Float, height: Float, state: Int = 1) {
        TextureManager.bindTexture(widgetsLocation)
        GlStateManager.enableBlend()
        defaultBlendFunc()

        val stateY = 46f + state * 20f
        val halfWidth = width / 2f
        drawTexturedRectangle(
            x = x,
            y = y,
            width = halfWidth,
            height = height,
            u0 = 0f,
            u1 = halfWidth / 256f,
            v0 = stateY / 256f,
            v1 = (stateY + height) / 256f
        )

        drawTexturedRectangle(
            x = x + halfWidth,
            y = y,
            width = halfWidth,
            height = height,
            u0 = (200f - halfWidth) / 256f,
            u1 = 200f / 256f,
            v0 = stateY / 256f,
            v1 = (stateY + height) / 256f
        )

        FontRenderer.drawCenteredString(
            str = text,
            x = x + halfWidth,
            y = y + (height - 8f) / 2f,
            color = when (state) {
                0 -> 0xA0A0A0
                1 -> 0xE0E0E0
                2 -> 0xFFFFA0
                else -> 0
            },
            shadow = true
        )
    }

    fun defaultBlendFunc() = GlStateManager.tryBlendFuncSeparate(
        src = GLConstants.GL_SRC_ALPHA,
        dest = GLConstants.GL_ONE_MINUS_SRC_ALPHA,
        srcAlpha = GLConstants.GL_ONE,
        destAlpha = GLConstants.GL_ZERO,
    )

    fun drawBackground(width: Float, height: Float, alpha: Float = .7f) =
        drawRectangle(0f, 0f, width, height, 0f, 0f, 0f, alpha)
}

abstract class ScreenFacade : CustomScreen {
    protected val elementExtension = ElementExtension()
    protected var width = 0
    protected var height = 0

    abstract fun setup()

    override fun updateSize(width: Int, height: Int) {
        this.width = width
        this.height = height
        setup()
    }

    override fun draw(mouseX: Int, mouseY: Int, delta: Float) {
        DrawUtils.drawBackground(width.toFloat(), height.toFloat())
        elementExtension.draw(mouseX, mouseY)
    }

    override fun mouseClicked(x: Int, y: Int, button: Int) {
        elementExtension.mouseClicked(x, y)
    }
}

fun displayCustomScreen(screen: CustomScreen) = runLater { Minecraft.displayScreen(screen.asScreenBridge()) }