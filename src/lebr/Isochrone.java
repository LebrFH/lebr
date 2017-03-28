package lebr;

// Testaufruf:
// java -cp geo.jar;lebr.jar lebr.Isochrone geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20 CAR_CACHE_de_noCC.CAC 49.46591000 11.15800500 15 20505600 20505699

// Testaufruf - dorenda:
// java -cp .;geo.jar pp.dorenda.client2.testapp.TestActivity  -m webservice;geosrv.informatik.fh-nuernberg.de  -c pp.dorenda.client2.additional.UniversalPainter -a result.txt;s
public class Isochrone {

    // Parameter:           Testaufruf:
    // 0: <dbaccesstr>      geosrv.informatik.fh-nuernberg.de/5432/dbuser/dbuser/deproDB20
    // 1: <cacfile>         CAR_CACHE_de_noCC.CAC
    // 2: <latitude>        49.46591000
    // 3: <longitude>       11.15800500
    // 4: <minutes>         15
    // 5: <fromLSI>         20505600
    // 6: <toLSI>           20505699
    public static void main(String[] args){
        throw new UnsupportedOperationException("Nicht implementiert");
    }
}
