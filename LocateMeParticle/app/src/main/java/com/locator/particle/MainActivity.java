package com.locator.particle;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.constraint.solver.ArrayLinkedVariables;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.locator.particle.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Smart Phone Sensing Example 4. Wifi received signal strength.
 */
public class MainActivity extends Activity implements View.OnClickListener, SensorEventListener {


    /**
     * Values of every window
     * Pair of magnitude and timestamp
     */
    private ArrayList<Window> windows = new ArrayList<>();

    /**
     * the time to complete a window
     */
    private double windowEnd = 1.0;

    /**
     * starting timestsamp of current window
     */
    private double windowStart = 0;

    /**
     * Boolean to check if all particles are dead
     */
    private boolean allDead = false;

    private double windowTime = windowEnd - windowStart;

    /**
     * Boolean to check if we want cellular scan
     */
    private boolean useCellularScan = false;

    /**
     * Value got from light sensor
     */
    private float grayShade;

    /**
     * Arrays to keep accel & magnetic values
     */
    private float[] mGravity;
    private float[] mGeomagnetic;

    /**
     * vertical acceleration used for dynamic stride
     * calculation on Window
     */
    private float verticalAcc;

    /**
     * values to transform & scale particles
     */
    private float top, left, right, bot, scaleX, scaleY;

    private HashMap<String, Integer> cellularStrength;

    /**
     * The Sensor manager.
     */
    private SensorManager sensorManager;

    /**
     * Cellular scanner
     */
    private CellularScanner cellularScanner;

    private Sensor step;
    private Sensor accel;
    private Sensor magnetic;
    private Sensor light;
    private Long sensUpdTime = 0L;

    private int stepCounter = 0;

    /**
     * Standard deviation of gaussian distribution
     * for angle
     * TODO: Find best std
     */
    double stdA = Math.PI/12;
    /**
     * Standard deviation of gaussian distribution
     * for dist
     * TODO: Find best std
     */
    double stdD = 0.1;

    /**
     * Weight threshold to destroy a particle
     * TODO: Find best threshold
     */
    private double weightThreshold = 0.1;

    /**
     * Height of person using the app in m
     */
    private double height = 1.75;

    /**
     * Average step length in meters
     * TODO: Find accurate multiplier
     */
    private Double stepLength = 0.54; //height * 0.35;

    /**
     * Number of steps to change weights
     * TODO: Find best numSteps
     */
    private int numSteps = 3;

    private float direction = 0;

    /**
     * Number of particles
     * TODO: Find best numParticles
     */
    private int numParticles = 10000;

    /**
     * Map
     */
    private Map map;

    /**
     * Previous map to use when all particles die
     */
    private Map prevMap;

    /**
     * View and first bitmap for drawings
     */
    private View outerLayout;
    private View innerLayout;
    private Bitmap bm;

    /**
     * Flag for enabling/disabling response to sensor events.
     */
    boolean sensorEnable = false;

    /**
     * The text view.
     */
    private TextView textStatus;

    /**
     * Textviews of different Rooms
     * will be used to paint them
     */
    private TextView tvCell1, tvCell2, tvCell3, tvCell4, tvCell5, tvCell6, tvCell7, tvCell8,
            currTextViewPos;

    private Button buttonStart, buttonSnap;

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
     * Particles object
     */
    private Particles particles = new Particles(numParticles, useCellularScan);

    /**
     * Keep last state of particles to revive them when they all die
     */
    private Particles prevState;

    private ArrayList<Particle> particleList;

    /**
     * Activate/Deactivate reading of sample from sensor
     */
    boolean sampleRead = true;

    /**
     * Sampling Period For Sensor (in ms)
     */
    int samplingPeriod = 50;

    /**
     * Timer for reading accel values
     */
    Timer accelTimer = new Timer();

    /**
     * Number of samples collected in a window
     */
    int sampleCount = 0;

    /**
     * Max number of samples in 1 window
     */
    int windowNumSamples = (int) ((windowEnd*1000)/samplingPeriod);

    private Window window = new Window(windowNumSamples);

    boolean walking = false;

    boolean bufferingDone = false;

    String probCell = "";

    boolean restrictPerpendicular = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set application context
        mContext = getApplicationContext();

        // Create items.
        textStatus = findViewById(R.id.textStatus);
        buttonStart = findViewById(R.id.buttonStart);
        buttonSnap = findViewById(R.id.buttonSnap);
        tvCell1 = findViewById(R.id.tvCell1);
        tvCell2 = findViewById(R.id.tvCell2);
        tvCell3 = findViewById(R.id.tvCell3);
        tvCell4 = findViewById(R.id.tvCell4);
        tvCell5 = findViewById(R.id.tvCell5);
        tvCell6 = findViewById(R.id.tvCell6);
        tvCell7 = findViewById(R.id.tvCell7);
        tvCell8 = findViewById(R.id.tvCell8);

        buttonStart.setOnClickListener(this);

        // Set listener for the button.
        buttonSnap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                restrictPerpendicular = !restrictPerpendicular;
                if (restrictPerpendicular) {
                    buttonSnap.setText("⟂ Snap ON");
                    buttonSnap.setBackgroundColor(Color.parseColor("#3BA55C"));
                }
                else {
                    buttonSnap.setText("⟂ Snap ON");
                    buttonSnap.setBackgroundColor(Color.parseColor("#CA3E47"));
                }
            }
        });

        // Add any new rooms here
        cellNames.addAll(Arrays.asList("Cell 1", "Cell 2", "Cell 3", "Cell 4", "Cell 5", "Cell 6", "Cell 7", "Cell 8"));

        //Add sensor stuff
        // Check if we want stepDetector instead (better latency, worse accuracy)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        step = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    //    light =  sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        cellularScanner = new CellularScanner(mContext, 3);

        sensorManager.registerListener( this, step, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener( this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener( this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
     //   sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_FASTEST);

        // Initialize map
        map = new Map(particles, numParticles);

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onClick(View v) {
//        textStatus.setText("Running...\n");
        buttonStart.setEnabled(false);
        buttonSnap.setEnabled(false);
        sensorEnable = true;

        //setContentView(paint);
        outerLayout = getWindow().getDecorView().findViewById(R.id.root);
        innerLayout = getWindow().getDecorView().findViewById(R.id.relativeLayout);
        // Store first bitmap, will be used to redraw everytime
        outerLayout.setDrawingCacheEnabled(true);
        bm = Bitmap.createBitmap(outerLayout.getDrawingCache());
        outerLayout.setDrawingCacheEnabled(false);

        // Calculate transform & scale values according to screen
        float mW = innerLayout.getWidth();
        float mH = innerLayout.getHeight();

        scaleX = mW / (float) 6.56;
        scaleY = mH / (float) 10.81;
        top = innerLayout.getTop();
        left = innerLayout.getLeft();
        bot = innerLayout.getBottom();
        right = innerLayout.getRight();

        runOnUiThread(draw);
//        handler.post(runner);
        accelTimer.schedule(readAccelSensor, 0, samplingPeriod);

    }

//    private Runnable runner = new Runnable() {
//        @RequiresApi(api = Build.VERSION_CODES.N)
//        @Override
//        public void run() {
//            sampleRead = true;
//            handler.postDelayed(runner, samplingPeriod);
//        }
//    };

    private TimerTask readAccelSensor = new TimerTask() {
        @Override
        public void run() {
            if (mGravity == null) {
                return;
            }
            double magnitude = Math.sqrt(Math.pow(mGravity[0], 2) +
                    Math.pow(mGravity[1], 2) + Math.pow(mGravity[2], 2));

//            window.addValues(sampleCount, magnitude, direction, verticalAcc);
            window.addSample(magnitude, direction);
            sampleCount++;

            // windowEnd: sec, samplingPeriod: ms
            if (sampleCount == windowNumSamples) {
                Pair<Double, Float> motion = window.getMotion(stepLength, bufferingDone);
                walking = (motion != null);
//                System.out.println(walking);
                if (!bufferingDone) {
                    bufferingDone = true;
                    windowNumSamples /= 2;
                }

                if (walking && motion.first != 0) {
                    Particles prevState = new Particles(particles);
                    particles.updatePositions(motion.first, motion.second, stdA, stdD, map, restrictPerpendicular);
                    particles.calculateWeights(map, weightThreshold, cellularStrength, grayShade);
                    particles.normalizeWeights();
                    allDead = particles.updateParticles(stdD);
                    particles.normalizeWeights();  //TODO: Is this Normalize weights again needed? ( I think yes for weightThreshold to be effective )

                    if (allDead) {
                        particles = prevState;
                        map.setParticles(particles);
                    }
                    int aliveParticles = 0;
                    for (Particle p : particles.totalParticles) {
                        if (p.isAlive) aliveParticles++;
                    }
                    probCell = map.getPosition();
                    System.out.println("Alive particles : " + aliveParticles);
                }


//                if (walking) {
//
//                    particles.updatePositions(state.first, state.second, stdA, stdD, map);
//                    particles.calculateWeights(map, weightThreshold, cellularStrength, grayShade);
//                    particles.normalizeWeights();
//                    allDead = particles.updateParticles(stdD);
//                    particles.normalizeWeights();  //TODO: Is this Normalize weights again needed? ( I think yes for weightThreshold to be effective )
//
//                    if (allDead) {
//                        map = new Map(particles, numParticles);
//                    }
//                    int aliveParticles = 0;
//                    for (Particle p : particles.totalParticles) {
//                        if (p.isAlive) aliveParticles++;
//                    }
//                    probCell = map.getPosition();
//                    System.out.println("Alive particles : " + aliveParticles);
//                }
                if (!allDead) {
                    runOnUiThread(draw);
                }

                sampleCount = 0;
//                windows.add(window);
//                window = new Window(windowNumSamples);
            }
        }
    };


    public void setCurrTextViewClr(String majority) {
        tvCell1.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell2.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell3.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell4.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell5.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell6.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell7.setBackgroundColor(Color.parseColor("#D3D3D3"));
        tvCell8.setBackgroundColor(Color.parseColor("#D3D3D3"));
        switch (majority) {
            case ("Cell 1") :
                tvCell1.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 2") :
                tvCell2.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 3") :
                tvCell3.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 4") :
                tvCell4.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 5") :
                tvCell5.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 6") :
                tvCell6.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 7") :
                tvCell7.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
            case ("Cell 8") :
                tvCell8.setBackgroundColor(Color.parseColor("#FFA500"));
                break;
        }
    }

    private Runnable draw = new Runnable() {
        @Override
        public void run() {
            Bitmap firstBm = Bitmap.createBitmap(bm);
            Canvas cv = new Canvas(firstBm);
            Paint paint = new Paint();
            paint.setTextSize(right*4/100);
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setAntiAlias(true);

            for (Particle p : particles.totalParticles) {
                cv.drawPoint(left + (float) p.x * scaleX, top + (float) p.y * scaleY, paint);
            }

            String status = "Running...";
            String statusCell = "";
            if (walking) {
                status += ("State: Walking");
            }
            else {
                status += ("State: Stationary");
            }
            if (probCell != "") {
                statusCell += ("Position: " + probCell);
            }
            cv.drawText(status, left, bot - bot*4/100,  paint);
            cv.drawText(statusCell, left, bot,  paint);
            ImageView imageView = new ImageView(mContext);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(imageView);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setImageBitmap(firstBm);

        }
    };

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEnable == false) {
            return;
        }
        sensUpdTime = sensorEvent.timestamp/1000000;

        if (sensorEvent.sensor == step) {
            // Scan cellular networks
            if (useCellularScan) {
                cellularStrength = cellularScanner.scanNetworks();
            }
        }
        if (sensorEvent.sensor == accel) {
            mGravity = sensorEvent.values;
            verticalAcc = sensorEvent.values[1];
        }
        if (sensorEvent.sensor == magnetic) {
            mGeomagnetic = sensorEvent.values;
        }
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                direction = orientation[0];
            }
        }
//        else if (sensorEvent.sensor == light) {
//            grayShade = sensorEvent.values[0];
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}