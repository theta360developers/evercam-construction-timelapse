package io.evercam.constructiontimelapse;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.theta360.pluginlibrary.activity.PluginActivity;
import com.theta360.pluginlibrary.callback.KeyCallback;
import com.theta360.pluginlibrary.receiver.KeyReceiver;
import com.theta360.pluginlibrary.values.LedColor;
import com.theta360.pluginlibrary.values.LedTarget;

import org.theta4j.webapi.Theta;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.theta4j.webapi.Options.OFF_DELAY;
import static org.theta4j.webapi.Options.SLEEP_DELAY;

public class MainActivity extends PluginActivity {

    private ButtonCallback buttonCallback = new ButtonCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAutoClose(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setKeyCallback(buttonCallback);

        if (isApConnected()) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        setKeyCallback(null);
        notificationLedHide(LedTarget.LED3);
    }


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

                notificationLed3Show(LedColor.YELLOW);
                notificationAudioSelf();

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
                            notificationLedBlink(LedTarget.LED3, LedColor.YELLOW, 1000);
                            notificationAudioSelf();
                            Thread.sleep(delay/2);
                            notificationLedBlink(LedTarget.LED3, LedColor.YELLOW, 500);
                            notificationAudioSelf();
                            Thread.sleep(delay/4);
                            notificationLedBlink(LedTarget.LED3, LedColor.YELLOW, 250);
                            notificationAudioSelf();
                            Thread.sleep(delay/4);
                            theta.takePicture();


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
                        notificationLedHide(LedTarget.LED3);
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
}
