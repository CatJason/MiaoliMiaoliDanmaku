package com.miaolimiaoli.danmaku

import android.graphics.Color
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random

class DanmakuRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private val danmakuList = mutableListOf<TextDanmaku>()
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // 添加全局速度控制变量
    private var globalSpeed: Float = 5.0f // 默认全局速度为1.0
    private var baseSpeed: Float = 100.0f // 基础速度值（像素/秒）

    // 视频总时长和当前播放时间（毫秒）
    private var videoDuration: Long = 60000 // 默认1分钟
    private var currentPosition: Long = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("DanmakuRenderer", "Surface created")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 初始化矩阵
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)

        // 生成100条随机弹幕
        val colors = listOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, Color.WHITE, Color.rgb(255, 165, 0) // 橙色
        )

        val texts = listOf(
            "Hello World!", "这是弹幕测试", "OpenGL ES 2.0",
            "弹幕来了", "666", "awsl", "哈哈哈",
            "Nice!", "太强了", "程序员永不秃头",
            "Android开发", "Kotlin赛高", "Java也不错",
            "Flutter跨平台", "React Native", "Unity3D",
            "游戏开发", "前端开发", "后端开发",
            "全栈工程师", "AI人工智能", "机器学习",
            "深度学习", "大数据", "云计算",
            "区块链", "元宇宙", "Web3.0",
            "加油!", "冲冲冲", "干就完了",
            "奥利给", "yyds", "绝绝子",
            "破防了", "蚌埠住了", "笑死",
            "针不戳", "好家伙", "绝了",
            "可以可以", "太秀了", "学到了",
            "感谢分享", "收藏了", "马克一下",
            "先码后看", "不明觉厉", "大佬带带我",
            "求源码", "求教程", "求分享",
            "感谢开源", "开源万岁", "自由软件",
            "GPL协议", "MIT协议", "Apache协议",
            "Copyleft", "Copyright", "专利保护",
            "用户体验", "交互设计", "UI设计",
            "产品经理", "需求文档", "原型图",
            "敏捷开发", "Scrum", "看板",
            "持续集成", "DevOps", "自动化测试",
            "单元测试", "集成测试", "压力测试",
            "性能优化", "内存泄漏", "ANR",
            "OOM", "Crash", "Bug",
            "Debug", "断点调试", "日志分析",
            "代码重构", "设计模式", "算法优化",
            "数据结构", "时间复杂度", "空间复杂度",
            "LeetCode", "Codeforces", "牛客网",
            "力扣", "刷题", "面试题",
            "面经", "内推", "HR",
            "薪资", "年终奖", "股票期权",
            "955", "996", "007",
            "福报", "加班", "调休",
            "年假", "病假", "产假",
            "五险一金", "社保", "公积金",
            "租房", "买房", "房贷",
            "车贷", "信用卡", "花呗",
            "白条", "网贷", "理财",
            "基金", "股票", "比特币",
            "以太坊", "狗狗币", "NFT",
            "元宇宙地产", "虚拟货币", "数字人民币"
        )

        // 生成100条随机弹幕
        repeat(100) {
            val randomText = texts.random()
            val randomColor = colors.random()
            val randomSize = 30f + Random.nextFloat() * 30f
            val randomY = 100f + Random.nextFloat() * 700f
            val randomSpeed = 0.3f + Random.nextFloat() * 1.5f
            val randomTime = Random.nextLong(videoDuration) // 随机出现在视频的某个时间点

            addDanmaku(randomText, randomSize, randomColor, randomY, randomSpeed, randomTime)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("DanmakuRenderer", "Surface changed: $width x $height")
        GLES20.glViewport(0, 0, width, height)

        // 设置正交投影矩阵
        val ratio = width.toFloat() / height
        Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)

        // 初始计算MVP矩阵
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 绘制所有弹幕
        synchronized(danmakuList) {
            val iterator = danmakuList.iterator()
            while (iterator.hasNext()) {
                val danmaku = iterator.next()

                // 更新弹幕进度
                if (videoDuration > 0) {
                    danmaku.progress = currentPosition.toFloat() / videoDuration.toFloat()
                }

                // 设置是否显示进度条（根据弹幕出现时间）
                danmaku.showProgressBar = currentPosition >= danmaku.appearTime

                // 更新弹幕位置
                danmaku.x -= danmaku.speed * globalSpeed * baseSpeed / 60f // 假设60fps

                // 如果弹幕完全离开屏幕，移除它
                if (danmaku.x + danmaku.width < 0) {
                    danmaku.release()
                    iterator.remove()
                } else {
                    danmaku.draw(mvpMatrix)
                }
            }
        }

        checkGLError("After draw")
    }

    /**
     * 添加弹幕
     * @param text 弹幕文本
     * @param textSize 文字大小
     * @param color 颜色
     * @param y Y轴位置
     * @param speed 相对速度（相对于全局速度）
     * @param appearTime 弹幕应该出现的时间（毫秒）
     */
    fun addDanmaku(text: String, textSize: Float, color: Int, y: Float, speed: Float = 1.0f, appearTime: Long = 0) {
        synchronized(danmakuList) {
            val danmaku = TextDanmaku(context, text, textSize, color)
            danmaku.x = 1080f // 从右侧进入
            danmaku.y = y
            danmaku.speed = speed // 存储相对速度
            danmaku.appearTime = appearTime // 设置弹幕出现时间
            danmaku.progressBarColor = color // 默认使用弹幕颜色作为进度条颜色
            danmakuList.add(danmaku)
        }
    }

    /**
     * 设置全局速度
     * @param speed 全局速度系数（0.0-2.0）
     */
    fun setGlobalSpeed(speed: Float) {
        globalSpeed = speed.coerceIn(0.0f, 2.0f)
    }

    /**
     * 设置基础速度（像素/秒）
     * @param speed 基础速度值
     */
    fun setBaseSpeed(speed: Float) {
        baseSpeed = speed.coerceAtLeast(0.0f)
    }

    /**
     * 设置视频总时长
     * @param duration 视频总时长（毫秒）
     */
    fun setVideoDuration(duration: Long) {
        videoDuration = duration.coerceAtLeast(1)
    }

    /**
     * 更新当前播放位置
     * @param position 当前播放位置（毫秒）
     */
    fun updateCurrentPosition(position: Long) {
        currentPosition = position.coerceIn(0, videoDuration)
    }

    fun clearDanmaku() {
        synchronized(danmakuList) {
            for (danmaku in danmakuList) {
                danmaku.release()
            }
            danmakuList.clear()
        }
    }

    private fun checkGLError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e("DanmakuRenderer", "$op: GL error ${getGLErrorString(error)}")
        }
    }

    private fun getGLErrorString(error: Int): String {
        return when (error) {
            GLES20.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GLES20.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GLES20.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GLES20.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            else -> "Unknown error ($error)"
        }
    }
}