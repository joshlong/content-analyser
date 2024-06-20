package bootiful.content_analyser;

import com.joshlong.podbean.Episode;
import com.joshlong.podbean.PodbeanClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


/**
 * todo: springsource videos
 * todo: coffeesoftware videos
 * todo: contentful blogs
 * todo: podbean podcasts
 */


@SpringBootApplication
public class ContentAnalyserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentAnalyserApplication.class, args);
    }


    private static <T> Collection<ContentWriter<T>> contents(
            Collection<T> ts,
            CsvWriter<T> csvWriter) {
        var list = new ArrayList<ContentWriter<T>>();
        for (var t : ts)
            list.add(new ContentWriter<>(t, csvWriter));
        return list;
    }


    @Bean
    ApplicationRunner podbean(Podcasts podcasts) {
        return args -> {
            var start = beginningOfTheYear();
            var content = new ArrayList<ContentWriter<?>>();

            content.addAll(contents(
                    podcasts.podcastsSince(start),
                    ts -> List.of(ts.title(), "podcast", "export for reporting", dateToString(ts.date()), ts.url().toString())
            ));

            var resource = new FileSystemResource(new File(new File(System.getenv("HOME"), "Desktop"), "out.csv"));
            write(content, resource);

            Assert.state(resource.exists(), "the resource [" +
                    resource.getFile().getAbsolutePath() + "] does not exist");


        };
    }


    private static void write(Collection<ContentWriter<?>> contentWriters, Resource resource)
            throws Exception {
        // write it all out
        var HEADERS = "NAME,CONTENT TYPE,EXPORT INDICATOR,DATE".split(",");
        var sw = new StringWriter();
        var csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .build();

        try (var printer = new CSVPrinter(sw, csvFormat)) {
            for (var t : contentWriters) {
                printer.printRecord(t.csv());
            }
        }
        try (var out = new FileOutputStream(resource.getFile())) {
            out.write(sw.toString().getBytes());
        }
    }

    private static String dateToString(Date date) {
        var sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    private static Instant beginningOfTheYear() {
        var currentYear = LocalDateTime.now().getYear();
        var beginningOfYear = LocalDateTime.of(currentYear, 1, 1, 0, 0);
        var zonedDateTime = beginningOfYear.atZone(ZoneId.systemDefault());
        return zonedDateTime.toInstant();
    }
}

interface CsvWriter<T> {
    List<String> write(T ts);
}

record ContentWriter<T>(T element, CsvWriter<T> writer)
        implements Csv {

    @Override
    public List<String> csv() {
        return writer.write(this.element());
    }
}

interface Csv {
    List<String> csv();
}

@Component
class Podcasts {


    private final PodbeanClient pc;

    Podcasts(PodbeanClient pc) {
        this.pc = pc;
    }

    Collection<Podcast> podcastsSince(Instant instant) {

        return this.pc.getAllEpisodes()
                .stream()
                .filter(episode -> validDateFor(episode).toInstant().isAfter(instant))
                .map(e -> {
                    try {
                        return new Podcast(e.getTitle(), e.getPermalinkUrl().toURL(), validDateFor(e));
                    } catch (MalformedURLException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(Collectors.toList());
    }


    private static Date validDateFor(Episode episode) {
        var ms = episode.getPublishTime().getTime() * 1000;
        return new Date(ms);
    }


}

record Content(String title, URL url, Date date) {
}

record Podcast(String title, URL url, Date date) {
}
