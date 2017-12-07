package com.example.dlscj.dash;

import android.content.DialogInterface;
import android.renderscript.ScriptGroup;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.Manifest;
import android.net.Uri;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import android.util.Log;

import org.opencv.core.Mat;

import static org.opencv.core.CvType.CV_8UC3;


public class SplashActivity extends AppCompatActivity {

    Dash d;

    private static final int REQUEST_FINE_LOCATION = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_READ_FILE = 102;
    private static final int REQUEST_WRITE_FILE = 103;
///////////////////////////////
    public Mat m = new Mat( 400, 400, CV_8UC3);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        d = (Dash) getApplication();

        initPermission();

        dialogInit();

        //File copy to local drive
        String localPath = this.getFilesDir().getPath();
        try{
            CopyIfNotExist(R.raw.patternm, localPath + "/pattern.caffemodel");
            CopyIfNotExist(R.raw.patternt, localPath + "/pattern.prototxt");

        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void CopyIfNotExist(int res, String target) throws IOException
    {
        File targetFile = new File(target);

        if(!targetFile.exists()){
            CopyFromPack(res, targetFile.getName());
        }
    }

    public void CopyFromPack(int res, String target) throws IOException
    {
        FileOutputStream IOS = openFileOutput(target, Context.MODE_PRIVATE);
        InputStream IIS = getResources().openRawResource(res);
        int readByte;
        byte[] buff = new byte[2048000];

        while((readByte = IIS.read(buff)) != -1)
        {
            IOS.write(buff, 0, readByte);
        }
        IOS.flush();
        IOS.close();
        IIS.close();
    }


    private void initPermission() {
        int i;

        for(i = 0; i < 2; i++) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(getApplicationContext(), "동의를 해주셔야 실행 가능합니다.", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA);
                } else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA);
            } else break;
        }

        for(i = 0; i < 2; i++) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(getApplicationContext(), "동의를 해주셔야 실행 가능합니다.", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FINE_LOCATION);
                } else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FINE_LOCATION);
            } else break;
        }

        for(i = 0; i < 2; i++) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(getApplicationContext(), "동의를 해주셔야 실행 가능합니다.", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_READ_FILE);
                } else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_READ_FILE);
            } else break;
        }

        for(i = 0; i < 2; i++) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(getApplicationContext(), "동의를 해주셔야 실행 가능합니다.", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_FILE);
                } else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_FILE);
            } else break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(d.TAG, ">>> 동의함.");
                } else {
                    Log.e(d.TAG, ">>> 동의하셔야 함.");
                }
            }
            case REQUEST_CAMERA: {
                if(grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(d.TAG, ">>> 동의함.");
                } else {
                    Log.e(d.TAG, ">>> 동의하셔야 함.");
                }
            }

            case REQUEST_READ_FILE: {
                if(grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(d.TAG, ">>> 동의함.");
                } else {
                    Log.e(d.TAG, ">>> 동의하셔야 함.");
                }
            }

            case REQUEST_WRITE_FILE: {
                if(grantResults.length > 0 && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                    Log.e(d.TAG, ">>> 동의함.");
                } else {
                    Log.e(d.TAG, ">>> 동의하셔야 함.");
                }
            }
        }
    }

    public void dialogInit() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("초기화");
        dialog
                .setMessage("초기화를 진행하시겠습니까?")
                .setCancelable(false)
                .setNegativeButton("네",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                startActivity(new Intent(SplashActivity.this, InitActivity.class));
                                SplashActivity.this.finish();
                            }
                        })
                .setPositiveButton("아니오",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    d.LoadRange("range.txt");
                                }catch(IOException e){
                                    e.printStackTrace();
                                }
                                dialog.dismiss();
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                SplashActivity.this.finish();
                            }
                        });
        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
        //if(alertDialog.isShowing()) alertDialog.dismiss();
    }

}
