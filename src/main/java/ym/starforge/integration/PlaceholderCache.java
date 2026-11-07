package ym.starforge.integration;

import java.util.UUID;

/** No PAPI linkage in the plugin lifecycle or equipment synchronizer. */
public interface PlaceholderCache extends AutoCloseable {
    void update(UUID id,int level,int failures);
    void remove(UUID id);
    void close();
}
