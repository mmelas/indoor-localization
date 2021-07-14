package com.locator.particle;


import java.util.List;

public class Map implements Cloneable{
    private Line walls[] = new Line[]{
            new Line(0.12, 0.12, 3.62, 0.12), new Line(3.62, 0.12, 3.62, 3.32), //a, b
            new Line(3.62, 3.32, 4.92, 3.32), new Line(4.92, 3.32, 4.92, 5.54), //c, d
            new Line(3.74, 5.54, 6.44, 5.54), new Line(6.44, 5.54, 6.44, 10.69), //e, f
            new Line(6.44, 10.69, 3.74, 10.69), new Line(3.74, 10.69, 3.74, 7.43), //g, h
            new Line(3.74, 8.33, 0.12, 8.33), new Line(0.12, 8.33, 0.12, 0.12), //i, j
            new Line(3.74, 5.54, 3.74, 6.44), new Line(3.74, 8.3, 4.64, 8.3), //k, l
            new Line(5.54, 8.3, 6.44, 8.3)};

    // cells points are clockwise
    private Cell cells[] = new Cell[]{
                             new Cell(new double[][]{{0.12, 0.12}, {1.87, 0}, {1.87, 3.32}, {1.2, 3.32}}, true), //Cell 1
                             new Cell(new double[][]{{1.87, 0.12}, {3.62, 0.12}, {3.62, 3.32}, {1.87, 3.32}}, true),//, //Cell 2
                             new Cell(new double[][]{{0.12, 3.32}, {2.52, 3.32}, {2.52, 5.42}, {0.12, 5.42}}, false), //Cell 3
                             new Cell(new double[][]{{2.52, 3.32}, {4.92, 3.32}, {4.92, 5.42}, {2.52, 5.42}}, false), //Cell 4
                             new Cell(new double[][]{{0.12, 5.42}, {1.87, 5.42}, {1.87, 8.33}, {0.12, 8.33}}, true), //Cell 5
                             new Cell(new double[][]{{1.87, 5.42}, {3.62, 5.42}, {3.62, 8.33}, {1.87, 8.33}}, true), //Cell 6
                             new Cell(new double[][]{{3.74, 5.54}, {6.44, 5.54}, {6.44, 8.24}, {3.74, 8.24}}, false), //Cell 7
                             new Cell(new double[][]{{3.74, 8.36}, {6.44, 8.36}, {6.44, 10.69}, {3.74, 10.69}}, false), //Cell 8
                             };

    private Obj objs[];

    public void setParticles(Particles particles) {
        this.particles = particles;
    }

    // All the particles of the map
    private Particles particles;
    private int numParticles;
    private double totalArea;

//
    public Map(Particles particles, int numParticles) {
        // TODO: Calculate the total area of all cells, and find proportion of area of each cell
        // Assign particles to each cell according to the proportion of area.

        // Assume k particles per meter, in each dimension
        // Total particles q = (kl * kb) = (k^2) * (l*b)
        // k^2 = q/a => k = sqrt(q/a)
        // k particles per meter
        // Distance between adjacent particles = 1/k meters => dimension od square grids
        // Particles along length = l * k
        // Particles along breadth = b * k

        // Iterate over all cells. Initialize particles in each of them, with the computed positions.
        this.particles = particles;
        this.numParticles = numParticles;
        this.totalArea = calcTotalArea();
        for (Cell cell : cells) {
            addParticlesForCell(cell);
        }
    }

    private double calcTotalArea() {
        double totalArea = 0;
        for (Cell cell : cells) {
            totalArea += cell.getArea();
        }
        return totalArea;
    }

    private void addParticlesForCell(Cell cell) {
        double particleSpace, cellBase, cellHeight, equalWeight;
        int particlesPerMeter;

        particlesPerMeter = calcParticlesPerMeter(cell);

        cellBase = cell.getBase();
        cellHeight = cell.getHeight();
        particleSpace = 1 / (double) particlesPerMeter;
        equalWeight =  1 / (double) 9113; //TODO: numParticles is not the same as array size yet. needs fix.

        double relativeX = cell.getTopX();
        double relativeY = cell.getTopY();
        for (double i = 0; i < cellBase; i+=particleSpace) {
            for (double j = 0; j < cellHeight; j+=particleSpace) {
                particles.addParticle(i + relativeX, j + relativeY, equalWeight);
            }
        }
    }

    public int calcParticlesPerMeter(Cell cell) {
        double cellArea, cellPercentageArea;
        int cellParticles, particlesPerMeter;

        cellArea = cell.getArea();
        cellPercentageArea = cellArea / totalArea;
        cellParticles = (int) (cellPercentageArea * numParticles);
        particlesPerMeter = (int) Math.sqrt(cellParticles / cellArea);
        return particlesPerMeter;
    }

    public Cell[] getCells() { return cells; }
//    public Line[] getWalls() { return walls; }

    boolean isInteriorCells(double x, double y) {
        boolean ret = false;
        for (Cell cell: cells) {
            if (cell.isInterior(x, y)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    boolean isInteriorObjs(double x, double y) {
        boolean ret = false;
        for (Obj obj: objs) {
            if (obj.isInterior(x, y)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    public String getPosition() {
        List<Particle> parts = particles.getTotalParticles();
        int[] cellNumParts = new int[cells.length];
        int probCell = -1;
        int maxCellParts = 0;
        for (Particle part: parts) {
            for (int i=0; i<cells.length; i++) {
                if (cells[i].isInterior(part.x, part.y, false)) {
                    cellNumParts[i] += 1;
                    if (cellNumParts[i] > maxCellParts) {
                        maxCellParts = cellNumParts[i];
                        probCell = i;
                    }
                };
            }
        }
        return "Cell " + (probCell+1);
//        if (maxCellParts > 0.5*numParticles) {
//            return "Cell " + (probCell+1);
//        }
//        else {
//            return prevCell;
//        }
    }

    public int intersectWalls(double x, double y, double r, double theta) {
        int numInt = 0;
        for (Line wall: walls) {
            if (wall.intersect(x, y, r, theta)) {
                numInt += 1;
            }
        }
        return numInt;
    }

    public int intersectObjs(double x, double y, double r, double theta) {
        int numInt = 0;
        for (Obj obj: objs) {
            if (obj.intersect(x, y, r, theta)) {
                numInt += 1;
            }
        }
        return numInt;
    }

}
