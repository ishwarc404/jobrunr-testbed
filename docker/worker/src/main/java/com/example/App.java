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

import com.mysql.cj.jdbc.MysqlDataSource;

public class App {

    public static void main(String[] args) {

        MysqlDataSource dataSource = new MysqlDataSource();
 
        String jdbcUrl  = System.getenv().getOrDefault("JOBRUNR_DB_URL",  "");
        String user     = System.getenv().getOrDefault("JOBRUNR_DB_USER", "root");
        String password = System.getenv().getOrDefault("JOBRUNR_DB_PASS", "password");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUser(user);
        dataSource.setPassword(password);


        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS jobrunr");
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to create the database", e);
        }
        dataSource.setUrl(jdbcUrl+"jobrunr?rewriteBatchedStatements=true");
        StorageProvider storageProvider = new MySqlStorageProvider(dataSource);
        
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer()
                .initialize();

    }
}

