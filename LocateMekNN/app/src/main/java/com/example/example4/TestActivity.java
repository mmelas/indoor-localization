package com.example.example4;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.example.example4.PosTrainActivity.Room;
import com.example.example4.MtnTrainActivity.Motion;

@RequiresApi(api = Build.VERSION_CODES.N)
public class TestActivity extends Activity implements OnClickListener, SensorEventListener {

    /**
     * The values of k for the kNN algorithm
     */
    private int kPos = 7;
    private int kMtn = 7;

    /**
     * the best k values found by cross validation
     */
    private int bestKPos;
    private int bestKMtn;

    /**
     * The value of K for K-fold cross validation
     */
    private int kFold = 4;

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;

    /**
     * The Sensor manager.
     */
    private SensorManager sensorManager;

    /**
     * The Accel and Gyro objects.
     */
    private Sensor accel;
    private Sensor gyro;

    /**
     * Buffers for accelerometers timestamps and resultants.
     */
    List<Long> accT = new ArrayList<Long>();
    List<Float> accR = new ArrayList<Float>();

    /**
     * Buffers for gyroscope timestamps and components.
     */
    List<Long> gyrT = new ArrayList<Long>();
    List<Float> gyrX = new ArrayList<Float>();
    List<Float> gyrY = new ArrayList<Float>();
    List<Float> gyrZ = new ArrayList<Float>();

    /**
     * Flag for enabling/disabling response to sensor events.
     */
    boolean sensorEnable = false;

    /**
     * Start time of motion recording.
     */
    Long motionRecordStartT = 0L;

    /**
     * Motion record duration (in seconds)
     */
    private int motionRecordDuration = 2;

    /**
     * The text view.
     */
    private TextView textTest;

    /**
     * Test and CV button
     */
    private Button buttonTest, buttonCV;

    /**
     * Textviews of different Rooms
     * will be used to paint them
     */
    private TextView tvLivingRoom, tvBedroom1, tvBedroom2, tvToilet, tvBalcony, tvKitchen,
            tvSit, tvWalk, tvPushUp, currTextViewPos, currTextViewMtn;

    /**
     * The list of rooms with RSSI mean values
     * generated from scan
     */
    private List<Room> roomList;

    /**
     * The names of the rooms being used
     */
    private List<String> roomNames = new ArrayList<>();

    /**
     * The names of the motion being used
     */
    private List<String> motionNames = new ArrayList<>();

    /**
     * The list of motions with accel and gyro stats
     * generated from scan
     */
    private List<Motion> motionList;

    /**
     * Priority queues containing the results in ascending
     * order according to the distance from the scanned values
     */
    private PriorityQueue<Result> posResultList = new PriorityQueue<>(new DistanceComparator());
    private PriorityQueue<Result> mtnResultList = new PriorityQueue<>(new DistanceComparator());

    /**
     * The scan results obtained
     * after pressing Test button
     * TODO: Change value to Double if we get the mean from more than one samples
     */
//    private HashMap<String, Integer> queryPos = new HashMap<>();
//
//    /**
//     * The sensor results obtained after pressing Locate button
//     */
//    private HashMap<String, Float> queryMtn = new HashMap<>();

    /**
     * List containing the k nearest rooms
     */
    private List<Result> kNearestRooms = new ArrayList<>();
    private List<Result> kNearestMotions = new ArrayList<>();

    /**
     * Current result of kNN
     */
    private String currResultPos;
    private String currResultMtn;

    /**
     * Calculate how many successes kNN had
     */
    private int cntSuccess;

    /**
     * Count of how many cross validations did run
     */
    private int runTestsPos = 0;
    private int runTestsMtn = 0;

    /**
     * best accuracy found
     */
    private double maxAccuracy = Double.MIN_VALUE;

    /**
     * Handler used for analysing recorded sensor data
     */
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        // Create items.
        textTest = findViewById(R.id.textTest);
        buttonTest = findViewById(R.id.buttonTest);
        buttonCV = findViewById(R.id.buttonCV);
        tvLivingRoom = findViewById(R.id.tvLivingRoom);
        tvBedroom1 = findViewById(R.id.tvBedroom1);
        tvBedroom2 = findViewById(R.id.tvBedroom2);
        tvBalcony = findViewById(R.id.tvBalcony);
        tvKitchen = findViewById(R.id.tvKitchen);
        tvToilet = findViewById(R.id.tvToilet);
        tvSit = findViewById(R.id.tvSit);
        tvWalk = findViewById(R.id.tvWalk);
        tvPushUp = findViewById(R.id.tvPushUp);

        textTest.setMovementMethod(new ScrollingMovementMethod());
        buttonTest.setOnClickListener(this);
        buttonCV.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);

        // Add any new rooms here
        roomNames.addAll(Arrays.asList("Living Room", "Kitchen", "Bedroom1", "Bedroom2", "Toilet", "Balcony"));
        motionNames.addAll(Arrays.asList("Sitting", "Walking", "Push-Ups"));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(View v) {            // Start a wifi scan.
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        textTest.setText("\n\tScanning all access points.");

        DataHolder app = (DataHolder) getApplicationContext();
        roomList = app.getRoomList();
        motionList = app.getMotionList();
        if (v.getId() == R.id.buttonCV) {
            kFoldCVPos();
            kFoldCVMtn();
        }
        else {
            sensorEnable = true;
            handler.postDelayed(processSensorData, motionRecordDuration * 1000);

            int numberOfScans = 3;
            HashMap<String, Integer> queryPos = new HashMap<>();
            HashMap<String, Integer> numOcc = new HashMap<>();
            for (int i = 0; i < numberOfScans; ++i) {
                // Start a wifi scan.
                wifiManager.startScan();

                List<ScanResult> scanResults = wifiManager.getScanResults();
                for (ScanResult scanResult : scanResults) {
                    String currentBSSID = scanResult.BSSID;
                    queryPos.put(currentBSSID, scanResult.level);
                    //      textLocate.setText(textLocate.getText() + "\n\tBSSID = "
                    //              + currentBSSID + "    RSSI = "
                    //              + scanResult.level + "dBm");
                    numOcc.put(currentBSSID, numOcc.getOrDefault(currentBSSID, 0) + 1);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queryPos.replaceAll((key, val) -> val / numOcc.get(key));

            kNNPos(roomList, queryPos);
//
//            calculatePosDists(roomList, queryPos);
//            initializeKClosest("Pos");
//            String majPos = findPosMajority();
//            setCurrTextViewPos(majPos);
//            currTextViewPos.setBackgroundColor(Color.parseColor("#FFA500"));
        }
    }

    // Cross-Validation
    private void kFoldCVPos() {
        maxAccuracy = Double.MIN_VALUE;
        // Shuffle samples randomly
        Collections.shuffle(roomList);

        // Extract 20% of data for testing
        List<Room> testSet = roomList.subList(roomList.size()*80/100, roomList.size());
        List<Room> tempRoomList = roomList.subList(0, roomList.size()*80/100);

        // K-cross validation so repeat K times
        int size = tempRoomList.size() / kFold;
        double accuracy;

        for (int i_k = 1; i_k <= 13; i_k+=2) {
            kPos = i_k;
            accuracy = 0.0;
            for (int i = 0; i < kFold; ++i) {
                List<Room> trainingSet = new ArrayList<>();
                List<Room> validationSet = new ArrayList<>();
                // pick items as validation set
                for (int j = i * size; j < (i + 1) * size; ++j) {
                    validationSet.add(tempRoomList.get(j));
                }

                // pick items as training set (must not be same data with validation set)
                for (int j = 0; j < tempRoomList.size(); ++j) {
                    if (j < i * size || j >= (i + 1) * size) {
                        trainingSet.add(tempRoomList.get(j));
                    }
                }
                System.out.println("Training set size : " + trainingSet.size());
                System.out.println("Validation set size : " + validationSet.size());
                System.out.println("size of each val : " + size + " with index " + i + " bb " + tempRoomList.size() + " " + kFold);
                accuracy += calcAccuracyPos(trainingSet, validationSet);
            }

            // get mean accuracy for current kNN model
            accuracy /= kFold;
            if (accuracy > maxAccuracy) {
                bestKPos = kPos;
                maxAccuracy = accuracy;
            }
        }
        // Check if accuracy is good enough with the starting not seen testset
        // rerun up to 5 times
        Log.i("Pos Confusion Matrix", "Training size : " + tempRoomList.size());
        Log.i("Pos Confusion Matrix", "Testing size  : " + testSet.size());
        if (calcAccuracyPos(tempRoomList, testSet) < 85 && ++runTestsPos < 5) {
            Log.i("kFoldCV", "Accuracy less than 85%, rerun kFoldCV");
            kFoldCVPos();
        }
        kPos = bestKPos;
        Log.i(null, "Position k: " + String.valueOf(kPos));
        textTest.setText(textTest.getText() + "\nMax Accuracy : " + maxAccuracy + " with k : " + kPos);
    }

    private double calcAccuracyPos(List<Room> roomList, List<Room> testList) {
        cntSuccess = 0;
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();
        for (String s : roomNames) {
            confusionMatrix.put(s, new HashMap<>());
        }
        for (Room r : testList) {
            kNNPos(roomList, r.roomAttributes);
            int currCnt = confusionMatrix.get(r.roomName).getOrDefault(currResultPos, 0);
            Map currMap = confusionMatrix.get(r.roomName);
            currMap.put(currResultPos, currCnt+1);
            if (currResultPos == r.roomName) {
                cntSuccess++;
            }
        }
        Log.i("Pos Confusion Matrix: ", " Accuracy : " + String.valueOf(cntSuccess / Double.valueOf(testList.size()) * 100));


        /**             Room1 Room2
         *    Room1     3       1
         *    Room2     1       5
         */
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
        return cntSuccess / Double.valueOf(testList.size()) * 100;
        //textLocate.setText("Accuracy : " + cntSuccess / Double.valueOf(testList.size()) * 100 + "%" + "\nSucc : " + cntSuccess + " " + testList.size());
    }


    private void kNNPos(List<Room> roomList, HashMap<String, Integer> query) {
        // Reset hashmap/lists for new query
        posResultList.clear();
        kNearestRooms.clear();

        calculatePosDists(roomList, query);
        initializeKClosest("Pos");
        String majority = findPosMajority();
        setCurrTextViewPos(majority);
        currTextViewPos.setBackgroundColor(Color.parseColor("#FFA500"));
        //   textLocate.setText("You are in room : " + majority);
    }

    // Cross-Validation
    private void kFoldCVMtn() {
        maxAccuracy = Double.MIN_VALUE;
        // Shuffle samples randomly
        Collections.shuffle(motionList);

        // Extract 20% of data for testing
        List<Motion> testSet = motionList.subList(motionList.size()*80/100, motionList.size());
        List<Motion> tempMotionList = motionList.subList(0, motionList.size()*80/100);

        // K-cross validation so repeat K times
        int size = tempMotionList.size() / kFold;
        double accuracy = 0.0;
        for (int i_k = 1; i_k <= 13; i_k+=2) {
            kMtn = i_k;
            accuracy = 0.0;
            for (int i = 0; i < kFold; ++i) {
                List<Motion> trainingSet = new ArrayList<>();
                List<Motion> validationSet = new ArrayList<>();
                // pick items as validation set
                for (int j = i * size; j < (i + 1) * size; ++j) {
                    validationSet.add(tempMotionList.get(j));
                }

                // pick items as training set (must not be same data with validation set)
                for (int j = 0; j < tempMotionList.size(); ++j) {
                    if (j < i * size || j >= (i + 1) * size) {
                        trainingSet.add(tempMotionList.get(j));
                    }
                }
                System.out.println("Training set size : " + trainingSet.size());
                System.out.println("Validation set size : " + validationSet.size());
                System.out.println("size of each val : " + size + " with index " + i + " bb " + tempMotionList.size() + " " + kFold);
                accuracy += calcAccuracyMtn(trainingSet, validationSet);
            }

            // get mean accuracy for current kNN model
            accuracy /= kFold;
            if (accuracy > maxAccuracy) {
                bestKMtn = kMtn;
                maxAccuracy = accuracy;
            }
        }
        // Check if accuracy is good enough with the starting not seen testset
        // rerun up to 5 times
        Log.i("Mtn Confusion Matrix", "Training size : " + tempMotionList.size());
        Log.i("Mtn Confusion Matrix", "Testing size  : " + testSet.size());
        if (calcAccuracyMtn(tempMotionList, testSet) < 85 && ++runTestsMtn < 5) {
            Log.i("kFoldCV", "Accuracy less than 85%, rerun kFoldCV");
            kFoldCVMtn();
        }
        kMtn = bestKMtn;
        Log.i(null, "Motion k: " + String.valueOf(kMtn));
        textTest.setText(textTest.getText() + "\nMax Accuracy : " + maxAccuracy + " with k : " + kMtn);
    }

    private double calcAccuracyMtn(List<Motion> motionList, List<Motion> testList) {
        cntSuccess = 0;
        Map<String, Map<String, Integer>> confusionMatrix = new HashMap<>();
        for (String s : motionNames) {
            confusionMatrix.put(s, new HashMap<>());
        }
        for (Motion m : testList) {
            kNNMtn(motionList, m.motionAttributes);
            int currCnt = confusionMatrix.get(m.motionName).getOrDefault(currResultMtn, 0);
            Map currMap = confusionMatrix.get(m.motionName);
            currMap.put(currResultMtn, currCnt+1);
            if (currResultMtn == m.motionName) {
                cntSuccess++;
            }
        }
        Log.i("Mtn Confusion Matrix: ", " Accuracy : " + String.valueOf(cntSuccess / Double.valueOf(testList.size()) * 100));


        /**             Motion1 Motion2
         *    Motion1   3       1
         *    Motion2   1       5
         */
        String formattedConfusionMatrix = "{";
        for (Map.Entry<String, Map<String, Integer>> entry : confusionMatrix.entrySet()) {
            formattedConfusionMatrix += "\"" + entry.getKey() + "\"" + ":{";
            for (Map.Entry<String, Integer> entry2 : entry.getValue().entrySet()) {
                formattedConfusionMatrix += "\"" +  entry2.getKey() + "\"" + ":" + entry2.getValue() + ",";
            }
            formattedConfusionMatrix += "},";
        }
        formattedConfusionMatrix += "}";

        Log.i("Mtn Confusion Matrix", formattedConfusionMatrix);
        Log.i("Mtn", String.valueOf(cntSuccess / Double.valueOf(testList.size()) * 100));
        return cntSuccess / Double.valueOf(testList.size()) * 100;
        //textLocate.setText("Accuracy : " + cntSuccess / Double.valueOf(testList.size()) * 100 + "%" + "\nSucc : " + cntSuccess + " " + testList.size());
    }


    private void kNNMtn(List<Motion> motionList, HashMap<String, Float> query) {
        // Reset hashmap/lists for new query
        mtnResultList.clear();
        kNearestMotions.clear();

        calculateMtnDists(motionList, query);
        initializeKClosest("Mtn");
        String majority = findMtnMajority();
        setCurrTextViewMtn(majority);
        currTextViewMtn.setBackgroundColor(Color.parseColor("#FFA500"));
        //   textLocate.setText("You are in performing activity : " + majority);
    }


    private Runnable processSensorData = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            sensorEnable = false;
            handler.removeCallbacksAndMessages(null);

            float arStd = 0, arMin=accR.get(0), arMax=accR.get(0);
            float gxStd = 0, gxMin=gyrX.get(0), gxMax=gyrX.get(0);
            float gyStd = 0, gyMin=gyrY.get(0), gyMax=gyrY.get(0);
            float gzStd = 0, gzMin=gyrZ.get(0), gzMax=gyrZ.get(0);

            HashMap<String, Float> queryMtn = new HashMap<>();

            // Accel Total & Mean
            for(int i = 0; i < accR.size(); i++){
                arStd += accR.get(i);
                if (accR.get(i) < arMin) {
                    arMin = accR.get(i);
                }
                else if (accR.get(i) > arMax) {
                    arMax = accR.get(i);
                }
            }
            arStd = arStd/accR.size();

            // Gyro Total & Mean
            for(int i = 0; i < gyrX.size(); i++){
                gxStd += gyrX.get(i);
                if (gyrX.get(i) < gxMin) {
                    gxMin = gyrX.get(i);
                }
                else if (gyrX.get(i) > gxMax) {
                    gxMax = gyrX.get(i);
                }

                gyStd += gyrY.get(i);
                if (gyrY.get(i) < gyMin) {
                    gyMin = gyrY.get(i);
                }
                else if (gyrY.get(i) > gyMax) {
                    gyMax = gyrY.get(i);
                }

                gzStd += gyrZ.get(i);
                if (gyrZ.get(i) < gzMin) {
                    gzMin = gyrZ.get(i);
                }
                else if (gyrZ.get(i) > gzMax) {
                    gzMax = gyrZ.get(i);
                }

            }
            gxStd = gxStd/gyrX.size();
            gyStd = gyStd/gyrY.size();
            gzStd = gzStd/gyrZ.size();

            // Accel Square Errors
            for(int i = 0; i < accR.size(); i++){
                accR.set(i, (float) Math.pow((accR.get(i)-arStd),2));
            }

            // Gyro Square Errors
            for(int i = 0; i < gyrX.size(); i++){
                gyrX.set(i, (float) Math.pow((gyrX.get(i)-gxStd),2));
                gyrY.set(i, (float) Math.pow((gyrY.get(i)-gyStd),2));
                gyrZ.set(i, (float) Math.pow((gyrZ.get(i)-gzStd),2));
            }

            // Accel Variance and Std
            arStd = 0;
            for(int i = 0; i < accR.size(); i++){
                arStd += accR.get(i);
            }
            arStd = (float) Math.sqrt(arStd/accR.size());

            // Gyro Variance and Std
            gxStd = 0;
            gyStd = 0;
            gzStd = 0;
            for(int i = 0; i < gyrX.size(); i++){
                gxStd += gyrX.get(i);
                gyStd += gyrY.get(i);
                gzStd += gyrZ.get(i);
            }
            gxStd = (float) Math.sqrt(gxStd/gyrX.size());
            gyStd = (float) Math.sqrt(gyStd/gyrY.size());
            gzStd = (float) Math.sqrt(gzStd/gyrZ.size());

            queryMtn.put("arStd", arStd);
            queryMtn.put("arAmp", arMax - arMin);
            queryMtn.put("gxStd", gxStd);
            queryMtn.put("gxAmp", gxMax - gxMin);
            queryMtn.put("gyStd", gyStd);
            queryMtn.put("gyAmp", gyMax - gyMin);
            queryMtn.put("gzStd", gzStd);
            queryMtn.put("gzAmp", gzMax - gzMin);
            Log.i(null, String.valueOf(queryMtn));

            motionRecordStartT = 0L;
            accT.clear();
            accR.clear();
            gyrT.clear();
            gyrX.clear();
            gyrY.clear();
            gyrZ.clear();

            kNNMtn(motionList, queryMtn);

//            calculateMtnDists(motionList, queryMtn);
//            initializeKClosest("Mtn");
//            String majMtn = findMtnMajority();
//
//            //   textLocate.setText("You are in room : " + majority);
//            setCurrTextViewMtn(majMtn);
//            currTextViewMtn.setBackgroundColor(Color.parseColor("#FFA500"));
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void calculatePosDists(List<Room> roomList, HashMap<String, Integer> query) {
     //   String t = "";
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
        //    t += "\ndistance from room " + r.roomName + " = " + distance;
            //   textLocate.setText(textLocate.getText() + "\ndistance from room " + r.roomName + " = " + distance);
            posResultList.add(new Result(distance, r.roomName));
        }
     //   textTest.setText(t);
//        for (Room r : roomList) {
//            double dist = 0.0;
//            int cnt = 0;
//            textTest.setText(textTest.getText() + "\nAGAIN (Position)");
//            for (Map.Entry<String, Integer> entry : r.roomAttributes.entrySet()) {
//                int d1 = entry.getValue();
//                int d2 = query.getOrDefault(entry.getKey(), d1);
//                dist += Math.pow(d1 - d2, 2);
//                // consider only 5 common nearest APs
//                if (dist != 0 && ++cnt == 5) {
//                    break;
//                }
//            }
//            double distance = Math.sqrt(dist);
//            textTest.setText(textTest.getText() + "\ndistance from room " + r.roomName + " = " + distance);
//            posResultList.add(new Result(distance, r.roomName));
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void calculateMtnDists(List<Motion> motionList, HashMap<String, Float> query) {
      //  String t = "";
        for (Motion m : motionList) {
            double dist = 0.0;
            int cnt = 0;
            // textLocate.setText(textLocate.getText() + "\nAGAIN");
            for (Map.Entry<String, Float> entry : m.motionAttributes.entrySet()) {
                float d1 = entry.getValue();
                float d2 = query.get(entry.getKey());
                ++cnt;
                dist += Math.pow(d1 - d2, 2);
                //      test += "\ndistance from entry " + d1 + " - " + d2;
                //     if (cnt == 5) {
                //         break;
                //     }
            }
            double distance = Math.sqrt(dist);
         //   t += "\ndistance from room " + m.motionName + " = " + distance;
            //   textLocate.setText(textLocate.getText() + "\ndistance from room " + r.roomName + " = " + distance);
            mtnResultList.add(new Result(distance, m.motionName));
        }
      //  textTest.setText(t);
//        for (Motion m : motionList) {
//            double dist = 0.0;
//            int cnt = 0;
//            textTest.setText(textTest.getText() + "\nAGAIN (Motion)");
//            for (Map.Entry<String, Float> entry : m.motionAttributes.entrySet()) {
//                float d1 = entry.getValue();
//                float d2 = query.getOrDefault(entry.getKey(), d1);
//                dist += Math.pow(d1 - d2, 2);
//                // consider only 5 common nearest APs
//                if (dist != 0 && ++cnt == 5) {
//                    break;
//                }
//            }
//            double distance = Math.sqrt(dist);
//            textTest.setText(textTest.getText() + "\ndistance from motion " + m.motionName + " = " + distance);
//            mtnResultList.add(new Result(distance, m.motionName));
//        }
    }

    public void initializeKClosest(String type) {
        List<Result> kNearest = null;
        PriorityQueue<Result> resultList = null;
        int k=7;
        if (type == "Pos") {
            kNearest = kNearestRooms;
            resultList = posResultList;
            k = kPos;
        }
        else if (type == "Mtn") {
            kNearest = kNearestMotions;
            resultList = mtnResultList;
            k = kMtn;
        }
        for (int i = 0; i < k; i++) {
            kNearest.add(resultList.poll());
        }
    }

    private String findPosMajority() {
        currResultPos = findMajority("Pos");;
        return currResultPos;
    }

    private String findMtnMajority() {
        currResultMtn = findMajority("Mtn");;
        return currResultMtn;
    }

    private String findMajority(String type) {
        String majority = "Empty";
        HashMap<String, Double> weights = new HashMap<>();

        // Take weights into account
        int totalSum = 0;
        List<Result> kNearest = null;
        if (type == "Pos") {
            kNearest = kNearestRooms;
        }
        else if (type == "Mtn") {
            kNearest = kNearestMotions;
        }
        for (Result r : kNearest){
            totalSum += r.distance;
            weights.put(r.name, weights.getOrDefault(r.name, 0.0) + 1/r.distance);
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

        return majority;
    }

    public void setCurrTextViewPos(String majority) {
        if (currTextViewPos != null) {
            currTextViewPos.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }
        switch (majority) {
            case ("Kitchen") :
                currTextViewPos = tvKitchen;
                break;
            case ("Balcony") :
                currTextViewPos = tvBalcony;
                break;
            case ("Bedroom1") :
                currTextViewPos = tvBedroom1;
                break;
            case ("Bedroom2") :
                currTextViewPos = tvBedroom2;
                break;
            case ("Toilet") :
                currTextViewPos = tvToilet;
                break;
            case ("Living Room") :
                currTextViewPos = tvLivingRoom;
                break;
        }
    }

    public void setCurrTextViewMtn(String majority) {
        if (currTextViewMtn != null) {
            currTextViewMtn.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }
        switch (majority) {
            case ("Sitting") :
                currTextViewMtn = tvSit;
                break;
            case ("Walking") :
                currTextViewMtn = tvWalk;
                break;
            case ("Push-Ups") :
                currTextViewMtn = tvPushUp;
                break;
            case ("Bedroom2") :
                currTextViewPos = tvBedroom2;
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        if (sensorEnable == false) {
            return;
        }
        if (motionRecordStartT == 0) {
            motionRecordStartT = evt.timestamp/1000000;
        }
        if (evt.sensor == accel) {
            accT.add(evt.timestamp/1000000); // Store milliseconds
            accR.add((float) Math.sqrt(Math.pow(evt.values[0], 2) + Math.pow(evt.values[1], 2) + Math.pow(evt.values[0], 2)));
        }
        else if (evt.sensor == gyro) {
            gyrT.add(evt.timestamp/1000000); // Store milliseconds
            gyrX.add(evt.values[0]);
            gyrY.add(evt.values[1]);
            gyrZ.add(evt.values[2]);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static class Result {
        double distance;
        String name;
        public Result(double distance, String name) {
            this.distance = distance;
            this.name = name;
        }
    }

    private static class DistanceComparator implements Comparator<Result> {
        @Override
        public int compare(Result a, Result b) {
            return a.distance < b.distance ? -1 : a.distance == b.distance ? 0 : 1;
        }
    }
}