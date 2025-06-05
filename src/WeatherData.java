// src/WeatherData.java

import java.util.ArrayList;
import java.util.List;

/**
 * A lightweight model class that holds:
 *   • cityName  – e.g. "Bangkok"
 *   • latitude, longitude
 *   • currentTime             – ISO “yyyy-MM-dd'T'HH:mm” (local to city)
 *   • temperatureC            – in °C
 *   • relativeHumidityPercent – %
 *   • precipitationMM         – mm
 *   • windSpeedKmh            – km/h
 *   • weatherCode             – from Open-Meteo (int)
 *   • A short forecast (next 3 × 1‑hour slots): list of (time, tempC)
 */
public class WeatherData {
    public String cityName;
    public double latitude;
    public double longitude;

    public String currentTime;       // e.g. "2025-06-05T18:00"
    public double temperatureC;
    public double relativeHumidityPercent;
    public double precipitationMM;
    public double windSpeedKmh;
    public int weatherCode;

    public List<ForecastEntry> forecastList = new ArrayList<>();

    /** One forecast entry: localDateTime + tempC. */
    public static class ForecastEntry {
        public String time;   // e.g. "2025-06-05T19:00"
        public double tempC;

        public ForecastEntry(String time, double tempC) {
            this.time = time;
            this.tempC = tempC;
        }
    }
}
