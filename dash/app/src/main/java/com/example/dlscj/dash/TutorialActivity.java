package com.example.dlscj.dash;

import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
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
import dashcontrol.control.Speaker;

import static dashcontrol.config.SpeakerConfig.getSoundFileSequence;
import static dashcontrol.utils.Debug.TAG;

/**
 * Created by dlscj on 2017-11-19.
 */



public class TutorialActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Dash d;
    private CameraBridgeViewBase mOpenCvCameraView;
    ImageButton next, exit;
    ImageView t, note;
    GlideDrawableImageViewTarget imgt;

    private MediaPlayer mp;

    private int start_flag = 0;
    private double start_x, start_y, end_x, end_y;
    private double deltaX = 0, deltaY = 0;

    Timer timer = new Timer();
    private boolean isPV = false;
    private boolean isIA = false;
    private boolean isOK = false;
    private boolean stageNext = false;
    private long startTime = 0;
    private long STAGETIME = 3000;
    private int currentStage = 1;

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

        if(d.bgm!=null && d.bgm.isPlaying() == false) {
            d.bgm.start();
        }
        note = (ImageView) findViewById(R.id.note);
        note.setImageAlpha(70);

        t = (ImageView) findViewById(R.id.tvimg);
        t.setVisibility(View.INVISIBLE);
        imgt = new GlideDrawableImageViewTarget(t);

        initTut();
        d.sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t.setVisibility(View.VISIBLE);
                set_gif();
            }
        }, 1000);


        d.initHeadCnt();
        timer.schedule(new TimerTask() {
            public void run() {
                d.callHeadCommand();
                Log.d("send", "del,ta : " + deltaX + ", " + deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());

                //Tutorial codes
                if (currentStage >= 5) {
                    d.OutputSound(mp, "success");
                    //Toast.makeText(getApplicationContext(), "연습이 끝났습니다!", Toast.LENGTH_SHORT).show();
                    d.sleepHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {finish();
                        }
                    }, 2000);
                } else {
                    if (isOK && checkTut(currentStage)) {
                        //RIGHT ANSWER
                        isOK = false;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        d.Send_WW_Command(new Speaker(getSoundFileSequence()).getSpeaker());
                                        d.OutputSound(mp, "success");
                                        Toast.makeText(getApplicationContext(), "맞았어요! 다음 단계로 넘어갑니다.", Toast.LENGTH_SHORT).show();
                                        d.sleepHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                next.setVisibility(View.VISIBLE);
                                                exit.setVisibility(View.VISIBLE);
                                                note.setVisibility(View.VISIBLE);
                                                t.setVisibility(View.VISIBLE);
                                                set_gif();
                                            }
                                        }, 2000);
                                    }
                                });
                            }
                        }).start();
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

        if(isOK == true) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            next.setVisibility(View.INVISIBLE);
                            //exit.setVisibility(View.INVISIBLE);
                            note.setVisibility(View.INVISIBLE);
                            t.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }).start();
        }

        if (d.HSVFilter(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), d.rsltarr)) {
            //버튼 투명하게!, 초깃값 설정
            if (start_flag == 0) {
                start_x = d.rsltarr[0] + d.rsltarr[2] / 2;
                start_y = d.rsltarr[1] + d.rsltarr[3] / 2;
                if(isOK == true)
                    start_flag = 1;
                Log.d(TAG, "start : " + start_x + ", " + start_y);

                d.getRealPose();

                if (d.isTouchInside(exit, d.rel_x, d.rel_y)) exitButtontClicked(exit);
                else if (d.isTouchInside(next, d.rel_x, d.rel_y)) nextButtontClicked(next);
            }
            if(isOK == false)
                start_flag = 0;
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
                                exit.setVisibility(View.VISIBLE);
                                if((isOK == false) & (next.getVisibility() == View.INVISIBLE))
                                    next.setVisibility(View.VISIBLE);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        note.setVisibility(View.INVISIBLE);
                        t.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
        initTut();
        isOK = true;
    }

    public boolean checkTut(int stage) {
        //Stage 1: 직진 2: 후진 3: 왼쪽 4: 오른쪽
        boolean ret = false;
        if (startTime != 0) {


            switch (stage) {
                case 1:
                    //직진
                    isPV &= (deltaY > 5);
                    isIA &= (-20 < deltaX && deltaX < 20);
                    ret = isPV && isIA;
                    break;
                case 2:
                    //후진
                    isPV &= (deltaY < -5);
                    isIA &= (-20 < deltaX && deltaX < 20);
                    ret = isPV && isIA;
                    break;
                case 3:
                    //좌회전
                    isPV &= (deltaY > 5);
                    isIA &= (deltaX > 30);
                    ret = isPV && isIA;
                    break;
                case 4:
                    //우회전
                    isPV &= (deltaY > 5);
                    isIA &= (deltaX < -30);
                    ret = isPV && isIA;
                    break;
            }
            boolean isOver = STAGETIME < System.currentTimeMillis() - startTime;
            Log.d("TUTORIAL", "isOver?: " + String.valueOf(isOver) + " isRight?: " + String.valueOf(ret) + " Current stage: " + currentStage);
            if (isOver & ret) {
                Log.d("TUTORIAL", "GO TO NEXT STAGE");
                //타임오버, 맞음 -> 다음 스테이지로
                ret = true;
                //if (currentStage < 4) {
                    currentStage += 1;
                    initTut();
                //} else {

                //}
            } else if (!isOver & ret) {
                Log.d("TUTORIAL", "ING...");
                //중간에 옳게 진행 중 -> 일단 진행
                ret = false;
            } else if (isOver & !ret) {
                Log.d("TUTORIAL", "WRONG / RESTART");
                //타임오버, 틀림 -> 틀림 / 재시작
                ret = false;
                initTut();
            } else if (!isOver & !ret) {
                Log.d("TUTORIAL", "WRONG / RESTART");
                //중간에 틀림 -> 현재 스테이지 재시작
                ret = false;
                initTut();
            }
        }
        return ret;
    }

    public void initTut() {
        startTime = System.currentTimeMillis();
        isPV = true;
        isIA = true;
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
