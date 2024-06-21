package bootiful.content_analyser.podcasts;

import bootiful.content_analyser.Content;
import bootiful.content_analyser.ContentProducer;
import com.joshlong.podbean.Episode;
import com.joshlong.podbean.PodbeanClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Configuration
class PodcastsConfiguration {

    @Bean
    PodbeanPodcasts podbeanPodcasts(PodbeanClient pc) {
        return new PodbeanPodcasts(pc);
    }
}

class PodbeanPodcasts implements ContentProducer {

    private final PodbeanClient podbeanClient;

    PodbeanPodcasts(PodbeanClient podbeanClient) {
        this.podbeanClient = podbeanClient;
    }

    private Collection<Podcast> podcastsSince(Instant instant) {
        return this.podbeanClient
                .getAllEpisodes()
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

    @Override
    public Collection<Content> contentFrom(Instant instant) {
        return podcastsSince(instant)
                .stream()
                .map(pod -> new Content(pod.title(), pod.url(), pod.date(), "podcast"))
                .toList();
    }
}

record Podcast(String title, URL url, Date date) {
}
