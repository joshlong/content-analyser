package bootiful.content_analyser;

import java.net.URL;
import java.util.Date;

public record Content(String title, URL url, Date date, String type) {
}
