import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherConsoleApp {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter city name: ");
            String city = scanner.nextLine().trim();

            // Geocoding API to get latitude and longitude
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                    city.replace(" ", "%20") + "&count=1";

            HttpURLConnection geoConn = (HttpURLConnection) new URL(geoUrl).openConnection();
            geoConn.setRequestMethod("GET");

            BufferedReader geoReader = new BufferedReader(new InputStreamReader(geoConn.getInputStream()));
            StringBuilder geoResponse = new StringBuilder();
            String line;
            while ((line = geoReader.readLine()) != null) {
                geoResponse.append(line);
            }
            geoReader.close();

            JSONObject geoJson = new JSONObject(geoResponse.toString());
            JSONArray results = geoJson.getJSONArray("results");
            if (results.length() == 0) {
                System.out.println("City not found.");
                return;
            }

            JSONObject location = results.getJSONObject(0);
            double latitude = location.getDouble("latitude");
            double longitude = location.getDouble("longitude");

            // Weather API with extra parameters
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,wind_speed_10m,precipitation,relative_humidity_2m,weather_code&timezone=auto",
                latitude, longitude);

            HttpURLConnection weatherConn = (HttpURLConnection) new URL(weatherUrl).openConnection();
            weatherConn.setRequestMethod("GET");

            BufferedReader weatherReader = new BufferedReader(new InputStreamReader(weatherConn.getInputStream()));
            StringBuilder weatherResponse = new StringBuilder();
            while ((line = weatherReader.readLine()) != null) {
                weatherResponse.append(line);
            }
            weatherReader.close();

            JSONObject weatherJson = new JSONObject(weatherResponse.toString());
            JSONObject current = weatherJson.getJSONObject("current");

            String time = current.getString("time");
            double temperature = current.getDouble("temperature_2m");
            double windSpeed = current.getDouble("wind_speed_10m");
            double humidity = current.getDouble("relative_humidity_2m");
            double precipitation = current.getDouble("precipitation");
            int weatherCode = current.getInt("weather_code");

            System.out.println("\nCurrent Weather in " + city + ":");
            System.out.println("Time: " + time);
            System.out.println("Temperature: " + temperature + "°C");
            System.out.println("Humidity: " + humidity + "%");
            System.out.println("Precipitation: " + precipitation + " mm");
            System.out.println("Wind Speed: " + windSpeed + " km/h");
            System.out.println("Condition: " + interpretWeatherCode(weatherCode));

        } catch (Exception e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }

    public static String interpretWeatherCode(int code) {
        if (code == 0) return "Clear sky ☀️";
        else if (code == 1 || code == 2) return "Partly cloudy ⛅";
        else if (code == 3) return "Overcast ☁️";
        else if (code == 45 || code == 48) return "Fog 🌫️";
        else if (code >= 51 && code <= 67) return "Drizzle 🌦️";
        else if (code >= 71 && code <= 77) return "Snowfall ❄️";
        else if (code >= 80 && code <= 82) return "Rain showers 🌧️";
        else if (code >= 95 && code <= 99) return "Thunderstorm ⛈️";
        else return "Unknown weather";
    }
}
