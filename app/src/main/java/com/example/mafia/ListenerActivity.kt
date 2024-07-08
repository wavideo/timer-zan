package com.example.mafia

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import java.io.IOException
import android.widget.Toast
import kotlinx.coroutines.MainScope
import java.util.Date


class ListenerActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")

// [ onCreate 바깥 : 선언부 ]

    // <선언> 터치 업/다운으로 타이머 조절을 위해
    private lateinit var timerCircle: ProgressBar // 둘레 -> 터치 업/다운으로 타이머 조절
    private var period: Int = 5 // 타이머 반복주기

    // <선언> 터치로 녹음하기 위해
    private lateinit var anywhere_touch: View // 전체 영역 -> 터치 감지
    private lateinit var durationCircle: ImageView // 원 -> 녹음 시간에 따라 점점 커짐

    // <핸들러 + 러너블> 터치 녹음용
    // 핸들러.postDelayed의 delay마다, 러너블의 run()을 지속적으로 반복해
    // run() 속에 postDelayed (this, 000) 으로 자체 반복하도록 했습니다
    private var handler: Handler = Handler(Looper.getMainLooper())
    private var downRun: Runnable = object : Runnable {
        override fun run() {
            if (durationCircle.scaleX < 0.2) {
                durationCircle.scaleX = durationCircle.scaleX + 0.1f
                durationCircle.scaleY = durationCircle.scaleY + 0.1f
                handler.postDelayed(this, 10) // 원이 작을 땐, 10밀리초 마다 0.1%씩 커져라
            } else if (durationCircle.scaleX < 0.3) {
                durationCircle.scaleX = durationCircle.scaleX + 0.02f
                durationCircle.scaleY = durationCircle.scaleY + 0.02f
                handler.postDelayed(this, 10)
            } else if (durationCircle.scaleX < 0.9) { // 90% 되면 스탑 !
                durationCircle.scaleX = durationCircle.scaleX + 0.002f
                durationCircle.scaleY = durationCircle.scaleY + 0.002f
                handler.postDelayed(this, 10) // 원이 클 땐, 10밀리초 마다 0.002%씩 커져라
            }
        }
    } // <핸들러 + 러너블> 끝


    // [ onCreate ]
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_listener)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.listener)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // <레이아웃 연결>
        anywhere_touch = findViewById<ConstraintLayout>(R.id.listener) // 전체화면 터치영역
        durationCircle = findViewById<ImageView>(R.id.iv_circle_duration) // 원 - 녹음
        timerCircle = findViewById<ProgressBar>(R.id.pgb_timer) // 둘레 - 타이머

        // <setText> 터치 이전
        var tvListen = findViewById<TextView>(R.id.tv_listen)
        period = intent.getIntExtra("period", 5)
        timerCircle.progress = period * 10
        tvListen.setText("${period.toInt()}분 뒤의 나에게\n잔소리를 남겨주세요!")

        // <선언> 터치 Y좌표 변화 계산용
        var beforeY = 0f
        var afterY = 0f

        // <터치 리스너> "닿는다 / 움직인다 / 뗀다" 가 느껴지면 -> 아래 구문을 수행
        anywhere_touch.setOnTouchListener { v, event ->

            // <intent> 원이 90% 이상 커지면 녹음 중단 ->  Timer 액티비티로 전환
            val ListenerToTimerIntent: Intent = Intent(this, TimerActivity::class.java)
            if (durationCircle.scaleX > 0.9) {
                ListenerToTimerIntent.putExtra("period", period) // 타이머 주기
                ListenerToTimerIntent.putExtra("scaleX", durationCircle.scaleX) // 동그라미 크기
                startActivity(ListenerToTimerIntent)
            }

            // <터치 리스너 when> event.action == MotionEvent.ACTION_??? 이 감지되면 . . .
            when (event.action) {

                // <터치 리스너 DOWN> 닿으면
                MotionEvent.ACTION_DOWN -> {
                    tvListen.setText("듣고 있어요!\n") // <setText> 터치 하는동안
                    handler.post(downRun) // 원이 점점 커진다. handler로 "downRun" 러너블을 지속 실행
                    startRec()
                    beforeY = event.y // 현재 y좌표 기록
                    true
                }

                // <터치 리스너 MOVE> 움직이면
                MotionEvent.ACTION_MOVE -> {

                    // <direction> 방향에 따라 up down
                    afterY = event.y // 움직인 후의 y좌표 기록
                    var difY = beforeY - afterY // 차이 값 ... 양수면 위로, 음수면 아래로 움직임
                    var direction = if (difY >= 0) {
                        "up"
                    } else {
                        "down"
                    }

                    // <press> 속도에 따라 압력 조절
                    var press: Int = 0
                    if (timerCircle.isVisible == false && Math.abs(difY) > 12) {
                        timerCircle.isVisible = true // 최소 10은 움직여야 -> 타이머 조절 시작
                        press = 0
                    }
                    // 최소 10은 움직여야 5씩 증가, 5분 미만일 땐 느리게 . . .
                    if (Math.abs(difY) > 10) {
                        press = if (timerCircle.progress <= 50) {
                            2
                        } else if (Math.abs(difY) > 20) {
                            10
                        } else {
                            5
                        }
                    }

                    // up, down에 따라 press 만큼 progressBar 증감
                    when (direction) {
                        "up" -> {
                            timerCircle.progress = timerCircle.progress + press
                            beforeY = event.y
                        }

                        "down" -> {
                            timerCircle.progress = timerCircle.progress - press
                            beforeY = event.y // 현재 y 좌표 기록
                        }
                    }

                    // progressBar는 타이머 10배 비율입니다
                    period = timerCircle.progress.toInt() / 10

                    // <setText> 5분 단위로
                    if (timerCircle.isVisible == false) {
                        tvListen.setText("듣고 있어요!\n")
                    } else if (period > 5) {
                        period = period / 5 * 5
                        tvListen.setText("잔소리를 ${period}분 마다\n들려드릴게요!")
                        // <setText> 5분 이하는 1분 단위로
                    } else if (period > 0) {
                        tvListen.setText("잔소리를 ${period}분 마다\n들려드릴게요!")
                    } else {
                        tvListen.setText("잔소리를 30초 마다\n들려드릴게요!")
                    }
                    true

                } // <터치 리스너 MOVE> 끝

                // <터치 리스너 UP > 떼면
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(downRun) // 러너블 remove. 동그라미 커지는 것 중단
                    stopRecording()

                    ListenerToTimerIntent.putExtra("period", period) // 타이머 주기
                    ListenerToTimerIntent.putExtra("scaleX", durationCircle.scaleX) // 동그라미 크기
                    // <intent> 손을 떼면 녹음 중단 ->  Timer 액티비티로 전환
                    val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
                    // options.toBundle() 로 애니메이션 제거
                    startActivity(ListenerToTimerIntent, options.toBundle())
                    true
                }

                else -> false
            } // <터치 리스너 when> 종료
        } // <터치 리스너> 종료
    } // <onCreate> 종료

    fun startRec() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Permission is not granted
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
        } else {
            startRecording()
        }
    }


    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false

    private fun startRecording(){
        //config and create MediaRecorder Object
        val fileName: String = Date().getTime().toString() + ".mp3"
        output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + "tazan.mp3" //내장메모리 밑에 위치
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(this, "레코딩 시작되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
            Toast.makeText(this, "중지 되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "레코딩 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }


} // <Class> 종료