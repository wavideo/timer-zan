package com.example.mafia

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.Runnable
import android.media.MediaPlayer
import android.net.Uri

class TimerActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timer)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
////////////////////////////////////////////////////////////////
// 선언부
////////////////////////////////////////////////////////////////
        var handler: Handler = Handler(Looper.getMainLooper())

        // 녹음파일 경로 설정
        val downloadFolder =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        var mediaPlayer =
            MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory().absolutePath + "/Download/" + "mysound007.mp3"))

        val anywhere_touch = findViewById<ConstraintLayout>(R.id.timer) // 전체 터치 영역
        var btnCancelMini = findViewById<ImageView>(R.id.iv_btn_cancel_timer) // 기본 동그라미
        var durationCircle = findViewById<ImageView>(R.id.iv_circle_duration_timer) // 기본 동그라미
        var tvTimer = findViewById<TextView>(R.id.tv_timer) // 텍스트

        // 카운트 다운 모드
        var countLayout = findViewById<ConstraintLayout>(R.id.group_time) // 레이아웃 전체
        var countText = findViewById<TextView>(R.id.tv_count_text_timer) // text
        var countProgress = findViewById<ProgressBar>(R.id.pgb_count_progress_timer) // 진행도
        // 타이머 조절 모드
        var timerCircle = findViewById<ProgressBar>(R.id.pgb_timer_timer) // 둘레
        // 잔소리 재생 모드
        val voiceCircle = findViewById<ImageView>(R.id.iv_circle_voice_timer) // 녹음 동그라미
        val btnCancel = findViewById<TextView>(R.id.tv_btn_cancel_timer) // 취소 버튼

        // 카운트 다운 모드 - text
        var period:Int = 5
        fun textUpdate() {
            if (period > 5) {
                period = period / 5 * 5
                tvTimer.setText("잔소리를 기억했어요\n${period}분 마다 들려드릴게요!")
                // <setText> 5분 이하는 1분 단위로
            } else if (period > 0) {
                tvTimer.setText("잔소리를 기억했어요\n${period}분 마다 들려드릴게요!")
            } else {
                tvTimer.setText("잔소리를 기억했어요\n30초 마다 들려드릴게요!")
            }
        }

        // 잔소리 재생 모드 - text
        fun textUpdateControl() {
            if (period > 5) {
                period = period / 5 * 5
                tvTimer.setText("\n${period}분 마다 반복할게요")
                // <setText> 5분 이하는 1분 단위로
            } else if (period > 0) {
                tvTimer.setText("\n${period}분 마다 반복할게요")
            } else {
                tvTimer.setText("\n30초 마다 반복할게요")
            }
        }

////////////////////////////////////////////////////////////////
// Extra 받아와서 초기 세팅
////////////////////////////////////////////////////////////////

        // <Extra> 동그라미 크기 받아와서 출력
        var timerScale = intent.getFloatExtra("scaleX", 0.0f)
        durationCircle.scaleX = timerScale.toFloat()
        durationCircle.scaleY = timerScale.toFloat()

        // <Extra> 타이머 주기를 Extra로 받아와서 text 출력
        period = intent.getIntExtra("period", 5)
        textUpdate()
        timerCircle.progress = period * 10 // 둘레

        // 잔소리 출력
        handler.postDelayed( {mediaPlayer.start()}, 500)

////////////////////////////////////////////////////////////////
// [모드 스위칭] 카운트 다운 . 타이머 조절 . 잔소리 재생
////////////////////////////////////////////////////////////////

// <<< 카운트 다운 모드 >>>
        fun countdownMode() {
            timerCircle.isVisible = false
            voiceCircle.isVisible = false
            btnCancel.isVisible = false
            // 다 끄고, 진행바만 켜
            countLayout.isVisible = true
            textUpdate()
        }
        // 러너블의 delay를 활용해, 사운드 재생시간이 끝난 후 카운트다운으로 돌아오는 기능 구현
        var countdownModeRun: Runnable = object : Runnable {
            override fun run() {
                countdownMode()
            }
        }

// <<< 타이머 조절 모드 >>>
        fun timerEditMode() {
            voiceCircle.isVisible = false
            btnCancel.isVisible = false
            countLayout.isVisible = false
            // 다 끄고, 둘레 조절만 켜
            timerCircle.isVisible = true
        }
        // 러너블의 delay를 활용해, 화면이 1초이상 눌린 다음에 timer 모드 켜지는 기능 구현
        var timerEditModeRun = object : Runnable {
            override fun run() {
                timerEditMode()
                tvTimer.setText("\n몇 분마다 반복할까요?")
            }
        }

// <<< 잔소리 재생 모드 >>>
        fun playSoundMode() {
            tvTimer.setText("과거의 내가 녹음했던\n잔소리입니다!")
            mediaPlayer.start() // 사운드 재생

            countLayout.isVisible = false
            // 빨간 동그라미와 취소버튼 켜
            voiceCircle.isVisible = true
            btnCancel.isVisible = true

            handler.postDelayed(countdownModeRun, mediaPlayer.duration.toLong()+1000L)
            // 재생이 끝나면 카운트다운 모드
        }



////////////////////////////////////////////////////////////////
// 카운트 다운 시간 계산
////////////////////////////////////////////////////////////////
        // 선언부
        var timeStarted = System.currentTimeMillis()
        var timeNow = 0L

        var timeCount = 0L // 흐른 시간
        var timeMin = 0
        var timeSec = 0
        var timeString: String = ""

        // 무한반복 카운트 다운
        var countdownLoopRun: Runnable = object : Runnable {
            override fun run() {
                timeNow = System.currentTimeMillis()
                timeCount = (timeNow - timeStarted) / 1000L // 흐른 시간을 초 단위로 변환

                // 타임오버 되면
                if ( (period != 0 && timeCount.toInt() > period * 60)
                    || (period == 0 && timeCount.toInt() > 30) ) {

                    // 잔소리 모드 on + 카운트다운 재개
                    playSoundMode()
                    timeStarted = System.currentTimeMillis() + 2000L
                    }

                // 남은 시간을 분, 초 단위로 계산해 text와 progressBar로 출력
                timeMin = ( period*60 - timeCount.toInt() ) / 60
                timeSec = if (period != 0) { ( period*60 - timeCount.toInt() ) % 60 } else { 30 -(timeCount.toInt()% 60) }

                timeString = if (timeMin > 0 && timeSec != 0) {
                    "${timeMin}분 ${timeSec}초"
                } else if (timeMin > 0 && timeSec == 0) {
                    "${timeMin}분"
                } else {
                    "${timeSec}초"
                }

                if (timeCount == 3L ) {
                    tvTimer.setText("화면을 잠시 꺼놓거나\n내려도 괜찮아요 :)")
                    handler.postDelayed({textUpdate()}, 3000)
                }

                countText.setText(timeString)// text
                countProgress.progress = timeCount.toInt() // progressBar
                handler.postDelayed(this, 1000) // 1초마다 무한히 반복 실행
            }
        }
        handler.post(countdownLoopRun) // 카운트 다운 실행



////////////////////////////////////////////////////////////////
// 취소 버튼
////////////////////////////////////////////////////////////////

        fun btnCancel() {
            mediaPlayer.stop() // 사운드 정지
            handler.removeCallbacks(countdownLoopRun) // 무한반복 중단!

            var timerToReminderIntent = Intent(this, ListenerActivity::class.java)
            val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)

            timerToReminderIntent.putExtra("period", period)
            startActivity(
                timerToReminderIntent,
                options.toBundle()
            ) // 애니메이션 없이, "타이머 반복주기" Extra로 넘김
        }
        // 클릭 리스너
        btnCancel.setOnClickListener {
            btnCancel()
        }

        btnCancelMini.setOnClickListener {
            btnCancel()
        }


////////////////////////////////////////////////////////////////
// 터치 리스너 : 닿으면
////////////////////////////////////////////////////////////////

        // 화면 전체 터치를 위한 세팅
        var beforeY = 0f
        var afterY = 0f // 터치 전후 Y 좌표 기록
        var beforeTouchTime: Long = 0L
        var afterTouchTime: Long = 0L // 터치 전후 시간 기록
        var isMoved:Boolean = false // up, down 한 적 있는지

        // <터치 리스너> "Down 닿는다 / Move 움직인다 / Up 뗀다" 가 느껴지면 -> 아래 구문을 수행
        anywhere_touch.setOnTouchListener { v, event ->

            when (event.action) {
                // <터치 리스너 DOWN> 닿으면
                MotionEvent.ACTION_DOWN -> {
                    handler.postDelayed(timerEditModeRun, 300)
                    //0.3초 꾸-욱 누르면 타이머 조절 on

                    beforeY = event.y // 현재 y좌표 기록
                    beforeTouchTime = System.currentTimeMillis() // 현재 시간 기록
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
                    // 최소 10 움직이면 -> 5씩 증가, 주기가 5분 미만일 땐 느리게 증가
                    var press: Int = 0
                    if (Math.abs(difY) > 10) {

                        press = if (timerCircle.progress <= 50) {
                            2
                        } else if (Math.abs(difY) > 20) {
                            10
                        } else {
                            5
                        }

                        // up, down과 press에 따라 원 둘레 변화
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

                        // 텍스트 출력. 5단위로 정리
                        textUpdateControl()

                        // 최소 40은 움직여야 타이머 둘레 on
                            if (Math.abs(difY) > 20) {
                            isMoved = true
                            timerEditMode()
                            handler.removeCallbacks(timerEditModeRun) // 타이머 조절 러너블 작동은 중단.
                        }
                    } // <press> 종료

                    // 타이머 반복 주기에 둘레 조절 결과를 업데이트
                    period = timerCircle.progress.toInt() / 10
                    // 카운트 다운 max 변경
                    if (period != 0) {
                        countProgress.max = period * 60
                    } else {
                        countProgress.max = 30
                    }

                    true
                } // <터치 리스너 MOVE> 끝


////////////////////////////////////////////////////////////////
// 터치 리스너 : 손을 떼면
////////////////////////////////////////////////////////////////

                // <터치 리스너 UP > 손을 떼면

                MotionEvent.ACTION_UP -> {

                    // 꾸-욱, 스-윽 -> 타이머 조절 모드를 마치고 카운트 다운 모드 on
                    handler.removeCallbacks(timerEditModeRun) // timer 러너블 중단
                    countdownMode() // 카운트 다운 모드 실행

                    // 콕! -> 잔소리 모드 재생
                    afterTouchTime = System.currentTimeMillis() // 현재 시간 기록
                    if (isMoved == false && afterTouchTime - beforeTouchTime < 300L) {
                        playSoundMode() // 잔소리 모드 실행
                    }
                    isMoved = false
                    true
                }
                else -> false
            } // <터치 리스너 when> 종료
        } // <터치 리스너> 종료
    }

}