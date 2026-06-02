package com.example.eye

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var analyzer: FrameAnalyzer

    private lateinit var guideText: TextView
    private lateinit var debugText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        analyzer = FrameAnalyzer(this)

        guideText = findViewById(R.id.guideText)
        debugText = findViewById(R.id.debugText)

        // 🔥 버튼 연결
        val btnVisible = findViewById<Button>(R.id.btnVisible)
        val btnCovered = findViewById<Button>(R.id.btnCovered)
        val btnClosed = findViewById<Button>(R.id.btnClosed)
        val btnBad = findViewById<Button>(R.id.btnBad)

        btnVisible.setOnClickListener {
            analyzer.currentLabel = "visible_eye"
        }

        btnCovered.setOnClickListener {
            analyzer.currentLabel = "covered_eye"
        }

        btnClosed.setOnClickListener {
            analyzer.currentLabel = "closed_eye"
        }

        btnBad.setOnClickListener {
            analyzer.currentLabel = "bad_roi"
        }
    }

    // 🔥 ROI 들어오는 부분 (네 기존 코드에서 연결해야 함)
    fun onEyeDetected(
        leftEye: Bitmap?,
        rightEye: Bitmap?
    ) {
        val result = analyzer.analyze(leftEye, rightEye)

        // ✅ 눈 가림 판단
        if (result.rightCovered && !result.leftCovered) {
            guideText.text = "오른쪽 눈 가림 감지"
        } else if (result.leftCovered && !result.rightCovered) {
            guideText.text = "왼쪽 눈 가림 감지"
        } else {
            guideText.text = "눈 상태 확인 중"
        }

        // ✅ 디버그
        debugText.visibility = View.VISIBLE
        debugText.text = result.debugText
    }
}