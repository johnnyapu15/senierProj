package com.example.dlscj.dash;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.util.UUID;

import dashcontrol.config.BluetoothConfig;
import dashcontrol.config.SpeakerConfig;
import dashcontrol.control.LightRGB;
import dashcontrol.control.Speaker;
import dashcontrol.utils.DeviceFilter;
import dashcontrol.utils.DeviceStatus;

import static dashcontrol.config.SpeakerConfig.getSoundFileSequence;
import static dashcontrol.utils.Debug.LOG;
import static dashcontrol.utils.Debug.TAG;

public class MainActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2{

    Dash d;

    private int start_flag = 1;

    Point d_size;
    Display display;

    ImageButton b1, b2, b3, logo, exit;
    TextView t1, t2, t3;

    private TextView mSearchResult;
    private CameraBridgeViewBase mOpenCvCameraView;

    Boolean isRegistered = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        d = (Dash) getApplication();

        b1 = (ImageButton)findViewById(R.id.Button1);
        b2 = (ImageButton)findViewById(R.id.Button2);
        b3 = (ImageButton)findViewById(R.id.Button3);
        logo = (ImageButton)findViewById(R.id.logoButton);
        t1 = (TextView)findViewById(R.id.textView1);
        t2 = (TextView)findViewById(R.id.textView2);
        t3 = (TextView)findViewById(R.id.textView3);
        exit = (ImageButton)findViewById(R.id.exitButton);

        d.bgm = MediaPlayer.create(this, R.raw.bgm);
        d.bgm.start();

        initControlText();

        LOG("Idle");

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);


        // Bluetooth 상태 변화 확인을 위한 Intent Filter 설정
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothConfig.ACTION_GATT_CONNECTED);
        mFilter.addAction(BluetoothConfig.ACTION_GATT_DISCONNECTED);
        mFilter.addAction(BluetoothConfig.ACTION_GATT_DISCONNECTING);
        mFilter.addAction(BluetoothConfig.ACTION_GATT_SERVICES_DISCOVERED);
        mFilter.addAction(BluetoothConfig.ACTION_DATA_AVAILABLE);
        registerReceiver(mGattUpdateReceiver, mFilter);
        isRegistered = true;

        // Bluetooth 설정 점검
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.bluetooth_ble_no, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (d.mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_disabled), Toast.LENGTH_LONG).show();
            finish();
            return;
        } else if (!d.mBluetoothAdapter.isEnabled()) {
            d.mBluetoothAdapter.enable();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        display = getWindowManager().getDefaultDisplay();
        d_size = new Point();
        display.getSize(d_size);

        // Bluetooth LE 검색 시작
        startBLEScan();

    }


    //로고
    public void logoClicked(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.infomark.co.kr"));
        startActivity(intent);
        this.finish();
    }
    public void button1Clicked(View view) {
        Intent intent = new Intent(getApplicationContext(), TutorialActivity.class);
        startActivity(intent);
    }
    public void button2Clicked(View view) {
        Intent intent = new Intent(getApplicationContext(), FreeActivity.class);
        startActivity(intent);
    }
    public void button3Clicked(View view) {
        Intent intent = new Intent(getApplicationContext(), PatternActivity.class);
        startActivity(intent);
    }

    public void exitButtonClicked(View view) {
        CloseBluetoothGATT();
        this.finishAffinity();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        d.matInput = inputFrame.rgba();

        //ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

        //if stop인 경우 Send_WW_Command(new BodyLinearAngular(0.0, 0.0).getBodyLinearAngular());
        if(d.HSVFilter(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), d.rsltarr)) {
            //버튼 투명하게!, 초깃값 설정
            // d.LineS2E(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), start_x, start_y, end_x, end_y);

            if(start_flag == 0) {
                start_flag = 1;

                /*
                rel_x = (float)(d.rsltarr[0] + d.rsltarr[2] / 2) * d_size.x / d.matInput.cols();
                rel_y = (float)(d.rsltarr[1] + d.rsltarr[3] / 2) * d_size.y / d.matInput.rows();
                */

                d.getRealPose();

                if(d.isTouchInside(exit, d.rel_x, d.rel_y))
                    exitButtonClicked(exit);
                else {
                    if(d.isTouchInside(b2, d.rel_x, d.rel_y)) button2Clicked(b2);
                    else {
                        if(d.isTouchInside(b1, d.rel_x, d.rel_y)) button1Clicked(b1);
                        if(d.isTouchInside(b3, d.rel_x, d.rel_y)) button3Clicked(b3);
                    }
                }
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            b1.setVisibility(View.INVISIBLE);
                            b2.setVisibility(View.INVISIBLE);
                            b3.setVisibility(View.INVISIBLE);
                            logo.setVisibility(View.INVISIBLE);
                            t1.setVisibility(View.INVISIBLE);
                            t2.setVisibility(View.INVISIBLE);
                            t3.setVisibility(View.INVISIBLE);
                            exit.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }).start();
        } else {
            start_flag = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            b1.setVisibility(View.VISIBLE);
                            b2.setVisibility(View.VISIBLE);
                            b3.setVisibility(View.VISIBLE);
                            logo.setVisibility(View.VISIBLE);
                            t1.setVisibility(View.VISIBLE);
                            t2.setVisibility(View.VISIBLE);
                            t3.setVisibility(View.VISIBLE);
                            exit.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
        }
        return d.matInput;
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_POWER) {
            CloseBluetoothGATT();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        CloseBluetoothGATT();
        if(d.bgm != null) {
            d.bgm.release();
        }
        d.bgm = null;
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        mOpenCvCameraView.enableView();
        super.onResume();
    }

    private void CloseGATT() {
        if (d.mBluetoothGatt != null) {
            d.mBluetoothGatt.disconnect();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            d.mBluetoothGatt.close();
            d.mBluetoothGatt = null;
        }
    }

    private void CloseBluetoothGATT() {

        if (d.mBluetoothAdapter != null) {
            d.mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        if (isRegistered) {
            unregisterReceiver(mGattUpdateReceiver);
            isRegistered = false;
        }

        CloseGATT();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 블루투스 연결 관리부 및 제어부
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void setText_Status(int Status) {
        if (Status == DeviceStatus._IDLE) {
            mSearchResult.setText(R.string.not_found);
        } else if (Status == DeviceStatus._SEARCHING) {
            mSearchResult.setText(getString(R.string.searching));
        } else if (Status == DeviceStatus._CONNECTING) {
            mSearchResult.setText(getString(R.string.connecting));
        } else if (Status == DeviceStatus._CONNECTED) {
            mSearchResult.setText(getString(R.string.connected));
        } else if (Status == DeviceStatus._DISCONNECTING) {
            mSearchResult.setText(getString(R.string.disconnecting));
        } else if (Status == DeviceStatus._READY) {
            mSearchResult.setText(getString(R.string.ready));
        }
        mSearchResult.invalidate();
    }

    private void DelayedScan() {
        d.mRetryWaitingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!d.mSearching && d.mDeviceStatus == DeviceStatus._IDLE) {
                    startBLEScan();
                }
            }
        }, d.RETRY_WAITING_PERIOD);
    }

    private void startBLEScan() {
        d.mScanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (d.mSearching) {
                    d.mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    d.mSearching = false;
                    if (d.mDeviceStatus <= DeviceStatus._SEARCHING) {
                        d.mDeviceStatus = DeviceStatus._IDLE;
                        setText_Status(d.mDeviceStatus);
                        DelayedScan();
                    }
                    LOG("STOP Scan delayed");
                }
            }
        }, d.SCAN_PERIOD);

        d.mDeviceStatus = DeviceStatus._SEARCHING;
        setText_Status(d.mDeviceStatus);

        d.mSearching = true;
        DeviceFilter.clear();

        // Scan Only WonderWorkshop
        UUID[] uuids = new UUID[1];
        uuids[0] = UUID.fromString(BluetoothConfig.WW_SERVICE_UUID);
        d.mBluetoothAdapter.startLeScan(uuids, mLeScanCallback);

        LOG("Start Scan");
    }

    private void stopBLEScan() {
        d.mBluetoothAdapter.stopLeScan(mLeScanCallback);
        d.mSearching = false;
        LOG("STOP Scan");
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     final byte[] scanRecord) {
                   /* if (rssi == 0 || rssi < BluetoothConfig.RSSI_LVL_FILTER) {  // 근접 설정, BluetoothConfig.RSSI_LVL_FILTER = -50
                        return;
                    }*/
                    if(rssi == 0 || rssi < -65) {   // 검색 범위 확장
                        return;
                    }

                    if (!BluetoothConfig.isMyDevice(device)) {
                        return;
                    }

                    LOG("FOUND MAC:" + device.getAddress() + " NAME:" + device.getName());
                    if (device.getName() != null) {
                        LOG("FOUND NAME:" + device.getName() + " RSSI:" + rssi);
                    }
                    d.mDeviceStatus = DeviceStatus._SEARCHED;

                    if (DeviceFilter.getSize() == 0) {
                        DeviceFilter.add(device.getAddress(), rssi);
                        d.mDeviceUpdateHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                d.mDeviceStatus = DeviceStatus._CONNECTING;
                                setText_Status(d.mDeviceStatus);

                                stopBLEScan();
                                d.mBluetoothGatt = d.mBluetoothAdapter.getRemoteDevice(DeviceFilter.getBestDeviceAddress()).connectGatt(getApplicationContext(), false, d.mBluetoothGattCallback);
                                LOG("Device Connecting");
                            }
                        }, d.RETRY_WAITING_PERIOD);
                    }
                    else {
                        DeviceFilter.add(device.getAddress(), rssi);
                    }
                }
            };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothConfig.ACTION_GATT_CONNECTED.equals(action)) {
                LOG("GATT Connected");

                // to display.....  Auto Connected case
                if (d.mSearching) {
                    stopBLEScan();
                }

                d.mDeviceStatus = DeviceStatus._CONNECTED;
                setText_Status(d.mDeviceStatus);

                // start service discover
                d.mBluetoothGatt.discoverServices();
            } else if (BluetoothConfig.ACTION_GATT_DISCONNECTED.equals(action)) {
                LOG("GATT Disconnected");

                CloseGATT();

                // to display.....  Auto Connected case

                d.mDeviceStatus = DeviceStatus._IDLE;
                setText_Status(d.mDeviceStatus);

                // try again.
                startBLEScan();
            }
            else if(BluetoothConfig.ACTION_GATT_DISCONNECTING.equals(action)) {
                LOG("GATT Disconnecting");
                d.mDeviceStatus = DeviceStatus._DISCONNECTING;
                setText_Status(d.mDeviceStatus);
            } else if (BluetoothConfig.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                LOG("GATT Service Discovered");
                Set_WW_BLE_Communication();
            } else if (BluetoothConfig.ACTION_DATA_AVAILABLE.equals(action)) {
                String _data = intent.getStringExtra(BluetoothConfig.EXTRA_DATA);
                // for DEBUGGING
                LOG("NOTIFIED = " + _data);
            }
        }
    };



    private void Set_WW_BLE_Communication() {

        d.mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        d.mWWService = d.mBluetoothGatt.getService(UUID.fromString(BluetoothConfig.WW_SERVICE_UUID));

        if (d.mWWService != null) {

            d.mWW_Noti_Report1 = d.mWWService.getCharacteristic(UUID.fromString(BluetoothConfig.WW_NOTIFY_REPORT1));
            if (d.mWW_Noti_Report1 != null) {
                setNotificationEnable(d.mWW_Noti_Report1, true);
            }

            d.mWW_Noti_Report2 = d.mWWService.getCharacteristic(UUID.fromString(BluetoothConfig.WW_NOTIFY_REPORT2));
            if (d.mWW_Noti_Report2 != null) {
                setNotificationEnable(d.mWW_Noti_Report2, true);
            }

            d.mWW_Noti = d.mWWService.getCharacteristic(UUID.fromString(BluetoothConfig.WW_NOTIFY));
            if (d.mWW_Noti != null) {
                setNotificationEnable(d.mWW_Noti, true);
            }

            d.mWW_Noti_Write = d.mWWService.getCharacteristic(UUID.fromString(BluetoothConfig.WW_NOTIFY_WRITE));
            if (d.mWW_Noti_Write != null) {
                setNotificationEnable(d.mWW_Noti_Write, true);
            }

            d.mWW_Write_Command = d.mWWService.getCharacteristic(UUID.fromString(BluetoothConfig.WW_WRITE_CMD));

        }

        if (d.mWWService != null) {
            LOG("Service Ready!");
            d.mDeviceStatus = DeviceStatus._READY;
            setText_Status(d.mDeviceStatus);
            sendInitialCommand();
        } else {
            LOG("Service Unstable, Please Try Again");
        }
    }

    private void setNotificationEnable(BluetoothGattCharacteristic Notify_Gatt, Boolean enable) {
        Boolean ret;

        BluetoothGattDescriptor descriptor = Notify_Gatt.getDescriptor(UUID.fromString(BluetoothConfig.CLIENT_CHARACTERISTIC_CONFIG));
        if (descriptor != null) {
            if (enable) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }

            do {
                ret = d.mBluetoothGatt.writeDescriptor(descriptor);
                if (!ret) {
                    if(!d.mBluetoothGattBusy) {
                        Log.e(TAG,"Gatt WriteDescriptor ERROR return = " + ret);
                        return ;
                    }
                    LOG("setNotificationEnable return = " + ret);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    d.mBluetoothGattBusy = true;
                }
            } while (!ret);
        }
        d.mBluetoothGatt.setCharacteristicNotification(Notify_Gatt, enable);
    }

    // 로봇과 연결시 로봇이 해야할 행동을 명령에 전송
    private void sendInitialCommand() {
        // Wow 효과음을 play
        d.Send_WW_Command(new Speaker(SpeakerConfig.SOUNDFILE_WOW).getSpeaker());

        // 가슴조명을 녹색으로 설정
        d.Send_WW_Command(new LightRGB().getChestColor(0, 1.0f, 0));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // 블루투스 및 카메라 초기화
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void initControlText() {
        mSearchResult = (TextView) findViewById(R.id.search_result_view);
        mSearchResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!d.mSearching && d.mDeviceStatus == DeviceStatus._IDLE) {
                    startBLEScan();
                }
            }
        });
        mSearchResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.Send_WW_Command(new Speaker(getSoundFileSequence()).getSpeaker());
            }
        });
    }

}
