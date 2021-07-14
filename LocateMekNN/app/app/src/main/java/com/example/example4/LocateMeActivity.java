package com.example.example4;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.example.example4.ScanActivity.Room;

@RequiresApi(api = Build.VERSION_CODES.N)
public class LocateMeActivity extends AppCompatActivity implements OnClickListener{

    /**
     * The testing value of k for the kNN algorithm
     * (default value = 3). This value is used for
     * locate button. If user uses Cross validation first the
     * best k value will be used, otherwise the default value will
     * be used
     */
    private int k = 7;

    /**
     * the best k value found by cross validation
     */
    private int bestK;

    /**
     * The value of K for K-fold cross validation
     */
    private int kFold = 4;

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;

    /**
     * The text view.
     */
    private TextView textLocate;

    /**
     * Locate button
     */
    private Button buttonLocate, buttonCV;

    /**
     * Textviews of different Rooms
     * will be used to paint them
     */
    private TextView tvLivingRoom, tvBedroom1, tvBedroom2, tvToilet,
                     tvBalcony, tvKitchen, currTextView;

    /**
     * The list of rooms with RSSI mean values
     * generated from scan
     */
    private List<Room> roomList;


    /**
     * A priority queue containing the results in ascending
     * order according to the distance from the scanned values
     */
    private PriorityQueue<Result> resultList = new PriorityQueue<>(new DistanceComparator());


    /**
     * List containing the k nearest rooms
     */
    private List<Result> kNearestRooms = new ArrayList<>();

    /**
     * Current result of kNN
     */
    private String currResult;

    /**
     * Calculate how many successes kNN had
     */
    private int cntSuccess;


    /**
     * best accuracy found
     */
    private double maxAccuracy = Double.MIN_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        // Create items.
        buttonLocate = findViewById(R.id.buttonTest);
        buttonCV = findViewById(R.id.buttonCV);
        textLocate = findViewById(R.id.textTest);
        tvLivingRoom = findViewById(R.id.tvLivingRoom);
        tvBedroom1 = findViewById(R.id.tvBedroom1);
        tvBedroom2 = findViewById(R.id.tvBedroom2);
        tvBalcony = findViewById(R.id.tvBalcony);
        tvKitchen = findViewById(R.id.tvKitchen);
        tvToilet = findViewById(R.id.tvToilet);

        textLocate.setMovementMethod(new ScrollingMovementMethod());
        buttonLocate.setOnClickListener(this);
        buttonCV.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(View v) {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        textLocate.setText("\n\tScanning all access points.");

        // get all sampling data from scans
        DataHolder app = (DataHolder) getApplicationContext();
        //TODO: Shall we use other training set here? (one from Cross validation ?)
        roomList = app.getRoomList();

        if (v.getId() == R.id.buttonCV) {
            kFoldCV();
        }
        else {
            // Start a wifi scan.
            int numberOfScans = 3;
            HashMap<String, Integer> query = new HashMap<>();
            HashMap<String, Integer> numOcc = new HashMap<>();
            for (int i = 0; i < numberOfScans; ++i) {
                wifiManager.startScan();

                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult scanResult : scanResults) {
                    String currentBSSID = scanResult.BSSID;
                    query.put(currentBSSID, query.getOrDefault(currentBSSID, 0) + scanResult.level);
                    numOcc.put(currentBSSID, numOcc.getOrDefault(currentBSSID, 0) + 1);
                    //      textLocate.setText(textLocate.getText() + "\n\tBSSID = "
                    //              + currentBSSID + "    RSSI = "
                    //              + scanResult.level + "dBm");
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("k value : " + k + "majority : " + currResult);
            query.replaceAll((key, val) -> val / numOcc.get(key));

            kNN(roomList, query);
        }

    }

    // Cross-Validation
    private void kFoldCV() {
        // Shuffle samples randomly
        Collections.shuffle(roomList);

        // Extract 20% of data for testing
        List<Room> testSet = roomList.subList(roomList.size()*80/100, roomList.size());
        roomList = roomList.subList(0, roomList.size()*80/100);

        // K-cross validation so repeat K times
        int size = roomList.size() / kFold;
        double accuracy = 0.0;
        for (int i_k = 1; i_k < 13; i_k+=2) {
            k = i_k;
            accuracy = 0.0;
            for (int i = 0; i < kFold; ++i) {
                List<Room> trainingSet = new ArrayList<>();
                List<Room> validationSet = new ArrayList<>();
                // pick items as validation set
                for (int j = i * size; j < (i + 1) * size; ++j) {
                    validationSet.add(roomList.get(j));
                }

                // pick items as training set (must not be same data with validation set)
                for (int j = 0; j < roomList.size(); ++j) {
                    if (j < i * size || j >= (i + 1) * size) {
                        trainingSet.add(roomList.get(j));
                    }
                }
                System.out.println("Training set size : " + trainingSet.size());
                System.out.println("Validation set size : " + validationSet.size());
                System.out.println("size of each val : " + size + " with index " + i + " bb " + roomList.size() + " " + kFold);
                accuracy += calcAccuracy(trainingSet, validationSet);
            }

            // get mean accuracy for current kNN model
            accuracy /= kFold;
            if (accuracy > maxAccuracy) {
                bestK = k;
                maxAccuracy = accuracy;
            }
        }
        k = bestK;
        textLocate.setText("\nMax Accuracy : " + maxAccuracy + " with k : " + k);
    }

    private double calcAccuracy(List<Room> roomList, List<Room> testList) {
        cntSuccess = 0;
        for (Room r : testList) {
            kNN(roomList, r.roomAttributes);
            if (currResult == r.roomName) {
                cntSuccess++;
            }
        }
        return cntSuccess / Double.valueOf(testList.size()) * 100;
        //textLocate.setText("Accuracy : " + cntSuccess / Double.valueOf(testList.size()) * 100 + "%" + "\nSucc : " + cntSuccess + " " + testList.size());
    }


    private void kNN(List<Room> roomList, HashMap<String, Integer> query) {
        // Reset hashmap/lists for new query
        resultList.clear();
        kNearestRooms.clear();

        calculateDistances(roomList, query);
        initializeKClosest();
        String majority = findMajority();
        setCurrTextView(majority);
        currTextView.setBackgroundColor(Color.parseColor("#FFA500"));
        //   textLocate.setText("You are in room : " + majority);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void calculateDistances(List<Room> roomList, HashMap<String, Integer> query) {
        String t = "";
        for (Room r : roomList) {
            double dist = 0.0;
            int cnt = 0;
           // textLocate.setText(textLocate.getText() + "\nAGAIN");
            for (Map.Entry<String, Integer> entry : r.roomAttributes.entrySet()) {
                int d1 = entry.getValue();
                int d2 = query.getOrDefault(entry.getKey(), -120);
                ++cnt;
                dist += Math.pow(d1 - d2, 2);
          //      test += "\ndistance from entry " + d1 + " - " + d2;
           //     if (cnt == 5) {
           //         break;
           //     }
            }
            double distance = Math.sqrt(dist);
            t += "\ndistance from room " + r.roomName + " = " + distance;
         //   textLocate.setText(textLocate.getText() + "\ndistance from room " + r.roomName + " = " + distance);
            resultList.add(new Result(distance, r.roomName));
        }
        textLocate.setText(t);
    }


    public void initializeKClosest() {
        for (int i = 0; i < k; i++) {
            kNearestRooms.add(resultList.poll());
        }
    }

    private String findMajority() {
        String majority = "Empty";
        HashMap<String, Double> weights = new HashMap<>();

        // Take weights into account
        int totalSum = 0;
        for (Result r : kNearestRooms){
            totalSum += r.distance;
            weights.put(r.roomName, weights.getOrDefault(r.roomName, 0.0) + 1/r.distance);
        }

        int finalTotalSum = totalSum;
        weights.replaceAll((k, v) -> v / finalTotalSum);

        double max = -1.0;
        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                majority = entry.getKey();
            }
        }

        currResult = majority;
        return majority;
    }


    public void setCurrTextView(String majority) {
        if (currTextView != null) {
            currTextView.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }
        switch (majority) {
            case ("Kitchen") :
                currTextView = tvKitchen;
                break;
            case ("Balcony") :
                currTextView = tvBalcony;
                break;
            case ("Bedroom1") :
                currTextView = tvBedroom1;
                break;
            case ("Bedroom2") :
                currTextView = tvBedroom2;
                break;
            case ("Toilet") :
                currTextView = tvToilet;
                break;
            case ("Living Room") :
                currTextView = tvLivingRoom;
                break;
        }
    }

    private static class Result {
        double distance;
        String roomName;
        public Result(double distance, String roomName) {
            this.distance = distance;
            this.roomName = roomName;
        }
    }

    private static class DistanceComparator implements Comparator<Result> {
        @Override
        public int compare(Result a, Result b) {
            return a.distance < b.distance ? -1 : a.distance == b.distance ? 0 : 1;
        }
    }
}