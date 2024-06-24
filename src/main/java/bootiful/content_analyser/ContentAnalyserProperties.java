package bootiful.content_analyser;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "content-analyser")
public record ContentAnalyserProperties(String youtubeApiKey) {
}
