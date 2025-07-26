package com.miaolimiaoli.danmaku

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import com.kuaishou.akdanmaku.ui.DanmakuView
import com.kuaishou.akdanmaku.render.SimpleRenderer
import kotlin.random.Random

class AKDanmakuActivity : AppCompatActivity() {

    private lateinit var danmakuPlayer: DanmakuPlayer
    private lateinit var danmakuView: DanmakuView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_danmaku)

        // 初始化弹幕播放器并使用自定义渲染器
        danmakuPlayer = DanmakuPlayer(SimpleRenderer())
        danmakuView = findViewById(R.id.danmakuView)
        danmakuPlayer.bindView(danmakuView)

        // 配置弹幕参数
        val config = DanmakuConfig(
            textSizeScale = 1.0f,
            visibility = true,
            bold = true,
            density = 100,
            allowOverlap = true
        )

        // 开始播放弹幕
        danmakuPlayer.start(config)

        // 批量更新数据（模拟从网络加载）
        val danmakuList = parseDanmakuJson()
        danmakuPlayer.updateData(danmakuList)

        // 模拟5秒后发送一条新弹幕
        danmakuView.postDelayed({
            sendNewDanmaku()
        }, 50)
    }

    private fun parseDanmakuJson(): List<DanmakuItemData> {
        return List(500) { index ->
            DanmakuItemData(
                danmakuId = Random.nextLong(), // 随机生成ID
                position = index * 1000L, // 弹幕出现时间
                content = "弹幕${index + 1} ${Random.nextInt(1000)}", // 内容
                mode = DanmakuItemData.DANMAKU_MODE_ROLLING, // 滚动模式
                textSize = when (Random.nextInt(3)) {
                    0 -> 14
                    1 -> 18
                    else -> 22
                },
                textColor = when (Random.nextInt(6)) {
                    0 -> 0xFFFF0000.toInt() // 红
                    1 -> 0xFF00FF00.toInt() // 绿
                    2 -> 0xFF0000FF.toInt() // 蓝
                    3 -> 0xFFFFFF00.toInt() // 黄
                    4 -> 0xFFFF00FF.toInt() // 粉
                    else -> 0xFF00FFFF.toInt() // 青
                },
                score = 0, // 默认分数
                danmakuStyle = if (Random.nextInt(10) == 0) {
                    DanmakuItemData.DANMAKU_STYLE_SELF_SEND
                } else {
                    DanmakuItemData.DANMAKU_STYLE_NONE
                },
                rank = 0, // 默认排名
                userId = Random.nextLong(), // 随机用户ID
                mergedType = DanmakuItemData.MERGED_TYPE_NORMAL // 默认合并类型
            )
        }
    }

    private fun sendNewDanmaku() {
        // 单个添加新弹幕（模拟用户发送）
        val newDanmaku = DanmakuItemData(
            danmakuId = Random.nextLong(),
            position = System.currentTimeMillis(),
            content = "用户新发送的弹幕 ${Random.nextInt(1000)}",
            mode = DanmakuItemData.DANMAKU_MODE_ROLLING,
            textSize = 18,
            textColor = 0xFFFFFFFF.toInt(),
            score = 0,
            danmakuStyle = DanmakuItemData.DANMAKU_STYLE_SELF_SEND,
            rank = 0,
            userId = 123456789L, // 当前用户ID
            mergedType = DanmakuItemData.MERGED_TYPE_NORMAL
        )
        danmakuPlayer.send(newDanmaku)
    }

    override fun onDestroy() {
        super.onDestroy()
        danmakuPlayer.release()
    }
}