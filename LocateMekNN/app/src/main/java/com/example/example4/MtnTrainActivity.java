package com.example.example4;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MtnTrainActivity extends Activity implements OnClickListener, SensorEventListener {
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
     * Start time of sensor recording.
     */
    Long recordStartT = 0L;

    /**
     * The text view.
     */
    private TextView textMtnTrain;
    /**
     * The buttons.
     */
    private Button buttonMtnTrain, buttonSit, buttonWalk, buttonPushUp, currentButton;

    /**
     * Keep a list of all buttons for convenience
     */
    private ArrayList<Button> buttonList = new ArrayList<>();

    /**
     * The room we have selected
     */
    private String currentMotion = "Not Selected";

    /**
     * Activity record duration (in seconds)
     * Lower duration may result in insufficient samples for k-computation
     */
    private int recordDuration = 45;

    /**
     * Sliding window duration (in seconds)
     */
    private int windowLen = 2;

    /**
     * Gyro and Accel stats values for each room
     */
    private HashMap<String, Float> motionStats = new HashMap<>();

    /**
     * Context view
     */
    private Context mContext;

    /**
     * Hashmap containing the completed motions
     * Will be used for keeping colors
     */
    private HashMap<Integer, Boolean> completedMotions = new HashMap<>();

    /**
     * List containing instances of motions
     * A motion contains motion name and a list of
     * stats of accelerometer readings
     */
    private List<Motion> motionList = new ArrayList<>();

    /**
     * Animation used for completion
     */
    private Animation mShakeAnimation;

    /**
     * Handler used for updating text dynamically
     */
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mtn_train);

        //Set application context (will be used for animation)
        mContext = getApplicationContext();

        textMtnTrain = findViewById(R.id.textMtnTrain);
        buttonMtnTrain = findViewById(R.id.buttonMtnTrain);
        buttonWalk = findViewById(R.id.buttonWalk);
        buttonSit = findViewById(R.id.buttonSit);
        buttonPushUp = findViewById(R.id.buttonPushUp);

        Button tempList[] = new Button[]{buttonMtnTrain, buttonSit, buttonWalk,
                buttonPushUp};

        textMtnTrain.setMovementMethod(new ScrollingMovementMethod());
        buttonList.addAll(Arrays.asList(tempList));

        // Set listener for the buttons.
        for (Button b : buttonList) {
            b.setOnClickListener(this);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        // Set current motion
        if (v.getId() != R.id.buttonMtnTrain) {
            setMotion(v);
        }

        if (v.getId() == R.id.buttonMtnTrain) {
            if (currentButton == null) {
                textMtnTrain.setText("Please choose the motion you are currently in before scanning.\n");
            } else {
                // clear hashmap to calculate the mean for the new motion
                disableButtonsExceptSelf();
                buttonList.remove(currentButton);
                TransitionDrawable transition = (TransitionDrawable) currentButton.getBackground();
                transition.startTransition((recordDuration+2)*1000);
                //2 seconds to put phone in position
                textMtnTrain.setText("\n\tStarting...");
                handler.postDelayed(run, 2000);
            }
        }
    }

    private Runnable run = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if (sensorEnable == false) {
                // Set text.
                textMtnTrain.setText("\n\tRecording stats for " + currentMotion + ". Please wait until complete.");
                sensorEnable = true;
                handler.postDelayed(this, recordDuration*1000);
            }
            else {
                sensorEnable = false;
                handler.removeCallbacksAndMessages(null);
                for (long t=recordStartT; t<(recordStartT + (recordDuration-(windowLen/2))*1000); t+=((windowLen/2)*1000)) {
                    List<Float> ar = new ArrayList<Float>();
                    List<Float> gx = new ArrayList<Float>();
                    List<Float> gy = new ArrayList<Float>();
                    List<Float> gz = new ArrayList<Float>();
                    for (int i=0; i<accT.size(); i++) {
                        if (accT.get(i)>t && accT.get(i)<=t+(windowLen*1000)) {
                            ar.add(accR.get(i));
                        }
                    }
                    for (int i=0; i<gyrT.size(); i++) {
                        if (gyrT.get(i)>t && gyrT.get(i)<=t+(windowLen*1000)) {
                            gx.add(gyrX.get(i));
                            gy.add(gyrY.get(i));
                            gz.add(gyrZ.get(i));
                        }
                    }

                    float arStd = 0, arMin=ar.get(0), arMax=ar.get(0);
                    float gxStd = 0, gxMin=gx.get(0), gxMax=gx.get(0);
                    float gyStd = 0, gyMin=gy.get(0), gyMax=gy.get(0);
                    float gzStd = 0, gzMin=gz.get(0), gzMax=gz.get(0);

                    // Accel Total & Mean
                    for(int i = 0; i < ar.size(); i++){
                        arStd += ar.get(i);
                        if (ar.get(i) < arMin) {
                            arMin = ar.get(i);
                        }
                        else if (ar.get(i) > arMax) {
                            arMax = ar.get(i);
                        }
                    }
                    arStd = arStd/ar.size();

                    // Gyro Total & Mean
                    for(int i = 0; i < gx.size(); i++){
                        gxStd += gx.get(i);
                        if (gx.get(i) < gxMin) {
                            gxMin = gx.get(i);
                        }
                        else if (gx.get(i) > gxMax) {
                            gxMax = gx.get(i);
                        }

                        gyStd += gy.get(i);
                        if (gy.get(i) < gyMin) {
                            gyMin = gy.get(i);
                        }
                        else if (gy.get(i) > gyMax) {
                            gyMax = gy.get(i);
                        }

                        gzStd += gz.get(i);
                        if (gz.get(i) < gzMin) {
                            gzMin = gz.get(i);
                        }
                        else if (gz.get(i) > gzMax) {
                            gzMax = gz.get(i);
                        }

                    }
                    gxStd = gxStd/gx.size();
                    gyStd = gyStd/gy.size();
                    gzStd = gzStd/gz.size();

                    // Accel Square Errors
                    for(int i = 0; i < ar.size(); i++){
                        ar.set(i, (float) Math.pow((ar.get(i)-arStd),2));
                    }

                    // Gyro Square Errors
                    for(int i = 0; i < gx.size(); i++){
                        gx.set(i, (float) Math.pow((gx.get(i)-gxStd),2));
                        gy.set(i, (float) Math.pow((gy.get(i)-gyStd),2));
                        gz.set(i, (float) Math.pow((gz.get(i)-gzStd),2));
                    }

                    // Accel Variance and Std
                    arStd = 0;
                    for(int i = 0; i < ar.size(); i++){
                        arStd += ar.get(i);
                    }
                    arStd = (float) Math.sqrt(arStd/ar.size());

                    // Gyro Variance and Std
                    gxStd = 0;
                    gyStd = 0;
                    gzStd = 0;
                    for(int i = 0; i < gx.size(); i++){
                        gxStd += gx.get(i);
                        gyStd += gy.get(i);
                        gzStd += gz.get(i);
                    }
                    gxStd = (float) Math.sqrt(gxStd/gx.size());
                    gyStd = (float) Math.sqrt(gyStd/gy.size());
                    gzStd = (float) Math.sqrt(gzStd/gz.size());

                    motionStats.clear();
                    motionStats.put("arStd", arStd);
                    motionStats.put("arAmp", arMax - arMin);
                    motionStats.put("gxStd", gxStd);
                    motionStats.put("gxAmp", gxMax - gxMin);
                    motionStats.put("gyStd", gyStd);
                    motionStats.put("gyAmp", gyMax - gyMin);
                    motionStats.put("gzStd", gzStd);
                    motionStats.put("gzAmp", gzMax - gzMin);
//                    Log.i(null, String.valueOf(t) + ", " + String.valueOf(arStd) + ", " + String.valueOf(arMax - arMin) + ", " +
//                            String.valueOf(gxStd) + ", " + String.valueOf(gxMax - gxMin) + ", " +
//                            String.valueOf(gyStd) + ", " + String.valueOf(gyMax - gyMin) + ", " +
//                            String.valueOf(gzStd) + ", " + String.valueOf(gzMax - gzMin));
                    motionList.add(new MtnTrainActivity.Motion(motionStats, currentMotion));
                }
                recordStartT = 0L;
                accT.clear();
                accR.clear();
                gyrT.clear();
                gyrX.clear();
                gyrY.clear();
                gyrZ.clear();

                completedMotions.put(currentButton.getId(), true);
                mShakeAnimation = AnimationUtils.loadAnimation(mContext,R.anim.shake_animation);
                currentButton.startAnimation(mShakeAnimation);
                currentButton.setEnabled(false);
                enableButtonsExceptSelf();
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibe.vibrate(500);
                DataHolder app = (DataHolder) getApplicationContext();
                motionList.add(new Motion(motionStats, currentMotion));
                app.setMotionList(motionList);
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
    public void setMotion(View v) {

        if (currentButton != null && !completedMotions.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackgroundColor(Color.parseColor("#D3D3D3"));
        }

        switch (v.getId()) {
            case R.id.buttonSit:
                currentButton = buttonSit;
                currentMotion = "Sitting";
                break;
            case R.id.buttonWalk:
                currentButton = buttonWalk;
                currentMotion = "Walking";
                break;
            case R.id.buttonPushUp:
                currentButton = buttonPushUp;
                currentMotion = "Push-Ups";
                break;
        }

        int resId = R.drawable.button_gradual_fill;
        Drawable transitionDrawable = getApplicationContext().getResources().getDrawable(resId);
        if (!completedMotions.getOrDefault(currentButton.getId(), false)) {
            currentButton.setBackground(transitionDrawable);
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onSensorChanged(SensorEvent evt) {
        if (sensorEnable == false) {
            return;
        }
        if (recordStartT == 0) {
            recordStartT = evt.timestamp/1000000;
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

    static class Motion {
        HashMap<String, Float> motionAttributes;
        String motionName;
        public Motion(HashMap<String, Float>  motionAttributes, String motionName) {
            this.motionAttributes = (HashMap<String, Float>) motionAttributes.clone();
            this.motionName = motionName;
        }
    }

    public List<Motion> getMotionList() {
        return motionList;
    }
}