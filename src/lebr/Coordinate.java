package lebr;

import static java.lang.Math.*;

/**
 * Interface fuer eine geographische Koordinate.
 */
public interface Coordinate {

    /**
     * Mittlerer Erdradius
     */
    public static final double EARTH_RADIUS = 6371000;

    double getLatitude();

    double getLongitude();

    /**
     * Ermittelt die Kosten von dieser Koordinate zu einer anderen.
     * @param coordinate die andere Koordinate
     * @param speed die Geschwindigkeit
     * @return die Kosten in Sekunden
     */
    default double getCostToCoordinate(final Coordinate coordinate, final double speed) {
        final double longitude = coordinate.getLongitude();
        final double latitude = coordinate.getLatitude();

        //Distanz zwischen zwei Koordinaten: Haversine Formel
        final double dLongitude = toRadians(longitude - getLongitude());
        final double dLatitude = toRadians(latitude - getLatitude());
        final double a = pow((sin(dLatitude / 2)), 2) + cos(getLatitude()) * cos(latitude) * pow((sin(dLongitude / 2)), 2);
        final double b = 2 * asin(min(1, sqrt(a)));
        final double c = EARTH_RADIUS * b;
        return c / speed;
    }
}
