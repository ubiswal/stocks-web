package com.ubiswal.handlers;

import java.util.Timer;
import java.util.TimerTask;

public abstract class PageRefresh implements Runnable {
    int intervalMinutes;

    class RefreshTask extends TimerTask {
        @Override
        public void run() {
            updateHtml();
        }
    }

    public PageRefresh(int intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }

    @Override
    public void run() {
        Timer timer = new Timer();
        timer.schedule(new RefreshTask(), 0, intervalMinutes * 60 * 1000);
    }

    abstract void updateHtml();
}
