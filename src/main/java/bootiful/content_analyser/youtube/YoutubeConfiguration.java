package bootiful.content_analyser.youtube;

import bootiful.content_analyser.Content;
import bootiful.content_analyser.ContentAnalyserProperties;
import bootiful.content_analyser.ContentProducer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Josh Long
 */

@Configuration
@RegisterReflectionForBinding({
        YoutubeResponse.class,
        ChannelResponse.class,
        VideoStatsResponse.class,
        VideoInfo.class,
        PlaylistItemsResponse.class
})
class YoutubeConfiguration {

    @Bean
    YoutubeClient youTubeClient(RestTemplate restTemplate, ContentAnalyserProperties properties) {
        return new YoutubeClient(restTemplate, properties.youtubeApiKey());
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    PlaylistContentProducer playlistContentProducer(YoutubeClient youtubeClient) {
        var springtipsPlaylist = "PLgGXSWYM2FpPw8rV0tZoMiJYSCiLhPnOc";
        return new PlaylistContentProducer(youtubeClient, springtipsPlaylist);
    }

    @Bean
    ChannelContentProducer channelContentProducer(YoutubeClient youtubeClient) {
        //var springsourcedev = "UC7yfnfvEUlXUIfm8rGLwZdA";
        var coffeesoftware = "UCjcceQmjS4DKBW_J_1UANow";
        return new ChannelContentProducer(youtubeClient, coffeesoftware);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record VideoInfo(String title,
                 String description,
                 Instant publishedAt,
                 String viewCount,
                 String videoId) {

}

@JsonIgnoreProperties(ignoreUnknown = true)
record VideoStatsResponse(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(Statistics statistics) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Statistics(String viewCount) {
        }
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeResponse(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(Id id, Snippet snippet) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Id(String videoId) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Snippet(String title, String description, String publishedAt) {
        }
    }
}





@JsonIgnoreProperties(ignoreUnknown = true)
record ChannelResponse(List<Item> items) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String id, ContentDetails contentDetails) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record ContentDetails(RelatedPlaylists relatedPlaylists) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record RelatedPlaylists(String uploads) {
            }
        }
    }
}


@JsonIgnoreProperties(ignoreUnknown = true)
record PlaylistItemsResponse(List<Item> items, String nextPageToken) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(Snippet snippet) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Snippet(String title, String description,
                       String publishedAt,
                       ResourceId resourceId) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record ResourceId(String videoId) {
            }
        }
    }
}

class YoutubeClient {

    private static final String YOUTUBE_VIDEO_API_URL = "https://www.googleapis.com/youtube/v3/videos";

    private static final String YOUTUBE_PLAYLIST_ITEMS_API_URL = "https://www.googleapis.com/youtube/v3/playlistItems";

    private static final String YOUTUBE_API_URL = "https://www.googleapis.com/youtube/v3";

    private final int max = 500;

    private final RestTemplate restTemplate;
    private final String apiKey;

    YoutubeClient(RestTemplate restTemplate, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    public VideoStatsResponse getVideoStatistics(String videoId) {
        var url = UriComponentsBuilder
                .fromHttpUrl(YOUTUBE_VIDEO_API_URL)
                .queryParam("part", "statistics")
                .queryParam("id", videoId)
                .queryParam("key", this.apiKey)
                .toUriString();

        return restTemplate.getForObject(url, VideoStatsResponse.class);
    }

    public PlaylistItemsResponse getPlaylistItems(String playlistId) {
        var url = UriComponentsBuilder
                .fromHttpUrl(YOUTUBE_PLAYLIST_ITEMS_API_URL)
                .queryParam("part", "snippet")
                .queryParam("playlistId", playlistId)
                .queryParam("key", this.apiKey)
                .queryParam("maxResults", this.max)
                .toUriString();

        return restTemplate.getForObject(url, PlaylistItemsResponse.class);
    }
//

    public ChannelResponse getChannelById(String channelId) {
        var url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL + "/channels")
                .queryParam("part", "contentDetails")
                .queryParam("id", channelId)
                .queryParam("key", this.apiKey)
                .toUriString();

        return  this.restTemplate.getForObject(url, ChannelResponse.class);
    }

    public PlaylistItemsResponse getPlaylistItems(String playlistId, String pageToken) {
        var url = UriComponentsBuilder.fromHttpUrl(YOUTUBE_API_URL + "/playlistItems")
                .queryParam("part", "snippet")
                .queryParam("playlistId", playlistId)
                .queryParam("key", this.apiKey)
                .queryParam("maxResults", 50)
                .queryParam("pageToken", pageToken)
                .toUriString();

        return this.restTemplate.getForObject(url, PlaylistItemsResponse.class);
    }

    public List<VideoInfo> getVideosByChannelId(String channelId) {
        var channelResponse = this.getChannelById(channelId);
        if (channelResponse == null || channelResponse.items().isEmpty()) {
            throw new RuntimeException("Channel not found.");
        }
        var uploadsPlaylistId = channelResponse.items().getFirst()
                .contentDetails().relatedPlaylists().uploads();
        return this.getVideosFromPlaylist(uploadsPlaylistId);
    }

    public List<VideoInfo> getVideosFromPlaylist(String playlistId) {
        var videos = new ArrayList<VideoInfo>();
        var nextPageToken = (String) null;
        do {
            var playlistItemsResponse = this.getPlaylistItems(playlistId, nextPageToken);
            nextPageToken = playlistItemsResponse.nextPageToken();
            for (var item : playlistItemsResponse.items()) {
                var videoId = item.snippet().resourceId().videoId();
                var statsResponse = this.getVideoStatistics(videoId);
                var viewCount = statsResponse.items().getFirst().statistics().viewCount();
                videos.add(new VideoInfo(item.snippet().title(), item.snippet().description(),
                        Instant.parse(item.snippet().publishedAt()), viewCount, videoId));
            }
        }//
        while (nextPageToken != null);
        return videos;
    }

}


class ChannelContentProducer implements ContentProducer {

    private final YoutubeClient youtubeClient;

    private final String channelId;

    ChannelContentProducer(YoutubeClient client, String channelId) {
        this.youtubeClient = client;
        this.channelId = channelId;
        Assert.notNull(this.channelId, "the channel name must not be null");
        Assert.notNull(this.youtubeClient, "the youtubeClient must not be null");
    }

    @Override
    public Collection<Content> contentFrom(Instant instant) {

        var list = this.youtubeClient
                .getVideosByChannelId(this.channelId)
                .stream()
                .toList();

        return list
                .stream()
                .map(vi -> new Content(vi.title(),
                        youtubeVideoUrlFor(vi.videoId()),
                        new Date(vi.publishedAt().toEpochMilli()),
                        "video",
                        Integer.parseInt(vi.viewCount())
                ))
                .toList();
    }

    private static URL youtubeVideoUrlFor(String videoId) {
        try {
            return new URI("https://www.youtube.com/watch?v=" + videoId).toURL();
        } //
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


class PlaylistContentProducer implements ContentProducer {

    private final YoutubeClient youtubeClient;
    private final String playlistId;

    PlaylistContentProducer(YoutubeClient youtubeClient, String playlistId) {
        this.youtubeClient = youtubeClient;
        this.playlistId = playlistId;
    }

    @Override
    public Collection<Content> contentFrom(Instant instant) {
        var playlistItems = this.youtubeClient
                .getPlaylistItems(this.playlistId);
        return playlistItems
                .items()
                .stream()
                .map(item -> playlistItemToContent(this.youtubeClient, item))
                .toList();
    }

    private static Content playlistItemToContent(YoutubeClient youtubeClient, PlaylistItemsResponse.Item item) {
        var snippet = item.snippet();
        var videoId = item.snippet().resourceId().videoId();
        return new Content(
                item.snippet().title(),
                youtubeVideoUrlFor(videoId),
                new Date(Instant.parse(snippet.publishedAt()).toEpochMilli()),
                "video",
                defaultViewCountFor(youtubeClient.getVideoStatistics(videoId))
        );
    }

    private static int defaultViewCountFor(VideoStatsResponse vsri) {
        var vsr = vsri.items();
        if (vsr != null && !vsr.isEmpty())
            return Integer.parseInt(vsr.getFirst().statistics().viewCount());
        return 0;
    }


    private static URL youtubeVideoUrlFor(String videoId) {
        try {
            return new URI("https://www.youtube.com/watch?v=" + videoId).toURL();
        } //
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

