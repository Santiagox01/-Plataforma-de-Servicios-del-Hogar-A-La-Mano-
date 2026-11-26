package co.edu.udea.alamano.gestion_servicios.aplicacion;

import org.springframework.stereotype.Component;

@Component
public class CalculadorDistanciaHaversine {

    private static final double RADIO_TIERRA_KM = 6371.0;

    public double calcularKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distancia = RADIO_TIERRA_KM * c;
        return Math.round(distancia * 100.0) / 100.0;
    }
}
