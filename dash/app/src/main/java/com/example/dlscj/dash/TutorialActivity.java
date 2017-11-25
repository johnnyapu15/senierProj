package com.example.dlscj.dash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by dlscj on 2017-11-19.
 */



public class TutorialActivity extends AppCompatActivity {

    Dash d;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        d = (Dash)getApplicationContext();

        int a = (int)System.currentTimeMillis();
        float[] conf = null;

        //d.initParam2Img(a, this.getFilesDir().getPath() + "/resDir/");

        //d.updateParam2Img(a + 5, 20, 30 );
        //d.updateParam2Img(a + 10, 10, -40 );
        //d.getPredicted("CNN", conf);
    }
}
