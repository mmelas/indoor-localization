package com.locator.bayes;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.locator.bayes.R;

/**
 * Smart Phone Sensing Example 4. Wifi received signal strength.
 */
public class MainActivity extends Activity{


    private Button buttonPosTrain;
    private Button buttonDataCtrl;
    private Button buttonTest;
    private TextView textStatus;

    private int delPressCount = 0;

    private Context ctx;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("Dir: " + getExternalFilesDir(null));

        String[] PERMS_INITIAL={
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
        };
        ActivityCompat.requestPermissions(this, PERMS_INITIAL, 127);

        ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);

        textStatus = (TextView) findViewById(R.id.textStatus);

        DataHolder app = (DataHolder) getApplicationContext();
        if (!app.loadRaw()) {
            textStatus.setText("Trained data unavailable or unreadable.\n" +
                    "Empty object created.");
        };

        buttonPosTrain = (Button) findViewById(R.id.btnPosTrain);
        // Set listener for the button.
        buttonPosTrain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                textStatus.setText("");
                Intent myIntent = new Intent(view.getContext(), PosTrainActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });


        buttonTest = (Button) findViewById(R.id.btnTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                textStatus.setText("");
                Intent myIntent = new Intent(view.getContext(), TestActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });


        buttonDataCtrl = (Button) findViewById(R.id.btnDataCtrl);
        // Set listener for the button.
        buttonDataCtrl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                textStatus.setText("");
                Intent myIntent = new Intent(view.getContext(), DataCtrlActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }

}