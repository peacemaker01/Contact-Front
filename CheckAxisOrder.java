import org.geotools.referencing.CRS;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

public class CheckAxisOrder {
    public static void main(String[] args) throws Exception {
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
        CoordinateReferenceSystem mercator = CRS.decode("EPSG:3857");
        System.out.println("WGS84 axis order: " + CRS.getAxisOrder(wgs84));
        System.out.println("WebMercator axis order: " + CRS.getAxisOrder(mercator));
    }
}
