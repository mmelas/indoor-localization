package com.locator.particle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Particles implements Cloneable {

    ArrayList<Particle> totalParticles;
    private int minCellularRssi= Integer.MAX_VALUE;
    private int maxCellularRssi = Integer.MIN_VALUE;
    private double minGrayShade = Double.MAX_VALUE;
    private double maxGrayShade = Double.MIN_VALUE;


    private boolean nearWindowCellular = false;
    private boolean nearWindowLight = false;

    private boolean useCellularScan;

    public Particles(int numParticles, boolean useCellularScan) {
        // Create the specified number of particles
        this.totalParticles = new ArrayList<Particle>(numParticles);
        this.useCellularScan = useCellularScan;
    }


    public Particles(Particles parts) {
        // Create the specified number of particles
        ArrayList<Particle> prevParticles = new ArrayList<>(parts.totalParticles.size());

        for (Particle particle : parts.totalParticles) {
            Particle p = new Particle(particle);
            prevParticles.add(p);
        }
        this.totalParticles = prevParticles;
        this.useCellularScan = useCellularScan;
    }

    /**
     * Updates positions of particles
     * @param distance
     * @param direction
     */
    void updatePositions(double distance, double direction, double stdA, double stdD, Map map, boolean restrict) {
        // TODO: Distances and directions based on gaussian?
        for (Particle particle: totalParticles) {
            particle.updatePosition(distance, direction, stdA, stdD, map, restrict);
        }
    }

    /**
     * Calculate the weights of the particles
     * with respect to how many rounds they have survived
     * and how many objects they have hit
     */
    public void calculateWeights(Map map, double weightThreshold, HashMap<String, Integer> cellularStrength, double grayShade) {

        // Update min and max cellular values seen until now
        if (useCellularScan) {
            for (HashMap.Entry<String, Integer> cellularEntry : cellularStrength.entrySet()) {
                int cellularValue = cellularEntry.getValue();
                if (cellularValue < minCellularRssi) minCellularRssi = cellularValue;
                if (cellularValue > maxCellularRssi) maxCellularRssi = cellularValue;
            }
            int midCellularStrength = (minCellularRssi + maxCellularRssi) / 2;
            // if value higher than middle, possible near window
            // otherwise, possible near center
        }

        // not yet implemented these in the calculation (maybe we shouldn't)
//        if (grayShade < minGrayShade) minGrayShade = grayShade;
//        if (grayShade > maxGrayShade) maxGrayShade = grayShade;
//        double midGrayShade = (minGrayShade + maxGrayShade) / 2.0;
//        if (grayShade > midGrayShade) nearWindowLight = true;

        for (Particle particle : totalParticles) {
            if (map.isInteriorCells(particle.x, particle.y)) {
                //TODO: set weight according to object hit (if table instead of wall, maybe lower the weight instead of zeroing it?)
                particle.weight += particle.numRuns;
                if (particle.intersectionCnt > 0) particle.weight = 0;
                // If particle is inside a cell and it is above weight threshold increase alive count
                particle.numRuns++;
                if (particle.weight <= weightThreshold) {
                    particle.weight = 0;
                    particle.isAlive = false;
                    particle.numRuns = 1;
                }
            } else {
                    // Particle is not interior to any cell, kill it
                    particle.weight = 0;
                    particle.isAlive = false;
                    particle.numRuns = 1;
            }
            particle.intersectionCnt = 0;
        }
    }

    /**
     * Assigns all particles below a certain
     * weight threshold to other particle positions
     * with higher probability towards the more weight
     */
    public boolean updateParticles(double stdD) {
        double prob, totalProb;
        boolean allDead = true;
        for (Particle particle : totalParticles) {
            if (!particle.isAlive) {
                prob = Math.random();
                totalProb = 0.0;
                for (Particle possibleParticle : totalParticles) {
                    if (possibleParticle.isAlive) {
                        totalProb += possibleParticle.weight;
                        if (prob <= totalProb) {
                            particle.updatePosition(possibleParticle, stdD);
                            // particle.weight = possibleParticle.weight;
                            particle.weight = particle.numRuns;
                            break; //found new position for particle -> break
                        }
                    }
                }
            } else {
                allDead &= false;
            }
        }

        // return faster if all particles are dead
        if (allDead) return true;

        // Make all particles alive again
        for (Particle particle : totalParticles) {
            particle.isAlive = true;
        }
        return false;
    }

    /**
     * Normalize particle weights
     */
    public void normalizeWeights() {
        double totalWeight = 0.0;
        for (Particle particle : totalParticles) {
            totalWeight += particle.weight;
        }
//        System.out.println("Particle total weight = " + totalWeight);
        for (Particle particle : totalParticles) {
            if (totalWeight != 0) {
                particle.weight /= totalWeight;
            }
        }
    }

    /**
     * Adds a new particle to the list of particles
     * @param x
     * @param y
     * @param weight
     */
    public void addParticle(double x, double y, double weight) {
        totalParticles.add(new Particle(x, y, weight));
    }

    public List<Particle> getTotalParticles() {
        return totalParticles;
    }

}
