package bootiful.content_analyser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;


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

    @Bean
    ApplicationRunner contentApplicationRunner(Map<String, ContentProducer> producerMap) {
        return args -> {
            var start = beginningOfTheYear();
            var file = new File(new File(System.getenv("HOME"), "Desktop"), "csv");
            Assert.state(file.exists() || file.mkdirs(), "the directory [" + file.getAbsolutePath() +
                    "] does not exist");
            producerMap.forEach((producerName, producer) -> {
                var csv = new File(file, producerName + ".csv");
                var content = producer.contentFrom(start);
                var resource = new FileSystemResource(csv);

                if (csv.exists()) csv.delete();

                try {
                    write(content, resource);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Assert.state(resource.exists(), "the resource [" +
                    resource.getFile().getAbsolutePath() + "] does not exist");
            });
        };
    }


    private static void write(Collection<Content> collection, Resource resource) throws Exception {
        var contents = new ArrayList<>(collection);
        contents.sort(Comparator.comparing(Content::date));

        // write it all out
        var HEADERS = "NAME,CONTENT TYPE,EXPORT INDICATOR,DATE".split(",");
        var sw = new StringWriter();
        var csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(HEADERS)
                .build();
        try (var printer = new CSVPrinter(sw, csvFormat)) {
            for (var ts : contents) {
                var list = List.of(ts.title(), ts.type(),
                        "export for reporting", dateToString(ts.date()), ts.url().toString());
                printer.printRecord(list);
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

