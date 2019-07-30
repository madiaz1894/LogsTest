package com.ef;

import org.apache.commons.lang3.time.DateUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Parser {

    public static final String BLOCK_COMMENT = "This ip was blocked because it registered more " +
            "than %s requests between %s and %s";

    public static void main(String[] args) {

        String startDate = new String();
        String duration = new String();
        int threshold = 0;
        for (String arg : args) {
           arg = arg.substring(2);
           switch (arg.split("=")[0]){
               case "startDate":
                   startDate = arg.split("=")[1];
                   break;
               case "duration":
                   duration = arg.split("=")[1];
                   break;
               case "threshold":
                   threshold = Integer.parseInt(arg.split("=")[1]);
                   break;
           }
        }

        if (!alreadyRead()) {
            readAccessLog();
        }
        System.out.println("Access.log already read");
        try {
            printAndSaveBlockedIps(startDate, duration, threshold);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void printAndSaveBlockedIps(String startDate, String duration, int threshold) throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss");
        String endDate;
        if (duration.equalsIgnoreCase("hourly")){
            endDate = formatter.format(DateUtils.addHours(formatter.parse(startDate),1));
        } else if ((duration.equalsIgnoreCase("daily"))) {
            endDate = formatter.format(DateUtils.addDays(formatter.parse(startDate),1));
        } else{
            throw new Exception("Duration must be hourly or daily");
        }

        try {
            Connection conn = ConnectionSingleton.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT ip " +
                    "FROM log_records " +
                    "WHERE LOGDATE BETWEEN UNIX_TIMESTAMP(?) AND UNIX_TIMESTAMP(?) " +
                    "GROUP BY ip " +
                    "Having COUNT(*)>= ?");
            ps.setString(1, startDate);
            ps.setString(2, endDate);
            ps.setLong(3, threshold);

            List<String> blockedIps = new ArrayList<>();
            ResultSet result = ps.executeQuery();
            while(result.next()){
                blockedIps.add(result.getString("ip"));
            }

            //Save ips
            blockedIps.forEach(ip -> {
                System.out.println(ip);
                try {
                    saveBlockedIp(ip, startDate, endDate, threshold);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void saveBlockedIp( String ip, String startDate, String endDate, int threshold) throws SQLException {
        Connection conn= ConnectionSingleton.getInstance().getConnection();
        String blockMessage = String.format(BLOCK_COMMENT, threshold, startDate, endDate);
        PreparedStatement ps = conn.prepareStatement("INSERT INTO blocked_ips(IP, COMMENT) VALUES (?, ?)");
        ps.setString(1, ip);
        ps.setString(2, blockMessage);
        try{
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) { //if the record is already in the table, the comment will be updated
            PreparedStatement updatePs = conn.prepareStatement("update blocked_ips set COMMENT = ? where IP = ?");
            updatePs.setString(1, blockMessage);
            updatePs.setString(2, ip);
        }

    }

    private static boolean alreadyRead() {
        boolean alreadyRead = false;
        try {
            Connection conn = ConnectionSingleton.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM log_records");
            ResultSet result = ps.executeQuery();
            result.next();
            alreadyRead = result.getInt(1) > 0;
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alreadyRead;
    }

    private static void readAccessLog() {
        try {
            Path path = Paths.get("access.log");
            List<String> contents = Files.readAllLines(path);
            Connection conn = ConnectionSingleton.getInstance().getConnection();
            AtomicInteger numberOfRecords = new AtomicInteger(0);
            contents.forEach(log -> {
                String[] logArray = log.split("\\|");
                try {
                    System.out.println("Storing register number :" + numberOfRecords.incrementAndGet());
                    insertLog(conn, logArray[0], logArray[1],logArray[2], logArray[3], logArray[4]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertLog(Connection connection , String logDate, String ip, String request, String response, String userAgent) throws SQLException {
        PreparedStatement sqlInsert = connection.prepareStatement("INSERT INTO log_records(LOGDATE, IP, REQUEST, RESPONSE_STATUS, USER_AGENT)" +
                "VALUES (UNIX_TIMESTAMP(?), ?, ?, ?, ?)");
        sqlInsert.setString(1, logDate);
        sqlInsert.setString(2, ip);
        sqlInsert.setString(3, request);
        sqlInsert.setString(4, response);
        sqlInsert.setString(5, userAgent);
        sqlInsert.executeUpdate();
    }


}
