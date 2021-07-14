package com.locator.particle;

import java.util.Random;

import static java.lang.StrictMath.PI;
import static org.apache.commons.lang3.math.NumberUtils.min;

public class Particle implements Cloneable{
    /**
     * X,Y-coordinates
     */
    double x, y;

    //TODO: Maybe this isn't needed?
    /**
     * Direction
     */
    double direction;

    /**
     * Reference Direction
     */
    double refDir = -0.28;

    /**
     * Weight of the particle
     */
    double weight;

    /**
     * Number of runs since the particle was created;
     */
     int numRuns = 1;

    /**
     * Status of particle
     */
    boolean isAlive;

    int intersectionCnt = 0;

    public Particle(double x, double y, double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
        this.isAlive = true;
    }


    public Particle(Particle p) {
        this.x = p.x;
        this.y = p.y;
        this.weight = p.weight;
        this.isAlive = true;
    }


    /**
     * Updates particle's position according to movement
     * @param distance
     * @param direction
     */
    void updatePosition(double distance, double direction, double stdA, double stdD, Map map, boolean restrict) {
        double dir = direction-refDir;
        if (restrict) {
            if ((dir >= -5 * PI / 4 && dir < -3 * PI / 4) || (dir >= 3 * PI / 4 && dir < 5 * PI / 4)) {
                dir = -PI;
            } else if (dir >= -3 * PI / 4 && dir < -PI / 4) {
                dir = -PI / 2;
            } else if (dir >= -PI / 4 && dir < PI / 4) {
                dir = 0;
            } else if (dir >= PI / 4 && dir < 3 * PI / 4) {
                dir = PI / 2;
            }
        }
        //TODO: calculate noise for distance and angle for each particle
        double dirWithNoise = gaussianNoiseAngle(dir, stdA);
        double distWithNoise = gaussianNoiseDist(distance, 0.05*distance); // stdD);

        this.intersectionCnt = map.intersectWalls(this.x, this.y, distWithNoise, dirWithNoise);
        this.x += (distWithNoise) * Math.cos(dirWithNoise);
        this.y += (distWithNoise) * Math.sin(dirWithNoise);
    }

    /**
     * Sets a new particle's position to another particle's position
     * also adding some noise to it (according to the stdD specified
     * for gaussian distance noise)
     * @param p
     * @param stdD
     */
    void updatePosition(Particle p, double stdD) {
        Random rn = new Random();
        double k = stdD * rn.nextDouble();
        this.x = p.x + k;
        this.y = p.y + k;
    }

    /**
     * Gaussian noise for angle
     * @param angle
     * @param std
     * @return
     */
    double gaussianNoiseAngle(double angle, double std) {
        Random rand = new Random();

        //nextGaussian generates noise from N(0,1) -> make it N(0,σ)
        return angle + Math.max(Math.min(rand.nextGaussian() * std, PI/6), -PI/6);
    }

    /**
     * Gaussian noise for distance
     * @param r
     * @param std
     * @return
     */
    double gaussianNoiseDist(double r, double std) {
        double noiseR;
        Random rand = new Random();

        //nextGaussian generates noise from N(0,1) -> make it N(0,σ)
        noiseR = r + Math.max(Math.min(rand.nextGaussian() * std, 2*std), -2*std);
        return noiseR;
    }

}
