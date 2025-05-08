package com.example;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.io.FileWriter;
import java.io.IOException;

import com.mysql.cj.jdbc.MysqlDataSource;

public class App {

    public static void main(String[] args) {

        String logFilePath = "/logs/scheduled-job-ids.txt";
        MysqlDataSource dataSource = new MysqlDataSource();
 
        String jdbcUrl  = System.getenv().getOrDefault("JOBRUNR_DB_URL",  "");
        String user     = System.getenv().getOrDefault("JOBRUNR_DB_USER", "root");
        String password = System.getenv().getOrDefault("JOBRUNR_DB_PASS", "password");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);


        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DROP DATABASE IF EXISTS jobrunr");
                stmt.executeUpdate("CREATE DATABASE jobrunr");
            }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create the database", e);
        }
        dataSource.setUrl(jdbcUrl+"jobrunr?rewriteBatchedStatements=true");
        StorageProvider storageProvider = new MySqlStorageProvider(dataSource);
        
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .initialize();

        int jobCounter = 0;
        

        try (FileWriter logWriter = new FileWriter(logFilePath, true)) {
        LocalDateTime now = LocalDateTime.now().plusMinutes(2); // start 5 minutes from now
        int startHour = now.getHour();
        int startMinute = now.getMinute();
        
        for (int i = 0; i < 2; i++) { // 30 minutes → 60 jobs total (2 per minute)
            int minute = (startMinute + i) % 60;
            int hour = (startHour + (startMinute + i) / 60) % 24;
        
            String cron = String.format("%d %d * * *", minute, hour);
        
            for (int j = 0; j < 2; j++) {
                String jobId = "jobrunr-testbed-sf-weather-" + UUID.randomUUID();
                String runTime = String.format("%02d:%02d", hour, minute);
                System.out.println("Scheduling job " + jobCounter++ + " : " + jobId + " @ " + cron);
                logWriter.write(jobId + " scheduled for " + runTime + "\n");
                BackgroundJob.scheduleRecurrently(jobId, cron, GetWeather::fetchSanFranciscoWeather);
            }
        }
        
        System.out.println("✅ Scheduling recurring job DONE");

        jobCounter = 0;

        ZonedDateTime startTime = ZonedDateTime.now(ZoneId.of("UTC")).plusMinutes(2);

        for (int i = 0; i < 2; i++) { // 30 minutes → 60 jobs total
            ZonedDateTime minuteSlot = startTime.plusMinutes(i);

            for (int j = 0; j < 2; j++) {
                UUID jobId = UUID.randomUUID();
                String runTime = minuteSlot.withZoneSameInstant(ZoneId.systemDefault()).toLocalTime().toString();
                System.out.println("Scheduling job " + jobCounter++ + " : " + jobId + " @ " + minuteSlot);
                logWriter.write(jobId + " scheduled for " + runTime + "\n");
                BackgroundJob.schedule(jobId, minuteSlot, GetWeather::fetchSanFranciscoWeather);
            }
        }

        System.out.println("✅ Scheduling onetime job DONE ");

    } catch (IOException e) {
            throw new RuntimeException("Failed to write job IDs to log file", e);
        }
    }
}

