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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        d = (Dash)getApplicationContext();


        //JJA
        d.initParam2Img(time, this.getFilesDir().getPath());

        //d.updateParam2Img(a + 5, 20, 30 );
        //d.updateParam2Img(a + 10, 10, -40 );
        //d.getPredicted("CNN", conf);


        //Timer for dash control signal

    }

}
