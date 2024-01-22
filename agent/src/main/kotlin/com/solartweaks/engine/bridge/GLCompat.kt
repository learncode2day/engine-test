@file:Suppress("unused")

package com.solartweaks.engine.bridge

import com.solartweaks.engine.*
import com.solartweaks.engine.util.asDescription
import com.solartweaks.engine.util.generateMethod
import com.solartweaks.engine.util.loadThis
import com.solartweaks.engine.util.returnMethod
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.lang.reflect.Method
import java.nio.*

// Useful for huge static class accessors like GL
inline fun <reified V : Any> overloadableExactAccessor(className: String): Lazy<V> {
    val implName = { m: Method -> m.name + m.asDescription().descriptor }

    val finder = findNamedClass(className) {
        methods {
            (V::class.java.declaredMethods).forEach {
                (implName(it)) {
                    method named it.name
                    arguments hasExact Type.getArgumentTypes(it.asDescription().descriptor).toList()
                }
            }
        }
    }

    return lazy {
        getAppClass(className.replace('/', '.'))
        require(finder.hasValue) { "Exact accessor for ${V::class} was not found, incompatible?" }

        internalGenerateAccessor(
            methods = finder.methods.contents,
            fields = emptyMap(),
            accessorName = "${V::class.simpleName}ExactAccessor",
            typeToImplement = V::class.java,
            constructorImpl = { _, _ ->
                // Create constructor
                generateMethod("<init>", "()V") {
                    // Call super() on Object
                    loadThis()
                    visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)

                    // End constructor
                    returnMethod()
                }
            },
            receiverLoader = { _, reflected -> require(!reflected) { "Exact accessor was reflected" } },
            implementationSelector = { methods, _, target, _ ->
                methods[implName(target)]?.nullable()?.let { MethodImplementation(target, it) }
                    ?: NotImplementedImplementation(target)
            },
            allowNotImplemented = true
        ).instance()
    }
}

fun initGLCompat() = Unit

private inline fun <reified V : Any> glAccessor(version: Int) =
    overloadableExactAccessor<V>("org.lwjgl.opengl.GL$version")

// CBA to map these, xd

val gl11Access by glAccessor<GL11>(11)

interface GL11 {
    fun glAccum(arg0: Int, arg1: Float)
    fun glAlphaFunc(arg0: Int, arg1: Float)
    fun glClearColor(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glClearAccum(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glClear(arg0: Int)
    fun glCallLists(arg0: ByteBuffer)
    fun glCallLists(arg0: IntBuffer)
    fun glCallLists(arg0: ShortBuffer)
    fun glCallList(arg0: Int)
    fun glBlendFunc(arg0: Int, arg1: Int)
    fun glBitmap(arg0: Int, arg1: Int, arg2: Float, arg3: Float, arg4: Float, arg5: Float, arg6: ByteBuffer)
    fun glBitmap(arg0: Int, arg1: Int, arg2: Float, arg3: Float, arg4: Float, arg5: Float, arg6: Long)
    fun glBindTexture(arg0: Int, arg1: Int)
    fun glPrioritizeTextures(arg0: IntBuffer, arg1: FloatBuffer)
    fun glAreTexturesResident(arg0: IntBuffer, arg1: ByteBuffer): Boolean
    fun glBegin(arg0: Int)
    fun glEnd()
    fun glArrayElement(arg0: Int)
    fun glClearDepth(arg0: Double)
    fun glDeleteLists(arg0: Int, arg1: Int)
    fun glDeleteTextures(arg0: IntBuffer)
    fun glDeleteTextures(arg0: Int)
    fun glCullFace(arg0: Int)
    fun glCopyTexSubImage2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: Int)
    fun glCopyTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int)
    fun glCopyTexImage2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: Int)
    fun glCopyTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int)
    fun glCopyPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glColorPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Long)
    fun glColorPointer(arg0: Int, arg1: Int, arg2: Int, arg3: ByteBuffer)
    fun glColorMaterial(arg0: Int, arg1: Int)
    fun glColorMask(arg0: Boolean, arg1: Boolean, arg2: Boolean, arg3: Boolean)
    fun glColor3b(arg0: Byte, arg1: Byte, arg2: Byte)
    fun glColor3f(arg0: Float, arg1: Float, arg2: Float)
    fun glColor3d(arg0: Double, arg1: Double, arg2: Double)
    fun glColor3ub(arg0: Byte, arg1: Byte, arg2: Byte)
    fun glColor4b(arg0: Byte, arg1: Byte, arg2: Byte, arg3: Byte)
    fun glColor4f(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glColor4d(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glColor4ub(arg0: Byte, arg1: Byte, arg2: Byte, arg3: Byte)
    fun glClipPlane(arg0: Int, arg1: DoubleBuffer)
    fun glClearStencil(arg0: Int)
    fun glEvalPoint1(arg0: Int)
    fun glEvalPoint2(arg0: Int, arg1: Int)
    fun glEvalMesh1(arg0: Int, arg1: Int, arg2: Int)
    fun glEvalMesh2(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glEvalCoord1f(arg0: Float)
    fun glEvalCoord1d(arg0: Double)
    fun glEvalCoord2f(arg0: Float, arg1: Float)
    fun glEvalCoord2d(arg0: Double, arg1: Double)
    fun glEnableClientState(arg0: Int)
    fun glDisableClientState(arg0: Int)
    fun glEnable(arg0: Int)
    fun glDisable(arg0: Int)
    fun glEdgeFlagPointer(arg0: Int, arg1: ByteBuffer)
    fun glEdgeFlagPointer(arg0: Int, arg1: Long)
    fun glEdgeFlag(arg0: Boolean)
    fun glDrawPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ByteBuffer)
    fun glDrawPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: IntBuffer)
    fun glDrawPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ShortBuffer)
    fun glDrawPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Long)
    fun glDrawElements(arg0: Int, arg1: ByteBuffer)
    fun glDrawElements(arg0: Int, arg1: IntBuffer)
    fun glDrawElements(arg0: Int, arg1: ShortBuffer)
    fun glDrawElements(arg0: Int, arg1: Int, arg2: Int, arg3: Long)
    fun glDrawBuffer(arg0: Int)
    fun glDrawArrays(arg0: Int, arg1: Int, arg2: Int)
    fun glDepthRange(arg0: Double, arg1: Double)
    fun glDepthMask(arg0: Boolean)
    fun glDepthFunc(arg0: Int)
    fun glFeedbackBuffer(arg0: Int, arg1: FloatBuffer)
    fun glGetPixelMapfv(arg0: Int, arg1: Long)
    fun glGetPixelMapuiv(arg0: Int, arg1: Long)
    fun glGetPixelMapusv(arg0: Int, arg1: Long)
    fun glGetError(): Int
    fun glGetClipPlane(arg0: Int, arg1: DoubleBuffer)
    fun glGetBoolean(arg0: Int): Boolean
    fun glGetDouble(arg0: Int): Double
    fun glGetFloat(arg0: Int): Float
    fun glGetInteger(arg0: Int): Int
    fun glGenTextures(arg0: IntBuffer)
    fun glGenTextures(): Int
    fun glGenLists(arg0: Int): Int
    fun glFrustum(arg0: Double, arg1: Double, arg2: Double, arg3: Double, arg4: Double, arg5: Double)
    fun glFrontFace(arg0: Int)
    fun glFogf(arg0: Int, arg1: Float)
    fun glFogi(arg0: Int, arg1: Int)
    fun glFlush()
    fun glFinish()
    fun glIsEnabled(arg0: Int): Boolean
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: ByteBuffer)
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: DoubleBuffer)
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: FloatBuffer)
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: IntBuffer)
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: ShortBuffer)
    fun glInterleavedArrays(arg0: Int, arg1: Int, arg2: Long)
    fun glInitNames()
    fun glHint(arg0: Int, arg1: Int)
    fun glGetTexParameterf(arg0: Int, arg1: Int): Float
    fun glGetTexParameteri(arg0: Int, arg1: Int): Int
    fun glGetTexLevelParameterf(arg0: Int, arg1: Int, arg2: Int): Float
    fun glGetTexLevelParameteri(arg0: Int, arg1: Int, arg2: Int): Int
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ByteBuffer)
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: DoubleBuffer)
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: FloatBuffer)
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: IntBuffer)
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ShortBuffer)
    fun glGetTexImage(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Long)
    fun glGetTexGeni(arg0: Int, arg1: Int): Int
    fun glGetTexGenf(arg0: Int, arg1: Int): Float
    fun glGetTexGend(arg0: Int, arg1: Int): Double
    fun glGetTexEnvi(arg0: Int, arg1: Int): Int
    fun glGetTexEnvf(arg0: Int, arg1: Int): Float
    fun glGetString(arg0: Int): String
    fun glGetPolygonStipple(arg0: ByteBuffer)
    fun glGetPolygonStipple(arg0: Long)
    fun glIsList(arg0: Int): Boolean
    fun glMaterialf(arg0: Int, arg1: Int, arg2: Float)
    fun glMateriali(arg0: Int, arg1: Int, arg2: Int)
    fun glMapGrid1f(arg0: Int, arg1: Float, arg2: Float)
    fun glMapGrid1d(arg0: Int, arg1: Double, arg2: Double)
    fun glMapGrid2f(arg0: Int, arg1: Float, arg2: Float, arg3: Int, arg4: Float, arg5: Float)
    fun glMapGrid2d(arg0: Int, arg1: Double, arg2: Double, arg3: Int, arg4: Double, arg5: Double)
    fun glMap2f(
        arg0: Int,
        arg1: Float,
        arg2: Float,
        arg3: Int,
        arg4: Int,
        arg5: Float,
        arg6: Float,
        arg7: Int,
        arg8: Int,
        arg9: FloatBuffer
    )

    fun glMap2d(
        arg0: Int,
        arg1: Double,
        arg2: Double,
        arg3: Int,
        arg4: Int,
        arg5: Double,
        arg6: Double,
        arg7: Int,
        arg8: Int,
        arg9: DoubleBuffer
    )

    fun glMap1f(arg0: Int, arg1: Float, arg2: Float, arg3: Int, arg4: Int, arg5: FloatBuffer)
    fun glMap1d(arg0: Int, arg1: Double, arg2: Double, arg3: Int, arg4: Int, arg5: DoubleBuffer)
    fun glLogicOp(arg0: Int)
    fun glLoadName(arg0: Int)
    fun glLoadIdentity()
    fun glListBase(arg0: Int)
    fun glLineWidth(arg0: Float)
    fun glLineStipple(arg0: Int, arg1: Short)
    fun glLightModelf(arg0: Int, arg1: Float)
    fun glLightModeli(arg0: Int, arg1: Int)
    fun glLightf(arg0: Int, arg1: Int, arg2: Float)
    fun glLighti(arg0: Int, arg1: Int, arg2: Int)
    fun glIsTexture(arg0: Int): Boolean
    fun glMatrixMode(arg0: Int)
    fun glPolygonStipple(arg0: ByteBuffer)
    fun glPolygonStipple(arg0: Long)
    fun glPolygonOffset(arg0: Float, arg1: Float)
    fun glPolygonMode(arg0: Int, arg1: Int)
    fun glPointSize(arg0: Float)
    fun glPixelZoom(arg0: Float, arg1: Float)
    fun glPixelTransferf(arg0: Int, arg1: Float)
    fun glPixelTransferi(arg0: Int, arg1: Int)
    fun glPixelStoref(arg0: Int, arg1: Float)
    fun glPixelStorei(arg0: Int, arg1: Int)
    fun glPixelMapfv(arg0: Int, arg1: Int, arg2: Long)
    fun glPixelMapuiv(arg0: Int, arg1: Int, arg2: Long)
    fun glPixelMapusv(arg0: Int, arg1: Int, arg2: Long)
    fun glPassThrough(arg0: Float)
    fun glOrtho(arg0: Double, arg1: Double, arg2: Double, arg3: Double, arg4: Double, arg5: Double)
    fun glNormalPointer(arg0: Int, arg1: Int, arg2: Long)
    fun glNormalPointer(arg0: Int, arg1: Int, arg2: ByteBuffer)
    fun glNormal3b(arg0: Byte, arg1: Byte, arg2: Byte)
    fun glNormal3f(arg0: Float, arg1: Float, arg2: Float)
    fun glNormal3d(arg0: Double, arg1: Double, arg2: Double)
    fun glNormal3i(arg0: Int, arg1: Int, arg2: Int)
    fun glNewList(arg0: Int, arg1: Int)
    fun glEndList()
    fun glShadeModel(arg0: Int)
    fun glSelectBuffer(arg0: IntBuffer)
    fun glScissor(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glScalef(arg0: Float, arg1: Float, arg2: Float)
    fun glScaled(arg0: Double, arg1: Double, arg2: Double)
    fun glRotatef(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glRotated(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glRenderMode(arg0: Int): Int
    fun glRectf(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glRectd(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glRecti(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glReadPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: ByteBuffer)
    fun glReadPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: FloatBuffer)
    fun glReadPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: IntBuffer)
    fun glReadPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: ShortBuffer)
    fun glReadPixels(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Long)
    fun glReadBuffer(arg0: Int)
    fun glRasterPos2f(arg0: Float, arg1: Float)
    fun glRasterPos2d(arg0: Double, arg1: Double)
    fun glRasterPos2i(arg0: Int, arg1: Int)
    fun glRasterPos3f(arg0: Float, arg1: Float, arg2: Float)
    fun glRasterPos3d(arg0: Double, arg1: Double, arg2: Double)
    fun glRasterPos3i(arg0: Int, arg1: Int, arg2: Int)
    fun glRasterPos4f(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glRasterPos4d(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glRasterPos4i(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glPushName(arg0: Int)
    fun glPopName()
    fun glPushMatrix()
    fun glPopMatrix()
    fun glPushClientAttrib(arg0: Int)
    fun glPopClientAttrib()
    fun glPushAttrib(arg0: Int)
    fun glPopAttrib()
    fun glStencilFunc(arg0: Int, arg1: Int, arg2: Int)
    fun glVertexPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Long)
    fun glVertexPointer(arg0: Int, arg1: Int, arg2: Int, arg3: ByteBuffer)
    fun glVertex2f(arg0: Float, arg1: Float)
    fun glVertex2d(arg0: Double, arg1: Double)
    fun glVertex2i(arg0: Int, arg1: Int)
    fun glVertex3f(arg0: Float, arg1: Float, arg2: Float)
    fun glVertex3d(arg0: Double, arg1: Double, arg2: Double)
    fun glVertex3i(arg0: Int, arg1: Int, arg2: Int)
    fun glVertex4f(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glVertex4d(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glVertex4i(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glTranslatef(arg0: Float, arg1: Float, arg2: Float)
    fun glTranslated(arg0: Double, arg1: Double, arg2: Double)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: ByteBuffer)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: DoubleBuffer)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: FloatBuffer)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: IntBuffer)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: ShortBuffer)
    fun glTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: Long)
    fun glTexImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: ByteBuffer
    )

    fun glTexImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: DoubleBuffer
    )

    fun glTexImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: FloatBuffer
    )

    fun glTexImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: IntBuffer
    )

    fun glTexImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: ShortBuffer
    )

    fun glTexImage2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: Int, arg8: Long)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: ByteBuffer)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: DoubleBuffer)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: FloatBuffer)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: IntBuffer)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: ShortBuffer)
    fun glTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Long)
    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: ByteBuffer
    )

    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: DoubleBuffer
    )

    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: FloatBuffer
    )

    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: IntBuffer
    )

    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: ShortBuffer
    )

    fun glTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Long
    )

    fun glTexParameterf(arg0: Int, arg1: Int, arg2: Float)
    fun glTexParameteri(arg0: Int, arg1: Int, arg2: Int)
    fun glTexGenf(arg0: Int, arg1: Int, arg2: Float)
    fun glTexGend(arg0: Int, arg1: Int, arg2: Double)
    fun glTexGeni(arg0: Int, arg1: Int, arg2: Int)
    fun glTexEnvf(arg0: Int, arg1: Int, arg2: Float)
    fun glTexEnvi(arg0: Int, arg1: Int, arg2: Int)
    fun glTexCoordPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Long)
    fun glTexCoordPointer(arg0: Int, arg1: Int, arg2: Int, arg3: ByteBuffer)
    fun glTexCoord1f(arg0: Float)
    fun glTexCoord1d(arg0: Double)
    fun glTexCoord2f(arg0: Float, arg1: Float)
    fun glTexCoord2d(arg0: Double, arg1: Double)
    fun glTexCoord3f(arg0: Float, arg1: Float, arg2: Float)
    fun glTexCoord3d(arg0: Double, arg1: Double, arg2: Double)
    fun glTexCoord4f(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glTexCoord4d(arg0: Double, arg1: Double, arg2: Double, arg3: Double)
    fun glStencilOp(arg0: Int, arg1: Int, arg2: Int)
    fun glStencilMask(arg0: Int)
    fun glViewport(arg0: Int, arg1: Int, arg2: Int, arg3: Int)

    companion object : GL11 by gl11Access
}

val gl13Access by glAccessor<GL13>(13)

interface GL13 {
    fun glActiveTexture(arg0: Int)
    fun glClientActiveTexture(arg0: Int)
    fun glCompressedTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: ByteBuffer)
    fun glCompressedTexImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Long)
    fun glCompressedTexImage2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: ByteBuffer)
    fun glCompressedTexImage2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Int, arg7: Long)
    fun glCompressedTexImage3D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: ByteBuffer
    )

    fun glCompressedTexImage3D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Long
    )

    fun glCompressedTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: ByteBuffer)
    fun glCompressedTexSubImage1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int, arg6: Long)
    fun glCompressedTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: ByteBuffer
    )

    fun glCompressedTexSubImage2D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Long
    )

    fun glCompressedTexSubImage3D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Int,
        arg9: ByteBuffer
    )

    fun glCompressedTexSubImage3D(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Int,
        arg9: Int,
        arg10: Long
    )

    fun glGetCompressedTexImage(arg0: Int, arg1: Int, arg2: ByteBuffer)
    fun glGetCompressedTexImage(arg0: Int, arg1: Int, arg2: Long)
    fun glMultiTexCoord1f(arg0: Int, arg1: Float)
    fun glMultiTexCoord1d(arg0: Int, arg1: Double)
    fun glMultiTexCoord2f(arg0: Int, arg1: Float, arg2: Float)
    fun glMultiTexCoord2d(arg0: Int, arg1: Double, arg2: Double)
    fun glMultiTexCoord3f(arg0: Int, arg1: Float, arg2: Float, arg3: Float)
    fun glMultiTexCoord3d(arg0: Int, arg1: Double, arg2: Double, arg3: Double)
    fun glMultiTexCoord4f(arg0: Int, arg1: Float, arg2: Float, arg3: Float, arg4: Float)
    fun glMultiTexCoord4d(arg0: Int, arg1: Double, arg2: Double, arg3: Double, arg4: Double)
    fun glSampleCoverage(arg0: Float, arg1: Boolean)

    companion object : GL13 by gl13Access
}

val gl14Access by glAccessor<GL14>(14)

interface GL14 {
    fun glBlendEquation(arg0: Int)
    fun glBlendColor(arg0: Float, arg1: Float, arg2: Float, arg3: Float)
    fun glFogCoordf(arg0: Float)
    fun glFogCoordd(arg0: Double)
    fun glFogCoordPointer(arg0: Int, arg1: Int, arg2: Long)
    fun glMultiDrawArrays(arg0: Int, arg1: IntBuffer, arg2: IntBuffer)
    fun glPointParameteri(arg0: Int, arg1: Int)
    fun glPointParameterf(arg0: Int, arg1: Float)
    fun glSecondaryColor3b(arg0: Byte, arg1: Byte, arg2: Byte)
    fun glSecondaryColor3f(arg0: Float, arg1: Float, arg2: Float)
    fun glSecondaryColor3d(arg0: Double, arg1: Double, arg2: Double)
    fun glSecondaryColor3ub(arg0: Byte, arg1: Byte, arg2: Byte)
    fun glSecondaryColorPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Long)
    fun glBlendFuncSeparate(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glWindowPos2f(arg0: Float, arg1: Float)
    fun glWindowPos2d(arg0: Double, arg1: Double)
    fun glWindowPos2i(arg0: Int, arg1: Int)
    fun glWindowPos3f(arg0: Float, arg1: Float, arg2: Float)
    fun glWindowPos3d(arg0: Double, arg1: Double, arg2: Double)
    fun glWindowPos3i(arg0: Int, arg1: Int, arg2: Int)

    companion object : GL14 by gl14Access
}

val gl15Access by glAccessor<GL15>(15)

interface GL15 {
    fun glBindBuffer(arg0: Int, arg1: Int)
    fun glDeleteBuffers(arg0: IntBuffer)
    fun glDeleteBuffers(arg0: Int)
    fun glGenBuffers(arg0: IntBuffer)
    fun glGenBuffers(): Int
    fun glIsBuffer(arg0: Int): Boolean
    fun glBufferData(arg0: Int, arg1: Long, arg2: Int)
    fun glBufferData(arg0: Int, arg1: ByteBuffer, arg2: Int)
    fun glBufferData(arg0: Int, arg1: DoubleBuffer, arg2: Int)
    fun glBufferData(arg0: Int, arg1: FloatBuffer, arg2: Int)
    fun glBufferData(arg0: Int, arg1: IntBuffer, arg2: Int)
    fun glBufferData(arg0: Int, arg1: ShortBuffer, arg2: Int)
    fun glBufferSubData(arg0: Int, arg1: Long, arg2: ByteBuffer)
    fun glBufferSubData(arg0: Int, arg1: Long, arg2: DoubleBuffer)
    fun glBufferSubData(arg0: Int, arg1: Long, arg2: FloatBuffer)
    fun glBufferSubData(arg0: Int, arg1: Long, arg2: IntBuffer)
    fun glBufferSubData(arg0: Int, arg1: Long, arg2: ShortBuffer)
    fun glGetBufferSubData(arg0: Int, arg1: Long, arg2: ByteBuffer)
    fun glGetBufferSubData(arg0: Int, arg1: Long, arg2: DoubleBuffer)
    fun glGetBufferSubData(arg0: Int, arg1: Long, arg2: FloatBuffer)
    fun glGetBufferSubData(arg0: Int, arg1: Long, arg2: IntBuffer)
    fun glGetBufferSubData(arg0: Int, arg1: Long, arg2: ShortBuffer)
    fun glMapBuffer(arg0: Int, arg1: Int, arg2: ByteBuffer): ByteBuffer
    fun glMapBuffer(arg0: Int, arg1: Int, arg2: Long, arg3: ByteBuffer): ByteBuffer
    fun glUnmapBuffer(arg0: Int): Boolean
    fun glGetBufferParameteri(arg0: Int, arg1: Int): Int
    fun glGenQueries(arg0: IntBuffer)
    fun glGenQueries(): Int
    fun glDeleteQueries(arg0: IntBuffer)
    fun glDeleteQueries(arg0: Int)
    fun glIsQuery(arg0: Int): Boolean
    fun glBeginQuery(arg0: Int, arg1: Int)
    fun glEndQuery(arg0: Int)
    fun glGetQueryi(arg0: Int, arg1: Int): Int
    fun glGetQueryObjecti(arg0: Int, arg1: Int): Int
    fun glGetQueryObjectui(arg0: Int, arg1: Int): Int

    companion object : GL15 by gl15Access
}

val gl20Access by glAccessor<GL20>(20)

interface GL20 {
    fun glShaderSource(arg0: Int, arg1: CharSequence)
    fun glShaderSource(arg0: Int, arg1: Array<CharSequence>)
    fun glCreateShader(arg0: Int): Int
    fun glIsShader(arg0: Int): Boolean
    fun glCompileShader(arg0: Int)
    fun glDeleteShader(arg0: Int)
    fun glCreateProgram(): Int
    fun glIsProgram(arg0: Int): Boolean
    fun glAttachShader(arg0: Int, arg1: Int)
    fun glDetachShader(arg0: Int, arg1: Int)
    fun glLinkProgram(arg0: Int)
    fun glUseProgram(arg0: Int)
    fun glValidateProgram(arg0: Int)
    fun glDeleteProgram(arg0: Int)
    fun glUniform1f(arg0: Int, arg1: Float)
    fun glUniform2f(arg0: Int, arg1: Float, arg2: Float)
    fun glUniform3f(arg0: Int, arg1: Float, arg2: Float, arg3: Float)
    fun glUniform4f(arg0: Int, arg1: Float, arg2: Float, arg3: Float, arg4: Float)
    fun glUniform1i(arg0: Int, arg1: Int)
    fun glUniform2i(arg0: Int, arg1: Int, arg2: Int)
    fun glUniform3i(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glUniform4i(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glGetShaderi(arg0: Int, arg1: Int): Int
    fun glGetProgrami(arg0: Int, arg1: Int): Int
    fun glGetShaderInfoLog(arg0: Int, arg1: IntBuffer, arg2: ByteBuffer)
    fun glGetShaderInfoLog(arg0: Int, arg1: Int): String
    fun glGetProgramInfoLog(arg0: Int, arg1: IntBuffer, arg2: ByteBuffer)
    fun glGetProgramInfoLog(arg0: Int, arg1: Int): String
    fun glGetAttachedShaders(arg0: Int, arg1: IntBuffer, arg2: IntBuffer)
    fun glGetUniformLocation(arg0: Int, arg1: ByteBuffer): Int
    fun glGetUniformLocation(arg0: Int, arg1: CharSequence): Int
    fun glGetActiveUniform(arg0: Int, arg1: Int, arg2: IntBuffer, arg3: IntBuffer, arg4: IntBuffer, arg5: ByteBuffer)
    fun glGetShaderSource(arg0: Int, arg1: IntBuffer, arg2: ByteBuffer)
    fun glGetShaderSource(arg0: Int, arg1: Int): String
    fun glVertexAttrib1s(arg0: Int, arg1: Short)
    fun glVertexAttrib1f(arg0: Int, arg1: Float)
    fun glVertexAttrib1d(arg0: Int, arg1: Double)
    fun glVertexAttrib2s(arg0: Int, arg1: Short, arg2: Short)
    fun glVertexAttrib2f(arg0: Int, arg1: Float, arg2: Float)
    fun glVertexAttrib2d(arg0: Int, arg1: Double, arg2: Double)
    fun glVertexAttrib3s(arg0: Int, arg1: Short, arg2: Short, arg3: Short)
    fun glVertexAttrib3f(arg0: Int, arg1: Float, arg2: Float, arg3: Float)
    fun glVertexAttrib3d(arg0: Int, arg1: Double, arg2: Double, arg3: Double)
    fun glVertexAttrib4s(arg0: Int, arg1: Short, arg2: Short, arg3: Short, arg4: Short)
    fun glVertexAttrib4f(arg0: Int, arg1: Float, arg2: Float, arg3: Float, arg4: Float)
    fun glVertexAttrib4d(arg0: Int, arg1: Double, arg2: Double, arg3: Double, arg4: Double)
    fun glVertexAttrib4Nub(arg0: Int, arg1: Byte, arg2: Byte, arg3: Byte, arg4: Byte)
    fun glVertexAttribPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Boolean, arg4: Int, arg5: Long)
    fun glVertexAttribPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Boolean, arg4: Int, arg5: ByteBuffer)
    fun glEnableVertexAttribArray(arg0: Int)
    fun glDisableVertexAttribArray(arg0: Int)
    fun glBindAttribLocation(arg0: Int, arg1: Int, arg2: ByteBuffer)
    fun glBindAttribLocation(arg0: Int, arg1: Int, arg2: CharSequence)
    fun glGetActiveAttrib(arg0: Int, arg1: Int, arg2: IntBuffer, arg3: IntBuffer, arg4: IntBuffer, arg5: ByteBuffer)
    fun glGetAttribLocation(arg0: Int, arg1: ByteBuffer): Int
    fun glGetAttribLocation(arg0: Int, arg1: CharSequence): Int
    fun glDrawBuffers(arg0: IntBuffer)
    fun glDrawBuffers(arg0: Int)
    fun glStencilOpSeparate(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glStencilFuncSeparate(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glStencilMaskSeparate(arg0: Int, arg1: Int)
    fun glBlendEquationSeparate(arg0: Int, arg1: Int)

    companion object : GL20 by gl20Access
}

val gl30Access by glAccessor<GL30>(30)

interface GL30 {
    fun glGetStringi(arg0: Int, arg1: Int): String
    fun glClearBufferfi(arg0: Int, arg1: Int, arg2: Float, arg3: Int)
    fun glVertexAttribI1i(arg0: Int, arg1: Int)
    fun glVertexAttribI2i(arg0: Int, arg1: Int, arg2: Int)
    fun glVertexAttribI3i(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glVertexAttribI4i(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glVertexAttribI1ui(arg0: Int, arg1: Int)
    fun glVertexAttribI2ui(arg0: Int, arg1: Int, arg2: Int)
    fun glVertexAttribI3ui(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glVertexAttribI4ui(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glVertexAttribIPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ByteBuffer)
    fun glVertexAttribIPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: IntBuffer)
    fun glVertexAttribIPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: ShortBuffer)
    fun glVertexAttribIPointer(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Long)
    fun glUniform1ui(arg0: Int, arg1: Int)
    fun glUniform2ui(arg0: Int, arg1: Int, arg2: Int)
    fun glUniform3ui(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glUniform4ui(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glBindFragDataLocation(arg0: Int, arg1: Int, arg2: ByteBuffer)
    fun glBindFragDataLocation(arg0: Int, arg1: Int, arg2: CharSequence)
    fun glGetFragDataLocation(arg0: Int, arg1: ByteBuffer): Int
    fun glGetFragDataLocation(arg0: Int, arg1: CharSequence): Int
    fun glBeginConditionalRender(arg0: Int, arg1: Int)
    fun glEndConditionalRender()
    fun glMapBufferRange(arg0: Int, arg1: Long, arg2: Long, arg3: Int, arg4: ByteBuffer): ByteBuffer
    fun glFlushMappedBufferRange(arg0: Int, arg1: Long, arg2: Long)
    fun glClampColor(arg0: Int, arg1: Int)
    fun glIsRenderbuffer(arg0: Int): Boolean
    fun glBindRenderbuffer(arg0: Int, arg1: Int)
    fun glDeleteRenderbuffers(arg0: IntBuffer)
    fun glDeleteRenderbuffers(arg0: Int)
    fun glGenRenderbuffers(arg0: IntBuffer)
    fun glGenRenderbuffers(): Int
    fun glRenderbufferStorage(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glGetRenderbufferParameteri(arg0: Int, arg1: Int): Int
    fun glIsFramebuffer(arg0: Int): Boolean
    fun glBindFramebuffer(arg0: Int, arg1: Int)
    fun glDeleteFramebuffers(arg0: IntBuffer)
    fun glDeleteFramebuffers(arg0: Int)
    fun glGenFramebuffers(arg0: IntBuffer)
    fun glGenFramebuffers(): Int
    fun glCheckFramebufferStatus(arg0: Int): Int
    fun glFramebufferTexture1D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glFramebufferTexture2D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glFramebufferTexture3D(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int, arg5: Int)
    fun glFramebufferRenderbuffer(arg0: Int, arg1: Int, arg2: Int, arg3: Int)
    fun glGetFramebufferAttachmentParameteri(arg0: Int, arg1: Int, arg2: Int): Int
    fun glGenerateMipmap(arg0: Int)
    fun glRenderbufferStorageMultisample(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glBlitFramebuffer(
        arg0: Int,
        arg1: Int,
        arg2: Int,
        arg3: Int,
        arg4: Int,
        arg5: Int,
        arg6: Int,
        arg7: Int,
        arg8: Int,
        arg9: Int
    )

    fun glTexParameterIi(arg0: Int, arg1: Int, arg2: Int)
    fun glTexParameterIui(arg0: Int, arg1: Int, arg2: Int)
    fun glGetTexParameterIi(arg0: Int, arg1: Int): Int
    fun glGetTexParameterIui(arg0: Int, arg1: Int): Int
    fun glFramebufferTextureLayer(arg0: Int, arg1: Int, arg2: Int, arg3: Int, arg4: Int)
    fun glColorMaski(arg0: Int, arg1: Boolean, arg2: Boolean, arg3: Boolean, arg4: Boolean)
    fun glEnablei(arg0: Int, arg1: Int)
    fun glDisablei(arg0: Int, arg1: Int)
    fun glIsEnabledi(arg0: Int, arg1: Int): Boolean
    fun glBindBufferRange(arg0: Int, arg1: Int, arg2: Int, arg3: Long, arg4: Long)
    fun glBindBufferBase(arg0: Int, arg1: Int, arg2: Int)
    fun glBeginTransformFeedback(arg0: Int)
    fun glEndTransformFeedback()
    fun glTransformFeedbackVaryings(arg0: Int, arg1: Array<CharSequence>, arg2: Int)
    fun glGetTransformFeedbackVarying(
        arg0: Int,
        arg1: Int,
        arg2: IntBuffer,
        arg3: IntBuffer,
        arg4: IntBuffer,
        arg5: ByteBuffer
    )

    fun glGetTransformFeedbackVarying(arg0: Int, arg1: Int, arg2: Int, arg3: IntBuffer, arg4: IntBuffer): String
    fun glBindVertexArray(arg0: Int)
    fun glDeleteVertexArrays(arg0: IntBuffer)
    fun glDeleteVertexArrays(arg0: Int)
    fun glGenVertexArrays(arg0: IntBuffer)
    fun glGenVertexArrays(): Int
    fun glIsVertexArray(arg0: Int): Boolean

    companion object : GL30 by gl30Access
}

object GLCompat : GL11 by gl11Access, GL13 by gl13Access, GL14 by gl14Access,
    GL15 by gl15Access, GL20 by gl20Access, GL30 by gl30Access