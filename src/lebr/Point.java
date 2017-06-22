package lebr;

public class Point implements Coordinate {

    private final double latitude;
    private final double longitude;

    public Point(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

    public double getCostToPoint(final double latitude, final double longitude, final double speed){
        final double a = Math.abs(this.latitude - latitude);
        final double b = Math.abs(this.longitude - longitude);
        final double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
        return c / speed;
    }
}
