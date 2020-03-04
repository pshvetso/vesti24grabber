import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Util {
    private static final Logger log = Logger.getLogger("Util");

    static Connection connection;

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

    static Integer runSystemCommand(String[] cmd, String path) {
        Integer exitCode = null;
        String s;
        Process p;

        try {
            p = Runtime.getRuntime().exec(cmd, null, new File(path));

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));

            while ((s = br.readLine()) != null)
                System.out.println("line: " + s);
            p.waitFor();
            exitCode = p.exitValue();
            System.out.println("exit code: " + exitCode);

            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exitCode;
    }

    static String getJarPath() {
        //return new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        try {
            return Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent().toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
