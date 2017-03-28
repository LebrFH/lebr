package lebr.demo;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;

import java.util.ArrayList;

import fu.esi.SQL;
import fu.util.DBUtil;
import fu.keys.LSIClassCentreDB;
import fu.util.ConcaveHullGenerator;
import pp.dorenda.client2.additional.UniversalPainterWriter;

// TODO Demo entfernen
// java -cp .;geo.jar DBDemo geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20

public class DBDemo {

    private static GeometryFactory geomfact=new GeometryFactory();

    public static void main(String args[]) {

//        if (args.length!=1) {
//            System.out.println("usage DBDemo <dbaccess string>");
//            System.exit(1);
//        }

        Connection connection=null;
        ResultSet resultSet;
        Statement statement;

        long time;
        int cnt;

        try {
//            DBUtil.parseDBparams(args[0]);
            DBUtil.parseDBparams("geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20");
            connection=DBUtil.getConnection();
            connection.setAutoCommit(false);  
            LSIClassCentreDB.initFromDB(connection);
        }
        catch (Exception e) {
            System.out.println("Error initialising DB access: "+e.toString());
            e.printStackTrace();
            System.exit(1);
        }


        try {

// ************* DEMO-ABFRAGE 1: Domain-Tabelle (alle Objekt in einem Dreieck) *************
// Ueber SQL.createIndexQuery wird eine SQL-Bedingung generiert (stellt aber ein Rechteck - die Bounding Box dar)
// Spaeter werden die Resultate anhand der exakten Dreiecks-Geomtrie ueberprueft: geom.within(triangle)


            // KONSTRUKTION EINER VERGLEICHSGEOMETRIE

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

// ************* DEMO-ABFRAGE 1: Concave Hull *************
// Wie erzeugt man eine Concave-Hull aus einer Punktwolke

        System.out.println("Abfrage: berechne ein Cancave Hull aus Demopunkten");


        double[][] demoPointsRaw={{1,1}, {1,10}, {20,10}, {5,5}, {7,7}, {18, 2},{12,7},{9,6},{10,2},{6,3},{1,5}};
        ArrayList<double[]> demoPoints=new ArrayList<double[]>();
        for (double[] point:demoPointsRaw)
             demoPoints.add(point);

        ArrayList<double[]> concaveHull=ConcaveHullGenerator.concaveHull(demoPoints,1.0d);
        for (double[] hullPoint:concaveHull)
            System.out.println(hullPoint[0]+"/"+hullPoint[1]);



        try {

// ************* DEMO-ABFRAGE 3: Objekt f�r dorenda malen *************
// Eine Objektgeomtrie wird aus der Datenbank geholt und mit f�r dorenda gemalt

            System.out.println("Abfrage: Domain laden und f�r dorenda malen");
            time=System.currentTimeMillis(); // Zeitmessung beginnen

            statement=connection.createStatement();
            statement.setFetchSize(1000);
            resultSet = statement.executeQuery("select realname,gao_geometry from domain where realname ='Lorenzer Platz' AND geometry='A'");

            cnt=0;
 
            UniversalPainterWriter upw=new UniversalPainterWriter("result.txt");


            while (resultSet.next()) {
                String realname=resultSet.getString(1);
                byte[] gao_geometry=resultSet.getBytes(2);
                Geometry geom=SQL.wkb2Geometry(gao_geometry);
                System.out.println(realname);
                dumpGeometry(geom);

                upw.jtsGeometry(geom,255,255,255,100,4,4,4);

                cnt++;
            }
            resultSet.close();
            upw.close();

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





    }


    public static void dumpGeometry(Geometry geom) {
        if (geom instanceof Polygon) {
            System.out.println("Class: Polygon");
            LineString extring=((Polygon)geom).getExteriorRing();
            System.out.println(extring.getNumPoints()+" exterior ring points");
            int n=3;
            if (n>=extring.getNumPoints())
                n=extring.getNumPoints();
            System.out.println("First "+n+" poly points:");
            for (int i=0;i<n;i++) {
                 Coordinate coord=extring.getCoordinateN(i);
                 dumpCoord(coord);
            }
            System.out.println("");
        }

        else if (geom instanceof MultiPolygon) {
            System.out.println("Class: MultiPolygon");
            System.out.println(geom.getNumGeometries()+" part geometries");
            Geometry firstgeom=geom.getGeometryN(0);
            LineString extring=((Polygon)firstgeom).getExteriorRing();
            System.out.println(extring.getNumPoints()+" exterior ring points (geometry 0)");
            int n=3;
            if (n>=extring.getNumPoints())
                n=extring.getNumPoints();
            System.out.println("First "+n+" poly points (geometry 0):");
            for (int i=0;i<n;i++) {
                 Coordinate coord=extring.getCoordinateN(i);
                 dumpCoord(coord);
            }
            System.out.println("");

        }

        else if (geom instanceof LineString) {
            System.out.println("Class: LineString");
            LineString listring=((LineString)geom);
            System.out.println(listring.getNumPoints()+" line points");
            int n=3;
            if (n>=listring.getNumPoints())
                n=listring.getNumPoints();
            System.out.println("First "+n+" line points:");
            for (int i=0;i<n;i++) {
                 Coordinate coord=listring.getCoordinateN(i);
                 dumpCoord(coord);
            }
            System.out.println("");
        }

        else if (geom instanceof MultiLineString) {
            System.out.println("Class: MultiLineString");
            System.out.println(geom.getNumGeometries()+" part geometries");
            Geometry firstgeom=geom.getGeometryN(0);
            LineString listring=((LineString)firstgeom);
            System.out.println(listring.getNumPoints()+" line points (geometry 0)");
            int n=3;
            if (n>=listring.getNumPoints())
                n=listring.getNumPoints();
            System.out.println("First "+n+" line points  (geometry 0):");
            for (int i=0;i<n;i++) {
                 Coordinate coord=listring.getCoordinateN(i);
                 dumpCoord(coord);
            }
            System.out.println("");

        }

        else if (geom instanceof Point) {
            System.out.println("Class: Point");
            Coordinate coord=((Point)geom).getCoordinate();
            dumpCoord(coord);
        }
        else {
            System.out.println("don't know how to tell something about "+geom.getClass().getName());        
        }
    }


    public static void dumpCoord(Coordinate coord) {
        System.out.print(coord.y+","+coord.x);   // Lat,Long
        if (!Double.isNaN(coord.z))
            System.out.print(" ("+Math.round(coord.z)+"m)");
        System.out.print(" ");
    }   


}