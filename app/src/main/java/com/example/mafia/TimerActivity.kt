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
        var handler: Handler = Handler(Looper.getMainLooper())
        var tvTime = findViewById<TextView>(R.id.tv_time_stroke_timer)
        var ivTimeBack = findViewById<ImageView>(R.id.iv_time_background_timer)
        var timeProgress = findViewById<ProgressBar>(R.id.pgb_time_progress_timer)


// 1. Extra 받아오기
        // <Extra> 동그라미 크기 받아와서 출력

        var timerScale = intent.getFloatExtra("scaleX", 0.0f)
        var durationCircle = findViewById<ImageView>(R.id.iv_circle_duration_timer)
        durationCircle.scaleX = timerScale.toFloat()
        durationCircle.scaleY = timerScale.toFloat()

        // <Extra> 타이머 주기를 Extra로 받아와 출력
        var period = intent.getIntExtra("period", 5)

        var timerCircle = findViewById<ProgressBar>(R.id.pgb_timer_timer)
        timerCircle.progress = period * 10
        // 둘레 레이아웃 off 상태지만, 값만 입력해둠

        var tvTimer = findViewById<TextView>(R.id.tv_timer)
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
        textUpdate()
        // setText 조건문을 함수로 지정


        // 2. 화면 암전 기능 (현재 미구현)
        // 화면 자동꺼짐 기능
        fun darkMode() {
//            tvTimer.isVisible = false
//            var gray = Color.parseColor("#333333")
//            ImageViewCompat.setImageTintList(durationCircle, ColorStateList.valueOf(gray))
//            anywhere_touch.setBackgroundColor(parseColor("#000000"))
        }

        fun lightMode() {
//            tvTimer.isVisible = true
//            var white = Color.parseColor("#FFFFFF")
//            ImageViewCompat.setImageTintList(durationCircle, ColorStateList.valueOf(white))
//            anywhere_touch.setBackgroundColor(parseColor("#41D0CB"))
        }

        // <핸들러 + 러너블> 화면 암전
        // 1회 실행되는 러너블입니다.
        var darkRun: Runnable = object : Runnable {
            override fun run() {
                darkMode()
            }
        }
        handler.postDelayed(darkRun, 2000) // 2초 후 화면을 어둡게


// 3. 타이머 조절 <-> 카운트 다운 스위칭
        var countCircle = findViewById<ProgressBar>(R.id.pgb_countdown_timer)
        countCircle.progress = 1800
        countCircle.isVisible = true // 카운트다운 써클 (현재 안보임)
        val voiceCircle = findViewById<ImageView>(R.id.iv_circle_voice_timer) // 녹음 동그라미
        val btnCancel = findViewById<TextView>(R.id.tv_btn_cancel_timer) // 녹음 동그라미

        // 타이머조절 on
        fun timerCircleOn() {
            countCircle.isVisible = false
            timerCircle.isVisible = true
            voiceCircle.isVisible = false
            btnCancel.isVisible = false
            tvTime.isVisible = false
            ivTimeBack.isVisible = false
            timeProgress.isVisible = false
        }
        // 타이머조절 - 딜레이를 위해 러너블 사용
        var timerCircleRun: Runnable = object : Runnable {
            override fun run() {
                timerCircleOn()
                tvTimer.setText("\n몇 분마다 반복할까요?")
            }
        }

        // 카운트다운 on
        fun countCircleOn() {
            countCircle.isVisible = true
            timerCircle.isVisible = false
            voiceCircle.isVisible = false
            btnCancel.isVisible = false
            tvTime.isVisible = true
            ivTimeBack.isVisible = true
            timeProgress.isVisible = true
            textUpdate()
        }
        // 타이머조절 - 딜레이를 위해 러너블 사용
        var countCircleRun: Runnable = object : Runnable {
            override fun run() {
                countCircleOn()
            }
        }


// 4. 잔소리 그만하기 기능
        // 터치를 위한 세팅
        val anywhere_touch = findViewById<ConstraintLayout>(R.id.timer) // 전체 터치 영역
        var beforeY = 0f
        var afterY = 0f // 터치 전후 Y 좌표 기록
        var beforeTouchTime: Long = 0L
        var afterTouchTime: Long = 0L // 터치 전후 시간 기록

        // 0.3초 이내면 잔소리 재생
        fun playSound() {
            tvTimer.setText("과거의 내가 녹음했던\n잔소리입니다!")
            val downloadFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            var mediaPlayer =
                MediaPlayer.create(this, Uri.parse("${downloadFolder.absolutePath}/tazan.mp3"))
            mediaPlayer.start()

            voiceCircle.isVisible = true
            btnCancel.isVisible = true
            tvTime.isVisible = false
            ivTimeBack.isVisible = false
            timeProgress.isVisible = false
            handler.postDelayed(countCircleRun, mediaPlayer.duration.toLong())
        }

        btnCancel.setOnClickListener {
            var timerToReminderIntent = Intent(this, ListenerActivity::class.java)
            timerToReminderIntent.putExtra("period", period)
            // options.toBundle() 로 애니메이션 제거
            val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)
            startActivity(timerToReminderIntent, options.toBundle())
        }

        var timeStarted = System.currentTimeMillis()
        var timeNow = 0L
        var timeCount = 0L
        var timeString: String = ""
        var timeMin = 0
        var timeSec = 0
        var tvTimeRun: Runnable = object : Runnable {
            override fun run() {
                timeNow = System.currentTimeMillis()
                timeCount = (timeNow - timeStarted) / 1000L

                if (period != 0 && timeCount.toInt() > period * 60) {
                    playSound()
                    timeStarted = System.currentTimeMillis() + 2000L
                } else if (period == 0 && timeCount.toInt() > 30) {
                    playSound()
                    timeStarted = System.currentTimeMillis() + 2000L
                }

                timeMin = timeCount.toInt() / 60
                timeSec = timeCount.toInt() % 60
                timeString = if (timeMin > 0) {
                    "${timeMin}분 ${timeSec}초"
                } else {
                    "${timeSec}초"
                }

                tvTime.setText(timeString)
                handler.postDelayed(this, 1000)
                if (period == 0) {
                    timeProgress.max = 30
                } else {
                    timeProgress.max = period * 60
                }
                timeProgress.progress = timeCount.toInt()

            }
        }
        handler.post(tvTimeRun)

        // <터치 리스너> "닿는다 / 움직인다 / 뗀다" 가 느껴지면 -> 아래 구문을 수행
        anywhere_touch.setOnTouchListener { v, event ->

            when (event.action) {
                // <터치 리스너 DOWN> 닿으면
                MotionEvent.ACTION_DOWN -> {
                    lightMode()
                    handler.removeCallbacks(countCircleRun)
                    handler.postDelayed(timerCircleRun, 300)
                    //꾹누르면 타이머조절 on

                    beforeY = event.y // 현재 y좌표 기록
                    beforeTouchTime = System.currentTimeMillis() // 현재 시간 기록
                    true
                }


// 5. 터치 리스너 UP / DOWN

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
                    // 최소 10은 움직여야 타이머 조절 가능
                    var press: Int = 0
                    if (Math.abs(difY) > 10) {
                        press = if (timerCircle.progress <= 50) {
                            2 // 5분 미만일 땐 느리게 . . .
                        } else if (Math.abs(difY) > 20) {
                            10
                        } else {
                            5
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
                        // 텍스트 출력
                        textUpdateControl()
                        // 최소 0.3초, 최소 40은 움직여야 타이머조절 on
                        if (Math.abs(difY) > 40) {
                            timerCircleOn()
                            handler.removeCallbacks(timerCircleRun)
                            //ACTION_DOWN의 러너블 작동은 중단.
                        }
                    } // <press> 종료

                    // progressBar는 타이머 10배 비율입니다
                    period = timerCircle.progress.toInt() / 10

                    true

                } // <터치 리스너 MOVE> 끝


// 6. 터치리스너 손 뗐을 때

                // <터치 리스너 UP > 떼면 -> 사운드 재생, 또는 카운트다운 on
                MotionEvent.ACTION_UP -> {
                    countCircleOn()

                    afterTouchTime = System.currentTimeMillis() // 현재 시간 기록
                    if (afterTouchTime - beforeTouchTime < 300L) {
                        playSound()
                    }

                    handler.removeCallbacks(timerCircleRun) // timer 러너블 중단

                    handler.postDelayed(darkRun, 1000) // (현재 미적용)
                    true
                }

                else -> false
            } // <터치 리스너 when> 종료
        } // <터치 리스너> 종료
    }
}