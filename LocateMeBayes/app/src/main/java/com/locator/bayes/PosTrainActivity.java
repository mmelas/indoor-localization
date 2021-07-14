package com.locator.bayes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.locator.bayes.R;

public class PosTrainActivity extends Activity implements OnClickListener{

    /**
     * The wifi scanner.
     */
    private WiFiScanner wifiScanner;

    /**
     * The cellular scanner.
     */
    private CellularScanner cellularScanner;

    /**
     * The text view.
     */
    private TextView textPosTrain;
    /**
     * The buttons.
     */
    private Button buttonPosTrain, buttonCell1, buttonCell2, buttonCell3,
            buttonCell4, buttonCell5, buttonCell6, buttonCell7, buttonCell8, currentButton,
            buttonSample, buttonCellTogTrain;

    /**
     * Keep a list of all buttons for convenience
     */
    private ArrayList<Button> buttonList = new ArrayList<>();
    private ArrayList<Button> cpyButtonList = new ArrayList<>();

    /**
     * Number of Cells
     */
    private int numCells = 8;

    /**
     * The room we have selected
     */
    private String currentCell = "Not Selected";

    /**
     * The desired number of scan iterations.
     */
    private int numScans = 100; //Original : 100

    private boolean useWiFiData = true;

    private boolean useCellularData = false;

    /**
     * The desired number of WiFi scans in each iteration (for averaging).
     */
    private int numWiFiScansAve = 3;

    /**
     * The desired number of Cellular scans in each iteration (for averaging).
     */
    private int numCellularScansAve = 5;

    /**
     * Initial delay for starting the scan
     */
    private int scanStartDelay = 5000;

    /**
     * Number of dummy scans to be conducted at the start
     */
    private int numDummyScans = 4;

    /**
     * Time to wait between successive trainings.
     */
    private int trainWaitTime = 15;

    /**
     * A copy of wait time to keep track of pending time.
     */
    private int tempTrainWaitTime;

    /**
     * A copy of number of scans to help
     * keep track of iterations
     */
    private int tempNumOfScans;

    /**
     * Delay between consecutive WiFi Scans
     */
    private int wifiScanDelay = 0;

    /**
     * Delay between consecutive cellular scans
     */
    private int cellularScanDelay = 0;

    /**
     * Context view
     */
    private Context mContext;

    /**
     * Hashmap containing the completed cells
     * Will be used for keeping colors
     */
    private HashMap<Integer, Boolean> completedCells = new HashMap<>();
    private int completedSampleCells = 0; // number of completed sample cells

    /**
     * Animation used for completion
     */
    private Animation mShakeAnimation;

    /**
     * Handler used for updating text dynamically
     */
    private Handler handler = new Handler();

    private Sense wifiSense;
    private Sense cellularSense;

    private boolean trainState = false, sampleState = false;

    // Set containing samples for testing in order to find the best
    // rssi range that if a wifi is within that it should be considered
    // the same
    // String : Cell, List : List of the 3 wifi avg values for each bssid scanned
    private HashMap<String, List<HashMap<String, Integer>>> sampleSet = new HashMap<>();

    /**
     * Specifies whether the training data should be updated after each cell.
     * If false, data will be updated after the training of all cells is complete.
     */
    private boolean updateAfterEach = true;

    private String info = "";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos_train);

        //Set application context
        mContext = getApplicationContext();

        wifiScanner = new WiFiScanner(mContext, numWiFiScansAve);
        cellularScanner = new CellularScanner(mContext, numCellularScansAve);

        wifiSense = new Sense();
        cellularSense = new Sense();

        // Create items.
        textPosTrain = findViewById(R.id.textPosTrain);
        buttonSample = findViewById(R.id.buttonSample);
        buttonPosTrain = findViewById(R.id.buttonPosTrain);
        buttonCell1 = findViewById(R.id.buttonCell1);
        buttonCell2 = findViewById(R.id.buttonCell2);
        buttonCell3 = findViewById(R.id.buttonCell3);
        buttonCell4 = findViewById(R.id.buttonCell4);
        buttonCell5 = findViewById(R.id.buttonCell5);
        buttonCell6 = findViewById(R.id.buttonCell6);
        buttonCell7 = findViewById(R.id.buttonCell7);
        buttonCell8 = findViewById(R.id.buttonCell8);
        buttonCellTogTrain = findViewById(R.id.btnCellTogTrain);

        Button tempList[] = new Button[]{buttonPosTrain, buttonSample, buttonCellTogTrain, buttonCell1, buttonCell2,
                buttonCell3, buttonCell4, buttonCell5, buttonCell6, buttonCell7, buttonCell8};

        textPosTrain.setMovementMethod(new ScrollingMovementMethod());
        buttonList.addAll(Arrays.asList(tempList));

        cpyButtonList = (ArrayList) buttonList.clone();

        // Set listener for the buttons.
        for (Button b : buttonList) {
            if (b != buttonCellTogTrain) {
                b.setOnClickListener(this);
            }
        }

        // Set listener for the button.
        buttonCellTogTrain.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                useCellularData = !useCellularData;
                if (useCellularData) {
                    buttonCellTogTrain.setText("Cellular ON");
                    buttonCellTogTrain.setBackgroundColor(Color.parseColor("#3BA55C"));
                }
                else {
                    buttonCellTogTrain.setText("Cellular OFF");
                    buttonCellTogTrain.setBackgroundColor(Color.parseColor("#CA3E47"));
                }
            }
        });

        useCellularData = true;
        buttonCellTogTrain.setText("Cellular ON");
        buttonCellTogTrain.setBackgroundColor(Color.parseColor("#3BA55C"));
    }

    // onResume() registers the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
    }

    // onPause() unregisters the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        // Set current room
        if (v.getId() != R.id.buttonPosTrain && v.getId() != R.id.buttonSample && v.getId() != R.id.btnCellTogTrain) {
            setRoom(v);
        }

        // Set number of scans everytime the button is clicked
        tempNumOfScans = numScans;

        if (v.getId() == R.id.buttonPosTrain) trainState = true;
        if (v.getId() == R.id.buttonSample) sampleState = true;
        if (trainState || sampleState) {
            info = "";
            if (currentButton == null) {
                trainState = false;
                sampleState = false;
                textPosTrain.setText("Please choose the room you are currently in before scanning.\n");
            }
            else {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    textPosTrain.setText("Please enable location.\n");
                    return;
                }

                if (!((WifiManager) getSystemService(Context.WIFI_SERVICE)).isWifiEnabled()) {
                    ((WifiManager) getSystemService(Context.WIFI_SERVICE)).setWifiEnabled(true);
                }
                disableButtonsExceptSelf();
                if (v.getId() == R.id.buttonPosTrain) buttonList.remove(currentButton);

                textPosTrain.setText("Performing scan in " + currentCell + ".\nPlease wait until complete.\n");

                if (trainState && updateAfterEach && v.getId() == R.id.buttonPosTrain) {
                    wifiSense = new Sense();
                    cellularSense = new Sense();
                }

                handler.post(scanner);

            }
        }
    }

    private Runnable scanner = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if (tempNumOfScans == numScans) {
                for (int i=0; i<numDummyScans; i++) {
                    wifiScanner.scanAPs();
                    cellularScanner.scanNetworks();
                    SystemClock.sleep(scanStartDelay / numDummyScans);
                }
                TransitionDrawable transition = (TransitionDrawable) currentButton.getBackground();
                transition.startTransition((
                        (useWiFiData? 1: 0) * 200 * numWiFiScansAve +
                                (useCellularData? 1 : 0) * 500 * numCellularScansAve) * numScans);
            }

            // Set text.
            HashMap<String, Integer> wifiRssi = null;
            HashMap<String, Integer> cellularStrength = null;

            if (useWiFiData) {
                wifiRssi = wifiScanner.scanAPs();
                // Only add training data if we are in training state
                if (trainState) wifiSense.addCellScan(currentCell, wifiRssi);
                if (sampleState) {
                    if (sampleSet.get(currentCell) == null) {
                        sampleSet.put(currentCell, new ArrayList<>());
                    }
                    sampleSet.get(currentCell).add(wifiRssi);
                }
            }
            if (useCellularData) {
                cellularStrength = cellularScanner.scanNetworks();
                cellularSense.addCellScan(currentCell, cellularStrength);
            }


            // Do a scan every after the specified time for indicated number of scans.
            handler.postDelayed(this, wifiScanDelay);
            if (--tempNumOfScans == 0) {
                completedCells.put(currentButton.getId(), true);
                if (sampleState) completedSampleCells++;
                tempTrainWaitTime  = trainWaitTime;
                mShakeAnimation = AnimationUtils.loadAnimation(mContext,R.anim.shake_animation);
                currentButton.startAnimation(mShakeAnimation);
                handler.removeCallbacksAndMessages(null);
                // Scan is done, disable current button and enable the other buttons
                currentButton.setEnabled(false);
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(500);
                DataHolder app = (DataHolder) getApplicationContext();

                if (trainState && (updateAfterEach || (completedCells.size() == numCells))) {
                    app.addAndStore(wifiSense, cellularSense);
                }
                handler.post(wait);
            }
        }
    };

    private Runnable wait = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            handler.postDelayed(this, 1000);

            if (tempTrainWaitTime == trainWaitTime) {
                if (sampleState && (completedSampleCells == numCells)) {
                    // Find the best range for the values
                    double bestAccuracy = 0.0;
                    int bestRange = 3;
                    for (int i = 1; i <= 10; i += 1) {
                        double accuracy = computeAccuracy(i);
                        System.out.println("curr accuracy : " + accuracy + " for range : " + i);
                        if (accuracy > bestAccuracy) {
                            bestAccuracy = accuracy;
                            bestRange = i;
                        }
                    }
                    info = "All scans complete. Best Accuracy : " + bestAccuracy + " with range : " + bestRange;;
                    System.out.println(info);

                    // calculate confusion matrix for best range
                    confusionMatrix(bestRange);
                }
            }

            textPosTrain.setText(currentCell + " scan complete.\nPlease wait " + tempTrainWaitTime +
                    " seconds until next scan.\n\n" + info);

            if (tempTrainWaitTime-- == 0) {
                handler.removeCallbacksAndMessages(null);
                textPosTrain.setText(info);
                if (trainState) {
                    for (Button b : cpyButtonList) {
                        b.setEnabled(true);
                    }
                }
                else if (sampleState) {
                    enableButtonsExceptSelf();
                    buttonCellTogTrain.setEnabled(true);
                    buttonPosTrain.setEnabled(true);
                    buttonSample.setEnabled(true);
                }
                // Reset states
                trainState = false;
                sampleState = false;
            }
        }
    };

    private double computeAccuracy(int range) {
        double accuracy = 0.0;
        int cntCorrect = 0;
        int cntTotal = 0;

        for (Map.Entry<String,List<HashMap<String, Integer>>> cells : sampleSet.entrySet()) {
            for (HashMap<String, Integer> bssi_rssi : cells.getValue()) {
                String probableCell = wifiSense.probableCell(bssi_rssi, range);
                if (probableCell == cells.getKey()) cntCorrect++;
                cntTotal++;
            }
        }
        System.out.println("correct count : " + cntCorrect + " total count : " + cntTotal);
        if (cntTotal != 0) {
            accuracy = (double) cntCorrect / cntTotal;
        }
        return accuracy;
    }


    private void confusionMatrix(int range) {
        // HashMap with cell as key and list of identified cell occurencies as value
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();

        for (Map.Entry<String,List<HashMap<String, Integer>>> cells : sampleSet.entrySet()) {
            String cell = cells.getKey();
            confusionMatrix.put(cell, new HashMap<>());
            for (HashMap<String, Integer> bssi_rssi : cells.getValue()) {
                String probableCell = wifiSense.probableCell(bssi_rssi, range);
                if (confusionMatrix.get(cell).get(probableCell) == null) {
                    confusionMatrix.get(cell).put(probableCell, 0);
                } else {
                    confusionMatrix.get(cell).put(probableCell, confusionMatrix.get(cell).get(probableCell) + 1);
                }
            }
        }

        // Format confusion matrix for outputing to CSV
        String formattedConfusionMatrix = "{";
        for (Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()) {
            formattedConfusionMatrix += "\"" + entry.getKey() + "\"" + ":{";
            for (Map.Entry<String, Integer> entry2 : entry.getValue().entrySet()) {
                formattedConfusionMatrix += "\"" +  entry2.getKey() + "\"" + ":" + entry2.getValue() + ",";
            }
            formattedConfusionMatrix += "},";
        }
        formattedConfusionMatrix += "}";
        Log.i("Pos Confusion Matrix", formattedConfusionMatrix);
        try {
            File file = new File(getExternalFilesDir(null), "confMat.json");
            FileOutputStream matrixStream = new FileOutputStream(file);
            matrixStream.write(formattedConfusionMatrix.getBytes());
            matrixStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableButtonsExceptSelf() {
        for (Button b : buttonList) {
            if (b != currentButton) {
                b.setEnabled(false);
            }
        }
    }
    private void enableButtonsExceptSelf() {
        for (Button b : buttonList) {
            if (b != currentButton && b != buttonPosTrain && b != buttonSample && b != buttonCellTogTrain) {
                b.setEnabled(true);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setRoom(View v) {

        if (currentButton != null && !completedCells.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }

        switch (v.getId()) {
            case R.id.buttonCell1:
                currentButton = buttonCell1;
                currentCell = "Cell 1";
                break;
            case R.id.buttonCell2:
                currentButton = buttonCell2;
                currentCell = "Cell 2";
                break;
            case R.id.buttonCell3:
                currentButton = buttonCell3;
                currentCell = "Cell 3";
                break;
            case R.id.buttonCell4:
                currentButton = buttonCell4;
                currentCell = "Cell 4";
                break;
            case R.id.buttonCell5:
                currentButton = buttonCell5;
                currentCell = "Cell 5";
                break;
            case R.id.buttonCell6:
                currentButton = buttonCell6;
                currentCell = "Cell 6";
                break;
            case R.id.buttonCell7:
                currentButton = buttonCell7;
                currentCell = "Cell 7";
                break;
            case R.id.buttonCell8:
                currentButton = buttonCell8;
                currentCell = "Cell 8";
                break;
        }

        int resId = R.drawable.button_gradual_fill;
        Drawable transitionDrawable = getApplicationContext().getResources().getDrawable(resId);
        if (!completedCells.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackground(transitionDrawable);
        }
    }
}

