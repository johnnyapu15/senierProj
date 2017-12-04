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



public class tmpPatternActivity extends AppCompatActivity
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
    private boolean isPV = false;
    private boolean isIA = false;
    private boolean isOK = false;
    private boolean stageNext = false;
    private long startTime = 0;
    private long STAGETIME = 500; //올바르게 패턴을 그린 후 .5초 후 진행
    private int currentStage = 1;
    private int FINAL_STAGE = 6;
        /* 
        The CNN model trained: "CIRCLE"-0, "N"-1, "L"-2, "RECT"-3, "RS"-4, "S"-5, "INTERMEDIATE"-6
        Pattern game:
            Stage 1: CIRCLE
            Stage 2: L
            Stage 3: S
            Stage 4: RS
            Stage 5: N
            Stage 6: RECTANGLE
        */
    private int[] stagePatternIdx = {0, 2, 5, 4, 1, 3}; //usage: stagePatternIdx[currentStage] -> currentPattern
    private String[] stageStr = {"원", "기역", "S", "거꾸로 S", "N", "사각형"};
    private float THRESHOLD = 1500;

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
        setContentView(R.layout.activity_tutorial);

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

        startTime = System.currentTimeMillis();
        d.sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t.setVisibility(View.VISIBLE);
                set_gif();
            }
        }, 1000);


        timer.schedule(new TimerTask() {
            public void run() {
                Log.d("send", "delta : " + deltaX + ", " + deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());

                //Pattern-game codes
                if (currentStage > FINAL_STAGE) {
                    finish();
                } else {
                    if (isOK && checkPat(currentStage)) {
                        //RIGHT ANSWER
                        isOK = false;
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
                    } else if (isOK && !checkPat(currentStage)) {
                        d.sleepHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                next.setVisibility(View.INVISIBLE);
                                exit.setVisibility(View.INVISIBLE);
                                note.setVisibility(View.INVISIBLE);
                                t.setVisibility(View.INVISIBLE);
                            }
                        }, 1000);

                    } else {
                        //WRONG ANSWER
                        //NO ACTION.
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
            start_flag = 0;
            deltaX = 0;
            deltaY = 0;

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
        isOK = true;
    }

    //새로고침 버튼에 추가할 코드
    //d.initParam2Img(System.currentTimeMillis());

    //이미지를 띄울 위치에 추가할 코드
    //Mat patternImage;
    //d.getImgFromParam(patternImage);

    public boolean checkPat(int stage) {
        /*
        Stage 1: CIRCLE
        Stage 2: L
        Stage 3: S
        Stage 4: RS
        Stage 5: N
        Stage 6: RECTANGLE
        */
        boolean ret = false;
        if (startTime != 0) {


            ret = d.isValidPattern(stagePatternIdx[stage], THRESHOLD);

            boolean atInterval = STAGETIME < System.currentTimeMillis() - startTime;
            Log.d("PATTERN", "atInterval?: " + String.valueOf(atInterval) + " isRight?: " + String.valueOf(ret) + " Current stage: " + currentStage + ", " + stageStr[currentStage]);
            if (atInterval & ret) {
                Log.d("PATTERN", "GO TO NEXT STAGE");
                //타임오버, 맞음 -> 다음 스테이지로
                ret = true;
                currentStage += 1;
                startTime = System.currentTimeMillis();

            } else if (!atInterval & ret) {
                Log.d("PATTERN", "ING...");
                //중간에 옳게 진행 중 -> 일단 진행
                ret = false;
            } else if (atInterval & !ret) {
                Log.d("PATTERN", "WRONG / RESTART");
                //타임오버, 틀림 -> 틀림 / 재시작
                ret = false;
                startTime = System.currentTimeMillis();
            } else if (!atInterval & !ret) {
                Log.d("PATTERN", "WRONG / RESTART");
                //중간에 틀림 -> 현재 스테이지 재시작
                ret = false;
                startTime = System.currentTimeMillis();
            }
        }
        return ret;
    }

    public void initPat(int stage) {
        d.initParam2Img(System.currentTimeMillis());
    }

    public void invs_img() {
        t.setVisibility(View.INVISIBLE);
        startTime = System.currentTimeMillis();
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
