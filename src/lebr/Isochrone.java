package lebr;

// Testaufruf:
// java -cp geo.jar;lebr.jar lebr.Isochrone geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20 CAR_CACHE_de_noCC.CAC 49.46591000 11.15800500 15 20505600 20505699

import com.vividsolutions.jts.geom.Coordinate;
import fu.util.ConcaveHullGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import lebr.demo.NavDemo;
import nav.NavData;
import pp.dorenda.client2.additional.UniversalPainterWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
    private final int fromLSI;
    private final int toLSI;
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
        long start = System.currentTimeMillis();
        System.out.println("erreichbareCrossing - start...");
        final List<Crossing> erreichbareCrossings = ermittleErreichbareCrossings(startCrossing);
        System.out.println("erreichbareCrossing: " + (System.currentTimeMillis() - start) + " ms");
        start = System.currentTimeMillis();
        final List<double[]> konkaveHuelle = erzeugeKonkaveHuelle(erreichbareCrossings);
        System.out.println("konkaveHuellen: " + (System.currentTimeMillis() - start) + " ms");
        final List<Domain> erreichbareDomains = ermittleErreichbareDomains(konkaveHuelle);
        erzeugeKarte(startCrossing, konkaveHuelle, erreichbareDomains); //TODO startcrossing oder übergebene koords?
    }

    private List<Crossing> ermittleErreichbareCrossings(final Crossing startCrossing) {
        final List<Crossing> erreichbareCrossings = new ArrayList<>();
        erreichbareCrossings.add(startCrossing);
        final List<Crossing> closed = new ArrayList<>();
        final ObservableList<Crossing> open = FXCollections.observableArrayList(startCrossing);
        final SortedList<Crossing> openSorted = open.sorted((o1, o2)
                -> Double.compare(o1.getKostenVonStart(), o2.getKostenVonStart()));
        startCrossing.setKostenVonStart(0);
        do {
            final Crossing aktuell = openSorted.get(0);
            open.remove(aktuell);
            expand(closed, open, erreichbareCrossings, aktuell);
            closed.add(aktuell);
        } while (!open.isEmpty());
        System.out.println("ermittleErreichbareCrossings Ende");
        return erreichbareCrossings;
    }

    private void expand(final List<Crossing> closed, final List<Crossing> open,
            final List<Crossing> erreichbareCrossings, final Crossing aktuell) {
        for (final Crossing nachbar : aktuell.getNachbarn()) {
            if (closed.contains(nachbar)) {
                continue;
            }
            double kostenZuNachbarn = aktuell.getKostenZuNachbar(nachbar);
            double kostenVonStartzuNachbarn = aktuell.getKostenVonStart() + kostenZuNachbarn;
            if (kostenVonStartzuNachbarn < nachbar.getKostenVonStart()) {
                nachbar.setKostenVonStart(kostenVonStartzuNachbarn);
            }
            // TODO Vorgänger merken?
            if (open.contains(nachbar) || nachbar.getKostenVonStart() > sekunden) {
                continue;
            }
            open.add(nachbar);
            erreichbareCrossings.add(nachbar);
            //TODO impl
//            int kostenStartBisKnotenAktuell = kostenStartBisKnoten + aktuell.getKostenZuNachbar(nachbar);
//            int kostenGesamtGeschaetztAktuell = kostenStartBisKnotenAktuell +
        }
    }

    private List<double[]> erzeugeKonkaveHuelle(final List<Crossing> erreichbareCrossings) {
        final ArrayList<double[]> demoPoints = new ArrayList<>();
        for (final Crossing crossing : erreichbareCrossings) {
            double lon = Double.valueOf(crossing.getLongitude()).intValue();
            double lat = Double.valueOf(crossing.getLatitude()).intValue();
            demoPoints.add(new double[]{lat, lon});
        }
        return ConcaveHullGenerator.concaveHull(demoPoints, 1.0d); //TODO parameter anpassne
    }

    private List<Domain> ermittleErreichbareDomains(final List<double[]> konkaveHuelle) {
        final Coordinate[] coords = new Coordinate[konkaveHuelle.size()];
        for (int i = 0; i < konkaveHuelle.size(); i++) {
            final double[] doubles = konkaveHuelle.get(i);
            coords[i] = new Coordinate(doubles[0], doubles[1]);
        }

        //TODO Demoimpl
/*
        Coordinate[] coords=new Coordinate[4];
            coords[0]=new Coordinate(11.097026,49.460811);
            coords[1]=new Coordinate(11.104676,49.460811);
            coords[2]=new Coordinate(11.101730,49.455367);
            coords[3]=coords[0];
            Geometry triangle=geomfact.createPolygon(geomfact.createLinearRing(coords),new LinearRing[0]);

            Envelope boundingBox=triangle.getEnvelopeInternal(); // Bounding Box berechnen

            System.out.println("Abfrage: Alle Objekte Strassen im Bereich des angegebenen Dreiecks (Naehe Informatik-Gebaeude)");

            int[] lcStrassen=LSIClassCentreDB.lsiClassRange("STRASSEN_WEGE");

            time=System.currentTimeMillis(); // Zeitmessung beginnen

            statement=connection.createStatement();
            statement.setFetchSize(1000);
            resultSet = statement.executeQuery("SELECT realname, geodata_line FROM domain WHERE geometry='L' AND lsiclass1 BETWEEN "+lcStrassen[0]+" AND "+lcStrassen[1]+" AND"+
                                               SQL.createIndexQuery(boundingBox.getMinX(),boundingBox.getMaxY(),boundingBox.getMaxX(),boundingBox.getMinY(),SQL.COMPLETELY_INSIDE)
                                              );

            cnt=0;

            while (resultSet.next()) {
                String realname=resultSet.getString(1);
                byte[] geodata_line=resultSet.getBytes(2);
                Geometry geom=SQL.wkb2Geometry(geodata_line);

                if (geom.within(triangle)) {                       // Exact geometrisch testen, ob die Geometry im Dreieck liegt
                    System.out.println(realname);
                    dumpGeometry(geom);
                    cnt++;
                 }
                 else
                     System.out.println(realname+" ist nicht exakt in der gesuchten Geometry");
            }
            resultSet.close();
            System.out.println("Anzahl der Resultate: "+cnt);
            System.out.println("Zeit "+(System.currentTimeMillis()-time)/1000+" s");
            System.out.println("Ende Abfrage");
            System.out.println("=====================================================");
        }
        catch (Exception e) {
            System.out.println("Error processing DB queries: "+e.toString());
            e.printStackTrace();
            System.exit(1);
        }
         */
        return null;
    }


    private void erzeugeKarte(final Crossing startCrossing, final List<double[]> konkaveHuelle, final List<Domain> erreichbareDomains) {
        try {
            UniversalPainterWriter upw = new UniversalPainterWriter("result.txt");
            //upw.line(testLats,testLongs,0,255,0,200,4,3,"Start","...Route...","End");
            //upw.jtsGeometry(geom,255,255,255,100,4,4,4);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("TODO"); //TODO
        }
    }
}
