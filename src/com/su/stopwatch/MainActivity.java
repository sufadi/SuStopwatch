package com.su.stopwatch;

import static android.os.PowerManager.ON_AFTER_RELEASE;
import static android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.su.stopwatch.data.DataModel;
import com.su.stopwatch.data.Stopwatch;
import com.su.stopwatch.stopwatch.StopwatchCircleView;
import com.su.stopwatch.timer.CountingTimerView;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    /** Displays the current stopwatch time. */
    private CountingTimerView mTimeText;
    /** Draws the reference lap while the stopwatch is running. */
    private StopwatchCircleView mTime;
    /**
     * Scheduled to update the stopwatch time and current lap time while
     * stopwatch is running.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Held while the stopwatch is running and this fragment is forward to keep
     * the screen on.
     */
    private PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initValue();
        initListener();
    }

    private void initView() {
        mTime = (StopwatchCircleView) findViewById(R.id.stopwatch_time);
        // Timer text serves as a virtual start/stop button.
        mTimeText = (CountingTimerView) findViewById(R.id.stopwatch_time_text);
    }

    private void initValue() {
        DataModel.getDataModel().resetStopwatch();
        mTimeText.setTime(0, true, true);
        mTimeText.blinkTimeStr(false);
    }

    private void initListener() {

    }

    /**
     * Start the stopwatch.
     */
    public void doStart(View view) {
        // Update the stopwatch state.
        DataModel.getDataModel().startStopwatch();

        // Start UI updates.
        startUpdatingTime();
        mTime.update();
        mTimeText.blinkTimeStr(false);

        // Acquire the wake lock.
        acquireWakeLock();
    }

    /**
     * Pause the stopwatch.
     */
    public void doPause(View view) {
        // Update the stopwatch state
        DataModel.getDataModel().pauseStopwatch();

        // Redraw the paused stopwatch time.
        updateTime();

        // Stop UI updates.
        stopUpdatingTime();
        mTimeText.blinkTimeStr(true);
    }

    /**
     * Reset the stopwatch.
     */
    public void doReset(View view) {
        // Update the stopwatch state.
        DataModel.getDataModel().resetStopwatch();
        // Clear the times.
        mTimeText.setTime(0, true, true);
        // Release the wake lock.
        releaseWakeLock();
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule
     * itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first
        // stopping updates.
        stopUpdatingTime();
        mTime.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mTime.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * This runnable periodically updates times throughout the UI. It stops
     * these updates when the stopwatch is no longer running.
     */
    private final class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            Log.d("suhuazhi", "TimeUpdateRunnable");
            final long startTime = SystemClock.elapsedRealtime();

            updateTime();

            if (getStopwatch().isRunning()) {
                // The stopwatch is still running so execute this runnable again
                // after a delay.
                final boolean talkBackOn = true;

                // Grant longer time between redraws when talk-back is on to let
                // it catch up.
                final int period = talkBackOn ? 500 : 25;

                // Try to maintain a consistent period of time between redraws.
                final long endTime = SystemClock.elapsedRealtime();
                final long delay = Math.max(0, startTime + period - endTime);

                mTime.postDelayed(this, delay);
            }
        }
    }

    /**
     * Update all time displays based on a single snapshot of the stopwatch
     * progress. This includes the stopwatch time drawn in the circle, the
     * current lap time and the total elapsed time in the list of laps.
     */
    private void updateTime() {
        // Compute the total time of the stopwatch.
        final long totalTime = getStopwatch().getTotalTime();

        // Update the total time display.
        mTimeText.setTime(totalTime, true, true);
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            mWakeLock = pm.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK | ON_AFTER_RELEASE, TAG);
            mWakeLock.setReferenceCounted(false);
        }
        mWakeLock.acquire();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
