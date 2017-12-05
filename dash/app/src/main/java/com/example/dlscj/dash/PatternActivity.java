package com.example.dlscj.dash;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.util.Timer;
import java.util.TimerTask;

import dashcontrol.control.BodyLinearAngular;

import static dashcontrol.utils.Debug.TAG;

/**
 * Created by dlscj on 2017-11-19.
 */



public class PatternActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Dash d;
    private CameraBridgeViewBase mOpenCvCameraView;
    ImageButton next, exit;
    ImageView t, note;
    GlideDrawableImageViewTarget imgt;


    private int start_flag = 0;
    private double start_x, start_y, end_x, end_y;
    private double deltaX = 0, deltaY = 0;
    private float rel_x, rel_y;

    Timer timer = new Timer();
    private boolean isNext = false;
    private boolean stageNext = false;
    private long DETECTION_START = 0;
    private long DETECTION_INTERVAL = 2000;
    private int currentStage = 1;
    private int FINAL_STAGE = 5;
    private int[] stagePatternIdx = {3,2,1};
    private double PATTERN_THRESHOLD = 1500;
    private float[] confidence = new float[6];


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern);

        d = (Dash) getApplicationContext();
        next = (ImageButton) findViewById(R.id.nextButtont);
        exit = (ImageButton) findViewById(R.id.exitButtont);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);


        note = (ImageView) findViewById(R.id.note);
        note.setImageAlpha(70);
        t = (ImageView) findViewById(R.id.tvimg);
        imgt = new GlideDrawableImageViewTarget(t);
        t.setVisibility(View.INVISIBLE);
        d.initPatternMatch();
        d.initNN(this.getFilesDir().getPath());
        d.initParam2Img(System.currentTimeMillis());

        d.sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t.setVisibility(View.VISIBLE);
                set_gif();
            }
        }, 1000);


        timer.schedule(new TimerTask() {
            public void run() {
                Log.d("send", "del,ta : " + deltaX + ", " + deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());
                d.updateParam2Img(System.currentTimeMillis(), (float)deltaY, (float)deltaX);
                //Pattern codes
                if (currentStage > FINAL_STAGE) {
                    finish();
                } else {
                    //JJA: 인식 취소가 적정 인터벌이상 지속된 경우, 패턴 인식을 진행한다.
                    if (System.currentTimeMillis() - DETECTION_START > DETECTION_INTERVAL) {
                        //1. 패턴 인식으로 현재 스테이지와 맞는 지 불린값 체크
                        //2. 불린값이 true -> Next stage
                        //            false-> Process as is & DETECTIONSTART = System.currentTimeMillis();
                        if (isNext) {
                            d.getPredicted("CNN", confidence);
                            if (d.isValidPattern(stagePatternIdx[currentStage], (float)PATTERN_THRESHOLD)) {
                                //RIGHT ANSWER
                                isNext = false;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), "맞았어요! 다음 단계로 넘어갑니다.", Toast.LENGTH_SHORT).show();
                                                d.sleepHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        next.setVisibility(View.VISIBLE);
                                                        exit.setVisibility(View.VISIBLE);
                                                        note.setVisibility(View.VISIBLE);
                                                        t.setVisibility(View.VISIBLE);
                                                        set_gif();
                                                        start_flag = 0;
                                                    }
                                                }, 2000);
                                            }
                                        });
                                    }
                                }).start();
                            } else {
                                //WRONG ANSWER
                                DETECTION_START = System.currentTimeMillis();
                                d.sleepHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        next.setVisibility(View.INVISIBLE);
                                        exit.setVisibility(View.INVISIBLE);
                                        note.setVisibility(View.INVISIBLE);
                                        t.setVisibility(View.INVISIBLE);
                                    }
                                }, 1000);
                            }
                        }
                    }
                }
            }
        }, 0, 300);

    }


    @Override
    public void onPause() {
        super.onPause();
        mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(d.TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(d.TAG, "onResume :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
        timer.cancel();
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        super.onTouchEvent(event);

        //event
        //event 종류/각각의 특성

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            int x = (int) (event.getX() * d.matInput.cols() / d.d_size.x);
            int y = (int) (event.getY() * d.matInput.rows() / d.d_size.y);

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

        if (d.HSVFilter(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), d.rsltarr)) {
            //버튼 투명하게!, 초깃값 설정
            if (start_flag == 0) {
                start_x = d.rsltarr[0] + d.rsltarr[2] / 2;
                start_y = d.rsltarr[1] + d.rsltarr[3] / 2;
                start_flag = 1;
                Log.d(TAG, "start : " + start_x + ", " + start_y);


                rel_x = (float) start_x * d.d_size.x / d.matInput.cols();
                rel_y = (float) start_y * d.d_size.y / d.matInput.rows();


                if (d.isTouchInside(exit, (int) rel_x, (int) rel_y)) exitButtontClicked(exit);
                else if (d.isTouchInside(next, (int) rel_x, (int) rel_y)) nextButtontClicked(next);
            }
            end_x = d.rsltarr[0] + d.rsltarr[2] / 2;
            end_y = d.rsltarr[1] + d.rsltarr[3] / 2;
            Log.d(d.TAG, "end : " + end_x + ", " + end_y);

            deltaX = start_x - end_x;
            deltaY = start_y - end_y;

            deltaX /= 5;
            deltaY /= 10;

            if ((deltaX > 0 && deltaX <= 3) || (deltaX <= 0 && deltaX >= -3)) deltaX = 0;
            if ((deltaY > 0 && deltaY <= 3) || (deltaY <= 0 && deltaY >= -3)) deltaY = 0;


            d.LineS2E(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), start_x, start_y, end_x, end_y);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            next.setVisibility(View.INVISIBLE);
                            exit.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }).start();
        } else {
            //JJA
            //인식 취소가 시작되었을 때의 시간 기록
            if (start_flag != 0)
                DETECTION_START = System.currentTimeMillis();


            start_flag = 0;
            deltaX = 0;
            deltaY = 0;


            //현재 간헐적으로 발생하는 뷰 종료->부적절한 쓰레드 오류 이슈를 이 부분에서 해결할 수 있을 것 같다.
            //이때 생성한 쓰레드를 전역변수로 저장해서 해당 쓰레드를 기억한다면 해결 가능할 것.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            next.setVisibility(View.VISIBLE);
                            exit.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
        }


        return d.matInput;
    }

    public void exitButtontClicked(View v) {
        this.finish();
    }

    public void nextButtontClicked(View v) {
        invs_img();
        isNext = true;
    }

    public void invs_img() {
        t.setVisibility(View.INVISIBLE);
        d.initParam2Img(System.currentTimeMillis());
    }

    public void set_gif() {
        switch (currentStage) {
            case 1:
                t.setImageResource(R.drawable.tu1);
                Glide.with(this).load(R.drawable.tu1).into(imgt);
                break;
            case 2:
                t.setImageResource(R.drawable.tu2);
                Glide.with(this).load(R.drawable.tu2).into(imgt);
                break;
            case 3:
                t.setImageResource(R.drawable.tu3);
                Glide.with(this).load(R.drawable.tu3).into(imgt);
                break;
            case 4:
                t.setImageResource(R.drawable.tu4);
                Glide.with(this).load(R.drawable.tu4).into(imgt);
                break;
        }
    }
}
