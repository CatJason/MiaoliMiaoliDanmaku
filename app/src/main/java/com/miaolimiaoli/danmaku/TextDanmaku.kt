package com.miaolimiaoli.danmaku

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TextDanmaku(private val context: Context, private val text: String, private val textSize: Float, private val textColor: Int) {
    // 顶点坐标和纹理坐标
    private val vertices = floatArrayOf(
        // X, Y, Z, U, V
        -1f, -1f, 0f, 0f, 1f,
        1f, -1f, 0f, 1f, 1f,
        -1f, 1f, 0f, 0f, 0f,
        1f, 1f, 0f, 1f, 0f
    )

    private var vertexBuffer: FloatBuffer
    private var program: Int
    private var textureId: Int = -1
    var width: Float = 0f
    var height: Float = 0f

    // 位置和速度
    var x: Float = 0f
    var y: Float = 0f
    var speed: Float = 5f

    // 新增属性：进度条相关
    var progress: Float = 0f // 0.0-1.0
    var showProgressBar: Boolean = false
    var progressBarHeight: Float = 5f
    var progressBarColor: Int = Color.RED
    var appearTime: Long = 0L // 弹幕应该出现的时间（毫秒）

    // 着色器代码
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        attribute vec2 vTexCoord;
        varying vec2 texCoord;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
            texCoord = vTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform sampler2D uTexture;
        uniform float uProgress;
        uniform int uShowProgressBar;
        uniform vec4 uProgressBarColor;
        varying vec2 texCoord;
        void main() {
            vec4 color = texture2D(uTexture, texCoord);
            if (color.a < 0.1) discard;
            
            // 如果显示进度条且当前片段在进度条区域
            if (uShowProgressBar != 0 && texCoord.y > 0.95 && texCoord.x < uProgress) {
                gl_FragColor = uProgressBarColor;
            } else {
                gl_FragColor = color;
            }
        }
    """.trimIndent()

    init {
        // 初始化顶点缓冲区
        val bb = ByteBuffer.allocateDirect(vertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertices)
        vertexBuffer.position(0)

        // 创建纹理
        createTextTexture()

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // 创建OpenGL ES程序
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)

            // 检查链接错误
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val info = GLES20.glGetProgramInfoLog(it)
                GLES20.glDeleteProgram(it)
                throw RuntimeException("Could not link program: $info")
            }
        }
    }

    private fun createTextTexture() {
        // 创建Paint对象测量文本
        val paint = Paint().apply {
            color = textColor
            textSize = this@TextDanmaku.textSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // 测量文本宽度和高度
        width = paint.measureText(text)
        height = paint.descent() - paint.ascent()

        // 创建Bitmap
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 绘制文本到Bitmap
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawText(text, 0f, -paint.ascent(), paint)

        // 生成纹理
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]

        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 设置纹理参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 加载Bitmap到纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // 释放Bitmap
        bitmap.recycle()
    }

    fun draw(mvpMatrix: FloatArray) {
        // 更新位置
        x -= speed
        if (x < -width) {
            x = 1080f // 假设屏幕宽度为1080，从右侧重新进入
        }

        // 使用程序
        GLES20.glUseProgram(program)

        // 获取着色器变量位置
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "vTexCoord")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val progressHandle = GLES20.glGetUniformLocation(program, "uProgress")
        val showProgressBarHandle = GLES20.glGetUniformLocation(program, "uShowProgressBar")
        val progressBarColorHandle = GLES20.glGetUniformLocation(program, "uProgressBarColor")

        // 创建模型矩阵并应用平移
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, 0f)

        // 缩放模型矩阵以适应文本大小
        Matrix.scaleM(modelMatrix, 0, width / 2f, height / 2f, 1f)

        // 计算最终MVP矩阵
        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        // 传递矩阵
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0)

        // 传递进度条相关参数
        GLES20.glUniform1f(progressHandle, progress)
        GLES20.glUniform1i(showProgressBarHandle, if (showProgressBar) 1 else 0)

        // 传递进度条颜色
        val red = Color.red(progressBarColor) / 255f
        val green = Color.green(progressBarColor) / 255f
        val blue = Color.blue(progressBarColor) / 255f
        val alpha = Color.alpha(progressBarColor) / 255f
        GLES20.glUniform4f(progressBarColorHandle, red, green, blue, alpha)

        // 启用顶点属性
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        // 设置顶点坐标
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false,
            5 * 4, vertexBuffer
        )

        // 设置纹理坐标
        vertexBuffer.position(3)
        GLES20.glVertexAttribPointer(
            texCoordHandle, 2, GLES20.GL_FLOAT, false,
            5 * 4, vertexBuffer
        )
        vertexBuffer.position(0)

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureHandle, 0)

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val info = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compile error: $info")
            }
        }
    }

    fun release() {
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        GLES20.glDeleteProgram(program)
    }
}