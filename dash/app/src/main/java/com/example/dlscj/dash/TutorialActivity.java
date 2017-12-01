package com.example.dlscj.dash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

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



public class TutorialActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Dash d;

    private CameraBridgeViewBase mOpenCvCameraView;
    ImageButton back, next, exit, refresh;

    private int start_flag = 0;
    private double start_x, start_y, end_x, end_y;
    private double deltaX = 0, deltaY = 0;
    private float rel_x, rel_y;

    Timer timer = new Timer();
    private boolean isPV = false;
    private boolean isIA = false;
    private long startTime = 0;
    private long STAGETIME = 3000;
    private int currentStage = 1;

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        d = (Dash)getApplicationContext();
        back = (ImageButton)findViewById(R.id.backButtont);
        next = (ImageButton)findViewById(R.id.nextButtont);
        exit = (ImageButton)findViewById(R.id.exitButtont);
        refresh = (ImageButton)findViewById(R.id.refreshButtont);

        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        initTut(currentStage);
        timer.schedule(new TimerTask() {
            public void run() {
                Log.d("send", "del,ta : "+deltaX+", "+deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());

                //Tutorial codes
                if (currentStage >= 5){

                }
                else {
                    if (checkTut(currentStage)) {
                        //RIGHT ANSWER
                        //Toast.makeText(getApplicationContext(), "맞았어요! 다음 단계로 넘어갑니다.", Toast.LENGTH_SHORT).show();

                    } else {
                        //WRONG ANSWER
                        //NO ACTION.
                    }
                }

            }
        }, 0, 300 );


        // 이미지 로딩






        //int a = (int)System.currentTimeMillis();
        //float[] conf = null;







        //d.initParam2Img(a, this.getFilesDir().getPath() + "/resDir/");

        //d.updateParam2Img(a + 5, 20, 30 );
        //d.updateParam2Img(a + 10, 10, -40 );
        //d.getPredicted("CNN", conf);
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


                if(d.isTouchInside(exit, (int)rel_x, (int)rel_y)) exitButtontClicked(exit);
                else if(d.isTouchInside(back, (int)rel_x, (int)rel_y)) backButtontClicked(back);
                else if(d.isTouchInside(next, (int)rel_x, (int)rel_y)) nextButtontClicked(next);
            }
            end_x = d.rsltarr[0] + d.rsltarr[2] / 2;
            end_y = d.rsltarr[1] + d.rsltarr[3] / 2;
            Log.d(d.TAG, "end : "+end_x+", "+end_y);

            deltaX = start_x - end_x;
            deltaY = start_y - end_y;

            deltaX /= 5;
            deltaY /= 10;

            if((deltaX > 0 && deltaX <= 3) || (deltaX <= 0 && deltaX >= -3)) deltaX = 0;
            if((deltaY > 0 && deltaY <= 3) || (deltaY <= 0 && deltaY >= -3)) deltaY = 0;


            d.LineS2E(d.matInput.getNativeObjAddr(), d.matInput.getNativeObjAddr(), start_x, start_y, end_x, end_y);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            back.setVisibility(View.INVISIBLE);
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
                            back.setVisibility(View.VISIBLE);
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

    public void backButtontClicked(View v) {
        // 전 이미지 로딩

    }

    public void nextButtontClicked(View v) {
        // 다음 이미지 로딩

    }
    public boolean checkTut(int stage) {
        //Stage 1: 직진 2: 후진 3: 왼쪽 4: 오른쪽
        boolean ret = false;
        if (startTime != 0) {

            isPV &= (deltaY > 0);
            switch (stage) {
                case 1:
                    //직진
                    isIA &= (-20 < deltaX && deltaX < 20);
                    ret = isPV & isIA;
                    break;
                case 2:
                    //후진
                    isIA &= (-20 < deltaX && deltaX < 20);
                    ret = !isPV & isIA;
                    break;
                case 3:
                    //좌회전
                    isIA &= (deltaX > 30);
                    ret = isPV & isIA;
                    break;
                case 4:
                    //우회전
                    isIA &= (deltaX < -30);
                    ret = isPV & isIA;
                    break;
            }
            boolean isOver = STAGETIME < System.currentTimeMillis() - startTime;
            Log.d("TUTORIAL",String.valueOf(isOver) +" " + String.valueOf(ret) + " " + currentStage);
            if (isOver & ret) {
                Log.d("TUTORIAL","GO TO NEXT STAGE");
                //타임오버, 맞음 -> 다음 스테이지로
                ret = true;
                if (currentStage < 4) {
                    currentStage += 1;
                    initTut(stage + 1);
                }
                else{

                }
            } else if (!isOver & ret) {
                Log.d("TUTORIAL","ING... 2");
                //중간에 옳게 진행 중 -> 일단 진행
                ret = false;
            } else if (isOver & !ret) {
                Log.d("TUTORIAL","WRONG / RESTART");
                //타임오버, 틀림 -> 틀림 / 재시작
                ret = false;
                initTut(stage);
            } else if (!isOver & !ret) {
                Log.d("TUTORIAL","WRONG / RESTART 4");
                //중간에 틀림 -> 현재 스테이지 재시작
                ret = false;
                initTut(stage);
            }
        }
        return ret;
    }

    public void initTut(int stage){
        startTime = System.currentTimeMillis();
        switch (stage){
            case 1:
                isPV = true;
                break;
            case 2:
                isPV = false;
                break;
            case 3:
                isPV = true;
                break;
            case 4:
                isPV = true;
                break;
        }
        isIA = true;
    }
}