package com.example.killnono.watchdog;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.killnono.watchdog.databinding.ActivityMainBinding;
import com.example.killnono.watchdog.remote.apiservice.RoomApiService;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {


    private ActivityMainBinding mMainBinding;
    private static final String TAG = MainActivity.class.getSimpleName();

    private Gpio mLedGpio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        init();
    }

    private void init() {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mLedGpio = pioService.openGpio("BCM17");

            if (mLedGpio != null) {
                mLedGpio.setDirection(Gpio.DIRECTION_IN);
                mLedGpio.setActiveType(Gpio.ACTIVE_HIGH);
                mLedGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);

                mLedGpio.registerGpioCallback(new GpioCallback() {
                    @Override
                    public boolean onGpioEdge(Gpio gpio) {
                        try {
                            String text;
                            if (gpio.getValue()) {
                                text = "some body in Room";
                            } else {
                                text = "no body in Room";
                            }

                            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                            mMainBinding.text.setText(text);
                            RoomApiService.Factory.getInstance().cvs().subscribeOn(Schedulers.io()).subscribe();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return super.onGpioEdge(gpio);
                    }

                    @Override
                    public void onGpioError(Gpio gpio, int error) {
                        super.onGpioError(gpio, error);
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mLedGpio.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing GPIO " + e);
        }
    }
}
