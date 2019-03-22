package com.example.killnono.watchdog;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.killnono.watchdog.remote.apiservice.RoomApiService;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * 58f98fcf9d570c8583074629 NASA
     */
    private static final String ROOM_ID = "58f98fcf9d570c8583074629";  /* 房间ID唯一 */
    private static final String GPIO_NAME = "BCM4";  /* 传感器信号针脚名 */
    private static final Long LIMIT_TIME = 5L;

    private Gpio mLedGpio;
    private boolean isFull = false;/* 默认无人 */

    /* 资源 */
    int[] img_res = {R.mipmap.ic_turn_off, R.mipmap.ic_turn_on};
    String[] text_res = {"nobody in the Room", "somebody in the Room"};

    private long mCheckInTime = 0L;



    TextView nameTV;
    TextView sizeTV;
    TextView infoTV;

    TextView stateTV;
    ImageView stateIV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameTV = findViewById(R.id.tv_name);
        sizeTV = findViewById(R.id.tv_size);
        infoTV = findViewById(R.id.tv_info);
        stateTV = findViewById(R.id.tv_state);
        stateIV = findViewById(R.id.iv_state);
        init();
        getSelfInfo();
    }


    /**
     *
     */
    private void init() {
        PeripheralManager pioService = PeripheralManager.getInstance();
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

                mLedGpio.registerGpioCallback(
                        new GpioCallback() {
                            @Override
                            public boolean onGpioEdge(Gpio gpio) {
                                Log.i(TAG, "GPIO changed, button pressed");

                                long now = System.currentTimeMillis();
                                long dura = (now - mCheckInTime) / 1000;
                                mCheckInTime = now;
                                try {
                                    boolean value = gpio.getValue();
                                    if (value) {
                                        stateTV.setText(text_res[1]);
                                        stateIV.setImageResource(img_res[1]);
                                        Log.d(TAG, "低------>高:" + dura);
                                        mCheckInTime = System.currentTimeMillis();
                                    } else {
                                        stateIV.setImageResource(img_res[0]);
                                        stateTV.setText(text_res[0]);
                                        Log.d(TAG, "高----->低:" + dura);
                                    }
                                    postState(value);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return true;// 一定要返回true，不然只监听一次mmp
                            }

                            @Override
                            public void onGpioError(Gpio gpio, int error) {
                                Log.d(TAG, error+"");

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
            TextView wifiTV = findViewById(R.id.tv_wifi_state);
            wifiTV.setText(info);

            String ip = intToIp(wifiInfo.getIpAddress());
            if (TextUtils.isEmpty(ip)) {
                ip = "NULL";
            }
            TextView ipTV = findViewById(R.id.tv_ip_address);
            ipTV.setText(ip);

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


    private void postState(boolean b) {
        /* request net*/
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("roomId", ROOM_ID);
            jsonObject.put("roomState", b);
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

    private void getSelfInfo(){
        RoomApiService.Factory.getInstance().getSelf(ROOM_ID)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
                        JSONObject dataJO = null;
                        try {
                            dataJO = jsonObject.getJSONObject("data");
                            String name = dataJO.getString("roomName");
                            int size = dataJO.getInt("roomSize");
                            String info = dataJO.getString("roomInfo");

                            nameTV.setText(name);
                            sizeTV.setText(String.valueOf(size)+" 人");
                            infoTV.setText(info);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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
        postState(false);
    }
}
