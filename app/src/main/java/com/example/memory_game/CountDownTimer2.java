package com.example.memory_game;

import android.os.CountDownTimer;
import android.widget.TextView;

class CountDownTimer2 extends CountDownTimer {

    private TextView timer_box;
    private boolean is_running = true;

    public CountDownTimer2(long millisInFuture, long countDownInterval, TextView timer_box) {
        super(millisInFuture, countDownInterval);
        this.timer_box = timer_box;
    }

    int countUpSeconds = 0; // acts as a running sum
    // ticks every second
    public void onTick(long millisUntilFinished) {
        if (is_running) { countUpSeconds++; }
        timer_box.setText(this.getCountUpTime());
    }

    public String getCountUpTime() {
        int hours = countUpSeconds / 3600;
        int minutes = (countUpSeconds % 3600) / 60;
        int seconds = countUpSeconds % 60;

        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return timeString;
    }

    @Override
    public void onFinish() {

    }

    public void stop() {
        is_running = false;
    }
}