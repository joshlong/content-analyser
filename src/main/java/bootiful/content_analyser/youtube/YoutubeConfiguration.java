package bootiful.content_analyser.youtube;

import bootiful.content_analyser.Content;
import bootiful.content_analyser.ContentProducer;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Configuration
class YoutubeConfiguration {


}
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class YouTubeSearchApplication implements CommandLineRunner {

    private final YouTubeClient youTubeClient;

    public YouTubeSearchApplication(YouTubeClient youTubeClient) {
        this.youTubeClient = youTubeClient;
    }

    public static void main(String[] args) {
        SpringApplication.run(YouTubeSearchApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            String query = args[0];
            YouTubeResponse response = youTubeClient.searchVideos(query);
            response.getItems().forEach(item -> {
                String videoId = item.getId().getVideoId();
                System.out.println("Title: " + item.getSnippet().getTitle());
                System.out.println("Description: " + item.getSnippet().getDescription());
                System.out.println("Video ID: " + videoId);

                // Fetch and display video statistics
                VideoStatsResponse statsResponse = youTubeClient.getVideoStatistics(videoId);
                if (statsResponse != null && !statsResponse.getItems().isEmpty()) {
                    String viewCount = statsResponse.getItems().get(0).getStatistics().getViewCount();
                    System.out.println("View Count: " + viewCount);
                }

                System.out.println("----");
            });
        } else {
            System.out.println("Please provide a search query.");
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoStatsResponse {
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private Statistics statistics;

        public Statistics getStatistics() {
            return statistics;
        }

        public void setStatistics(Statistics statistics) {
            this.statistics = statistics;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Statistics {
            private String viewCount;

            public String getViewCount() {
                return viewCount;
            }

            public void setViewCount(String viewCount) {
                this.viewCount = viewCount;
            }
        }
    }
}
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class YouTubeClient {
    private static final String API_KEY = "YOUR_YOUTUBE_API_KEY"; // Replace with your YouTube API key
    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEO_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final RestTemplate restTemplate;

    public YouTubeClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public YouTubeResponse searchVideos(String query) {
        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("key", API_KEY)
                .queryParam("maxResults", 5)
                .toUriString();

        return restTemplate.getForObject(url, YouTubeResponse.class);
    }

    public VideoStatsResponse getVideoStatistics(String videoId) {
        String url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_VIDEO_API_URL)
                .queryParam("part", "statistics")
                .queryParam("id", videoId)
                .queryParam("key", API_KEY)
                .toUriString();

        return restTemplate.getForObject(url, VideoStatsResponse.class);
    }
}


class YoutubeVideos implements ContentProducer {



  @Override
  public Collection<Content> contentFrom(Instant instant) {
    return List.of();
  }
}