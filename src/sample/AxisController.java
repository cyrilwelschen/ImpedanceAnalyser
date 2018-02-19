package sample;

public class AxisController {

    private double newXMin = 0;
    private double newYMax = 0;

    protected void setNewXMinYMax(double xi, double yx){
        this.newXMin = xi;
        this.newYMax = yx;
    }

    public double getNewXMin() {
        return this.newXMin;
    }

    public double getNewYMax() {
        return this.newYMax;
    }
}
