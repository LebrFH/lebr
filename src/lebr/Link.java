package lebr;

import nav.NavData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Link {

    private static final Map<Integer, Link> cache = new HashMap<>();

    /**
     * TODO
     */
    private static final double SPEED_FACTOR = 0.8;

    private final int id;

    public static Link getLink(int id) {
        final Link link = cache.get(id);
        if (link != null) {
            return link;
        }
        final Link newLink = new Link(id);
        cache.put(id, newLink);
        return newLink;
    }

    private Link(final int id) {
        this.id = id;
    }

    public boolean goesCounterWay() {
        return NavDataProvider.getNavData().goesCounterOneway(id);
    }

    public Crossing getFrom() {
        final int crossingIDFrom = NavDataProvider.getNavData().getCrossingIDFrom(id);
        return Crossing.getCrossing(crossingIDFrom);
    }

    public Crossing getTo() {
        final int crossingIDTo = NavDataProvider.getNavData().getCrossingIDTo(id);
        return Crossing.getCrossing(crossingIDTo);
    }

    public double[][] getPoints() {
        final NavData navData = NavDataProvider.getNavData();
        final int domainID = navData.getDomainID(id);
        final double[] domainLongsE6 = toDoubleArray(navData.getDomainLongsE6(domainID));
        final double[] domainLatsE6 = toDoubleArray(navData.getDomainLatsE6(domainID));

        int from = navData.getDomainPosNrFrom(id);
        int to = navData.getDomainPosNrTo(id);
        if (from > to) {
            final int temp = from;
            from = to;
            to = temp;
        }
        final double[] linkLongsE6 = Arrays.copyOfRange(domainLongsE6, from, to + 1);
        final double[] linkLatsE6 = Arrays.copyOfRange(domainLatsE6, from, to + 1);
        return new double[][]{linkLongsE6, linkLatsE6};
    }

    private double[] toDoubleArray(final int[] intArray) {
        final double[] doubleArray = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            doubleArray[i] = intArray[i] / 1000000.0;
        }
        return doubleArray;
    }

    public double getCost() {
        return getLength() / getSpeed();
    }

    public int getLength() {
        return NavDataProvider.getNavData().getLengthMeters(id);
    }

    public double getSpeed() {
        double speed = NavDataProvider.getNavData().getMaxSpeedKMperHours(id);
        if (speed == 0) {
            int lsi = NavDataProvider.getNavData().getLSIclass(id);
            if (lsi == 34141000) {
                speed = 50.0;
            } else if (lsi == 34130000 || lsi == 34132000 || lsi == 34133000 || lsi == 34134000) { // Landstrasse
                speed = 100.0;
            } else if (lsi == 34110000 || lsi == 34131000) { // Autobahn und Bundesstrasse
                speed = 130.0;
            } else if (lsi == 34142000) { // Verkehrsberuhigter Bereich
                speed = 3.6; // Schrittgeschwindigkeit
            } else if (lsi == 32711000) { // Baustelle Verkehr
                speed = 30.0;
            } else if (lsi == 34120000) { // Kraftfarhrstrasse
                speed = 60.0;
            } else if (lsi == 34172000) { // ANSCHLUSSSTELLE_KRAFTFAHRSTRASSE
                speed = 40.0;
            } else if (lsi == 34171000 || lsi == 34173000 || lsi == 34174000 || lsi == 34175000) {
                /*
                    34171000 ANSCHLUSSSTELLE_AUTOBAHN
                    34173000 ANSCHLUSSSTELLE_BUNDESSTRASSE
                    34174000 ANSCHLUSSSTELLE_SEKUNDAER
                    34175000 ANSCHLUSSSTELLE_TERTIAER
                */
                speed = 60.0;
            } else if (lsi == 34176000) { // Kreisverkehr
                speed = 30.0;
            }
        }
        return speed / 3.6 * SPEED_FACTOR; // Meter pro Sekunde
    }

    public List<Coordinate> getReachablePoints(final double leftCostLimit) {
        final List<Coordinate> reachablePoints = new ArrayList<>();

        final double[][] points = getPoints();

        // Punkte sind die beiden Crossings, die den Link umschliessen
        if(points.length == 2){
            return reachablePoints;
        }

        final int from = NavDataProvider.getNavData().getDomainPosNrFrom(id);
        final int to = NavDataProvider.getNavData().getDomainPosNrTo(id);
        final boolean reversePoints = from > to;
        final double[] longs = points[0];
        final double[] lats = points[1];

        double currentCosts = 0.0;
        if (!reversePoints) {
            for (int i = 0; i < longs.length - 2; i++) {
                final Point point = new Point(lats[i], longs[i]);
                final Point nextPoint = new Point(lats[i + 1], longs[i + 1]);
                currentCosts += point.getCostToPoint(nextPoint.getLatitude(), nextPoint.getLongitude(), getSpeed());
                if (currentCosts <= leftCostLimit) {
                    reachablePoints.add(nextPoint);
                } else {
                    break;
                }
            }
        } else {
            for (int i = longs.length - 1; i > 1; i--) {
                final Point point = new Point(lats[i], longs[i]);
                final Point nextPoint = new Point(lats[i - 1], longs[i - 1]);
                currentCosts += point.getCostToPoint(nextPoint.getLatitude(), nextPoint.getLongitude(), getSpeed());
                if (currentCosts <= leftCostLimit) {
                    reachablePoints.add(nextPoint);
                } else {
                    break;
                }
            }
        }
        return reachablePoints;
    }

    public Link getReverseLink() {
        return getLink(NavDataProvider.getNavData().getReverseLink(id));
    }
}
