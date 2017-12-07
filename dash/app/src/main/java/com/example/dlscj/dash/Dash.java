package com.example.dlscj.dash;

import android.app.Application;

import android.media.MediaPlayer;
import android.view.Display;
import android.graphics.Point;

import android.Manifest;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.UUID;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import dashcontrol.config.BluetoothConfig;
import dashcontrol.config.SpeakerConfig;
import dashcontrol.control.BodyLinearAngular;
import dashcontrol.control.Head;
import dashcontrol.control.Eyes;
import dashcontrol.control.Light;
import dashcontrol.control.LightRGB;
import dashcontrol.control.Speaker;
import dashcontrol.utils.DeviceFilter;
import dashcontrol.utils.DeviceStatus;

import static dashcontrol.config.SpeakerConfig.getSoundFileSequence;
import static dashcontrol.utils.Debug.ByteToHexString;
import static dashcontrol.utils.Debug.LOG;
import static dashcontrol.utils.Debug.TAG;
import static org.opencv.core.CvType.CV_8UC3;

public class Dash extends Application {

    SharedPreferences sharedPref;
    protected Handler sleepHandler;


    // 카메라 정보
    protected static final String TAG = "OpenCV";
    protected Mat matInput;
    protected double[] rsltarr = new double[4];

    // 블루투스 객체
    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothGatt mBluetoothGatt;

    protected BluetoothGattService mWWService;
    protected BluetoothGattCharacteristic mWW_Noti, mWW_Noti_Write;
    protected BluetoothGattCharacteristic mWW_Write_Command;
    protected BluetoothGattCharacteristic mWW_Noti_Report1, mWW_Noti_Report2;

    protected static boolean mBluetoothGattBusy = false;

    protected boolean mSearching = false;
    protected int mDeviceStatus = DeviceStatus._IDLE;

    protected Handler mScanHandler;
    protected Handler mDeviceUpdateHandler;
    protected static final long SCAN_PERIOD = 12 * 1000;
    protected Handler mRetryWaitingHandler;
    protected static final long RETRY_WAITING_PERIOD = 5 * 1000;
    protected static final float PATTERN_THRESHOLD = (float) 0.7;
    PowerManager mPowerManager;
    PowerManager.WakeLock mWakeLock;

    MediaPlayer bgm;
    Point d_size;
    Display display;

    int rel_x, rel_y;
    int x, y;

    //Head random object
    Random random = new Random();
    int head_cnt = 0;
    int current_head_cnt = 0;
    int head_cnt_min = 3;
    int head_cnt_max = 8;
    int head_up_min = 2;
    int head_up_max = 10;
    int head_right_min = 5;
    int head_right_max = 20;
    int head_right_angle_max = 80;
    int head_up_angle = 0;
    int head_right_angle = 0;

    void initHeadCnt(){
        head_up_angle = 0;
        head_right_angle = 0;
        head_cnt = random.nextInt(head_cnt_max - head_cnt_min) + head_cnt_min;
    }

    void nextRandomHeadUP(){
        int range = head_up_max - head_up_min;
        int value = random.nextInt(range * 2) - range;
        if(value > 0)
            value += head_up_min;
        else
            value -= head_up_min;
        head_up_angle += value;
        if(head_up_angle > 7) head_up_angle = 0;
        if(head_up_angle < -20) head_up_angle = 0;
    }

    void nextRandomHeadRight(){
        int range = head_right_max - head_right_min;
        int value = random.nextInt(range * 2) - range;
        if(value > 0)
            value += head_right_min;
        else
            value -= head_right_min;
        head_right_angle += value;
        if(head_right_angle > head_right_angle_max) head_right_angle = 0;
        if(head_right_angle < -head_right_angle_max) head_right_angle = 0;
    }

    void callHeadCommand(){
        if(current_head_cnt >= head_cnt){
            current_head_cnt = 0;
            head_cnt = random.nextInt(head_cnt_max - head_cnt_min) + head_cnt_min;
            nextRandomHeadRight();
            nextRandomHeadUP();
            Send_WW_Command(new Head(head_right_angle, head_up_angle).getHead());
        }
        current_head_cnt++;
    }

    //Route param
    protected Mat routeMat = new Mat(400, 400, CV_8UC3);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    public Point convert2cv(View frame, int deltaX, int deltaY){
        x = (frame.getLeft() + frame.getRight()) / 2 + deltaX;
        y = (frame.getBottom() + frame.getTop()) / 2 + deltaY;
        rel_x = x * matInput.cols() / d_size.x;
        rel_y = y * matInput.rows() / d_size.y;
        return new Point(rel_x, rel_y);
    }

    public void onCreate() {
        super.onCreate();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "dashcontrol");

        mScanHandler = new Handler();
        mDeviceUpdateHandler = new Handler();
        mRetryWaitingHandler = new Handler();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        sleepHandler = new Handler();

        WindowManager wm = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);
        display = wm.getDefaultDisplay();
        d_size = new Point();
        display.getSize(d_size);
    }


    public void RunWriteCharacteristic(BluetoothGattCharacteristic writeChar) {
        Boolean ret;
        do {
            ret = mBluetoothGatt.writeCharacteristic(writeChar);
            if (!ret) {
                if (!mBluetoothGattBusy) {
                    Log.e(TAG, "GATT WriteCharacteristic ERROR return = " + ret);
                    return;
                }
                LOG("Write Command return = " + ret);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                mBluetoothGattBusy = true;
            }
        } while (!ret);
    }

    // 로봇에게 명령을 전송하는 기능. 각 기능별 명령 DATA를 생성하여 이 함수를 호출 함. (기본 제어 함수)
    public void Send_WW_Command(byte[] _data) {
        Log.d("send", "ww comand");
        if (_data.length == 0) return;

        if (mDeviceStatus != DeviceStatus._READY) return;

        if (mWW_Write_Command != null) {
            mWW_Write_Command.setValue(_data);
            RunWriteCharacteristic(mWW_Write_Command);
        }
    }

    protected void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    protected void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // For all other profiles, writes the data formatted in HEX.
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length * 3);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(BluetoothConfig.EXTRA_DATA, stringBuilder.toString());
        }
        sendBroadcast(intent);

        checkSensorData(characteristic);

        if (action.equals(BluetoothConfig.ACTION_DATA_AVAILABLE)) {
            // TODO:::
        }
    }

    protected BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            mBluetoothGattBusy = false;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Connected");
                broadcastUpdate(BluetoothConfig.ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Disconnected");
                broadcastUpdate(BluetoothConfig.ACTION_GATT_DISCONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.d(TAG, "GATT Disconnecting");
                broadcastUpdate(BluetoothConfig.ACTION_GATT_DISCONNECTING);
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "Gatt status " + status);
            broadcastUpdate(BluetoothConfig.ACTION_GATT_SERVICES_DISCOVERED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d(TAG, "READ : " + ByteToHexString(characteristic.getValue()) + " Status :" + status);

            mBluetoothGattBusy = false;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.d(TAG, "WRITE : " + ByteToHexString(characteristic.getValue()) + " Status :" + status);

            mBluetoothGattBusy = false;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d(TAG, "NOTIFIED UUID : " + characteristic.getUuid().toString());
            broadcastUpdate(BluetoothConfig.ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            Log.d(TAG, "READ Descriptor : " + ByteToHexString(descriptor.getValue()) + " Status :" + status);

            mBluetoothGattBusy = false;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            Log.d(TAG, "Write Descriptor : " + ByteToHexString(descriptor.getValue()) + " Status :" + status);

            mBluetoothGattBusy = false;
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);

            Log.d(TAG, "Write Completed : " + gatt.getDevice().getName() + " Status :" + status);

            mBluetoothGattBusy = false;
        }

    };

    protected final void checkSensorData(final BluetoothGattCharacteristic characteristic) {
        final byte[] data = characteristic.getValue();

        if (characteristic.getUuid().equals(mWW_Noti_Report1.getUuid())) {
            LOG("Sensor: Button Data = " + new StringBuilder(2).append(String.format("%02X", data[8])));
        } else if (characteristic.getUuid().equals(mWW_Noti_Report2.getUuid())) {
            LOG("Sensor: Right Data = " + new StringBuilder(2).append(String.format("%02X", data[6])));
            LOG("Sensor: Left Data = " + new StringBuilder(2).append(String.format("%02X", data[7])));
            LOG("Sensor: Tail Data = " + new StringBuilder(2).append(String.format("%02X", data[8])));
        }
    }

    public native boolean HSVFilter(long in, long out, double[] array);

    public native void SetRange(int[] min, int[] max);

    public native void GetMin(int[] min);
    public native void GetMax(int[] min);

    public native void TouchCallback(int x, int y);

    public native void LineS2E(long in, long out, double start_x, double start_y, double end_x, double end_y);


    ///////////pattern/////////
    public native void initPatternMatch();
    public native void initParam2Img(long time);
    public native void initNN(String path);
    public native void updateParam2Img(long time, float v, float a);
    public native void getPredicted(String predAlgorithm, float[] confs);
    public native boolean isValidPattern(int idx, float threshold);
    public native boolean isTopPattern(int idx, float threshold);
    public native boolean isTop3(int idx, float threshold);
    public native void getImageFromParam(long input);
    public native int getPointNum();

    //public native void getImageFromParam(Mat img);


    public boolean isTouchInside(View view, int x, int y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int realRight = location[0] + view.getWidth();
        int realBottom = location[1] + view.getHeight();
        return ((x >= location[0] && x <= realRight) && (y >= location[1] && y <= realBottom));
    }

    protected void OutputSound(MediaPlayer mp, String sound) {
        if(mp != null) {
            mp.reset();
        //    mp.release();
        }
        if(sound == "msg1")
            mp = MediaPlayer.create(this, R.raw.msg1);
        else if(sound == "msg2")
            mp = MediaPlayer.create(this, R.raw.msg2);
        else if(sound == "bgm")
            mp = MediaPlayer.create(this,R.raw.bgm);
        else if(sound == "success")
            mp = MediaPlayer.create(this,R.raw.success);
        mp.start();
    }


    public void LoadRange(String path)throws IOException{
        FileInputStream ios = openFileInput(path);
        int[] min = new int[3];
        int[] max = new int[3];
        byte[] tmp = new byte[Integer.SIZE/8];
        for(int i = 0; i<3; i++) {
           ios.read(tmp);
             min[i] = byteArrayToInt(tmp);
         }
          for(int i = 0; i<3; i++) {
              ios.read(tmp);
               max[i] = byteArrayToInt(tmp);
          }
         SetRange(min, max);
         ios.close();
    }

    public void SaveRange(String path, int[] min, int[] max)throws IOException{
        FileOutputStream fos = openFileOutput(path, Context.MODE_PRIVATE);
        for(int i = 0; i<3; i++)
            fos.write(intToByteArray(min[i]));
        for(int i = 0; i<3; i++)
            fos.write(intToByteArray(max[i]));
        fos.close();
    }

    private static byte[] intToByteArray(final int integer){
        ByteBuffer buff = ByteBuffer.allocate(Integer.SIZE/8);
        buff.putInt(integer);
        buff.order(ByteOrder.BIG_ENDIAN);
        return buff.array();
    }

    private static int byteArrayToInt(byte[] bytes){
        final int size = Integer.SIZE/8;
        ByteBuffer buff = ByteBuffer.allocate(size);
        final byte[] newBytes = new byte[size];
        for (int i = 0; i < size; i++){
            if(i + bytes.length < size){
                newBytes[i] = (byte) 0x00;
            }else{
                newBytes[i] = bytes[i + bytes.length - size];
            }
        }
        buff = ByteBuffer.wrap(newBytes);
        buff.order(ByteOrder.BIG_ENDIAN);
        return buff.getInt();
    }
}
