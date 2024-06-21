package bootiful.content_analyser;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ContentProducer {

    Collection<Content> contentFrom(Instant instant);
}
