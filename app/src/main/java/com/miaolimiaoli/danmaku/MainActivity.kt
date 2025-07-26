package com.miaolimiaoli.danmaku

import android.opengl.GLSurfaceView
import android.opengl.EGL14
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.miaolimiaoli.danmaku.databinding.ActivityMainBinding
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var topGlSurfaceView: GLSurfaceView
    private lateinit var bottomGlSurfaceView: GLSurfaceView

    // 共享的 EGLContext
    private var sharedContext: EGLContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化共享上下文（第一个 GLSurfaceView 创建时生成）
        sharedContext = null

        // 第一个 GLSurfaceView（Top）
        topGlSurfaceView = GLSurfaceView(this).apply {
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setEGLContextClientVersion(2)
            setEGLContextFactory(SharedContextFactory()) // 设置共享上下文
            setRenderer(MyGLRenderer3D())
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            preserveEGLContextOnPause = true
        }
        binding.glContainerTop.addView(topGlSurfaceView)

        // 第二个 GLSurfaceView（Bottom），共享同一个上下文
        bottomGlSurfaceView = GLSurfaceView(this).apply {
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            setEGLContextClientVersion(2)
            setEGLContextFactory(SharedContextFactory()) // 设置共享上下文
            setRenderer(DanmakuRenderer(this@MainActivity))
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            preserveEGLContextOnPause = true
        }
        binding.glContainerBottom.addView(bottomGlSurfaceView)
    }

    override fun onResume() {
        super.onResume()
        topGlSurfaceView.onResume()
        bottomGlSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        topGlSurfaceView.onPause()
        bottomGlSurfaceView.onPause()
    }

    // 自定义 EGLContextFactory，用于共享上下文
    private inner class SharedContextFactory : GLSurfaceView.EGLContextFactory {

        override fun createContext(
            egL10: EGL10?,
            eglDisplay: EGLDisplay?,
            eglConfig: EGLConfig?
        ): EGLContext? {
            val attribList = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, // OpenGL ES 2.0
                EGL10.EGL_NONE
            )

            // 如果 sharedContext 为空，则创建新的上下文
            // 否则，使用 sharedContext 作为共享上下文
            val context = if (sharedContext == null) {
                egL10?.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attribList).also {
                    sharedContext = it // 保存共享上下文
                }
            } else {
                egL10?.eglCreateContext(eglDisplay, eglConfig, sharedContext, attribList)
            }

            if (context == EGL10.EGL_NO_CONTEXT) {
                throw RuntimeException("Failed to create EGLContext")
            }
            return context
        }

        override fun destroyContext(
            egl10: EGL10?,
            eglDisplay: EGLDisplay?,
            eglContext: EGLContext?
        ) {
            if (eglContext != sharedContext) {
                egl10?.eglDestroyContext(eglDisplay, eglContext)
            }
            // 不销毁 sharedContext，直到 Activity 销毁
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}