package lebr.demo;

import nav.NavData;


import java.io.File;
import java.io.PrintWriter;


import fu.util.IntHisto;
import fu.keys.LSIClass;
import fu.keys.LSIClassCentre;

// TODO Demo entfernen
// java -Xmx6000M -cp .;geo.jar NavLSI CAR_CACHE_de_noCC.CAC

public class NavLSI {

    private final static String OUT_CSV="navlsi.csv";

    public static void main(String[] args) {

//        if (args.length!=1) {
//            System.out.println("usage AllLSI <navcache file>");
//            System.exit(1);
//        }

        try {
//            NavData nd=new NavData(args[0],true);
//            NavData nd=new NavData("CAR_CACHE_de_noCC.CAC",true);
            NavData nd=new NavData(NavLSI.class.getResource("/CAR_CACHE_mittelfranken_noCC.CAC").getFile(),true);

            IntHisto histo=new IntHisto();

            System.out.println("Iterate through "+nd.getLinkCount()+" links...");

            for (int linkID=0;linkID<nd.getLinkCount();linkID++)
                histo.add(nd.getLSIclass(linkID));

            int[] lsiClassNrs=histo.getElementsSortedByValue();

            System.out.println("Dump of LSI classes that appear in the road network to "+OUT_CSV);

            PrintWriter out=new PrintWriter(OUT_CSV);
            out.println("lsiclassnr;count;token;name");

            for (int i=0;i<lsiClassNrs.length;i++) {
                int lsiClassNr=lsiClassNrs[i];
                int count=histo.count(lsiClassNr);
                LSIClass lsiClass=LSIClassCentre.lsiClassByID(lsiClassNr);
                out.println(lsiClassNr+";"+count+";\""+lsiClass.classToken+"\";\""+lsiClass.className+"\"");
            }
            out.close();


        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }












}