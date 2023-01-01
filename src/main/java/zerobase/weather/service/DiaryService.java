package zerobase.weather.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DiaryService {

  @Value("${openweathermap.key}")
  private String apiKey;

  public void createDiary(LocalDate date, String text) {

    //open weather api 날씨 data get
    String weatherData = getWeatherString();

    //json parsing

    //db insert


  }

  private String getWeatherString() {

    String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

    try {

      URL url = new URL(apiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      int responseCode = connection.getResponseCode();

      BufferedReader br;

      if (responseCode == 200) {
        br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      } else {
        br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
      }

      String inputLine;
      StringBuilder response = new StringBuilder();

      while ((inputLine = br.readLine()) != null) {
        response.append(inputLine);
      }
      br.close();

      return response.toString();

    } catch (Exception e) {
      return "falied to get response";
    }

  }



}
