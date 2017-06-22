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
import lebr.demo.NavDemo;
import nav.NavData;
import pp.dorenda.client2.additional.UniversalPainterWriter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

// Testaufruf - dorenda:
// java -cp .;geo.jar pp.dorenda.client2.testapp.TestActivity  -m webservice;geosrv.informatik.fh-nuernberg.de  -c pp.dorenda.client2.additional.UniversalPainter -a result.txt;s

// Parameter:           Testaufruf:
// 0: <dbaccesstr>      geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20
// 1: <cacfile>         CAR_CACHE_de_noCC.CAC
// 2: <latitude>        49.46591000
// 3: <longitude>       11.15800500
// 4: <minuten>         15
// 5: <fromLSI>         20505600
// 6: <toLSI>           20505699
public class Isochrone {

    private final Params params;

    public static void main(final String[] args) throws Exception {
//        new Isochrone(new Params(args)).run();
        //TODO Testaufruf
        new Isochrone(Params.TEST_PARAMS).run();
    }

    public Isochrone(final Params params) {
        this.params = params;
        try {
            final NavData navData = new NavData(NavDemo.class.getResource("/" + params.getCacFile()).getFile(), true);
            NavDataProvider.setNavData(navData);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Erstellen der Navdata", e);
        }
    }

    private void run() {
        final Crossing startCrossing = new Crossing(Double.valueOf(params.getLatitude()).intValue(),
                Double.valueOf(params.getLongitude()).intValue());
        final List<Coordinate> reachableCrossings = computeReachableCrossings(startCrossing);
        final List<double[]> concaveHull = createConcaveHull(reachableCrossings);
        final List<Domain> reachableDomains;
        try {
            reachableDomains = computeReachableDomains(concaveHull);
        } catch (final SQLException | ParseException e) {
            throw new RuntimeException("Fehler beim ermitteln der erreichbaren Domains", e);
        }
        writeMap(startCrossing, concaveHull, reachableDomains);
    }

    private List<Coordinate> computeReachableCrossings(final Crossing startCrossing) {
        final Timer timer = Timer.start("computeReachableCrossings");

        final List<Coordinate> reachableCoordinates = new ArrayList<>();
        startCrossing.setCostFromStart(0);
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
        System.out.println("Anzahl erreichbarer Crossings: " + reachableCoordinates.size());
        return reachableCoordinates;
    }

    private void expand(final PriorityQueue<Crossing> open,
            final List<Coordinate> reachableCoordinates, final Crossing currentCrossing) {
        for (final Crossing neighbour : currentCrossing.getNeighbours()) {
            if (neighbour.isClosed()) {
                continue;
            }
            final double costToNeighbour = currentCrossing.getCostToNeighbour(neighbour);
            final double costFromStartOverCurrentToNeighbour = currentCrossing.getCostFromStart() + costToNeighbour;
            if (costFromStartOverCurrentToNeighbour < neighbour.getCostFromStart()) {
                neighbour.setCostFromStart(costFromStartOverCurrentToNeighbour);
            }
            if (open.contains(neighbour)) {
                continue;
            }
            if (neighbour.getCostFromStart() > params.getSeconds()) {
                final Link linkToNeighbour = currentCrossing.getLinkToNeighbour(neighbour);
                final double[][] points = linkToNeighbour.getPoints();
                final double[] longs = points[0];
                final double[] lats = points[1];
                for (int i = 0; i < longs.length - 1; i++) {
                    final Point point = new Point(lats[i], longs[i]);
                    if (currentCrossing.getCostFromStart() + point.getCostToPoint(lats[i], longs[i], linkToNeighbour.getSpeed() / 1.33) <= params.getSeconds()) { //TODO speed param
                        reachableCoordinates.add(point);
                    }
                }
                continue;
            }
            open.add(neighbour);
            reachableCoordinates.add(neighbour);
        }
    }

    private List<double[]> createConcaveHull(final List<Coordinate> erreichbareCrossings) {
        final Timer timer = Timer.start("createConcaveHull");
        final ArrayList<double[]> points = new ArrayList<>();
        for (final Coordinate coordinate : erreichbareCrossings) {
            double lon = coordinate.getLongitude();
            double lat = coordinate.getLatitude();
            points.add(new double[]{lon, lat});
        }
        final ArrayList<double[]> concaveHull = ConcaveHullGenerator.concaveHull(points, 0.1d);//TODO parameter anpassne
        timer.stop();
        return concaveHull;
    }

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

        Connection connection = null;
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

            if (geometry.within(polygon)) { // Exact geometrisch testen, ob die Geometry im Dreieck liegt
//                dumpGeometry(geometry); //TODO ??
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


    //TODO Start zeichnen
    private void writeMap(final Crossing startCrossing, final List<double[]> concaveHull, final List<Domain> reachableDomains) {
        final Timer timer = Timer.start("writeMap");
        try {
            final UniversalPainterWriter writer = new UniversalPainterWriter("result.txt");

            com.vividsolutions.jts.geom.Coordinate[] geomCords = new com.vividsolutions.jts.geom.Coordinate[]{};
            for (final double[] koords : concaveHull) {
                geomCords = upsizeArray(geomCords, new com.vividsolutions.jts.geom.Coordinate(koords[0], koords[1]));
            }
            final Geometry geometry = new GeometryFactory().createPolygon(geomCords);

            writer.jtsGeometry(geometry, 255, 255, 255, 100, 4, 4, 4);

            //TODO ?
            final ArrayList concaveHullAL = (ArrayList) concaveHull;
            writer.line(concaveHullAL, 0, 255, 0, 200, 4, 3, "Start", "...Route...", "End"); //TODO start route end
            for (final Domain domain : reachableDomains) {
                writer.flag(domain.getLatitude(), domain.getLongitude(), 255, 0, 0, 255, domain.getName());
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Zeichnen der Karte!", e);
        }
        timer.stop();
    }

    static <T> T[] upsizeArray(T[] arr, T element) {
        final int n = arr.length;
        arr = Arrays.copyOf(arr, n + 1);
        arr[n] = element;
        return arr;
    }
}
