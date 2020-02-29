import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public class Util {
    static void disconnect(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static void runSystemCommand(String[] cmd, String path) {
        String s;
        Process p;

        try {
            p = Runtime.getRuntime().exec(cmd, null, new File(path));

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));

            while ((s = br.readLine()) != null)
                System.out.println("line: " + s);
            p.waitFor();
            System.out.println("exit: " + p.exitValue());

            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
