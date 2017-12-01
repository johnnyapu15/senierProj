package com.example.dlscj.dash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.sleep;


public class InitActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    MediaPlayer mp1;
    MediaPlayer mp2;
    String msg1 = "msg1";
    String msg2 = "msg2";
    String msg3 = "msg3";
    Dash d;
    ImageView framec;

    private CameraBridgeViewBase mOpenCvCameraView;

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
        setContentView(R.layout.activity_init);

        d = (Dash) getApplication();
        framec = (ImageView)findViewById(R.id.frame);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
        super.onDestroy();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        initStart();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        d.matInput = inputFrame.rgba();


        if(d.HSVFilter(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), d.rsltarr)) {
            //버튼 투명하게!, 초깃값 설정

        }
        return d.matInput;
    }

    public void initStart() {

        d.sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Point cv_coord = d.convert2cv(framec, 0, 0);

                d.TouchCallback(cv_coord.x, cv_coord.y);
                Animation anim= AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move);
                framec.startAnimation(anim);
                anim.setFillEnabled(true);
                anim.setFillAfter(true);

                d.sleepHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Point cv_coord = d.convert2cv(framec, -450, -150);

                        d.TouchCallback(cv_coord.x, cv_coord.y);
                        d.sleepHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Point cv_coord = d.convert2cv(framec, 450, -150);

                                d.TouchCallback(cv_coord.x, cv_coord.y);
                                d.sleepHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Point cv_coord = d.convert2cv(framec, 450, 150);

                                        d.TouchCallback(cv_coord.x, cv_coord.y);
                                        d.sleepHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Point cv_coord = d.convert2cv(framec, -450, 150);

                                                d.TouchCallback(cv_coord.x, cv_coord.y);

                                                d.sleepHandler.postDelayed(new Runnable() {
                                                                               @Override
                                                                               public void run() {
                                                                                   Intent intent = new Intent(InitActivity.this, MainActivity.class);
                                                                                   startActivity(intent);
                                                                                   InitActivity.this.finish();
                                                                               }
                                                                           }, 1000);
                                            }
                                        }, 4000);
                                    }
                                }, 4000);
                            }
                        }, 4000);
                    }
                }, 4000);
            }
        }, 8000);

        Toast.makeText(getApplicationContext(), "초록색 원 정중앙에 물체를 올리고 원을 따라가주세요. ", Toast.LENGTH_SHORT).show();

       d.OutputSound(mp1, msg1);
        Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();

        //TODO : 적절한 delay값 입력해야함
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                d.OutputSound(mp2, msg2);
            }
        },8000);

        //Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();


        //Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();

        //Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();

        //Toast.makeText(getApplicationContext(), "3", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "2", Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), "1", Toast.LENGTH_SHORT).show();

        //editor.putInt(getString(R.string.init), new_init);
        //editor.commit();
        //view.getLocationOnScreen(Location);

        //startActivity(new Intent(InitActivity.this, MainActivity.class));
        //finish();
    }


}
