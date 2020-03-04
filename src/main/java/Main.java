import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    static final String SOURCE_VIDEO_FILENAME = "stream.mp4";
    static final String ENCODED_VIDEO_FILENAME = "encoded.mp4";

    static final String THUMB_FILENAME = "thumb.jpg";

    static final String USER_AGENT = "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.7";

    /*public static void main(String[] args) throws IOException, URISyntaxException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        YoutubeManager manager = new YoutubeManager();
        manager.deleteCopyrighted();
    }*/

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

        Util.connect();

        ParserThread parser = new ParserThread();
        //parser.VIDEO_ID_SEQ_START = Integer.parseInt(args[0]);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(parser, 0, 30, TimeUnit.MINUTES);

        // Для авторизации на аккаунте
        //YoutubeManager manager = new YoutubeManager();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Inside Add Shutdown Hook");
            executor.shutdownNow();
            Util.disconnect();
        }));
    }
}
