// src/WeatherAPIClient.java

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A minimal client to fetch:
 *   1. (lat, lon) from city name (Geocoding API)
 *   2. Current + hourly forecast (Forecast API)
 *
 * Uses Open‑Meteo (no API key required). All responses are in JSON,
 * parsed with org.json.
 */
public class WeatherAPIClient {
    /**
     * Given a city name (e.g. "Tokyo"), returns a WeatherData object.
     * If city not found or any error occurs, returns null.
     */
    public WeatherData fetchWeatherForCity(String city) {
        try {
            // 1) Geocoding: get latitude & longitude
            String cityEncoded = city.trim().replace(" ", "%20");
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?"
                          + "name=" + cityEncoded + "&count=1";

            HttpURLConnection geoConn = (HttpURLConnection) new URL(geoUrl).openConnection();
            geoConn.setRequestMethod("GET");
            if (geoConn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader geoReader = new BufferedReader(
                    new InputStreamReader(geoConn.getInputStream()));
            StringBuilder geoSb = new StringBuilder();
            String line;
            while ((line = geoReader.readLine()) != null) {
                geoSb.append(line);
            }
            geoReader.close();

            JSONObject geoJson = new JSONObject(geoSb.toString());
            if (!geoJson.has("results")) {
                return null;
            }
            JSONArray results = geoJson.getJSONArray("results");
            if (results.length() == 0) {
                return null; // city not found
            }

            JSONObject firstMatch = results.getJSONObject(0);
            double lat = firstMatch.getDouble("latitude");
            double lon = firstMatch.getDouble("longitude");
            String resolvedName = firstMatch.getString("name");

            // 2) Forecast API: current + hourly forecast (next few hours)
            //    Request: current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code
            //    & hourly=temperature_2m & timezone=auto
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?"
              + "latitude=%.4f&longitude=%.4f"
              + "&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code"
              + "&hourly=temperature_2m"
              + "&timezone=auto",
                lat, lon
            );

            HttpURLConnection weatherConn = (HttpURLConnection) new URL(weatherUrl).openConnection();
            weatherConn.setRequestMethod("GET");
            if (weatherConn.getResponseCode() != 200) {
                return null;
            }

            BufferedReader weatherReader = new BufferedReader(
                    new InputStreamReader(weatherConn.getInputStream()));
            StringBuilder weatherSb = new StringBuilder();
            while ((line = weatherReader.readLine()) != null) {
                weatherSb.append(line);
            }
            weatherReader.close();

            JSONObject weatherJson = new JSONObject(weatherSb.toString());
            if (!weatherJson.has("current")) {
                return null;
            }
            JSONObject current = weatherJson.getJSONObject("current");
            WeatherData data = new WeatherData();
            data.cityName = resolvedName;
            data.latitude = lat;
            data.longitude = lon;
            data.currentTime = current.getString("time"); // e.g. "2025-06-05T18:00"
            data.temperatureC = current.getDouble("temperature_2m");
            data.relativeHumidityPercent = current.getDouble("relative_humidity_2m");
            data.precipitationMM = current.getDouble("precipitation");
            data.windSpeedKmh = current.getDouble("wind_speed_10m");
            data.weatherCode = current.getInt("weather_code");

            // 3) Short‑term forecast: pick the next 3 entries from hourly arrays
            //    The “hourly” object has:
            //      "time": [ "2025-06-05T18:00", "2025-06-05T19:00", ... ]
            //      "temperature_2m": [ 28.5, 27.9, ... ]
            JSONObject hourly = weatherJson.getJSONObject("hourly");
            JSONArray times = hourly.getJSONArray("time");
            JSONArray temps = hourly.getJSONArray("temperature_2m");

            // Find the index of “current.time” in the hourly.time array:
            String now = data.currentTime;
            int idx = -1;
            for (int i = 0; i < times.length(); i++) {
                if (times.getString(i).equals(now)) {
                    idx = i;
                    break;
                }
            }
            // If we didn’t find it, just start at 0:
            if (idx < 0) idx = 0;

            // Collect the next three hourly entries (if available)
            for (int i = idx + 1; i < times.length() && data.forecastList.size() < 3; i++) {
                String t = times.getString(i);                 // e.g. "2025-06-05T19:00"
                double tempNext = temps.getDouble(i);
                data.forecastList.add(new WeatherData.ForecastEntry(t, tempNext));
            }

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
