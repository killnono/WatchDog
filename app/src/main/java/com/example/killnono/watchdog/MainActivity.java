package com.example.killnono.watchdog;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.example.killnono.watchdog.databinding.ActivityMainBinding;
import com.example.killnono.watchdog.remote.apiservice.RoomApiService;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * 58f98fcf9d570c8583074629 小树林
     */
    private static final String ROOM_ID    = "58f98fcf9d570c8583074629";  /* 房间ID唯一 */
    private static final String GPIO_NAME  = "BCM17";  /* 传感器信号针脚名 */
    private static final Long   LIMIT_TIME = 5L;

    private ActivityMainBinding mMainBinding;
    private Gpio                mLedGpio;
    private boolean isFull = false;/* 默认无人 */

    /* 资源 */
    int[]    img_res  = {R.mipmap.ic_turn_off, R.mipmap.ic_turn_on};
    String[] text_res = {"nobody in the Room", "somebody in the Room"};

    private long mCheckInTime = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        init();
    }


    /**
     *
     */
    private void init() {
        PeripheralManagerService pioService = new PeripheralManagerService();
        try {
            mLedGpio = pioService.openGpio(GPIO_NAME);
            if (mLedGpio != null) {
                mLedGpio.setDirection(Gpio.DIRECTION_IN);
                mLedGpio.setActiveType(Gpio.ACTIVE_HIGH);
                mLedGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
                isFull = mLedGpio.getValue();
                /**
                 * 容错
                 * 0. 延时策略（如果有活动信号产生后，延时25秒。每次活动后延时时间重新计算。保证人突然不动几秒就误判为无人了）
                 * 1. 当显示无信号的时候，机智的延长一个时间段再来做判断是否是真的离开的（人突然这段时间确实静止了一会。）
                 */
                mLedGpio.registerGpioCallback(new GpioCallback() {
                    @Override
                    public boolean onGpioEdge(Gpio gpio) {
                        long now = System.currentTimeMillis();
                        long dura = (now - mCheckInTime) / 1000;
                        mCheckInTime = now;
                        try {
                            boolean value = gpio.getValue();
                            if (value) {
                                mMainBinding.text.setText(text_res[1]);
                                mMainBinding.image.setImageResource(img_res[1]);
                                Log.d(TAG, "低------>高:" + dura);
                                mCheckInTime = System.currentTimeMillis();
                            } else {
                                mMainBinding.image.setImageResource(img_res[0]);
                                mMainBinding.text.setText(text_res[0]);
                                Log.d(TAG, "高----->低:" + dura);
                            }
                            postRequest(value);
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

        showConnectedState();
    }


    private long preOffStartTime = 0L;

    private void showConnectedState() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String info = wifiInfo.toString();
            if (TextUtils.isEmpty(info)) {
                info = "NULL";
            }
            mMainBinding.tvWifiState.setText(info);

            String ip = intToIp(wifiInfo.getIpAddress());
            if (TextUtils.isEmpty(ip)) {
                ip = "NULL";
            }
            mMainBinding.tvIpAddress.setText(ip);

        }
    }


    /**
     * @param i
     * @return
     */
    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }


    private String getLocalIpAddress() {
        try {
            String ipv4 = null;
            List<NetworkInterface> nilist = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ni : nilist) {
                List<InetAddress> ialist = Collections.list(ni.getInetAddresses());
                for (InetAddress address : ialist) {
                    ipv4 = address.getHostAddress();
                    if (!address.isLoopbackAddress()) {
                        return ipv4;
                    }
                }
            }
        } catch (SocketException ex) {

        }
        return "0.0.0.0";
    }


    private void postRequest(boolean b) {
           /* request net*/
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = formatter.format(new Date());
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("roomId", ROOM_ID);
            jsonObject.put("state", b);
            jsonObject.put("date", date);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RoomApiService.Factory.getInstance().postCurrentState(jsonObject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mLedGpio.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing GPIO " + e);
        }
        postRequest(false);
    }
}
