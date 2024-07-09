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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import java.io.IOException
import java.util.Date
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class ListenerActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    private var handler: Handler = Handler(Looper.getMainLooper())  // 러너블 사용을 위해 핸들러 소환

////////////////////////////////////////////////////////////////
// 권한 획득 쉽게, 한번에 !
////////////////////////////////////////////////////////////////
    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach { (permission, isGranted) ->
            when {
                isGranted -> {
                    // 권한이 승인된 경우 처리할 작업
                }
                !isGranted -> {
                    // 권한이 거부된 경우 처리할 작업
                }
                else -> {
                    // 사용자가 "다시 묻지 않음"을 선택한 경우 처리할 작업
                }
            }
        }
        // multiple permission 처리에 대한 선택적 작업
        // - 모두 허용되었을 경우에 대한 code
        // - 허용되지 않은 Permission에 대한 재요청 code
    }

    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

////////////////////////////////////////////////////////////////
// onCreate 바깥 : 선언부
////////////////////////////////////////////////////////////////

    // <선언부>
    // 터치업 다운 :: 타이머주기 + 둘레에 반영
    private var period: Int = 5 // 타이머 반복주기
    private lateinit var timerCircle: ProgressBar // 둘레 -> 터치 업/다운으로 타이머 조절

    // 전체영역을 터치 :: 녹음 시작 + 원이 커진다
    private lateinit var anywhere_touch: View // 전체 영역 -> 터치 감지
    private lateinit var durationCircle: ImageView // 원 -> 녹음 시간에 따라 점점 커짐

    // <러너블> 누르는 동안 원이 점점 커진다
    // run() 에 postDelayed (this, 000) 를 붙여서 딜레이 주기마다 코드를 반복합니다
    private var scaleUpLoopRun: Runnable = object : Runnable {
        override fun run() {
            if (durationCircle.scaleX < 0.2) {
                durationCircle.scaleX = durationCircle.scaleX + 0.1f
                durationCircle.scaleY = durationCircle.scaleY + 0.1f
                handler.postDelayed(this, 10) // 원이 작을 땐, 10밀리초 마다 0.1%씩 커져라
            } else if (durationCircle.scaleX < 0.3) {
                durationCircle.scaleX = durationCircle.scaleX + 0.02f
                durationCircle.scaleY = durationCircle.scaleY + 0.02f
                handler.postDelayed(this, 10)
            } else if (durationCircle.scaleX < 0.9) { // 90%가 되면 스탑 !
                durationCircle.scaleX = durationCircle.scaleX + 0.002f
                durationCircle.scaleY = durationCircle.scaleY + 0.002f
                handler.postDelayed(this, 10) // 원이 클 땐, 10밀리초 마다 0.002%씩 커져라
            }
        }
    } // <핸들러 + 러너블> 끝



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

////////////////////////////////////////////////////////////////
// onCreate 시작
////////////////////////////////////////////////////////////////

        // <권한 획득>
        multiplePermissionsLauncher.launch(permissions)

        // <레이아웃 연결>
        anywhere_touch = findViewById<ConstraintLayout>(R.id.listener) // 전체화면 터치영역
        durationCircle = findViewById<ImageView>(R.id.iv_circle_duration) // 원 - 녹음
        timerCircle = findViewById<ProgressBar>(R.id.pgb_timer) // 둘레 - 타이머

        // <터치 전, 타이머 반복주기 기본값 세팅>
        period = intent.getIntExtra("period", 5)
        timerCircle.progress = period * 10 // 타이머 반복주기 Extra로 받아와서 둘레에 대입
        // 타이머 반복주기 setText
        var tvListen = findViewById<TextView>(R.id.tv_listen)
        if (period != 0) {
            tvListen.setText("${period.toInt()}분 뒤의 나에게\n잔소리를 남겨주세요!")
        } else {
            tvListen.setText("30초 뒤의 나에게\n잔소리를 남겨주세요!")
        }

        // <선언> 터치 Y좌표 변화 계산용
        var beforeY = 0f
        var afterY = 0f
        var timeStarted = 0L
        var timeNow = 0L



////////////////////////////////////////////////////////////////
// 터치 리스너 : 닿으면
////////////////////////////////////////////////////////////////

        // <터치 리스너> "Down 닿는다 / Move 움직인다 / Up 뗀다" 가 느껴지면 -> 아래 구문을 수행
        anywhere_touch.setOnTouchListener { v, event ->

            // <인텐트> 원이 90% 이상 커지면 녹음 중단 ->  Timer 액티비티로 전환
            val ListenerToTimerIntent: Intent = Intent(this, TimerActivity::class.java)
            if (durationCircle.scaleX > 0.9) {
                handler.postDelayed( {stopRecording()}, 500) // 1초 뒤 녹음 종료

                ListenerToTimerIntent.putExtra("period", period)
                ListenerToTimerIntent.putExtra("scaleX", durationCircle.scaleX)
                // 타이머 반복주기, 원의 크기를 Extra로 담아
                val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
                // makeCustomAnimation으로 애니메이션 제거
                handler.postDelayed( {startActivity(ListenerToTimerIntent, options.toBundle()) }, 1000) // 액티비티 전환
            }

            // <터치 리스너 when>
            when (event.action) {
                // <터치 리스너 DOWN> 손이 닿으면
                MotionEvent.ACTION_DOWN -> {

                    tvListen.setText("듣고 있어요!\n")
                    handler.post(scaleUpLoopRun) // 원이 점점점점점 커진다
                    startRecording() // 녹음시작
                    beforeY = event.y // 현재 터치한 y좌표 기록
                    timeStarted = System.currentTimeMillis()
                    true
                }



////////////////////////////////////////////////////////////////
// 터치 리스너 : 움직이면
////////////////////////////////////////////////////////////////

                // <터치 리스너 MOVE> 움직이면
                MotionEvent.ACTION_MOVE -> {

                    // <direction> 방향에 따라 up down
                    afterY = event.y // 움직인 후의 y좌표 기록
                    var difY = beforeY - afterY
                    var direction = if (difY >= 0) { "up" } else { "down" }
                    // y좌표 차이 값 -> 양수면 상단방향, 음수면 하단방향

                    // <press> 움직인 거리에 따라 강약 조절
                    var press: Int = 0
                    // 최소 10 움직이면 -> 원 둘레 on
                    if (timerCircle.isVisible == false && Math.abs(difY) > 10) {
                        timerCircle.isVisible = true
                    }

                    // 최소 10 움직이면 -> 5씩 증가, 주기가 5분 미만일 땐 느리게 증가
                    if (Math.abs(difY) > 10) {
                        press = if (timerCircle.progress <= 50) {
                            2
                        } else if (Math.abs(difY) > 20) {
                            10
                        } else {
                            5
                        }
                    }

                    // up, down과 press에 따라 원 둘레 변화
                    when (direction) {
                        "up" -> {
                            timerCircle.progress = timerCircle.progress + press
                            beforeY = event.y
                        }

                        "down" -> {
                            timerCircle.progress = timerCircle.progress - press
                            beforeY = event.y // 현재의 y 좌표를 다시 기록
                        }
                    }

                    // 타이머 반복 주기에, 둘레 조절 결과를 업데이트
                    period = timerCircle.progress.toInt() / 10

                    // <setText> 5분 단위로 끊어서 표현
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



////////////////////////////////////////////////////////////////
// 터치 리스너 : 손을 떼면
////////////////////////////////////////////////////////////////

                // <터치 리스너 UP > 손을 떼면 녹음 중단 ->  Timer 액티비티로 전환
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(scaleUpLoopRun) // 동그라미 커지기 중단 !
                    handler.postDelayed( {stopRecording()}, 500) // 1초 뒤 녹음 종료

                    timeNow = System.currentTimeMillis()
                    var timeCount = (timeNow - timeStarted) // 시간이 짧으면
                    if (timeCount <  700L) {
                        Toast.makeText(this, "녹음이 너무 짧아요", Toast.LENGTH_SHORT).show()
                        durationCircle.scaleX = 0f
                        durationCircle.scaleY = 0f
                        timerCircle.isVisible = false
//                        handler.postDelayed({
//                            durationCircle.scaleX = 0f
//                            durationCircle.scaleY = 0f
//                        }, 1000)

                    } else {
                        ListenerToTimerIntent.putExtra("period", period)
                        ListenerToTimerIntent.putExtra("scaleX", durationCircle.scaleX)
                        // 타이머 반복주기, 원의 크기를 Extra로 담아
                        val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
                        // makeCustomAnimation으로 애니메이션 제거

                        handler.postDelayed( {startActivity(ListenerToTimerIntent, options.toBundle()) }, 1000) // 액티비티 전환
                    }
                    true
                }

                else -> false
            } // <터치 리스너 when> 종료
        } // <터치 리스너> 종료
    } // <onCreate> 종료



////////////////////////////////////////////////////////////////
// onCreate 바깥 : 녹음 기능
////////////////////////////////////////////////////////////////
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false

    private fun startRecording(){

        // 다운로드 - "타잔.mp3" 파일 하나로 녹음 덮어쓰며 관리
        val fileName: String = Date().getTime().toString() + ".mp3"
        output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + "mysound007.mp3"

        // MediaRecorder() 클래스의 프로퍼티에 오디오 포맷 대입
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        // MediaRecorder() 클래스의 메서드로 녹음 준비 - 시작
        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
        // 오류 캐치
        } catch (e: IllegalStateException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun stopRecording(){

        // 녹음이 정상적으로 시작되었다면, 녹음 종료
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
        } else {
//            Toast.makeText(this, "레코딩 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 화면에서 앱이 사라지면 -> 일단 녹음 종료
    override fun onPause() {
        super.onPause()
        stopRecording()
    }

} // <Class> 종료