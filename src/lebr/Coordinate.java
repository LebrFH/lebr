package lebr;

import static java.lang.Math.*;

public interface Coordinate {

    //TODO Radius anpassen? kleiner machen
    public static final double EARTH_RADIUS = 6380000;

    double getLatitude();

    double getLongitude();

    default double getCostToPoint(final Point point, final double speed) {
        return getCostToPoint(point.getLatitude(), point.getLongitude(), speed);
    }

    default double getCostToPoint(final double latitude, final double longitude, final double speed) {
        //Distanz zwischen zwei Koordinaten: Haversine Formel
        final double dLongitude = toRadians(longitude - getLongitude());
        final double dLatitude = toRadians(latitude - getLatitude());
        final double a = pow((sin(dLatitude / 2)), 2) + cos(getLatitude()) * cos(latitude) * pow((sin(dLongitude / 2)), 2);
        final double b = 2 * asin(min(1, sqrt(a)));
        final double c = EARTH_RADIUS * b;
        return c / speed;
    }
}
