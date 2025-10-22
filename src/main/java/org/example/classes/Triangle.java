package org.example.classes;

import org.example.annotations.Generatable;

@Generatable
public class Triangle implements Shape {
    private double sideA;
    private double sideB;
    private double sideC;

    public Triangle(double sideA, double sideB, double sideC) {
        this.sideA = sideA;
        this.sideB = sideB;
        this.sideC = sideC;
    }

    @Override
    public double getArea() {
        double s = (sideA + sideB + sideC) / 2;
        return Math.sqrt(s * (s - sideA) * (s - sideB) * (s - sideC));
    }

    @Override
    public double getPerimeter() {
        return sideA + sideB + sideC;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Triangle{")
                .append("sideA=").append(sideA)
                .append(", sideB=").append(sideB)
                .append(", sideC=").append(sideC)
                .append(", area=").append(getArea())
                .append(", perimeter=").append(getPerimeter())
                .append('}')
                .toString();
    }
}