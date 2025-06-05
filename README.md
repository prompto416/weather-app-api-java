

# make sure you are in the src directory before executing anything and also the json file and lib folder is present
# Compile (Windows)
javac -cp ".;lib\json-20230227.jar" src\WeatherData.java src\WeatherAPIClient.java src\WeatherApp.java

# Run (Windows)
java -cp ".;lib\json-20230227.jar;src" WeatherApp



# Optionally if there is any problem you can try:
java -cp "lib\*;src" WeatherApp

