package com.example.nifi.dto;

public class ProcessorPosition {

    private double x;
    private double y;

    public ProcessorPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}