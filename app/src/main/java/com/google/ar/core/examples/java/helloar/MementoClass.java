package com.google.ar.core.examples.java.helloar;

/**
 * Created by prajna on 11/9/17.
 */

public class MementoClass {

    private double x;
    private double y;
    private double z;
    private double lattitude;
    private double longitude;

    public MementoClass(double  x, double y,double z,double lattitude,double longitude) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lattitude = lattitude;
        this.longitude = longitude;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getLattitude() {
        return lattitude;
    }

    public void setLattitude(double lattitude) {
        this.lattitude = lattitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}

