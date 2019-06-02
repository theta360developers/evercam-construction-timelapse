package io.evercam.constructiontimelapse;

import android.util.Log;
import android.view.KeyEvent;

import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;

import org.theta4j.webapi.Theta;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.theta4j.webapi.Options.OFF_DELAY;
import static org.theta4j.webapi.Options.SLEEP_DELAY;

public class ButtonCallback implements KeyCallback {

    private Theta theta = Theta.createForPlugin();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExecutorService progressExecutor = Executors.newSingleThreadExecutor();


    private int delay = 60000;
//    private int delay = 4000;
    private int currentPicture = 0;
    private int maxPicture = 4500;
    final String TAG = "THETA";
    private int initialSleepDelay;
    private int initialOffDelay;

    private boolean inProgess = false;

    @Override
    public void onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyReceiver.KEYCODE_CAMERA) {
            executor.submit(() -> {
                try {
                    initialOffDelay = theta.getOption(OFF_DELAY);
                    initialSleepDelay = theta.getOption(SLEEP_DELAY);
                    Log.d(TAG, "Initial Sleep Delay: " + Integer.toString(initialSleepDelay));
                    Log.d(TAG, "Initial Off Delay " + Integer.toString(initialOffDelay));

                    // https://developers.theta360.com/en/docs/v2.1/api_reference/options/sleep_delay.html
                    theta.setOption(SLEEP_DELAY, 65535);
                    // https://developers.theta360.com/en/docs/v2.1/api_reference/options/off_delay.html
                    theta.setOption(OFF_DELAY, 65535);
                    Log.d(TAG, "Sleep Delay: " + theta.getOption(SLEEP_DELAY).toString());
                    Log.d(TAG, "Off Delay " + theta.getOption(OFF_DELAY).toString());



                    while (currentPicture < maxPicture) {
                        Log.d(TAG, "current picture " + Integer.toString(currentPicture));
                        theta.takePicture();
                        Thread.sleep(delay);
                        currentPicture = currentPicture + 1;
                    }
                    theta.setOption(SLEEP_DELAY, initialSleepDelay);
                    theta.setOption(OFF_DELAY, initialOffDelay);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            progressExecutor.submit(() -> {
                if (inProgess) {
                    currentPicture = maxPicture;
                    inProgess = false;
                    Log.d(TAG, "set current picture to " + Integer.toString(currentPicture));
                } else {
                    inProgess = true;
                    currentPicture = 0;
                }

            });
        }



    }

    @Override
    public void onKeyUp(int keyCode, KeyEvent keyEvent) {

    }

    @Override
    public void onKeyLongPress(int keyCode, KeyEvent keyEvent) {

        if (keyCode == KeyReceiver.KEYCODE_MEDIA_RECORD) {
            Log.d(TAG, "pressed side button");
            currentPicture = maxPicture;
            executor.shutdown();
            try {
                if (executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    Log.d(TAG, "task completed");
                } else {
                    Log.d(TAG, "forcing shutdown");
                    executor.shutdownNow();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
