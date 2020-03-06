import org.jsoup.Jsoup;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Util {
    private static final Logger log = Logger.getLogger("Util");
    private static final String LOG_FILENAME = "log.txt";
    private static FileHandler fh;

    static Connection connection;

    static {
        try {
            fh = new FileHandler(String.format("%s/%s", getInstallPath(), LOG_FILENAME));
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        addFileHandlerToLog(log);
    }

    static void addFileHandlerToLog(Logger logger) {
        try {
            // This block configure the logger with handler and formatter
            logger.addHandler(fh);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    static void connect() {
        String url = "jdbc:mysql://localhost:3306/russia24-tv?useUnicode=true&characterEncoding=utf-8&useSSL=false";
        String username = "root";
        String password = "";

        //log.setLevel(Level.ALL);
        log.info("Connecting database...");

        try {
            connection = DriverManager.getConnection(url, username, password);
            log.info("Database connected!");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }

    }

    static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        connection = null;
    }

    static Integer runSystemCommand(String[] cmd, String path, String logFilename) throws IOException, InterruptedException {
        int exitCode;
        String s;
        Process p;
        StringBuilder logText = new StringBuilder();

        p = Runtime.getRuntime().exec(cmd, null, new File(path));
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));

        while ((s = br.readLine()) != null) {
            System.out.println(s);
            logText.append(s).append("\r\n");
        }

        p.waitFor();
        exitCode = p.exitValue();
        System.out.println("Exit code: " + exitCode);
        logText.append("Exit code: ").append(exitCode);

        p.destroy();

        if(exitCode != 0) {
            BufferedWriter logger = new BufferedWriter(new FileWriter(logFilename, false));
            logger.append(logText);
            logger.close();
        }

        return exitCode;
    }

    static void scheduleTask(Runnable command, int hour, int minute, int second) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        ZonedDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(second);
        if(now.compareTo(nextRun) > 0)
            nextRun = nextRun.plusDays(1);

        Duration duration = Duration.between(now, nextRun);
        long initalDelay = duration.getSeconds();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(command,
                initalDelay,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }

    static String getInstallPath() {
        return "c:/projects/vesti24grabber";

        //return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        /*try {
            return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;*/
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }
}
