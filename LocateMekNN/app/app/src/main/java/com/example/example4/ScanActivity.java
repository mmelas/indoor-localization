package com.example.example4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class ScanActivity extends Activity implements OnClickListener{

    /**
     * The wifi manager.
     */
    private WifiManager wifiManager;
    /**
     * The text view.
     */
    private TextView textRssi;
    /**
     * The buttons.
     */
    private Button buttonRssi, buttonBalcony, buttonToilet, buttonLR,
                   buttonBedroom1, buttonBedroom2, buttonKitchen, currentButton;
    /**
     * Keep a list of all buttons for convenience
     */
    private ArrayList<Button> buttonList = new ArrayList<>();

    /**
     * The room we have selected
     */
    private String currentRoom = "Not Selected";

    /**
     * The desired number of samples (scans).
     */
    private int numberOfScans = 600;

    /**
     * A copy of number of scans to help
     * keep track of iterations
     */
    private int tempNumOfScans;

    /**
     * Delay between scans
     */
    private int delay = 200;

    /**
     * how many values are used for each average
     * (repeated scans)
     */
    private int repeatTimes = 3;

    /**
     * RSSI values for the indicated
     * number of scans for each room
     */
    private HashMap<String, Integer> roomRssi = new HashMap<>();
    private HashMap<String, Integer> roomOcc = new HashMap<>();

    /**
     * Context view
     */
    private Context mContext;

    /**
     * Hashmap containing the completed rooms
     * Will be used for keeping colors
     */
    private HashMap<Integer, Boolean> completedRooms = new HashMap<>();

    /**
     * List containing instances of Rooms
     * A room contains room name and a list of
     * mean wifi levels
     */
    private List<Room> roomList = new ArrayList<>();

    /**
     * List to collect samples for testing
     */
    private List<Room> testList = new ArrayList<>();

    /**
     * Animation used for completion
     */
    private Animation mShakeAnimation;

    /**
     * Handler used for updating text dynamically
     */
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos_train);

        //Set application context (will be used for animation)
        mContext = getApplicationContext();

        // Create items.
        textRssi = findViewById(R.id.textPosTrain);
        buttonRssi = findViewById(R.id.buttonPosTrain);
        buttonBalcony = findViewById(R.id.buttonBalcony);
        buttonToilet = findViewById(R.id.buttonToilet);
        buttonLR = findViewById(R.id.buttonLR);
        buttonBedroom1 = findViewById(R.id.buttonBedroom1);
        buttonBedroom2 = findViewById(R.id.buttonBedroom2);
        buttonKitchen = findViewById(R.id.buttonKitchen);

        Button tempList[] = new Button[]{buttonRssi, buttonBalcony, buttonToilet,
                buttonLR, buttonBedroom1, buttonBedroom2,
                buttonKitchen};

        textRssi.setMovementMethod(new ScrollingMovementMethod());
        buttonList.addAll(Arrays.asList(tempList));


        // Set listener for the buttons.
        for (Button b : buttonList) {
            b.setOnClickListener(this);
        }
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
        if (v.getId() != R.id.buttonPosTrain) {
            setRoom(v);
        }

        // Set number of scans everytime the button is clicked
        tempNumOfScans = numberOfScans;
        roomOcc.clear();
        roomRssi.clear();

        if (v.getId() == R.id.buttonPosTrain) {
            if (currentButton == null) {
                textRssi.setText("Please choose the room you are currently in before scanning.\n");
            } else {
                // clear hashmap to calculate the mean for the new room
                disableButtonsExceptSelf();
                buttonList.remove(currentButton);
                TransitionDrawable transition = (TransitionDrawable) currentButton.getBackground();
                transition.startTransition(delay * numberOfScans);

                handler.post(run);
            }
        }
    }

    private Runnable run = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            // Set text.
            textRssi.setText("\n\tScan all access points in " + currentRoom + ". Please wait until scan is complete.");
            // Set wifi manager.
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // Start a wifi scan.
            wifiManager.startScan();
            // Store results in a list.
            List<ScanResult> scanResults = wifiManager.getScanResults();
            // Write results to a label
            for (ScanResult scanResult : scanResults) {
                String currentBSSID = scanResult.BSSID;
                //roomRssi.put(currentBSSID, scanResult.level);
                roomRssi.put(currentBSSID, roomRssi.getOrDefault(currentBSSID, 0) + scanResult.level);
                roomOcc.put(currentBSSID, roomOcc.getOrDefault(currentBSSID, 0) + 1);
                textRssi.setText(textRssi.getText() + "\n\tBSSID = "
                        + currentBSSID + "    RSSI = "
                        + scanResult.level + "dBm");
            }
            // Add the scanned Rssi values to the list

      //      roomList.add(new Room(roomRssi, currentRoom));

            // Do a scan every 1 second for indicated number of scans.
            handler.postDelayed(this, delay);

            // Last 10% of samples will be used for testing
            if (--tempNumOfScans == 0) {
                completedRooms.put(currentButton.getId(), true);
                mShakeAnimation = AnimationUtils.loadAnimation(mContext,R.anim.shake_animation);
                currentButton.startAnimation(mShakeAnimation);
                handler.removeCallbacksAndMessages(null);
                // Scan is done, disable current button and enable the other buttons
                currentButton.setEnabled(false);
                enableButtonsExceptSelf();
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(500);
                DataHolder app = (DataHolder) getApplicationContext();
                // average RSSI values
        //        roomRssi.replaceAll((k,v) -> v/roomOcc.get(k));
                roomList.add(new Room(roomRssi, currentRoom));
                app.setRoomList(roomList);
                app.setTestList(testList);
            }
            else if (tempNumOfScans % repeatTimes == 0) {
                roomRssi.replaceAll((k,v) -> v/roomOcc.get(k));
                roomList.add(new Room(roomRssi, currentRoom));
                roomOcc.clear();
                roomRssi.clear();
            }
        }
    };

    private void disableButtonsExceptSelf() {
        for (Button b : buttonList) {
            if (b != currentButton) {
                b.setEnabled(false);
            }
        }
    }
    private void enableButtonsExceptSelf() {
        for (Button b : buttonList) {
            if (b != currentButton) {
                b.setEnabled(true);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setRoom(View v) {

        if (currentButton != null && !completedRooms.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }

        switch (v.getId()) {
            case R.id.buttonBalcony:
                currentButton = buttonBalcony;
                currentRoom = "Balcony";
                break;
            case R.id.buttonBedroom1:
                currentButton = buttonBedroom1;
                currentRoom = "Bedroom1";
                break;
            case R.id.buttonBedroom2:
                currentButton = buttonBedroom2;
                currentRoom = "Bedroom2";
                break;
            case R.id.buttonKitchen:
                currentButton = buttonKitchen;
                currentRoom = "Kitchen";
                break;
            case R.id.buttonLR:
                currentButton = buttonLR;
                currentRoom = "Living Room";
                break;
            case R.id.buttonToilet:
                currentButton = buttonToilet;
                currentRoom = "Toilet";
                break;
        }

        int resId = R.drawable.button_gradual_fill;
        Drawable transitionDrawable = getApplicationContext().getResources().getDrawable(resId);
        if (!completedRooms.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackground(transitionDrawable);
        }
    }

    static class Room {
        HashMap<String, Integer> roomAttributes;
        String roomName;
        public Room(HashMap<String, Integer>  roomAttributes, String roomName) {
            this.roomAttributes = (HashMap<String, Integer>) roomAttributes.clone();
            this.roomName = roomName;
        }
    }

    public List<Room> getRoomList() {
        return roomList;
    }

}