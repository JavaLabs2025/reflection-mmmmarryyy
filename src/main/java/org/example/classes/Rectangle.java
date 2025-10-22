package org.example.classes;

import org.example.annotations.Generatable;

@Generatable
public class Rectangle implements Shape {
    private double length;
    private double width;

    public Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    @Override
    public double getArea() {
        return length * width;
    }

    @Override
    public double getPerimeter() {
        return 2 * (length + width);
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Rectangle{")
                .append("length=").append(length)
                .append(", width=").append(width)
                .append(", area=").append(getArea())
                .append(", perimeter=").append(getPerimeter())
                .append('}')
                .toString();
    }
}