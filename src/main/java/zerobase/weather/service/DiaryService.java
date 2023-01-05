package zerobase.weather.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

@Service
public class DiaryService {

  @Value("${openweathermap.key}")
  private String apiKey;

  private final DiaryRepository diaryRepository;
  private final DateWeatherRepository dateWeatherRepository;

  private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);


  public DiaryService(DiaryRepository diaryRepository,
      DateWeatherRepository dateWeatherRepository) {
    this.diaryRepository = diaryRepository;
    this.dateWeatherRepository = dateWeatherRepository;
  }

  @Transactional
  @Scheduled(cron = "0 0 1 * * *")
  public void saveWeatherDate() {
    //매일 01시
    dateWeatherRepository.save(getWeatherFromApi());
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public void createDiary(LocalDate date, String text) {

    logger.info("started to create diary");

    //날씨 data get(캐싱 개념으로 DB에 저장된 data를 가져오기)
    DateWeather dateWeather = getDateWeather(date);

    //db insert
    Diary nowDiary = new Diary();

    nowDiary.setDateWeather(dateWeather);
    nowDiary.setText(text);
    diaryRepository.save(nowDiary);
    logger.info("end to create diary");
  }

  private DateWeather getWeatherFromApi() {

    //open weather api 날씨 data get
    String weatherData = getWeatherString();

    //json parsing
    Map<String, Object> parsedWeather = parseWeather(weatherData);

    DateWeather dateWeather = new DateWeather();
    dateWeather.setDate(LocalDate.now());
    dateWeather.setWeather(parsedWeather.get("main").toString());
    dateWeather.setIcon(parsedWeather.get("icon").toString());
    dateWeather.setTemperature( (Double)parsedWeather.get("temp"));
    return dateWeather;
  }

  private DateWeather getDateWeather(LocalDate date) {

    List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
    if (dateWeatherListFromDB.size() == 0) {
      //API Call
      // 현재 날씨 가져오도록 하기
      return getWeatherFromApi();
    } else {

      return dateWeatherListFromDB.get(0);
    }
  }

  @Transactional(readOnly = true)
  public List<Diary> readDiary(LocalDate date) {

    if (date.isAfter(LocalDate.ofYearDay(3050, 1))) {
      throw new InvalidDate();
    }

    return diaryRepository.findAllByDate(date);
  }

  public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {

    return diaryRepository.findAllByDateBetween(startDate, endDate);
  }


  public void updateDiary(LocalDate date, String text) {

    Diary nowDiary = diaryRepository.getFirstByDate(date);
    nowDiary.setText(text);
    diaryRepository.save(nowDiary);
  }


  public void deleteDiary(LocalDate date) {

    diaryRepository.deleteAllByDate(date);
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


  private Map<String, Object> parseWeather(String jsonString) {

    JSONParser jsonParser = new JSONParser();
    JSONObject jsonObject;

    try {

      jsonObject = (JSONObject) jsonParser.parse(jsonString);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }

    Map<String, Object> resultMap = new HashMap<>();


    JSONObject mainData = (JSONObject) jsonObject.get("main");
    resultMap.put("temp", mainData.get("temp"));

    JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
    JSONObject weatherData = (JSONObject) weatherArray.get(0);
    resultMap.put("main", weatherData.get("main"));
    resultMap.put("icon", weatherData.get("icon"));
    return resultMap;
  }



}
