package lebr;

import nav.NavData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klasse fuer einen Link
 */
public class Link {

    /**
     * Link-Cache. <br>
     * Da wir nur eine ID haben, benoetigen wir die Referenzen auf bereits angelegte Links.
     */
    private static final Map<Integer, Link> cache = new HashMap<>();

    /**
     * Geschindigkeitsfaktor. <br>
     * Korrektur, anhand der Annahme, dass nicht immer mit maximaler Geschwindigkeit gefahren wird.
     */
    private static final double SPEED_FACTOR = 0.8;

    private final int id;

    /**
     * Holt anhand einer ID einen vorhanden Link aus dem Cache oder legt einen neuen an, falls noch nicht gecached.
     *
     * @param id die Link-ID
     * @return der Link zur ID
     */
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

    /**
     * Gibt alle Geometriepunkte dieses Links zurueck.
     * @return die Liste aller Geometriepunkte dieses Links.
     */
    public double[][] getPoints() {
        final NavData navData = NavDataProvider.getNavData();
        final int domainID = navData.getDomainID(id);
        final double[] domainLongsE6 = convertIntCoordinatesToDouble(navData.getDomainLongsE6(domainID));
        final double[] domainLatsE6 = convertIntCoordinatesToDouble(navData.getDomainLatsE6(domainID));

        // Nur die Punkte dieses Links aus der Domain
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

    /**
     * Konvertiert die im Navdata als ints angegeben Koordinaten in double-Werte, wie sie sonst in der Logik verwendet werden.
     * @param intArray das int-Array
     * @return das double-Array
     */
    private double[] convertIntCoordinatesToDouble(final int[] intArray) {
        final double[] doubleArray = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            doubleArray[i] = intArray[i] / 1000000.0;
        }
        return doubleArray;
    }

    /**
     * Ermittelt die Geschwindigkeit dieses Links anhand des Strassen-Typs (LSI-Klasse)
     * @return die Geschwindigkeit in Metern pro Sekunde unter Beruecksichtigung des Geschw.-Faktors
     */
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

    /**
     * Ermittelt alle erreichbaren Geometriepunkte dieses Links innerhalb eines Kostenlimits
     * @param costLimit das Kostenlimit
     * @return eine Liste aller erreichbaren Geometriepunkte
     */
    public List<Coordinate> getReachablePoints(final double costLimit) {
        final List<Coordinate> reachablePoints = new ArrayList<>();
        final double[][] points = getPoints();
        // Punkte sind die nur beiden Crossings, die den Link umschliessen
        if(points[0].length == 2){
            return reachablePoints;
        }

        final int from = NavDataProvider.getNavData().getDomainPosNrFrom(id);
        final int to = NavDataProvider.getNavData().getDomainPosNrTo(id);
        // Anhand des from- und to-Index wird festgemacht, in welche Richtung iteriert werden muss
        final boolean reversePoints = from > to;
        final double[] longs = points[0];
        final double[] lats = points[1];

        double currentCosts = 0.0;
        if (!reversePoints) {
            for (int i = 0; i < longs.length - 2; i++) {
                final Point point = new Point(lats[i], longs[i]);
                final Point nextPoint = new Point(lats[i + 1], longs[i + 1]);
                currentCosts += point.getCostToCoordinate(nextPoint, getSpeed());
                if (currentCosts <= costLimit) {
                    reachablePoints.add(nextPoint);
                } else {
                    break;
                }
            }
        } else {
            // In die entgegengesetzte Richtung
            for (int i = longs.length - 1; i > 1; i--) {
                final Point point = new Point(lats[i], longs[i]);
                final Point nextPoint = new Point(lats[i - 1], longs[i - 1]);
                currentCosts += point.getCostToCoordinate(nextPoint, getSpeed());
                if (currentCosts <= costLimit) {
                    reachablePoints.add(nextPoint);
                } else {
                    break;
                }
            }
        }
        return reachablePoints;
    }

    public static void clearCache() {
        cache.clear();
    }

    public Crossing getFrom() {
        final int crossingIDFrom = NavDataProvider.getNavData().getCrossingIDFrom(id);
        return Crossing.getCrossing(crossingIDFrom);
    }

    public Crossing getTo() {
        final int crossingIDTo = NavDataProvider.getNavData().getCrossingIDTo(id);
        return Crossing.getCrossing(crossingIDTo);
    }

    public double getCost() {
        return getLength() / getSpeed();
    }

    public int getLength() {
        return NavDataProvider.getNavData().getLengthMeters(id);
    }

    public Link getReverseLink() {
        return getLink(NavDataProvider.getNavData().getReverseLink(id));
    }

    public boolean goesCounterWay() {
        return NavDataProvider.getNavData().goesCounterOneway(id);
    }
}
