package lebr.demo;

import nav.NavData;
import pp.dorenda.client2.additional.UniversalPainterWriter;

import java.io.File;

// TODO Demo entfernen
// java -Xmx6000M -cp .;geo.jar NavDemo CAR_CACHE_de_noCC.CAC

public class NavDemo {

    public static void main(String[] args) {

//        if (args.length!=1) {
//            System.out.println("usage NavDemo <navcache file>");
//            System.exit(1);
//        }

        try {
//            NavData nd=new NavData(args[0],true);
//            NavData nd=new NavData("CAR_CACHE_de_noCC.CAC",true);
            NavData nd=new NavData(NavDemo.class.getResource("/CAR_CACHE_mittelfranken_noCC.CAC").getFile(),true);


            System.out.println("Link IDs:    0..."+(nd.getLinkCount()-1));
            System.out.println("Crossing ID: 0..."+(nd.getCrossingCount()-1));
            System.out.println("Domain IDs:  "+nd.getDomainMinID()+"..."+nd.getDomainMaxID());

            // Zeige die ersten 10 Stra�enabschnitte (Links) der Navigationsdaten

            System.out.println("============= LINK examples =============");
            for (int i=0;i<10;i++) {
                System.out.println("------------ LINK "+i+" ------------");

                System.out.println("Reverse Link=       "+nd.getReverseLink(i));
                System.out.println("CrossingID (from)=  "+nd.getCrossingIDFrom(i));
                System.out.println("CrossingID (to)=    "+nd.getCrossingIDTo(i));
                System.out.println("Domain ID=          "+nd.getDomainID(i));
                System.out.println("Domain posNrFrom=   "+nd.getDomainPosNrFrom(i));
                System.out.println("Domain posNrTo=     "+nd.getDomainPosNrTo(i));
                System.out.println("LSIclass=           "+nd.getLSIclass(i));
                System.out.println("goes counter oneway="+nd.goesCounterOneway(i));
                System.out.println("Length=             "+nd.getLengthMeters(i)+" m");
                System.out.println("Max speed=          "+nd.getMaxSpeedKMperHours(i)+" km/h");
                System.out.println("North angle (from)= "+nd.getNorthAngleFrom(i)+" deg");
                System.out.println("North angle (to)=   "+nd.getNorthAngleTo(i)+" deg");
            }


            // Zeige die ersten 10 Kreuzungen der Navigationsdaten

            System.out.println("");
            System.out.println("");
            System.out.println("============= CROSSING examples =============");
            for (int i=0;i<10;i++) {
                System.out.println("------------ CROSSSING "+i+" ------------");
                if (nd.isIsolatedCrossing(i)) {
                    System.out.println("Isolated crossing");
                    continue;
                }
                System.out.println("Latitude=      "+nd.getCrossingLatE6(i));
                System.out.println("Longitude=     "+nd.getCrossingLongE6(i));
                System.out.print  ("Outgoing links=");
                for (int l:nd.getLinksForCrossing(i)) System.out.print(l+" ");
                System.out.println();
            }


            // Zeige die ersten 10 Domains der Navigationsdaten

            System.out.println("");
            System.out.println("");
            System.out.println("============= DOMAIN examples =============");
            for (int i=nd.getDomainMinID();i<nd.getDomainMinID()+10;i++) {
                System.out.println("------------ DOMAIN "+i+" ------------");
                if (!nd.isDomain(i)) {
                    System.out.println("No domain entry");
                    continue;
                }
                System.out.println("Name=     "+nd.getDomainName(i));
                System.out.print  ("Lat/Longs=");
                int[] lats=nd.getDomainLatsE6(i);
                int[] longs=nd.getDomainLongsE6(i);
                for (int j=0;j<Math.min(5,lats.length);j++) {
                    System.out.print(lats[j]+"/"+longs[j]+" ");
                }
                if (lats.length>5) 
                    System.out.print("...");
                System.out.println();
            }


            // Demo: Kreuzung anhand von Position suchen und alles �ber die Kreuzung ermitteln

            System.out.println("");
            System.out.println("");
            System.out.println("============= SEARCH CROSSING example =============");

            int lat=49466250;
            int lon=11157778;
            int crossingID=nd.getNearestCrossing(lat,lon);


            System.out.println("Search latitude=            "+lat);
            System.out.println("Search longitude=           "+lon);
            System.out.println("Nearest crossing latitude=  "+nd.getCrossingLatE6(crossingID));
            System.out.println("Nearest crossing longitude= "+nd.getCrossingLongE6(crossingID));

            int[] links=nd.getLinksForCrossing(crossingID);
            System.out.println("Number of outgoing links=   "+links.length);

            for (int i=0;i<links.length;i++) {
                int linkID=links[i];
                System.out.println("Link "+(i+1));
                System.out.println("  Name=                 "+nd.getDomainName(nd.getDomainID(linkID)));
                System.out.println("  Outgoing north angle= "+nd.getNorthAngleFrom(linkID)+" deg");
                System.out.println("  Length=               "+nd.getLengthMeters(linkID)+" m");
                System.out.println("  Max speed=            "+nd.getMaxSpeedKMperHours(linkID)+" km/h");
            }


            // Demo: Route ausgeben


            double[] TESTPOS=new double[]{
                49.48431   ,11.197552 , 49.48431   ,11.197552 , 49.484063  ,11.197551 , 49.483854  ,11.197585 , 49.483378  ,11.197678 , 49.483223  ,11.197723 , 49.483019  ,11.197751 , 49.482861  ,11.197742 , 49.482664  ,11.197764 , 49.482664  ,11.197764 , 49.482618  ,11.197775 , 49.482618  ,11.197775 , 49.482345  ,11.197854 , 49.48218   ,11.197945 , 49.482095  ,11.198011 , 49.482095  ,11.198011 , 
                49.482015  ,11.198055 , 49.482015  ,11.198055 , 49.481997  ,11.19801  , 49.481969  ,11.197979 , 49.481932  ,11.197965 , 49.4819    ,11.197972 , 49.481874  ,11.197992 , 49.481874  ,11.197992 , 49.48185   ,11.198034 , 49.48184   ,11.198069 , 49.481837  ,11.198102 , 49.48184   ,11.198144 , 49.481849  ,11.198179 , 49.481849  ,11.198179 , 49.481775  ,11.198229 , 49.481775  ,11.198229 , 
                49.48162   ,11.198374 , 49.48162   ,11.198374 , 49.481492  ,11.198508 , 49.481311  ,11.198772 , 49.481311  ,11.198772 , 49.481134  ,11.199031 , 49.481134  ,11.199031 , 49.481077  ,11.199145 , 49.480978  ,11.199324 , 49.480978  ,11.199324 , 49.480841  ,11.199501 , 49.480841  ,11.199501 , 49.480471  ,11.199974 , 49.480356  ,11.200101 , 49.480356  ,11.200101 , 49.480289  ,11.199878 , 
                49.480177  ,11.19928  , 49.480177  ,11.19928  , 49.480156  ,11.199102 , 49.480056  ,11.198338 , 49.48004   ,11.198207 , 49.48004   ,11.198207 , 49.479992  ,11.197821 , 49.479992  ,11.197821 , 49.47976   ,11.196034 , 49.47976   ,11.196034 , 49.479756  ,11.195977 , 49.479756  ,11.195977 , 49.479762  ,11.19581  , 49.47966   ,11.194924 , 49.47966   ,11.194924 , 49.47931   ,11.192661 , 
                49.479241  ,11.19199  , 49.479241  ,11.19199  , 49.479159  ,11.19138  , 49.479159  ,11.19138  , 49.478878  ,11.189327 , 49.478878  ,11.189327 , 49.47865   ,11.187668 , 49.478326  ,11.185277 , 49.478168  ,11.184106 , 49.478034  ,11.182976 , 49.478034  ,11.182976 , 49.477952  ,11.182201 , 49.477861  ,11.181494 , 49.477817  ,11.181199 , 49.477817  ,11.181199 , 49.47772   ,11.180574 , 
                49.477643  ,11.180071 , 49.477535  ,11.179363 , 49.477438  ,11.178636 , 49.477248  ,11.177196 , 49.477248  ,11.177196 , 49.477121  ,11.176294 , 49.477121  ,11.176294 , 49.477023  ,11.175532 , 49.477023  ,11.175532 , 49.476895  ,11.174501 , 49.476851  ,11.174156 , 49.476811  ,11.173866 , 49.476811  ,11.173866 , 49.476658  ,11.172833 , 49.476658  ,11.172833 , 49.476621  ,11.172576 , 
                49.476565  ,11.172293 , 49.476429  ,11.171785 , 49.476429  ,11.171785 , 49.476359  ,11.171289 , 49.476359  ,11.171289 , 49.476091  ,11.169366 , 49.476091  ,11.169366 , 49.473634  ,11.151545 , 49.473329  ,11.149513 , 49.472293  ,11.141982 , 49.472293  ,11.141982 , 49.47226   ,11.141659 , 49.47226   ,11.141659 , 49.472222  ,11.141095 , 49.472222  ,11.141095 , 49.472216  ,11.140997 , 
                49.472216  ,11.140997 , 49.472237  ,11.14062  , 49.472237  ,11.14062  , 49.47225   ,11.140398 , 49.472298  ,11.14002  , 49.472366  ,11.13957  , 49.472491  ,11.139012 , 49.472491  ,11.139012 , 49.472526  ,11.138779 , 49.472529  ,11.138722 , 49.472529  ,11.138722 , 49.472549  ,11.138486 , 49.472549  ,11.138486 , 49.472544  ,11.138256 , 49.472503  ,11.137394 , 49.472503  ,11.137394 , 
                49.472482  ,11.136959 , 49.472474  ,11.136854 , 49.472474  ,11.136854 , 49.472463  ,11.136667 , 49.472456  ,11.136613 , 49.472456  ,11.136613 , 49.472397  ,11.136301 , 49.472397  ,11.136301 , 49.472255  ,11.135915 , 49.472255  ,11.135915 , 49.472191  ,11.135722 , 49.471915  ,11.134997 , 49.471891  ,11.134937 , 49.471891  ,11.134937 , 49.471882  ,11.13492  , 49.471882  ,11.13492  , 
                49.471842  ,11.134815 , 49.471842  ,11.134815 , 49.471912  ,11.134704 , 49.471912  ,11.134704 , 49.471954  ,11.134628 , 49.472004  ,11.134527 , 49.472043  ,11.134335 , 49.472043  ,11.134335 , 49.472078  ,11.134244 , 49.472122  ,11.134131 , 49.472122  ,11.134131 , 49.472444  ,11.133342 , 49.472444  ,11.133342 , 49.472498  ,11.133201 , 49.472668  ,11.132841 , 49.472668  ,11.132841 , 
                49.472772  ,11.13262  , 49.472772  ,11.13262  , 49.47323   ,11.131685 , 49.473329  ,11.131468 , 49.473419  ,11.131259 , 49.47354   ,11.130945 , 49.473658  ,11.130598 , 49.47377   ,11.130214 , 49.47377   ,11.130214 , 49.473981  ,11.129332 , 49.474236  ,11.128255 , 49.474236  ,11.128255 , 49.474272  ,11.12811  , 49.474272  ,11.12811  , 49.474298  ,11.127993 , 49.474332  ,11.127849 , 
                49.474774  ,11.126004 , 49.474774  ,11.126004 , 49.474879  ,11.125512 , 49.474879  ,11.125512 , 49.47493   ,11.125146 , 49.474964  ,11.124793 , 49.474981  ,11.12446  , 49.474981  ,11.124319 , 49.474981  ,11.124319 , 49.47498   ,11.124123 , 49.474963  ,11.123553 , 49.474963  ,11.123553 , 49.474953  ,11.123128 , 49.47494   ,11.122905 , 49.47494   ,11.122905 , 49.474915  ,11.122614 , 
                49.474915  ,11.122614 };

             double[] testLats=new double[TESTPOS.length/2];
             double[] testLongs=new double[TESTPOS.length/2];
             for (int i=0;i<testLats.length;i++) {
                 testLats[i]=TESTPOS[i*2];
                 testLongs[i]=TESTPOS[i*2+1];
             }

             UniversalPainterWriter upw=new UniversalPainterWriter("result.txt");
             upw.line(testLats,testLongs,0,255,0,200,4,3,"Start","...Route...","End");
             upw.close();


        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }












}