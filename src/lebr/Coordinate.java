package lebr;

public interface Coordinate {

    double getLatitude();
    double getLongitude();

    default double getCostToPoint(final double latitude, final double longitude, final double speed){
        final double a = Math.abs(getLatitude() - latitude);
        final double b = Math.abs(getLongitude() - longitude);
        final double c = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
        return c / speed;
    }
}
