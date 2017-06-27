package lebr;

// Testaufruf:
// java -cp geo.jar;lebr.jar lebr.Isochrone geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20 CAR_CACHE_de_noCC.CAC 49.46591000 11.15800500 15 20505600 20505699

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.io.ParseException;
import fu.esi.SQL;
import fu.keys.LSIClassCentreDB;
import fu.util.ConcaveHullGenerator;
import fu.util.DBUtil;
import nav.NavData;
import pp.dorenda.client2.additional.UniversalPainterWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

// Testaufruf - dorenda:
// java -cp .;geo.jar pp.dorenda.client2.testapp.TestActivity  -m webservice;geosrv.informatik.fh-nuernberg.de  -c pp.dorenda.client2.additional.UniversalPainter -a result.txt;s

/**
 * Hauptklasse zur Berechnung eines Isochrons
 */
public class Isochrone {

    private final Params params;

    public static void main(final String[] args) throws Exception {
        new Isochrone(new Params(args)).run();
//        new Isochrone(Params.TEST_PARAMS).run();
    }

    public Isochrone(final Params params) {
        this.params = params;
        try {
            final NavData navData = new NavData(params.getCacFile(), true);
            NavDataProvider.setNavData(navData);
        } catch (final Exception e) {
            throw new RuntimeException("Fehler beim Erstellen der Navdata", e);
        }
    }

    private void run() {
        final Crossing startCrossing = new Crossing(params.getLatitude(), params.getLongitude());
        List<Coordinate> reachableCoordinates = computeReachableCoordinates(startCrossing);
        clearCaches();
//      Wenn mehr als 500.000 Eintraege, nur die aeusseren betrachten,
//      um Speicherverbrauch und Performance beim Erstellen der konkaven Huelle zu verbessern
        if (reachableCoordinates.size() > 500000) {
            System.out.println("Mehr als 500.000 Eintraege ermittelt. Entferne einige der innenliegenden Eintraege.");
            reachableCoordinates = filterReachableCoordinates(reachableCoordinates);
        }
        final List<double[]> concaveHull = createConcaveHull(reachableCoordinates);
        final List<Domain> reachableDomains;
        try {
            reachableDomains = computeReachableDomains(concaveHull);
        } catch (final SQLException | ParseException e) {
            throw new RuntimeException("Fehler beim ermitteln der erreichbaren Domains", e);
        }
        drawMap(startCrossing, concaveHull, reachableDomains);
    }

    /**
     * Ermittelt alle erreichbaren Koordinaten innerhalb des Kostenlimits.
     *
     * @param startCrossing das Start-Crossing
     * @return eine Liste aller erreichbaren Koordinaten
     */
    private List<Coordinate> computeReachableCoordinates(final Crossing startCrossing) {
        final Timer timer = Timer.start("computeReachableCoordinates");

        final List<Coordinate> reachableCoordinates = new ArrayList<>();
        startCrossing.setCostFromStart(0.0);
        reachableCoordinates.add(startCrossing);

        final PriorityQueue<Crossing> queue = new PriorityQueue<>((o1, o2)
                -> Double.compare(o1.getCostFromStart(), o2.getCostFromStart()));
        queue.add(startCrossing);

        do {
            final Crossing currentCrossing = queue.poll();
            expand(queue, reachableCoordinates, currentCrossing);
            currentCrossing.setClosed(true);
        } while (!queue.isEmpty());

        timer.stop();
        System.out.println("Anzahl erreichbarer Koordinaten: " + reachableCoordinates.size());
        return reachableCoordinates;
    }

    private void expand(final PriorityQueue<Crossing> open, final List<Coordinate> reachableCoordinates,
                        final Crossing currentCrossing) {
        for (final Crossing neighbour : currentCrossing.getNeighbours()) {
            final Link linkToNeighbour = currentCrossing.getLinkToNeighbour(neighbour);
            if (neighbour.isClosed()) {
                // Sonderfall 3
                specialCase3(reachableCoordinates, currentCrossing, neighbour, linkToNeighbour);
                continue;
            }
            final double costCurrentToNeighbour = currentCrossing.getCostToNeighbour(neighbour);
            final double costFromStartOverCurrentToNeighbour = currentCrossing.getCostFromStart() + costCurrentToNeighbour;
            if (costFromStartOverCurrentToNeighbour < neighbour.getCostFromStart()) {
                // Es wurde ein schnellerer Weg zum Nachbarn gefunden
                neighbour.setCostFromStart(costFromStartOverCurrentToNeighbour);
            }

            if (neighbour.getCostFromStart() > params.getSeconds()) {
                // Sonderfall 1
                reachableCoordinates.addAll(linkToNeighbour.getReachablePoints(params.getSeconds() - currentCrossing.getCostFromStart()));
                continue;
            }
            //Sonderfall 2
            specialCase2(reachableCoordinates, linkToNeighbour);

            if (open.contains(neighbour)) {
                continue;
            }
            open.add(neighbour);
            reachableCoordinates.add(neighbour);
        }
    }

    private void specialCase3(final List<Coordinate> reachableCoordinates, final Crossing currentCrossing,
                              final Crossing neighbour, final Link linkToNeighbour) {
        if (neighbour.getCostFromStart() + currentCrossing.getCostToNeighbour(neighbour) > params.getSeconds()) {
            if (!linkToNeighbour.goesCounterWay()) {
                reachableCoordinates.addAll(linkToNeighbour.getReachablePoints(params.getSeconds() - currentCrossing.getCostFromStart()));
            }
            final Link reverseLink = linkToNeighbour.getReverseLink();
            if (!reverseLink.goesCounterWay()) {
                reachableCoordinates.addAll(reverseLink.getReachablePoints(params.getSeconds() - neighbour.getCostFromStart()));
            }
        }
    }

    private void specialCase2(final List<Coordinate> reachableCoordinates, final Link linkToNeighbour) {
        if (linkToNeighbour.getLength() > 500) {
            final double[][] points = linkToNeighbour.getPoints();
            final double[] longs = points[0];
            final double[] lats = points[1];
            for (int i = 0; i < longs.length; i++) {
                final Point point = new Point(lats[i], longs[i]);
                reachableCoordinates.add(point);
            }
        }
    }

    private void clearCaches() {
        final Timer timer = Timer.start("clearCaches");
        Crossing.clearCache();
        Link.clearCache();
        timer.stop();
    }

    /**
     * Filtert die Liste der Koordinaten, indem die inneren Crossings nicht weiter beachtet werden. <br>
     * Nur bei sehr grosser Anzahl an Koordinaten sinnvoll.
     * @param reachableCoordinates die Liste der erreichbaren Koordinaten
     * @return eine neue Liste, welche die erreichbaren Koordinaten und nur noch die aeusseren Crossings enthaelt
     */
    private List<Coordinate> filterReachableCoordinates(final List<Coordinate> reachableCoordinates) {
        final Timer timer = Timer.start("filterReachableCoordinates");
        final List<Coordinate> filtered = new ArrayList<>();
        for (Coordinate reachableCoordinate : reachableCoordinates) {
            if (reachableCoordinate instanceof Crossing) {
                if (((Crossing) reachableCoordinate).getCostFromStart() > params.getSeconds() * 0.75) {
                    filtered.add(reachableCoordinate);
                }
            } else {
                filtered.add(reachableCoordinate);
            }
        }
        timer.stop();
        System.out.println("Anzahl Koordinaten: vorher = " + reachableCoordinates.size() + "   nachher = " + filtered.size());
        return filtered;
    }

    private List<double[]> createConcaveHull(final List<Coordinate> erreichbareCrossings) {
        final Timer timer = Timer.start("createConcaveHull");
        final ArrayList<double[]> points = new ArrayList<>();
        for (final Coordinate coordinate : erreichbareCrossings) {
            double lon = coordinate.getLongitude();
            double lat = coordinate.getLatitude();
            points.add(new double[]{lon, lat});
        }
        final ArrayList<double[]> concaveHull = ConcaveHullGenerator.concaveHull(points, 0.01d);
        timer.stop();
        return concaveHull;
    }

    /**
     * Ermittelt die in der konkaven Huelle erreichbaren Domains.
     * @param concaveHull die konkave Huelle
     * @return eine Liste aller Domains innerhalb der konkaven Huelle
     * @throws SQLException Fehler beim Aufruf der Datenbank
     * @throws ParseException Fehler beim parsen der Geometrie
     */
    private List<Domain> computeReachableDomains(final List<double[]> concaveHull) throws SQLException, ParseException {
        final Timer timer = Timer.start("computeReachableDomains");
        final com.vividsolutions.jts.geom.Coordinate[] geomCords = new com.vividsolutions.jts.geom.Coordinate[concaveHull.size()];
        for (int i = 0; i < concaveHull.size(); i++) {
            final double[] doubles = concaveHull.get(i);
            geomCords[i] = new com.vividsolutions.jts.geom.Coordinate(doubles[0], doubles[1]);
        }

        final GeometryFactory geometryFactory = new GeometryFactory();
        final Geometry polygon = geometryFactory.createPolygon(geometryFactory.createLinearRing(geomCords), new LinearRing[0]);
        final Envelope boundingBox = polygon.getEnvelopeInternal(); // Bounding Box berechnen

        //Datenbank-Verbindung initialiseren
        Connection connection;
        ResultSet resultSet;
        Statement statement;
        try {
            DBUtil.parseDBparams(params.getDbAccess());
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            LSIClassCentreDB.initFromDB(connection);
        } catch (final Exception e) {
            throw new RuntimeException("Fehler beim initialisieren der Datenbank", e);
        }

        statement = connection.createStatement();
        statement.setFetchSize(1000);
        resultSet = statement.executeQuery("SELECT realname, gao_geometry FROM domain WHERE geometry='P' AND lsiclass1 BETWEEN " + params.getFromLSI() + " AND " + params.getToLSI() + " AND" +
                SQL.createIndexQuery(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMaxX(), boundingBox.getMinY(), SQL.COMPLETELY_INSIDE)
        );

        final List<Domain> domains = new ArrayList<>();
        while (resultSet.next()) {
            final String realname = resultSet.getString(1);
            final byte[] gao_geometry = resultSet.getBytes(2);
            final Geometry geometry = SQL.wkb2Geometry(gao_geometry);
            // Testen, ob Geometrie wirklich im Polygon liegt
            if (geometry.within(polygon)) {
                final double latitude = geometry.getCentroid().getY();
                final double longitude = geometry.getCentroid().getX();
                domains.add(new Domain(realname, latitude, longitude));
            }
        }
        resultSet.close();

        timer.stop();
        System.out.println("Anzahl erreichbarer Domains: " + domains.size());
        return domains;
    }

    /**
     * Erstellt die Karte fuer Dorenda.
     * @param startCrossing das Start-Crossing
     * @param concaveHull die konkave Huelle
     * @param reachableDomains Liste der erreichbaren Domains
     */
    private void drawMap(final Crossing startCrossing, final List<double[]> concaveHull, final List<Domain> reachableDomains) {
        final Timer timer = Timer.start("drawMap");
        try {
            final UniversalPainterWriter writer = new UniversalPainterWriter("result.txt");

            // Start-Flagge zeichnen
            writer.flag(startCrossing.getLatitude(), startCrossing.getLongitude(), 0, 255, 0, 255, "Start");

            // Isochron zeichnen
            com.vividsolutions.jts.geom.Coordinate[] geomCoords = new com.vividsolutions.jts.geom.Coordinate[concaveHull.size()];
            for (int i = 0; i < concaveHull.size(); i++) {
                final double[] coords = concaveHull.get(i);
                geomCoords[i] = new com.vividsolutions.jts.geom.Coordinate(coords[0], coords[1]);
            }
            final Geometry geometry = new GeometryFactory().createPolygon(geomCoords);
            writer.jtsGeometry(geometry, 255, 255, 255, 100, 4, 4, 4);

            // Domain-Flaggen zeichnen
            for (final Domain domain : reachableDomains) {
                writer.flag(domain.getLatitude(), domain.getLongitude(), 255, 0, 0, 120, domain.getName());
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Zeichnen der Karte!", e);
        }
        timer.stop();
    }

}
