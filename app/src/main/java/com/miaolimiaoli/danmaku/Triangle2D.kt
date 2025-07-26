package com.miaolimiaoli.danmaku

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle2D {
    // 顶点坐标
    private val triangleCoords = floatArrayOf(
        0.0f,  0.5f, 0.0f,  // 顶部
        -0.5f, -0.5f, 0.0f, // 左下
        0.5f, -0.5f, 0.0f   // 右下
    )

    // 颜色（红色）
    private val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)

    // 顶点着色器代码
    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uRotationMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * uRotationMatrix * vPosition;
        }
    """.trimIndent()

    // 片段着色器代码
    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """.trimIndent()

    private var vertexBuffer: FloatBuffer
    private val program: Int

    // 旋转控制
    private var rotationAngle = 0f
    private val rotationSpeed = 2.0f // 旋转速度（度/帧）
    private val rotationMatrix = FloatArray(16)

    init {
        // 初始化顶点缓冲区
        val bb = ByteBuffer.allocateDirect(triangleCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleCoords)
        vertexBuffer.position(0)

        // 初始化旋转矩阵
        Matrix.setIdentityM(rotationMatrix, 0)

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

    fun draw(mvpMatrix: FloatArray) {
        // 更新旋转角度（顺时针）
        rotationAngle += rotationSpeed
        if (rotationAngle >= 360f) rotationAngle -= 360f

        // 计算旋转矩阵（绕Z轴顺时针旋转）
        Matrix.setRotateM(rotationMatrix, 0, rotationAngle, 0f, 0f, 1f)

        // 使用程序
        GLES20.glUseProgram(program)

        // 获取并设置统一变量
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val rotationMatrixHandle = GLES20.glGetUniformLocation(program, "uRotationMatrix")
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")

        // 传递矩阵和颜色
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(rotationMatrixHandle, 1, false, rotationMatrix, 0)
        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        // 启用顶点属性
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle, 3, GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )

        // 绘制三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)

        // 禁用顶点属性
        GLES20.glDisableVertexAttribArray(positionHandle)
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
}