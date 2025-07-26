package com.miaolimiaoli.danmaku

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer3D : GLSurfaceView.Renderer {

    private lateinit var line: Line
    private lateinit var cube: Cube
    private var lastTime: Long = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mVPMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景颜色
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)

        // 初始化线和立方体对象
        line = Line()
        cube = Cube()
        lastTime = System.currentTimeMillis()

        // 设置视图矩阵，拉远摄像机位置以容纳旋转的立方体
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -6f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲和深度缓冲
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // 检查OpenGL错误
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("Renderer", "OpenGL error after glClear: ${getGLErrorString(error)}")
            return
        }

        // 绘制线
        line.draw()

        // 检查OpenGL错误
        val lineError = GLES20.glGetError()
        if (lineError != GLES20.GL_NO_ERROR) {
            Log.e("Renderer", "OpenGL error after line.draw(): ${getGLErrorString(lineError)}")
            return
        }

        // 计算组合矩阵
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // 更新自转的立方体
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime) / 1000.0f
        lastTime = currentTime

        // 绘制立方体
        cube.draw(mVPMatrix, deltaTime)

        // 检查OpenGL错误
        val cubeError = GLES20.glGetError()
        if (cubeError != GLES20.GL_NO_ERROR) {
            Log.e("Renderer", "OpenGL error after cube.draw(): ${getGLErrorString(cubeError)}")
            return
        }

        // 所有绘制操作成功完成
        return
    }

    private fun getGLErrorString(error: Int): String {
        return when (error) {
            GLES20.GL_NO_ERROR -> "No error"
            GLES20.GL_INVALID_ENUM -> "Invalid enum"
            GLES20.GL_INVALID_VALUE -> "Invalid value"
            GLES20.GL_INVALID_OPERATION -> "Invalid operation"
            GLES20.GL_OUT_OF_MEMORY -> "Out of memory"
            else -> "Unknown error ($error)"
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()

        // 设置透视投影矩阵，增加视野角度以容纳旋转的立方体
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 10f)
    }

    companion object {
        fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            return shader
        }
    }
}
