package com.example.example4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Smart Phone Sensing Example 4. Wifi received signal strength.
 */
public class MainActivity extends Activity{


    private Button buttonPosTrain;
    private Button buttonMtnTrain;
    private Button buttonTest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonPosTrain = (Button) findViewById(R.id.btnPosTrain);
        // Set listener for the button.
        buttonPosTrain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), PosTrainActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        buttonMtnTrain = (Button) findViewById(R.id.btnMtnTrain);
        // Set listener for the button.
        buttonMtnTrain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), MtnTrainActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });

        buttonTest = (Button) findViewById(R.id.btnTest);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent myIntent = new Intent(view.getContext(), TestActivity.class);
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