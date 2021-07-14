package com.locator.particle;

import java.util.List;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.tan;
import static java.lang.StrictMath.sin;

public class Line {
    /**
     * Coordinates of line
     */
    double x1, y1, x2, y2;

    double m1, c1;

    public Line(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;

        // Line: (y - y1)/(y1 - y2) = (x - x1)/(x1 - x2)
        //    => (y - y1) = [(y1 - y2)/(x1 - x2)]*(x - x1)
        //    => y = [(y1 - y2)/(x1 - x2)]*x + {y1 - [(y1 - y2)/(x1 - x2)]*x1}
        m1 = (y1 - y2)/(x1 - x2);
        c1 = y1 - (m1 * x1);
    }

    public double getLength() {
        return Math.sqrt(Math.pow((x2 - x1),2) + Math.pow((y2 - y1),2));
    }
    public double getX() { return x1; }
    public double getY() { return y1; }

    public boolean intersect(double x, double y, double r, double theta) {
        // Line: (y - y1) = m*(x - x1) => y = m*x + (y1 - m*x1)
        double m2 = tan(theta);
        double c2 = y - (m2*x);

        double xFinal = x + (r*cos(theta));
        double yFinal = y + (r*sin(theta));

        double xInt, yInt;

        if (x1 == x2) {
            // Vertical Line
            xInt = x1;
            yInt = (m2 * xInt) + c2;
        }
        else if (theta == PI/2 || theta == -PI/2) {
            xInt = x;
            yInt = (m1 * xInt) + c1;
        }
        else {
            // y = m1*x + c1
            // y = m2*x + c2
            // (c1 - c2) = (m2 - m1)*x
            // x = (c1 - c2)/(m2 - m1)
            xInt = (c1 - c2) / (m2 - m1);
            yInt = (m1 * xInt) + c1; // Y-coord using eqn of line
        }

        // Check if point lies inside of both line segments
        if ((xInt-x)*(xInt-xFinal)<=0 && (yInt-y)*(yInt-yFinal)<=0 &&
                (xInt-x1)*(xInt-x2)<=0 && (yInt-y1)*(yInt-y2)<=0) {
            return true;
        }
        else {
            return false;
        }
    }
}
