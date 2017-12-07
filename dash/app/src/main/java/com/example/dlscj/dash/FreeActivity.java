package com.example.dlscj.dash;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import dashcontrol.config.BluetoothConfig;
import dashcontrol.config.SpeakerConfig;
import dashcontrol.control.BodyLinearAngular;

import static dashcontrol.utils.Debug.TAG;


public class FreeActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Dash d;

    private CameraBridgeViewBase mOpenCvCameraView;

    private int start_flag = 0;
    private double start_x, start_y, end_x, end_y;
    private double deltaX = 0, deltaY = 0;
    private int head_cnt = 0;

    ImageButton back;

    Timer timer = new Timer();


    float rel_x, rel_y;

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
        setContentView(R.layout.activity_free);

        d = (Dash)getApplicationContext();

        back = (ImageButton) findViewById (R.id.backButton);
        back.setVisibility(View.VISIBLE);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        if(d.bgm!=null && d.bgm.isPlaying() == false) {
            d.bgm.start();
        }

        d.initHeadCnt();
        timer.schedule(new TimerTask() {
            public void run() {
                d.callHeadCommand();
                Log.d("send", "delta : "+deltaX+", "+deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());
            }
        }, 0, 300 );

    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(d.TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(d.TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        super.onTouchEvent(event);

        //event
        //event 종류/각각의 특성

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            int x = (int)(event.getX() * d.matInput.cols() / d.d_size.x);
            int y = (int)(event.getY() * d.matInput.rows() / d.d_size.y);

            d.TouchCallback(x, y);

            return true;
        }
        return false;
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

        if(d.HSVFilter(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), d.rsltarr)) {
            //버튼 투명하게!, 초깃값 설정
            if(start_flag == 0) {
                start_x = d.rsltarr[0] + d.rsltarr[2] / 2;
                start_y = d.rsltarr[1] + d.rsltarr[3] / 2;
                start_flag = 1;
                Log.d(TAG, "start : "+start_x+", "+start_y);


                rel_x = (float)start_x * d.d_size.x / d.matInput.cols();
                rel_y = (float)start_y * d.d_size.y / d.matInput.rows();


                if(d.isTouchInside(back, (int)rel_x, (int)rel_y)) backButtonClicked(back);
            }
            end_x = d.rsltarr[0] + d.rsltarr[2] / 2;
            end_y = d.rsltarr[1] + d.rsltarr[3] / 2;
            Log.d(d.TAG, "end : "+end_x+", "+end_y);

            deltaX = start_x - end_x;
            deltaY = start_y - end_y;

            deltaX /= 10;
            deltaY /= 5;

            if((deltaX > 0 && deltaX <= 3) || (deltaX <= 0 && deltaX >= -3))
                deltaX = 0;
            if((deltaY > 0 && deltaY <= 3) || (deltaY <= 0 && deltaY >= -3))
                deltaY = 0;


            d.LineS2E(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), start_x, start_y, end_x, end_y);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            back.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }).start();
        } else {
            start_flag = 0;
            deltaX = 0;
            deltaY = 0;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            back.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
        }
        return d.matInput;
    }


    public void backButtonClicked(View v) {
        timer.cancel();
        this.finish();
    }

}
