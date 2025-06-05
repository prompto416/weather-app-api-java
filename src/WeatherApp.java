// src/WeatherApp.java

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Main Swing‐based Weather Application.
 *
 * Features:
 *  1) Uses WeatherAPIClient to fetch data from Open‑Meteo (no API key).
 *  2) Displays current weather: time, temp, humidity, precipitation, wind, condition (emoji).
 *  3) Short‑term forecast: next 3 hourly slots.
 *  4) Unit conversion: °C ↔ °F, km/h ↔ mph.
 *  5) Error handling: invalid city → JOptionPane.
 *  6) History tracking: JList on the right.
 *  7) Dynamic background image indicating sunrise/day/sunset/night for the city’s local time.
 *  8) “Reset” button clears fields and restores default background.
 *
 *  ★ Note: Place your 4 background images in WeatherApp/images/:
 *      • sunrise.jpg
 *      • day.jpg
 *      • sunset.jpg
 *      • night.jpg
 *
 *  To compile:
 *    javac -cp ".;../lib/json-20230227.jar" WeatherData.java WeatherAPIClient.java WeatherApp.java
 *
 *  To run:
 *    java  -cp ".;../lib/json-20230227.jar" WeatherApp
 */
public class WeatherApp extends JFrame {
    private final WeatherAPIClient apiClient = new WeatherAPIClient();

    // UI components
    private final JTextField cityField = new JTextField(15);
    private final JButton searchButton = new JButton("Search");
    private final JButton resetButton = new JButton("Reset");
    private final JComboBox<String> unitBox = new JComboBox<>(new String[]{"Celsius", "Fahrenheit"});

    private final JLabel lblLocation = new JLabel("City: N/A");
    private final JLabel lblTime = new JLabel("Time: N/A");
    private final JLabel lblTemperature = new JLabel("Temperature: N/A");
    private final JLabel lblHumidity = new JLabel("Humidity: N/A");
    private final JLabel lblPrecipitation = new JLabel("Precipitation: N/A");
    private final JLabel lblWind = new JLabel("Wind Speed: N/A");
    private final JLabel lblCondition = new JLabel("Condition: N/A");

    private final JTextArea forecastArea = new JTextArea(5, 20);
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);

    // For unit conversion
    private WeatherData lastData = null;
    private boolean showingCelsius = true;

    // BackgroundPanel that draws an image
    private final BackgroundPanel backgroundPanel = new BackgroundPanel();

    public WeatherApp() {
        super("Weather Information App");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);

        // The backgroundPanel will be our content pane
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        // Top panel: city input + buttons + unit selector
        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("City:"));
        topPanel.add(cityField);
        topPanel.add(searchButton);
        topPanel.add(resetButton);
        topPanel.add(new JLabel("Units:"));
        topPanel.add(unitBox);

        // Middle panel: weather info & forecast
        JPanel midPanel = new JPanel(new GridBagLayout());
        midPanel.setOpaque(false);
        midPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        // Row 0: Location
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        midPanel.add(lblLocation, c);

        // Row 1: Time
        c.gridx = 0; c.gridy = 1;
        midPanel.add(lblTime, c);

        // Row 2: Temperature
        c.gridx = 0; c.gridy = 2;
        midPanel.add(lblTemperature, c);

        // Row 3: Humidity
        c.gridx = 0; c.gridy = 3;
        midPanel.add(lblHumidity, c);

        // Row 4: Precipitation
        c.gridx = 0; c.gridy = 4;
        midPanel.add(lblPrecipitation, c);

        // Row 5: Wind Speed
        c.gridx = 0; c.gridy = 5;
        midPanel.add(lblWind, c);

        // Row 6: Condition
        c.gridx = 0; c.gridy = 6;
        midPanel.add(lblCondition, c);

        // Row 7: Forecast label + area
        c.gridx = 0; c.gridy = 7;
        midPanel.add(new JLabel("Next 3‑Hour Forecast:"), c);
        c.gridx = 1; c.gridy = 7;
        forecastArea.setEditable(false);
        forecastArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        forecastArea.setOpaque(false);
        forecastArea.setForeground(Color.BLACK);
        forecastArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        midPanel.add(new JScrollPane(forecastArea), c);

        // Right panel: search history
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.add(new JLabel("Search History:"), BorderLayout.NORTH);
        historyList.setVisibleRowCount(10);
        JScrollPane scrollHistory = new JScrollPane(historyList);
        scrollHistory.setOpaque(false);
        scrollHistory.getViewport().setOpaque(false);
        rightPanel.add(scrollHistory, BorderLayout.CENTER);

        // Assemble into the frame
        backgroundPanel.add(topPanel, BorderLayout.NORTH);
        backgroundPanel.add(midPanel, BorderLayout.CENTER);
        backgroundPanel.add(rightPanel, BorderLayout.EAST);

        // Button Listeners
        searchButton.addActionListener(this::onSearch);
        resetButton.addActionListener(e -> resetUI());
        unitBox.addActionListener(e -> onUnitChange());

        // Initial state: default background (no image)
        backgroundPanel.setBackgroundImage(null);

        setVisible(true);
    }

    /** Called when Search is clicked. */
    private void onSearch(ActionEvent e) {
        String city = cityField.getText().trim();
        if (city.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a city name.",
                "Input Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Fetch weather data
        WeatherData data = apiClient.fetchWeatherForCity(city);
        if (data == null) {
            JOptionPane.showMessageDialog(
                this,
                "Could not retrieve data for \"" + city + "\".\nMake sure the city name is valid.",
                "API Error",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        lastData = data;
        showingCelsius = true;
        unitBox.setSelectedIndex(0); // Reset to Celsius

        // Display fields:
        lblLocation.setText("City: " + data.cityName);
        lblTime.setText("Time: " + data.currentTime);

        lblTemperature.setText(String.format("Temperature: %.1f °C", data.temperatureC));
        lblHumidity.setText(String.format("Humidity: %.1f %%", data.relativeHumidityPercent));
        lblPrecipitation.setText(String.format("Precipitation: %.1f mm", data.precipitationMM));
        lblWind.setText(String.format("Wind Speed: %.1f km/h", data.windSpeedKmh));
        lblCondition.setText("Condition: " + interpretWeatherCode(data.weatherCode));

        // Forecast: next 3 hourly entries
        StringBuilder sb = new StringBuilder();
        for (WeatherData.ForecastEntry fe : data.forecastList) {
            sb.append(String.format("%s → %.1f °C%n", fe.time, fe.tempC));
        }
        forecastArea.setText(sb.toString());

        // Append to history list: "City – 2025‑06‑05T18:00"
        historyModel.addElement(data.cityName + " – " + data.currentTime);

        // Update dynamic background based on data.currentTime’s hour
        applyDynamicBackgroundForHour(data.currentTime);
    }

    /** Called when the user changes units (Celsius ↔ Fahrenheit). */
    private void onUnitChange() {
        if (lastData == null) return;

        String unit = (String) unitBox.getSelectedItem();
        if (unit.equals("Celsius") && !showingCelsius) {
            // Back to Celsius
            lblTemperature.setText(String.format("Temperature: %.1f °C", lastData.temperatureC));
            lblWind.setText(String.format("Wind Speed: %.1f km/h", lastData.windSpeedKmh));
            showingCelsius = true;
        } else if (unit.equals("Fahrenheit") && showingCelsius) {
            // Convert
            double tempF = lastData.temperatureC * 9.0 / 5.0 + 32.0;
            double windMph = lastData.windSpeedKmh * 0.621371;
            lblTemperature.setText(String.format("Temperature: %.1f °F", tempF));
            lblWind.setText(String.format("Wind Speed: %.1f mph", windMph));
            showingCelsius = false;
        }
    }

    /** Resets all UI fields and restores default background. */
    private void resetUI() {
        cityField.setText("");
        lastData = null;
        showingCelsius = true;
        unitBox.setSelectedIndex(0);

        lblLocation.setText("City: N/A");
        lblTime.setText("Time: N/A");
        lblTemperature.setText("Temperature: N/A");
        lblHumidity.setText("Humidity: N/A");
        lblPrecipitation.setText("Precipitation: N/A");
        lblWind.setText("Wind Speed: N/A");
        lblCondition.setText("Condition: N/A");

        forecastArea.setText("");
        // **We do NOT clear history**; only clear the current display.

        // Remove background image (back to default)
        backgroundPanel.setBackgroundImage(null);
        backgroundPanel.repaint();
    }

    /**
     * Based on the city's local time string (ISO "yyyy‑MM‑dd'T'HH:mm"),
     * chooses one of four backgrounds: sunrise, day, sunset, night.
     *
     * You must have placed your images in WeatherApp/images/ with exactly these names:
     *   • sunrise.jpg   (sunrise scene)
     *   • day.jpg       (daytime scene)
     *   • sunset.jpg    (sunset scene)
     *   • night.jpg     (nighttime scene)
     *
     * We parse the hour from the time string and pick:
     *   05–07 → sunrise
     *   08–17 → day
     *   18–19 → sunset
     *   else  → night
     */
    private void applyDynamicBackgroundForHour(String isoDateTime) {
        try {
            // isoDateTime looks like "2025-06-05T18:00"
            String hourStr = isoDateTime.substring(11, 13);
            int hour = Integer.parseInt(hourStr);

            String folder = "images";
            String filename;
            if (hour >= 5 && hour <= 7) {
                filename = "sunrise.jpg";
            } else if (hour >= 8 && hour <= 17) {
                filename = "day.jpg";
            } else if (hour >= 18 && hour <= 19) {
                filename = "sunset.jpg";
            } else {
                filename = "night.jpg";
            }

            File imageFile = new File(folder + File.separator + filename);
            if (!imageFile.exists()) {
                // If user hasn’t provided images yet, just leave default.
                backgroundPanel.setBackgroundImage(null);
            } else {
                BufferedImage img = ImageIO.read(imageFile);
                backgroundPanel.setBackgroundImage(img);
            }
            backgroundPanel.repaint();
        } catch (Exception ex) {
            // If parsing fails or image not found, revert to default
            backgroundPanel.setBackgroundImage(null);
            backgroundPanel.repaint();
        }
    }

    /**
     * Maps Open‑Meteo weather_code → human‑readable text + emoji.
     * (per Open‑Meteo documentation)
     */
    public static String interpretWeatherCode(int code) {
        if (code == 0) return "Clear sky ☀️";
        else if (code == 1 || code == 2) return "Partly cloudy ⛅";
        else if (code == 3) return "Overcast ☁️";
        else if (code == 45 || code == 48) return "Fog 🌫️";
        else if (code >= 51 && code <= 67) return "Drizzle 🌦️";
        else if (code >= 71 && code <= 77) return "Snowfall ❄️";
        else if (code >= 80 && code <= 82) return "Rain showers 🌧️";
        else if (code >= 95 && code <= 99) return "Thunderstorm ⛈️";
        else return "Unknown ⛅";
    }

    /** Entry point. */
    public static void main(String[] args) {
        // Use system look‑and‑feel for Swing
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(WeatherApp::new);
    }

    // ───────────────────────────────────────────────────────────────────────────
    // BackgroundPanel: a JPanel that, if given a BufferedImage, draws it stretched
    // to fill the panel. Otherwise, it uses the default background.
    // ───────────────────────────────────────────────────────────────────────────
    private static class BackgroundPanel extends JPanel {
        private BufferedImage backgroundImage = null;

        public void setBackgroundImage(BufferedImage img) {
            this.backgroundImage = img;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                // Draw the image scaled to fit this panel’s size
                g.drawImage(
                    backgroundImage,
                    0, 0,
                    getWidth(), getHeight(),
                    null
                );
            }
        }
    }
}
