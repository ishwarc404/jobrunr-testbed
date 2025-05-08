package com.example;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;

import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GetWeather {

    public static void fetchSanFranciscoWeather() {
        try {
            Random rand = new Random();
            // ✅ Schedule a new recurring job with random UUID ID
            
            // if (rand.nextDouble() > 0.1) {
            //     BackgroundJob.scheduleRecurrently(randomId, "*/7 * * * *", GetWeather::fetchSanFranciscoWeather);
            //     System.out.println("Scheduled recurring job: " + randomId);
            // } else {
            //     System.out.println("Skipped scheduling for: " + randomId);
            // }


            Random _rand = new Random();
            double chance = _rand.nextDouble();

            if(chance > 0.5) {
                System.out.println("Skipping weather fetch due to random chance: " + chance);
                try {
                    Thread.sleep(3000);
                    return;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            HttpClient client = HttpClient.newHttpClient();

            // Step 1: Get forecast metadata
            HttpRequest metaRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.weather.gov/points/37.7749,-122.4194"))
                    .header("User-Agent", "jobrunr-weather-demo")
                    .build();

            HttpResponse<String> metaResponse = client.send(metaRequest, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            String forecastUrl = mapper.readTree(metaResponse.body())
                    .path("properties")
                    .path("forecast")
                    .asText();

            // Step 2: Get forecast data
            HttpRequest forecastRequest = HttpRequest.newBuilder()
                    .uri(URI.create(forecastUrl))
                    .header("User-Agent", "jobrunr-weather-demo")
                    .build();

            HttpResponse<String> forecastResponse = client.send(forecastRequest, HttpResponse.BodyHandlers.ofString());

            String shortForecast = mapper.readTree(forecastResponse.body())
                    .path("properties")
                    .path("periods")
                    .get(0)
                    .path("shortForecast")
                    .asText();

            System.out.println("San Francisco Weather: " + shortForecast + " at " + ZonedDateTime.now());

        } catch (Exception e) {
            System.err.println("Failed to fetch weather: " + e.getMessage());
        }
    }
}
