package com.locator.particle;

import android.util.Pair;

public class Window {
    private double[] samplesAcc;
    private double[] samplesAccFilt;
//    private double[] verticalAcc;
    private float[] samplesDir;

    private double[] butterX = new double[2];
    private double[] butterY = new double[2];

    boolean sampStarted = false;

    private int numSamples = 0;
    private int currId = 0;

    // static var for stride equation
//    private double K = 0.32;

//    public Window(int numSamples) {
//        samplesAcc = new double[numSamples];
//        samplesDir = new float[numSamples];
//        verticalAcc = new double[numSamples];
//    }

    public Window(int numSamples) {
        samplesAcc = new double[numSamples];
        samplesAccFilt = new double[numSamples];
        samplesDir = new float[numSamples];
        this.numSamples = numSamples;
    }

//    public void addValues(int index, Double acc, Float dir, double vAcc) {
//        samplesAcc[index] = acc;
//        samplesDir[index] = dir;
//        verticalAcc[index] = vAcc;
//    }
    public double butterFirst(double val) {
        if (!sampStarted) {
            sampStarted = true;
            butterX[1] = val;
            butterY[1] = val;
        }
        butterX[0] = butterX[1];
        butterX[1] = val;
        butterY[0] = butterY[1];
        butterY[1] = ((butterX[0] + butterX[1]) + (62.657 * butterY[0]))/64.657;
        return butterY[1];
    }

    public void addSample(Double acc, Float dir) {
        samplesAcc[currId] = acc;
        samplesDir[currId] = dir;
        samplesAccFilt[currId] = butterFirst(acc);
        currId = (currId+1)%numSamples;
    }

//    public double subWindowCorr(int t) {
//        int t2;
//        if (t > samplesAcc.length/2) {
//            t2 = samplesAcc.length - t;
//        }
//        else {
//            t2 = t;
//        }
//        double mean1 = mean(0, t2);
//        double mean2 = mean(t, t2);
//
//        double std1= std(0, t2);
//        double std2 = std(t, t2);
//
//        double coeff = 0;
//        for (int i=0; i<t2; i++) {
//            coeff += ((samplesAcc[i]-mean1) * (samplesAcc[t+i]-mean2));
//        }
//        coeff /= (t2*std1*std2);
//        return coeff;
//    }

//    public Pair<Integer, Double> maxCorr() {
//        int t0 = 0;
//        double coeff0 = 0;
//        for (int t = (samplesAcc.length/4); t<=(samplesAcc.length/2); t++) {
//            double coeff = subWindowCorr(t);
//            if (coeff > coeff0) {
//                t0 = t;
//                coeff0 = coeff;
//            }
//        }
//        Pair<Integer, Double> corr = new Pair<>(t0, coeff0);
//        return corr;
//    }

//    public Pair<Double, Float> getMotion(boolean currWalking) {
//        double mean = mean();
//        double std = std();
//        Pair<Integer, Double> corr = maxCorr();
////        System.out.print(String.format ("%,.3f", std) + "\t" + corr.first + "\t" + String.format ("%,.3f", corr.second) + "\t");
//        if (std<0.02*mean) {
//            // Idle
//            return null;
//        }
//        else if (!(corr.second > 0.7)) {
//            // previous
//            if (!currWalking) {
//                return null;
//            }
//        }
//
//        float dir = 0;
//        for (int i=0; i< samplesDir.length; i++) {
//            dir += samplesDir[i];
//        }
//        dir /= samplesDir.length;
//
//        //Walking
//        double stepCount = ((double) samplesAcc.length)/corr.first;
//       // double stepLength = 0.3; // stride();
//        double stepLength = stride();
//        System.out.println(stepLength);
////        System.out.println(stepLength);
//        return new Pair<>(stepCount*stepLength, dir);
//    }

    public double getStepCount() {
        double mean = mean();
        double std = std();
//        System.out.println(mean + ", " + std);

        if (std<0.04*mean) {
            // Idle
            return -1;
        }

        double extremaCount = 0;
        double diffAcc = samplesAccFilt[(currId+1)%numSamples] - samplesAccFilt[currId];
        boolean countingStarted = false;
        int startId = 0;
        int endId = 0;

        for (int i=2; i<numSamples; i++) {
            int id = (currId+i)%numSamples;
            int id_prev = (currId+i-1)%numSamples;
            double tmp = samplesAccFilt[id] - samplesAccFilt[id_prev];
            if (tmp*diffAcc<0) {
                if (countingStarted == false) {
                    countingStarted = true;
                    startId = id_prev;
                    endId = id_prev;
                }
                else {
                    extremaCount += 1;
                    endId = id_prev;
                }
            }
            diffAcc = tmp;
        }
//        System.out.println(startId + ", " + endId);
        if (endId == startId) {
            return 0;
        }
        return 0.5*(extremaCount/((endId - startId + numSamples)%numSamples)*numSamples);
    }

    public Pair<Double, Float> getMotion(double strideLength, boolean halfWindow) {

//        System.out.print(String.format ("%,.3f", std) + "\t" + corr.first + "\t" + String.format ("%,.3f", corr.second) + "\t");
        double stepCount = getStepCount();
        if (stepCount == -1) {
            return null;
        }
        else if (stepCount == 0) {
            return new Pair<>(0.0, 0f);
        }
        float dir = 0;
        for (int i=(halfWindow? (samplesDir.length/2) : 0); i < samplesDir.length; i++) {
            dir += samplesDir[(currId + i)%samplesDir.length];
        }
        dir /= (halfWindow? (samplesDir.length - (samplesDir.length/2)) : samplesDir.length);
//        System.out.println(stepCount + ", " + stepCount*strideLength*(halfWindow? 0.5 : 1));
        return new Pair<>(stepCount*strideLength*(halfWindow? 0.5 : 1), dir);
    }

//    public double stride(double N) {
//        double strideLen;
//
//        double accMax = 0;
//        double accMin = 1000;
//        for (int i = 0; i < N; i++) {
//            if (verticalAcc[i] > accMax) accMax = verticalAcc[i];
//            if (verticalAcc[i] < accMax) accMin = verticalAcc[i];
//        }
//
//        strideLen = K * Math.pow(accMax - accMin, (float) 1/4);
//
//        return strideLen;
//    }

//    public double stride() {
//        return stride(verticalAcc.length);
//    }

    public double std(int k, int t) {
        double mean = this.mean(k, t);
        double std = 0;

        for (int i = k; i < k + t; i++) {
            std += Math.pow(samplesAcc[i] - mean, 2);
        }

        std /= (t);
        std = Math.sqrt(std);
        return std;
    }
//
    public double mean(int k, int t) {
        double totalR = 0;

        for (int i = k; i < k + t; i++) {
            totalR += samplesAcc[i];
        }

        double mean = totalR / t;
        return mean;
    }

    public double std() {
        return std(0, samplesAcc.length);
    }

    public double mean() {
        return mean(0, samplesAcc.length);
    }


}
