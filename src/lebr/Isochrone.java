package lebr;

// Testaufruf:
// java -cp geo.jar;lebr.jar lebr.Isochrone geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20 CAR_CACHE_de_noCC.CAC 49.46591000 11.15800500 15 20505600 20505699

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.io.ParseException;
import fu.esi.SQL;
import fu.keys.LSIClassCentreDB;
import fu.util.ConcaveHullGenerator;
import fu.util.DBUtil;
import javafx.util.Pair;
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

    private final String dbAccess;
    private final String cacFile;
    private final double latitude;
    private final double longitude;
    private final int sekunden;
    private int fromLSI;
    private int toLSI;
    private final NavData navData;

    public static void main(final String[] args) throws Exception {
//        new Isochrone(args).run();
        final String[] testargs = {"geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20",
                "CAR_CACHE_mittelfranken_noCC.CAC",
                "49.46591000",
                "11.15800500",
                "10",
                "20505600",
                "20505699"
        };
        new Isochrone(testargs).run();
    }

    public Isochrone(final String[] args) {
        if (args.length != 7) {
            throw new IllegalArgumentException(args.length + " Argumente übergeben; 7 erwartet!");
        }
        dbAccess = args[0];
        cacFile = args[1];
        latitude = Double.parseDouble(args[2]) * 1000000;
        longitude = Double.parseDouble(args[3]) * 1000000;
        sekunden = Integer.parseInt(args[4]) * 60; // Übergeben werden Minuten
        fromLSI = Integer.parseInt(args[5]);
        toLSI = Integer.parseInt(args[6]);

        try {
            navData = new NavData(NavDemo.class.getResource("/" + cacFile).getFile(), true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e); //TODO
        }
    }

    private void run() {
        final Crossing startCrossing = new Crossing(navData, Double.valueOf(latitude).intValue(),
                Double.valueOf(longitude).intValue());
        final List<Punkt> erreichbareCrossings = ermittleErreichbareCrossings(startCrossing);
        final List<double[]> konkaveHuelle = erzeugeKonkaveHuelle(erreichbareCrossings);
        final List<Domain> erreichbareDomains;
        try {
            erreichbareDomains = ermittleErreichbareDomains(konkaveHuelle);
        } catch (SQLException | ParseException e) {
            throw new RuntimeException(e);
        }
        erzeugeKarte(startCrossing, konkaveHuelle, erreichbareDomains); //TODO startcrossing oder übergebene koords?
    }

    private List<Punkt> ermittleErreichbareCrossings(final Crossing startCrossing) {
        final Timer timer = Timer.start("ermittleErreichbareCrossings");

        final List<Punkt> erreichbarePunkte = new ArrayList<>();
        erreichbarePunkte.add(startCrossing);

        final PriorityQueue<Crossing> queue = new PriorityQueue<>((o1, o2)
                -> Double.compare(o1.getKostenVonStart(), o2.getKostenVonStart()));
        queue.add(startCrossing);

        startCrossing.setKostenVonStart(0);
        do {
            final Crossing aktuell = queue.poll();
            expand(queue, erreichbarePunkte, aktuell);
            aktuell.closed = true;
        } while (!queue.isEmpty());

        timer.stop();
        System.out.println("Anzahl erreichbarer Crossings: " + erreichbarePunkte.size());
        return erreichbarePunkte;
    }

    private void expand(final PriorityQueue<Crossing> open,
            final List<Punkt> erreichbarePunkte, final Crossing aktuell) {
        for (final Crossing nachbar : aktuell.getNachbarn()) {
            if (nachbar.closed) {
                continue;
            }
            double kostenZuNachbarn = aktuell.getKostenZuNachbar(nachbar);
            double kostenVonStartzuNachbarn = aktuell.getKostenVonStart() + kostenZuNachbarn;
            if (kostenVonStartzuNachbarn < nachbar.getKostenVonStart()) {
                nachbar.setKostenVonStart(kostenVonStartzuNachbarn);
            }
            if (open.contains(nachbar)) {
                continue;
            }
            if (nachbar.getKostenVonStart() > sekunden) {
                crossingListeStart.add(aktuell);
                crossingListeEnde.add(nachbar);


                final Link linkZuNachbar = aktuell.getLinkZuNachbar(nachbar);
                final double[][] geometriepunkte = linkZuNachbar.getGeometriepunkte();
                final double[] longs = geometriepunkte[0];
                final double[] lats = geometriepunkte[1];

                for (int i = 0; i < longs.length - 1; i++) {
                    final GeometriePunkt gp = new GeometriePunkt(lats[i], longs[i]);
                    if (aktuell.getKostenVonStart() + gp.getKostenZuPunkt(lats[i], longs[i], linkZuNachbar.getSpeed() / 1.33) <= sekunden) { //TODO speed param
                        erreichbarePunkte.add(gp);
                        geometriePunkteListe.add(new Pair<>(lats[i], longs[i]));
                    }
                }


                continue;
            }
            open.add(nachbar);
            erreichbarePunkte.add(nachbar);
        }
    }

    boolean b = true;

    //TODO
    List<Pair<Double, Double>> geometriePunkteListe = new ArrayList<>();
    List<Crossing> crossingListeStart = new ArrayList<>();
    List<Crossing> crossingListeEnde = new ArrayList<>();

    private List<double[]> erzeugeKonkaveHuelle(final List<Punkt> erreichbareCrossings) {
        final Timer timer = Timer.start("erzeugeKonkaveHuelle");
        final ArrayList<double[]> demoPoints = new ArrayList<>();
        for (final Punkt punkt : erreichbareCrossings) {
            double lon = punkt.getLongitude();
            double lat = punkt.getLatitude();
            demoPoints.add(new double[]{lon, lat});
        }

        final ArrayList<double[]> concaveHull = ConcaveHullGenerator.concaveHull(demoPoints, 0.1d);//TODO parameter anpassne
        timer.stop();
        return concaveHull;
    }

    private List<Domain> ermittleErreichbareDomains(final List<double[]> konkaveHuelle) throws SQLException, ParseException {
        final Timer timer = Timer.start("ermittleErreichbareDomains");
        final Coordinate[] coords = new Coordinate[konkaveHuelle.size()];
        for (int i = 0; i < konkaveHuelle.size(); i++) {
            final double[] doubles = konkaveHuelle.get(i);
            coords[i] = new Coordinate(doubles[0], doubles[1]);
        }

        final GeometryFactory geomfact = new GeometryFactory();
        final Geometry polygon = geomfact.createPolygon(geomfact.createLinearRing(coords), new LinearRing[0]);

        Envelope boundingBox = polygon.getEnvelopeInternal(); // Bounding Box berechnen

        Connection connection = null;
        ResultSet resultSet;
        Statement statement;

        try {
            DBUtil.parseDBparams("geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20");
            connection = DBUtil.getConnection();
            connection.setAutoCommit(false);
            LSIClassCentreDB.initFromDB(connection);
        } catch (Exception e) {
            System.out.println("Error initialising DB access: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }

        statement = connection.createStatement();
        statement.setFetchSize(1000);
//        int[] lcStrassen=LSIClassCentreDB.lsiClassRange("STRASSEN_WEGE");
//        fromLSI = lcStrassen[0];
//        toLSI = lcStrassen[1];

        resultSet = statement.executeQuery("SELECT realname, gao_geometry FROM domain WHERE geometry='P' AND lsiclass1 BETWEEN " + fromLSI + " AND " + toLSI + " AND" +
                SQL.createIndexQuery(boundingBox.getMinX(), boundingBox.getMaxY(), boundingBox.getMaxX(), boundingBox.getMinY(), SQL.COMPLETELY_INSIDE)
        );

        final List<Domain> domains = new ArrayList<>();

        while (resultSet.next()) {
            String realname = resultSet.getString(1);
            byte[] geodata_line = resultSet.getBytes(2);
            Geometry geom = SQL.wkb2Geometry(geodata_line);

            if (geom.within(polygon)) {                       // Exact geometrisch testen, ob die Geometry im Dreieck liegt
//                dumpGeometry(geom);
//                domains.add(new Domain(realname, geom));
                final double latitude = geom.getCentroid().getY();
                final double longitude = geom.getCentroid().getX();
                domains.add(new Domain(realname, latitude, longitude));
            }
        }
        resultSet.close();

        timer.stop();
        System.out.println("Anzahl erreichbarer Domains: " + domains.size());
        return domains;
    }


    private void erzeugeKarte(final Crossing startCrossing, final List<double[]> konkaveHuelle, final List<Domain> erreichbareDomains) {
        final Timer timer = Timer.start("erzeugeKarte");
        try {
            UniversalPainterWriter upw = new UniversalPainterWriter("result.txt");

            Coordinate[] coordinates = new Coordinate[]{};
            for (double[] koords : konkaveHuelle) {
                coordinates = upsizeArray(coordinates, new Coordinate(koords[0], koords[1]));
            }
            Geometry geo = new GeometryFactory().createPolygon(coordinates);

            upw.jtsGeometry(geo, 255, 255, 255, 100, 4, 4, 4);


            //double[] lats = new double[]{};
            //double[] longs = new double[]{};
            //for (double[] koords: konkaveHuelle) {
            //   upsizeArray(lats, koords[0]);
            //    upsizeArray(longs, koords[1]);
            //}

            ArrayList huelle = (ArrayList) konkaveHuelle;
            //upw.line(lats,longs,0,255,0,200,4,3,"Start","...Route...","End");
            upw.line(huelle, 0, 255, 0, 200, 4, 3, "Start", "...Route...", "End");
            for (Domain domain : erreichbareDomains) {
                upw.flag(domain.getLatitude(), domain.getLongitude(), 255, 0, 0, 255, domain.getName());
            }
            for (Pair<Double, Double> pair : geometriePunkteListe) {
                upw.flag(pair.getKey(), pair.getValue(), 0, 255, 0, 120, "GP");
            }
            for (Crossing c : crossingListeStart) {
                upw.flag(c.getLatitude() / 1000000.0, c.getLongitude() / 1000000.0, 0, 0, 255, 255, "S");
            }
            for (Crossing c : crossingListeEnde) {
                upw.flag(c.getLatitude() / 1000000.0, c.getLongitude() / 1000000.0, 0, 0, 0, 255, "E");
            }

            upw.close();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("TODO"); //TODO
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
