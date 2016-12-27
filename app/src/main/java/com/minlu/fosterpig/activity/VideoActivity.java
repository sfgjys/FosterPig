/**
 * <p>VideoActivity Class</p>
 *
 * @author zhuzhenlei 2014-7-17
 * @version V1.0
 * @modificationHistory
 * @modify by user:
 * @modify by reason:
 */
package com.minlu.fosterpig.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.RealPlayCallBack;
import com.minlu.fosterpig.R;
import com.minlu.fosterpig.StringsFiled;
import com.minlu.fosterpig.http.OkHttpManger;
import com.minlu.fosterpig.manager.ThreadManager;
import com.minlu.fosterpig.util.ToastUtil;
import com.minlu.fosterpig.util.ViewsUitls;

import org.MediaPlayer.PlayM4.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * <pre>
 *  ClassName  VideoActivity Class
 * </pre>
 *
 * @author zhuzhenlei
 * @version V1.0
 */
public class VideoActivity extends Activity implements Callback, OnClickListener {

    private SurfaceView mSurfaceView = null;

    private int mLoginId = -1;                // return by NET_DVR_Login_v30
    private int mPlayID = -1;                // return by NET_DVR_RealPlay_V30

    private int m_iPort = -1;                // play port
    private int mPlayChannel = 0;                // start channel no

    private final String TAG = "VideoActivity";

    private LinearLayout mLoad;
    private LinearLayout mError;
    private String videoIP;
    private String videoUser;
    private String videoPassWord;
    private int videoPort;
    private int channelNumber;
    private int m_iPlaybackID = -1;

    private LinearLayout mTrueTimeData;

    private boolean isCanShowTrueTimeData = false;

    private boolean isAlreadyShowTrueTimeData = true;

    private int mTrueTimeDataWidth = -1;

    private TranslateAnimation mShiftOut;
    private TranslateAnimation mEnterInto;
    private int keepTime = -1;
    private TimerTask keepTimeTimerTask;
    private Timer keepTimeTimer;
    private TextView mAmmoniaData;
    private TextView mTemperatureData;
    private TextView mHumidityData;
    private TextView mPowerSupplyData1;
    private TextView mPowerSupplyData2;
    private TextView mPowerSupplyData3;
    private TextView mPowerSupplyData4;
    private TextView mPowerSupplyData5;
    private TextView mPowerSupplyData6;
    private TextView mPowerSupplyData7;
    private TextView mPowerSupplyData8;
    private TimerTask mGetHttpDataTask;
    private Timer mGetHttpData;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*Intent intent = new Intent(ViewsUitls.getContext(), VideoActivity.class);
        intent.putExtra(StringsFiled.VIDEO_IP, "192.168.1.67");
        intent.putExtra(StringsFiled.VIDEO_USER, "admin");
        intent.putExtra(StringsFiled.VIDEO_PASSWORD, "asdf1234");
        intent.putExtra(StringsFiled.VIDEO_PORT, 8000);
        intent.putExtra(StringsFiled.VIDEO_CHANNEL_NUMBER, 33);
        startActivity(intent);*/

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_screen);

        if (!initeSdk()) {
            goneLoad();
            return;
        }

        if (!initeActivity()) {
            goneLoad();
            return;
        }

        Intent intent = getIntent();
        videoIP = intent.getStringExtra(StringsFiled.VIDEO_IP);
        videoUser = intent.getStringExtra(StringsFiled.VIDEO_USER);
        videoPassWord = intent.getStringExtra(StringsFiled.VIDEO_PASSWORD);
        videoPort = intent.getIntExtra(StringsFiled.VIDEO_PORT, -1);
        channelNumber = intent.getIntExtra(StringsFiled.VIDEO_CHANNEL_NUMBER, -1);
        ToastUtil.showToast(getApplicationContext(), videoIP + "-" + videoUser + "-" + videoPassWord + "-" + videoPort + "-" + channelNumber);

        ThreadManager.getInstance().execute(new Runnable() {
            @Override
            public void run() {
                loginStart();
            }
        });
    }

    private void loginStart() {
        if (mLoginId < 0) {
            login();
            if (mLoginId >= 0 && mPlayID < 0) {
                previewStart();
            }
        }
    }

    //  =============================================SurfaceView接口=====================================================
    //@Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i(TAG, "surface is created" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (surface.isValid()) {
            // setVideoWindow 设置显示区域 参数一 播放库端口 参数二 显示区域序号 参数三 设置显示窗
            // setVideoWindow
            // 需要在该方法的参数三的回调接口中surfaceCreated和surfaceDestroyed中使用，其中在surfaceDestroyed中使用时参数三为null代表不用设置显示了
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    //@Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    //@Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Player setVideoWindow release!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (holder.getSurface().isValid()) {
            if (!Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }
    //  =============================================SurfaceView接口=====================================================


    //  =============================================onSaveInstanceState缓存=====================================================
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("m_iPort", m_iPort);
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_iPort = savedInstanceState.getInt("m_iPort");
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState");
    }
    //  =============================================onSaveInstanceState缓存=====================================================

    //  =============================================初始化=====================================================

    /**
     * @return true - success;false - fail
     */
    private boolean initeSdk() {
        // 初始化SDK 调用设备网络 SDK 其他函数的前提，TRUE 表示成功， FALSE 表示失败
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e(TAG, "HCNetSDK init is failed!");
            return false;
        }
        //  启用日志 后期可删除
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, Environment.getExternalStorageDirectory().getPath() + "/", true);
        return true;
    }

    // GUI init
    private boolean initeActivity() {
        findViews();
        mSurfaceView.getHolder().addCallback(this);
        return true;
    }

    // get controller instance
    private void findViews() {
        mLoad = (LinearLayout) findViewById(R.id.ll_loading);
        mError = (LinearLayout) findViewById(R.id.ll_error);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_player);
        mTrueTimeData = (LinearLayout) findViewById(R.id.ll_video_true_time_data);
        ViewTreeObserver viewTreeObserver = mTrueTimeData.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                mTrueTimeData.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mTrueTimeDataWidth = mTrueTimeData.getWidth();
                mShiftOut = new TranslateAnimation(0f, -mTrueTimeDataWidth - ViewsUitls.dptopx(6), 0f, 0f);
                mEnterInto = new TranslateAnimation(-mTrueTimeDataWidth - ViewsUitls.dptopx(6), 0, 0f, 0f);
            }
        });

        mAmmoniaData = (TextView) findViewById(R.id.tv_video_true_time_data_ammonia);
        mTemperatureData = (TextView) findViewById(R.id.tv_video_true_time_data_temperature);
        mHumidityData = (TextView) findViewById(R.id.tv_video_true_time_data_humidity);
        mPowerSupplyData1 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_1);
        mPowerSupplyData2 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_2);
        mPowerSupplyData3 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_3);
        mPowerSupplyData4 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_4);
        mPowerSupplyData5 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_5);
        mPowerSupplyData6 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_6);
        mPowerSupplyData7 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_7);
        mPowerSupplyData8 = (TextView) findViewById(R.id.tv_video_true_time_data_power_supply_8);

        mError.setOnClickListener(this);
        mSurfaceView.setOnClickListener(this);
    }
    //  =============================================初始化=====================================================

    //  =============================================登录具体实现代码=====================================================
    private void login() {
        try {
            if (mLoginId < 0) {//  此处没有登录成功过，所以开始登录代码
                // login on the device
                mLoginId = loginDevice();// 调用登录方法获取登录后返回的id
                if (mLoginId < 0) {
                    Log.e(TAG, "登录返回的id小于0，登录失败!");
                    goneLoad();
                    return;
                }
                // get instance of exception callback and set
                ExceptionCallBack exceptionCallBack = getExceptionCallBack();
                if (exceptionCallBack == null) {
                    Log.e(TAG, "创建ExceptionCallBack回调接口对象失败");
                    goneLoad();
                    return;
                }

                if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(exceptionCallBack)) {
                    Log.e(TAG, "将创建好的ExceptionCallBack回调接口设置进某个地方");
                    goneLoad();
                    return;
                }
                Log.i(TAG, "登录成功");
            } else {//  此处登录成功过
                Log.i(TAG, "Login already");
            }
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
    }

    /**
     * @return login ID
     */
    private int loginDevice() {
        // 创建对象 该对象用来存储登录成功后的信息
        NET_DVR_DEVICEINFO_V30 mNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == mNetDvrDeviceInfoV30) {
            Log.e(TAG, "HKNetDvrDeviceInfoV30对象创建失败");
            return -1;
        }

        // 使用四大要素与对象作为参数调用登录方法
        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(videoIP, videoPort, videoUser, videoPassWord, mNetDvrDeviceInfoV30);
        if (iLogID < 0) {
            Log.e(TAG, "NET_DVR_Login_V30登录方法失败!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        /*int m_iChanNum;
        if (mNetDvrDeviceInfoV30.byChanNum > 0) {// 设备模拟通道个数 byChanNum为0
            mPlayChannel = mNetDvrDeviceInfoV30.byStartChan;// 模拟通道起始通道号 byStartChan为1
            m_iChanNum = mNetDvrDeviceInfoV30.byChanNum; // 设备模拟通道个数 byChanNum为0
        } else if (mNetDvrDeviceInfoV30.byIPChanNum > 0) {// 设备最大数字通道个数，低 8 位  byIPChanNum为8
            mPlayChannel = mNetDvrDeviceInfoV30.byStartDChan;// 起始数字通道号  byStartDChan为33
            m_iChanNum = mNetDvrDeviceInfoV30.byIPChanNum + mNetDvrDeviceInfoV30.byHighDChanNum * 256;// 其中byHighDChanNum为数字通道个数，高 8 位
            System.out.println("走的是设备数字通道个数");
        }*/
        /*if (mNetDvrDeviceInfoV30.byChanNum > 0) {
            mPlayChannel = mNetDvrDeviceInfoV30.byStartChan;
        } else if (mNetDvrDeviceInfoV30.byIPChanNum > 0) {
            mPlayChannel = mNetDvrDeviceInfoV30.byStartDChan;
        }*/
        //  手动对视频的通道号进行设置
        mPlayChannel = channelNumber;

        Log.i(TAG, "NET_DVR_Login_V30登录方法Successful!");

        return iLogID;
    }

    /**
     * @return exception instance
     */
    private ExceptionCallBack getExceptionCallBack() {
        return new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                System.out.println("recv exception, iType:" + iType + "***************************************此处是ExceptionCallBack回调接口的回调方法****************************************");
                if (iType == 32773) {
                    if (!(mPlayID < 0)) {
                        System.out.println("停止播放");
                        stopSinglePreview();
                        ViewsUitls.runInMainThread(new TimerTask() {
                            @Override
                            public void run() {
                                goneLoad();
                            }
                        });
                    }
                }
            }
        };
    }
    //  =============================================登录具体实现代码=====================================================

    //  =============================================单屏幕播放=====================================================
    private void previewStart() {
        try {
            if (mLoginId < 0) {
                Log.e(TAG, "please login on device first");
                goneLoad();
                return;
            }
            if (mPlayID < 0) {
                startSinglePreview();
            }
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
    }

    private void startSinglePreview() {
        RealPlayCallBack realPlayCallBack = getRealPlayerCallBack();
        if (realPlayCallBack == null) {
            Log.e(TAG, "RealPlayCallBack回调接口对象创建failed!");
            goneLoad();
            return;
        }
        Log.i(TAG, "mPlayChannel:" + mPlayChannel);

        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = mPlayChannel;// 通道号
        previewInfo.dwStreamType = 1; // 码流类型：0-主码流，1-子码流，2-码流 3，3-虚拟码流，以此类推
        previewInfo.bBlocked = 1;// 0- 非阻塞取流，1- 阻塞取流
        // HCNetSDK start preview
        mPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(mLoginId, previewInfo, realPlayCallBack);
        if (mPlayID < 0) {
            Log.e(TAG, "NET_DVR_RealPlay_V40播放监控方法失败!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            goneLoad();
            return;
        }
        Log.i(TAG, "NET_DVR_RealPlay_V40播放监控方法sucess");
    }

    /**
     * @return callback instance
     */
    private RealPlayCallBack getRealPlayerCallBack() {
        return new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType, byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                VideoActivity.this.processRealData(1, iDataType, pDataBuffer, iDataSize, Player.STREAM_REALTIME);
            }
        };
    }

    /**
     * @param iPlayViewNo - player channel [in]
     * @param iDataType   - data type [in]
     * @param pDataBuffer - data buffer [in]
     * @param iDataSize   - data size [in]
     * @param iStreamMode - stream mode [in]
     */
    public void processRealData(int iPlayViewNo, int iDataType, byte[] pDataBuffer, int iDataSize, int iStreamMode) {
        if (HCNetSDK.NET_DVR_SYSHEAD == iDataType) {
            if (m_iPort >= 0) {// 头数据的时候还没有请求网络获取到m_iPort的值所以是-1
                goneLoad();
                return;
            }
            m_iPort = Player.getInstance().getPort();
            if (m_iPort == -1) {
                Log.e(TAG, "getPort is failed with: " + Player.getInstance().getLastError(m_iPort));
                goneLoad();
                return;
            }
            Log.i(TAG, "getPort succ with: " + m_iPort);
            if (iDataSize > 0) {
                if (!Player.getInstance().setStreamOpenMode(m_iPort, iStreamMode)) {
                    Log.e(TAG, "setStreamOpenMode failed");
                    goneLoad();
                    return;
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer, iDataSize, 2 * 1024 * 1024)) {
                    Log.e(TAG, "openStream failed");
                    goneLoad();
                    return;
                }
                if (!Player.getInstance().play(m_iPort, mSurfaceView.getHolder())) {
                    Log.e(TAG, "play failed");
                    goneLoad();
                    return;
                }
                if (!Player.getInstance().playSound(m_iPort)) {
                    Log.e(TAG, "playSound failed with error code:" + Player.getInstance().getLastError(m_iPort));
                    goneLoad();
                    return;  // 最后一个return不需要，已经走完代码准备出去了
                }
                goneAll();
            } else {
                goneLoad();
            }
        } else {
            if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize)) {
                for (int i = 0; i < 4000 && m_iPlaybackID >= 0; i++) {
                    System.out.println("=============================回放存储进来了=============================");
                    if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize))
                        Log.e(TAG, "inputData failed with: " + Player.getInstance().getLastError(m_iPort));
                    else
                        break;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
    //  =============================================单屏幕播放=====================================================

    //  =============================================单屏幕停止=====================================================
    private void stopSinglePreview() {
        if (mPlayID < 0) {
            Log.e(TAG, "播放返回的id小于0，没有播放成功过");
            return;
        }

        //  net sdk stop preview
        if (!HCNetSDK.getInstance().NET_DVR_StopRealPlay(mPlayID)) {
            Log.e(TAG, "NET_DVR_StopRealPlay停止播放方法失败!Err:" + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }

        mPlayID = -1;
        stopSinglePlayer();
    }

    private void stopSinglePlayer() {
        Player.getInstance().stopSound();
        // player stop play
        if (!Player.getInstance().stop(m_iPort)) {
            Log.e(TAG, "停止端口号失败");
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "关闭流失败");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "释放端口号失败" + m_iPort);
            return;
        }
        m_iPort = -1;
    }

    //  =============================================单屏幕停止=====================================================

    //  =============================================登出=====================================================
    private void logOut() {
        if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(mLoginId)) {
            Log.e(TAG, " NET_DVR_Logout is failed!");
            return;
        }
        mLoginId = -1;
    }

    //  =============================================登出=====================================================


    /*调用了该方法，代表播放成功*/
    private void goneAll() {
        ViewsUitls.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mLoad.setVisibility(View.GONE);
                mError.setVisibility(View.GONE);
                isCanShowTrueTimeData = true;
                mTrueTimeData.setVisibility(View.VISIBLE);
                requestHttpGetData();
                openKeepTimeTimer();
            }
        });
    }

    private void requestHttpGetData() {
        // TODO 测试数据
        list.add(data1);
        list.add(data2);
        list.add(data3);
        list.add(data4);
        list.add(data5);
        list.add(data6);
        list.add(data7);
        list.add(data8);
        // TODO 测试数据
        mGetHttpData = new Timer();
        mGetHttpDataTask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("==================================Timer==================================");
                OkHttpClient okHttpClient = OkHttpManger.getInstance().getOkHttpClient();
                // start查询数据的起点  limit要查多少条数据
                RequestBody formBody = new FormBody.Builder().build();
                Request request = new Request.Builder()
                        .url("https://www.baidu.com/")//IpFiled.VIDEO_TRUE_TIME_DATA
                        .post(formBody)
                        .build();
                okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        System.out.println("====================================onFailure====================================");
                        ViewsUitls.runInMainThread(new TimerTask() {
                            @Override
                            public void run() {
                                ToastUtil.showToast(ViewsUitls.getContext(), "网络异常，无法获取实时数据");
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        ViewsUitls.runInMainThread(new TimerTask() {
                            @Override
                            public void run() {
                                // TODO 测试数据
                                int[] ints = list.get(text);
                                setTrueTimeText(ints[0], ints[1], ints[2], ints[3], ints[4], ints[5], ints[6], ints[7], ints[8], ints[9], ints[10]);
                                if (text == 7) {
                                    text = 0;
                                } else {
                                    text++;
                                }
                                // TODO 测试数据
                            }
                        });
                    }
                });
            }
        };
        mGetHttpData.schedule(mGetHttpDataTask, 0, 15000);
    }

    // TODO 测试数据
    private int[] data1 = {9056, 45, 60, 0, 1, 0, 1, 0, 1, 0, 1};
    private int[] data2 = {64, 52, 45, 1, 0, 1, 0, 1, 0, 1, 0};
    private int[] data3 = {44, 21, 78, 0, 1, 0, 1, 0, 1, 0, 1};
    private int[] data4 = {24, 94, 34, 1, 0, 1, 0, 1, 0, 1, 0};
    private int[] data5 = {20, 34, 94, 0, 1, 0, 1, 0, 1, 0, 1};
    private int[] data6 = {78, 84, 75, 1, 0, 1, 0, 1, 0, 1, 0};
    private int[] data7 = {12, 17, 12, 0, 1, 0, 1, 0, 1, 0, 1};
    private int[] data8 = {25, 10, 37, 1, 0, 1, 0, 1, 0, 1, 0};
    private List<int[]> list = new ArrayList<>();
    private int text = 0;
    // TODO 测试数据

    private void openKeepTimeTimer() {
        keepTimeTimer = new Timer();
        // 实时数据已经展示，需要过一段时间后隐藏掉
        keepTimeTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (isAlreadyShowTrueTimeData) {// 实时数据已经展示，需要过一段时间后隐藏掉
                    keepTime++;
                    ViewsUitls.runInMainThread(new TimerTask() {
                        @Override
                        public void run() {
                            if (keepTime == StringsFiled.VIDEO_TRUE_TIME_DATA_KEEP_TIME && mShiftOut != null && isAlreadyShowTrueTimeData && animationIsCompletes) {
                                startAnimation(mShiftOut);
                                isAlreadyShowTrueTimeData = false;
                            }
                        }
                    });
                } else {
                    keepTime = -1;
                }
            }
        };
        keepTimeTimer.schedule(keepTimeTimerTask, 0, 1000);
    }

    private void goneLoad() {
        ViewsUitls.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mLoad.setVisibility(View.GONE);
                mError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void goneError() {
        ViewsUitls.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mLoad.setVisibility(View.VISIBLE);
                mError.setVisibility(View.GONE);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void setTrueTimeText(int ammoniaData, int temperatureData, int humidityData,
                                 int powerSupplyData1, int powerSupplyData2, int powerSupplyData3, int powerSupplyData4,
                                 int powerSupplyData5, int powerSupplyData6, int powerSupplyData7, int powerSupplyData8) {
        mAmmoniaData.setText("氨气 : " + ammoniaData + "ppm");
        mTemperatureData.setText("温度 : " + temperatureData + "℃");
        mHumidityData.setText("湿度 : " + humidityData + "%");
        mPowerSupplyData1.setText("市电一 : " + getOpenOrClose(powerSupplyData1));
        mPowerSupplyData2.setText("市电二 : " + getOpenOrClose(powerSupplyData2));
        mPowerSupplyData3.setText("市电三 : " + getOpenOrClose(powerSupplyData3));
        mPowerSupplyData4.setText("市电四 : " + getOpenOrClose(powerSupplyData4));
        mPowerSupplyData5.setText("市电五 : " + getOpenOrClose(powerSupplyData5));
        mPowerSupplyData6.setText("市电六 : " + getOpenOrClose(powerSupplyData6));
        mPowerSupplyData7.setText("市电七 : " + getOpenOrClose(powerSupplyData7));
        mPowerSupplyData8.setText("市电八 : " + getOpenOrClose(powerSupplyData8));
    }

    private String getOpenOrClose(int openOrClose) {
        if (openOrClose == 0) {
            return "断";
        } else {
            return "通";
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.ll_error:
                if (mLoginId < 0) { // 登录失败重新登录
                    goneError();
                    ThreadManager.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            loginStart();
                        }
                    });
                } else if (mPlayID < 0) {// 登录成功但播放失败
                    goneError();
                    ThreadManager.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (mPlayID < 0) {
                                previewStart();
                            }
                        }
                    });
                } else {
                    System.out.println("播放到一半时失败");
                }
                break;
            case R.id.sv_player:// 根据实时界面是否出现来进行退回动画
                if (isCanShowTrueTimeData) {
                    if (mShiftOut != null && isAlreadyShowTrueTimeData && animationIsCompletes) {
                        startAnimation(mShiftOut);
                        isAlreadyShowTrueTimeData = false;
                    } else if (mEnterInto != null && !isAlreadyShowTrueTimeData && animationIsCompletes) {
                        startAnimation(mEnterInto);
                        isAlreadyShowTrueTimeData = true;
                    }
                }
                break;
        }


    }

    private boolean animationIsCompletes = true;

    private void startAnimation(TranslateAnimation translateAnimation) {
        translateAnimation.setDuration(1000);
        //当动画执行结束后  动画停留在结束的位置上
        translateAnimation.setFillAfter(true);
        mTrueTimeData.startAnimation(translateAnimation);
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                animationIsCompletes = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animationIsCompletes = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                System.out.println("onAnimationRepeat");
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                ThreadManager.getInstance().execute(new TimerTask() {
                    @Override
                    public void run() {
                        if (!(mPlayID < 0)) {
                            System.out.println("停止播放");
                            stopSinglePreview();
                        }
                        if (!(mLoginId < 0)) {
                            System.out.println("登出");
                            logOut();
                        }
                        // 释放SDK资源
                        HCNetSDK.getInstance().NET_DVR_Cleanup();
                    }
                });

                finish();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onDestroy() {

        if (keepTimeTimer != null)
            keepTimeTimer.cancel();
        keepTimeTimerTask = null;
        keepTimeTimer = null;

        if (mGetHttpData != null)
            mGetHttpData.cancel();
        mGetHttpDataTask = null;
        mGetHttpData = null;

        super.onDestroy();
    }
}
