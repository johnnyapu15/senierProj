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

    }

}
