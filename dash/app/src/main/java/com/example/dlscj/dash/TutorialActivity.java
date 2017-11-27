package com.example.dlscj.dash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by dlscj on 2017-11-19.
 */



public class TutorialActivity extends AppCompatActivity {

    Dash d;
     //JJA
    private int time = (int)System.currentTimeMillis();
    private float[] conf = new float[6];
    private

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        d = (Dash)getApplicationContext();


        //JJA
        d.initParam2Img(time, this.getFilesDir().getPath());

        //d.updateParam2Img(a + 5, 20, 30 );
        //d.updateParam2Img(a + 10, 10, -40 );
        //d.getPredicted("CNN", conf);


        //Timer for dash control signal
        timer.schedule(new TimerTask() {
            public void run() {
                Log.d("send", "delta : "+deltaX+", "+deltaY);
                d.Send_WW_Command(new BodyLinearAngular(deltaX, deltaY).getBodyLinearAngular());
                //JJA, param : cm/sec, degree/sec
                d.updateParam2Img((int)System.currentTimeMiilis(), deltaY, deltaX );


                )
            }
        }, 0, 1000 );

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


                if(d.isTouchInside(back, (int)rel_x, (int)rel_y))
                    backButtonClicked(back);
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

}
