package com.locator.particle;

import static java.lang.Math.cos;
import static java.lang.StrictMath.sin;

public class Rectangle {
    /**
     * Coordinates of opposite corners
     */
    private Line top, right, bottom, left;
    private boolean isNearWindow;

    //    ((x,y), (x,y), (x, y), (x, y))
    public Rectangle(double coords[][]) {
        this.top = new Line(coords[0][0], coords[0][1],
                coords[1][0], coords[1][1]);

        this.right = new Line(coords[1][0], coords[1][1],
                coords[2][0], coords[2][1]);

        this.bottom = new Line(coords[2][0], coords[2][1],
                coords[3][0], coords[3][1]);

        this.left = new Line(coords[3][0], coords[3][1],
                coords[0][0], coords[0][1]);
    }

    double getArea() {
        return getBase() * getHeight();
    }
    double getBase() {
        return top.getLength();
    }
    double getHeight() {
        return left.getLength();
    }
    double getTopX() {
        return top.getX();
    }
    double getTopY() { return top.getY(); }

    public boolean isInterior(double x, double y, boolean inclBoundary) {
        double minX = Math.min(top.x1, top.x2);
        double maxX = Math.max(top.x1, top.x2);
        double minY = Math.min(left.y1, left.y2);
        double maxY = Math.max(left.y1, left.y2);

        if (inclBoundary) {
            if (x >= minX && x <= maxX && y >= minY && y <= maxY) {
                return true;
            } else {
                return false;
            }
        }
        else {
            if (x > minX && x < maxX && y > minY && y < maxY) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isInterior(double x, double y) {
        return isInterior(x, y, true);
    }

    public boolean intersect(double x, double y, double r, double theta) {
        double xFinal = x + (r*cos(theta));
        double yFinal = y + (r*sin(theta));

        // Check if initial point is in interior
        boolean iInt = isInterior(x, y);

        // Check if final point is in interior
        boolean fInt = isInterior(xFinal, yFinal);

        if (iInt != fInt) {
            // If 1 is in interior and other is not, definitely one intersection
            return true;
        }
        else if (iInt == true) {
            // If both are in interior, then no intersections
            return false;
        }
        else {
            // If both in exterior, there may be intersections
            if (top.intersect(x, y, r, theta) || right.intersect(x, y, r, theta) ||
                    bottom.intersect(x, y, r, theta) || left.intersect(x, y, r, theta)) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}
