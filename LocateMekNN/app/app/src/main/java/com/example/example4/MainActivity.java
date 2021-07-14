package com.example.example4;

import java.util.List;
import android.app.Activity;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Smart Phone Sensing Example 4. Wifi received signal strength.
 */
public class MainActivity extends Activity{


    private Button buttonScan;
    private Button buttonLocate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonScan = (Button) findViewById(R.id.btnPosTrain);
        buttonLocate = (Button) findViewById(R.id.buttonTest);
        // Set listener for the button.
        buttonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), ScanActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
        buttonLocate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), LocateMeActivity.class);
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