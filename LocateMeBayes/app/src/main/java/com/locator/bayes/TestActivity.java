package com.locator.bayes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.N)
public class TestActivity extends Activity implements OnClickListener {

    /**
     * The wifi scanner.
     */
    private WiFiScanner wifiScanner;

    /**
     * The cellular scanner.
     */
    private CellularScanner cellularScanner;

    /**
     * The Sensor manager.
     */
    private SensorManager sensorManager;

    /**
     * Flag for enabling/disabling response to sensor events.
     */
    boolean sensorEnable = false;

    /**
     * The text view.
     */
    private TextView textTest;

    /**
     * Test and Cellular Toggle button
     */
    private Button buttonTest, buttonCellTogTest, buttonSerParTest;

    /**
     * Textviews of different Rooms
     * will be used to paint them
     */
    private TextView tvCell1, tvCell2, tvCell3, tvCell4, tvCell5, tvCell6, tvCell7, tvCell8,
            currTextViewPos;

    /**
     * The desired number of WiFi scans in each iteration (for averaging).
     */
    private int numWiFiScansAve = 5;

    /**
     * The desired number of Cellular scans in each iteration (for averaging).
     */
    private int numCellularScansAve = 3;

    /**
     * Specifies whether Cellular scan should be used or not.
     */
    private boolean useCellularScan = false;

    /**
     * Specifies whether Parallel or Serial computation should be used.
     */
    private boolean parallelComp = false;

    /**
     * Initial delay for starting the scan
     */
    private int scanStartDelay = 5000;

    /**
     * Number of dummy scans to be conducted at the start
     */
    private int numDummyScans = 4;

    /**
     * Time to wait between successive inferences.
     */
    private int testWaitTime = 5;

    /**
     * A copy of wait time to keep track of pending time.
     */
    private int tempTestWaitTime;

    /**
     * Number of strongest access points to use
     */
    private int numStrongestAPs = 4;

    /**
     * Text to be displayed in the status box
     */
    String status = "";

    /**
     * Context view
     */
    private Context mContext;

    /**
     * Number of cells
     */
    private int numCells = 8;

    /**
     * The names of the rooms being used
     */
    private List<String> cellNames = new ArrayList<>();

    /**
     * Handler used for analysing recorded sensor data
     */
    private Handler handler = new Handler();

    /**
     * Use the best range found in sample testing
     * Default : 3
     */
    private int range = 3;

    private Sense wifiSense;
    private Sense cellularSense;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        //Set application context
        mContext = getApplicationContext();

        wifiScanner = new WiFiScanner(mContext, numWiFiScansAve);
        cellularScanner = new CellularScanner(mContext, numCellularScansAve);

        // Create items.
        textTest = findViewById(R.id.textTest);
        buttonTest = findViewById(R.id.buttonTest);
        buttonCellTogTest = findViewById(R.id.btnCellTogTest);
        buttonSerParTest = findViewById(R.id.btnSerPar);
        tvCell1 = findViewById(R.id.tvCell1);
        tvCell2 = findViewById(R.id.tvCell2);
        tvCell3 = findViewById(R.id.tvCell3);
        tvCell4 = findViewById(R.id.tvCell4);
        tvCell5 = findViewById(R.id.tvCell5);
        tvCell6 = findViewById(R.id.tvCell6);
        tvCell7 = findViewById(R.id.tvCell7);
        tvCell8 = findViewById(R.id.tvCell8);

        textTest.setMovementMethod(new ScrollingMovementMethod());
        buttonTest.setOnClickListener(this);

        // Set listener for the button.
        buttonCellTogTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (!useCellularScan) {
                    if (!((DataHolder) getApplicationContext()).getCellularSense().isTrained()) {
                        textTest.setText("Please train the model with the cellular data first.\n");
                        return;
                    }
                    buttonCellTogTest.setText("Cellular ON");
                    buttonCellTogTest.setBackgroundColor(Color.parseColor("#3BA55C"));
                }
                else {
                    buttonCellTogTest.setText("Cellular OFF");
                    buttonCellTogTest.setBackgroundColor(Color.parseColor("#CA3E47"));
                }
                useCellularScan = !useCellularScan;
            }
        });

        useCellularScan = ((DataHolder) getApplicationContext()).getCellularSense().isTrained();
        if (useCellularScan) {
            buttonCellTogTest.setText("Cellular ON");
            buttonCellTogTest.setBackgroundColor(Color.parseColor("#3BA55C"));
        }
        else {
            buttonCellTogTest.setText("Cellular OFF");
            buttonCellTogTest.setBackgroundColor(Color.parseColor("#CA3E47"));
        }

        // Set listener for the button.
        buttonSerParTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                parallelComp = !parallelComp;
                if (parallelComp) {
                    buttonSerParTest.setText("Parallel");
                    buttonSerParTest.setBackgroundColor(Color.parseColor("#45ADFF"));
                }
                else {
                    buttonSerParTest.setText("Serial");
                    buttonSerParTest.setBackgroundColor(Color.parseColor("#BB86FC"));
                }
            }
        });

        parallelComp = true;

        // Add any new rooms here
        cellNames.addAll(Arrays.asList("Cell 1", "Cell 2", "Cell 3", "Cell 4", "Cell 5", "Cell 6", "Cell 7", "Cell 8"));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(View v) {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            textTest.setText("Please enable location.\n");
            return;
        }

        if (!((DataHolder) getApplicationContext()).getWifiSense().isTrained()) {
            textTest.setText("Please train the model first.\n");
            buttonTest.setEnabled(true);
            buttonCellTogTest.setEnabled(true);
            buttonSerParTest.setEnabled(true);
            return;
        }

        buttonTest.setEnabled(false);
        buttonCellTogTest.setEnabled(false);
        buttonSerParTest.setEnabled(false);
        setCurrTextViewPos("");
        if (!((WifiManager) getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()) {
            ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
        }

        textTest.setText("Performing scan.\nPlease wait until complete.\n");

        handler.post(scanner);
    }

    private Runnable scanner = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            // Set text.
            handler.removeCallbacksAndMessages(null);

            // Dummy scan, only to discard old results.
            for (int i=0; i<numDummyScans; i++) {
                wifiScanner.scanAPs();
                // Cellular Scan is not really required here since, it is performed in background
                // automatically, and app has no control over it
                SystemClock.sleep(scanStartDelay / numDummyScans);
            }

            tempTestWaitTime = testWaitTime;

            HashMap<String, Integer> wifiRssi = wifiScanner.scanAPs(5);
            HashMap<String, Integer> cellularStrength = cellularScanner.scanNetworks();

            String probableCell = getProbCell(wifiRssi, cellularStrength, parallelComp);

            // If main method does not work, try the other one.
            if (probableCell == "") {
                probableCell = getProbCell(wifiRssi, cellularStrength, !parallelComp);
            }

            if (probableCell == "") {
                status = "Position could not be determined\n";
            }
            else {
                status = "Position: " + probableCell + "\n";
                setCurrTextViewPos(probableCell);
                currTextViewPos.setBackgroundColor(Color.parseColor("#FFA500"));
            }
            textTest.setText(status);
            handler.post(wait);

        }
    };

    private Runnable wait = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            handler.postDelayed(this, 1000);
            textTest.setText(status + "Please wait " + tempTestWaitTime +
                    " seconds until next scan.");
            if (tempTestWaitTime-- == 0) {
                handler.removeCallbacksAndMessages(null);
                textTest.setText(status);

                // Scan is done, enable the button
                buttonTest.setEnabled(true);
                buttonCellTogTest.setEnabled(true);
                buttonSerParTest.setEnabled(true);
            };

        }
    };

    public String getProbCell(HashMap<String, Integer> wifiRssi,
                              HashMap<String, Integer> cellularStrength,
                              boolean parallel) {
        //Bayes Code
        DataHolder app = (DataHolder) getApplicationContext();

        // 6 WiFi 2 Mobile
        ArrayList<HashMap<String, Float>> priorsList = new ArrayList<>();
        priorsList.add(new HashMap<>());
        for (String cellName: cellNames) {
            priorsList.get(0).put(cellName, 1f / cellNames.size());
        }

        priorsList = app.getWifiSense().ps(wifiRssi, priorsList, parallel);

        if (useCellularScan) {
            priorsList = app.getCellularSense().ps(cellularStrength, priorsList, parallel);
        }

        return (new Sense()).probCellFromPostsList(priorsList);
    }


    public void setCurrTextViewPos(String majority) {
        if (currTextViewPos != null) {
            currTextViewPos.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }
        switch (majority) {
            case ("Cell 1") :
                currTextViewPos = tvCell1;
                break;
            case ("Cell 2") :
                currTextViewPos = tvCell2;
                break;
            case ("Cell 3") :
                currTextViewPos = tvCell3;
                break;
            case ("Cell 4") :
                currTextViewPos = tvCell4;
                break;
            case ("Cell 5") :
                currTextViewPos = tvCell5;
                break;
            case ("Cell 6") :
                currTextViewPos = tvCell6;
                break;
            case ("Cell 7") :
                currTextViewPos = tvCell7;
                break;
            case ("Cell 8") :
                currTextViewPos = tvCell8;
                break;
        }
    }

    private static class Result {
        double distance;
        String name;
        public Result(double distance, String name) {
            this.distance = distance;
            this.name = name;
        }
    }

}