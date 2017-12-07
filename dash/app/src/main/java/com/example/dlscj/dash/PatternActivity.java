package com.example.dlscj.dash;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
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



public class PatternActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Dash d;
    private CameraBridgeViewBase mOpenCvCameraView;
    ImageButton next, exit, refresh;
    ImageView t, note, minimap;
    Bitmap m;
    GlideDrawableImageViewTarget imgt;

    private MediaPlayer mp;

    private int start_flag = 0;
    private double start_x, start_y, end_x, end_y;
    private double deltaX = 0, deltaY = 0;

    Timer timer = new Timer();
    private boolean isOK = false;
    private boolean stageNext = false;
    private long CNN_START = 0;
    private long DETECTION_INTERVAL = 2000;
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
    private double PATTERN_THRESHOLD = 2500;
    private float[] confidence = new float[7];


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
        next = (ImageButton) findViewById(R.id.nextButtonp);
        exit = (ImageButton) findViewById(R.id.exitButtonp);
        refresh = (ImageButton) findViewById(R.id.refreshButtonp);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        note = (ImageView) findViewById(R.id.notep);
        note.setImageAlpha(70);
        t = (ImageView) findViewById(R.id.tvimgp);

        minimap = (ImageView) findViewById(R.id.minimapp);
        minimap.setImageAlpha(50);


        imgt = new GlideDrawableImageViewTarget(t);
        t.setVisibility(View.INVISIBLE);
        d.initPatternMatch();
        d.initNN(this.getFilesDir().getPath());
        d.initParam2Img(System.currentTimeMillis());
        CNN_START = System.currentTimeMillis();
        d.sleepHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t.setVisibility(View.VISIBLE);
                set_gif();
            }
        }, 1000);

        if(d.bgm!=null && d.bgm.isPlaying() == false) {
            d.bgm.start();
        }

        d.initHeadCnt();

        timer.schedule(new TimerTask() {
            public void run() {
                d.callHeadCommand();
                Log.d("PATTERN", "delta : " + (float) (deltaY) + ", " + (float)( - deltaX));
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());
                d.updateParam2Img(System.currentTimeMillis(), (float) (deltaY / 1.3), (float)( - deltaX / 1.6));
                d.getImageFromParam(d.routeMat.getNativeObjAddr());
                m = Bitmap.createBitmap(d.routeMat.cols(), d.routeMat.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(d.routeMat, m);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                minimap.setImageBitmap(m);
                            }
                        });
                    }
                }).start();


                //Pattern-game codes
                if (currentStage > FINAL_STAGE) {
                    d.OutputSound(mp, "success");
                    //Toast.makeText(getApplicationContext(), "성공했습니다!", Toast.LENGTH_SHORT).show();
                    d.sleepHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {finish();
                        }
                    }, 2000);
                } else {
                    //JJA: 인식 취소가 적정 인터벌이상 지속된 경우, 패턴 인식을 진행한다.
                    if (System.currentTimeMillis() - CNN_START > DETECTION_INTERVAL) {
                        //1. 패턴 인식으로 현재 스테이지와 맞는 지 불린값 체크
                        //2. 불린값이 true -> Next stage
                        //            false-> Process as is & DETECTIONSTART = System.currentTimeMillis();
                        if (isOK) {
                            d.getPredicted("CNN", confidence);
                            //d.getImageFromParam(d.routeMat.getNativeObjAddr());
                            Log.d("PATTERN",   "Current stage: " + stagePatternIdx[currentStage - 1] + ", " + stageStr[currentStage - 1]);
                            Log.d("PATTERN", "Confidences: " + String.valueOf(confidence[0])+ " " + String.valueOf(confidence[1])+ " " + String.valueOf(confidence[2])+ " " + String.valueOf(confidence[3])+ " " + String.valueOf(confidence[4])+ " " + String.valueOf(confidence[5])+ " " + String.valueOf(confidence[6]));

                            if (d.isTop3(stagePatternIdx[currentStage - 1], (float) PATTERN_THRESHOLD)) {
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
                                                currentStage++;
                                                d.sleepHandler.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        new Thread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        next.setVisibility(View.VISIBLE);
                                                                        exit.setVisibility(View.VISIBLE);
                                                                        note.setVisibility(View.VISIBLE);
                                                                        t.setVisibility(View.VISIBLE);
                                                                        refresh.setVisibility(View.VISIBLE);
                                                                        minimap.setVisibility(View.VISIBLE);
                                                                        set_gif();
                                                                    }
                                                                });
                                                            }
                                                        }).start();
                                                        start_flag = 0;
                                                    }
                                                }, 2000);
                                            }
                                        });
                                    }
                                }).start();
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


          if((isOK == true) & (next.getVisibility() == View.VISIBLE)) {
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
                            //refresh.setVisibility(View.INVISIBLE);
                            minimap.setVisibility(View.VISIBLE);
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
                start_flag = 1;
                Log.d(TAG, "start : " + start_x + ", " + start_y);

               d.getRealPose(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight());

                if (d.isTouchInside(exit, d.rel_x, d.rel_y)) exitButtonpClicked(exit);
                else if (d.isTouchInside(next, d.rel_x, d.rel_y)) nextButtonpClicked(next);
                else if (d.isTouchInside(refresh, d.rel_x, d.rel_y)) refreshButtonpClicked(refresh);
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
                            refresh.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }).start();
            CNN_START = System.currentTimeMillis();
        } else {
            start_flag = 0;
            deltaX = 0;
            deltaY = 0;
/*
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            next.setVisibility(View.VISIBLE);
                            exit.setVisibility(View.VISIBLE);
                            refresh.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
            */

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if((isOK == false) & (next.getVisibility() == View.INVISIBLE))
                                    next.setVisibility(View.VISIBLE);
                                exit.setVisibility(View.VISIBLE);
                                refresh.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }).start();


        }


        return d.matInput;
    }

    public void exitButtonpClicked(View v) {
        this.finish();
    }

    public void nextButtonpClicked(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        t.setVisibility(View.INVISIBLE);
                        note.setVisibility(View.INVISIBLE);
                    }
                });
            }
        }).start();
        d.initParam2Img(System.currentTimeMillis());
        CNN_START = System.currentTimeMillis();
        Log.d("PATTERN", "INIT Pattern...getPointNum: " + String.valueOf(d.getPointNum()));
        isOK = true;
    }

    public void refreshButtonpClicked(View v) {
        d.initParam2Img(System.currentTimeMillis());
        Log.d("PATTERN", "새로고침");
    }



    public void set_gif() {
        switch (currentStage) {
            case 1:
                t.setImageResource(R.drawable.circle1);
                Glide.with(this).load(R.drawable.circle1).into(imgt);
                break;
            case 2:
                t.setImageResource(R.drawable.letterl2);
                Glide.with(this).load(R.drawable.letterl2).into(imgt);
                break;
            case 3:
                t.setImageResource(R.drawable.s3);
                Glide.with(this).load(R.drawable.s3).into(imgt);
                break;
            case 4:
                t.setImageResource(R.drawable.rs4);
                Glide.with(this).load(R.drawable.rs4).into(imgt);
                break;
            case 5:
                t.setImageResource(R.drawable.lettern5);
                Glide.with(this).load(R.drawable.lettern5).into(imgt);
                break;
            case 6:
                t.setImageResource(R.drawable.rect6);
                Glide.with(this).load(R.drawable.rect6).into(imgt);
                break;
        }
    }


}
