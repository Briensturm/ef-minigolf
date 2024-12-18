package org.moparforia.shared.tracks.filesystem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.moparforia.shared.tracks.*;
import org.moparforia.shared.tracks.util.FileSystemExtension;

class FileSystemTrackManagerTest {
    @RegisterExtension
    final FileSystemExtension extension = new FileSystemExtension("v2/");

    TrackManager manager;
    TracksLocation tracksLocation;

    @BeforeEach
    void beforeEach() {
        FileSystem fileSystem = this.extension.getFileSystem();
        tracksLocation = new TracksLocation(fileSystem, "tracks");
        manager = new FileSystemTrackManager();
    }

    /**
     * Loads modern tracks Loads Tracksets
     *
     * <p>oakpark.trackset should be ignored because it didnt contain any loaded tracks
     * birchwood.trackset should have only 2 tracks
     */
    @Test
    void testSimpleSetLoad() throws IOException, URISyntaxException, TrackLoadException {
        extension.copyAll();

        manager.load(tracksLocation);
        assertEquals(1, manager.getTrackSets().size());
        TrackSet birchwood = manager.getTrackSet("Birchwood");

        assertEquals(2, birchwood.getTracks().size());
        assertEquals("Birchwood", birchwood.getName());
        assertEquals(TrackSetDifficulty.EASY, birchwood.getDifficulty());
    }

    @Test
    void testLoad() throws IOException, URISyntaxException, TrackLoadException {
        extension.copyAll();

        manager.load(tracksLocation);
        assertEquals(17, manager.getTracks().size());
        assertEquals(1, manager.getTrackSets().size());

        assertEquals(6, manager.findAllByCategory(TrackCategory.MODERN).size());
        assertEquals(17, manager.findAllByCategory(TrackCategory.ALL).size());
        assertEquals(2, manager.findAllByCategory(TrackCategory.SHORT).size());
        assertEquals(3, manager.findAllByCategory(TrackCategory.TRADITIONAL).size());
        assertEquals(2, manager.findAllByCategory(TrackCategory.HIO).size());
        assertEquals(3, manager.findAllByCategory(TrackCategory.BASIC).size());

        assert manager.isLoaded();
    }

    @Test
    void testRandomTracksIncorrectLimit() {
        assertThrows(IllegalArgumentException.class, () -> manager.getRandomTracks(0, TrackCategory.ALL));
        assertThrows(IllegalArgumentException.class, () -> manager.getRandomTracks(-1, TrackCategory.ALL));
    }

    @Test
    void testRandomTracks() throws IOException, URISyntaxException, TrackLoadException {
        extension.copyAll();

        manager.load(tracksLocation);
        assertEquals(3, manager.getRandomTracks(3, TrackCategory.MODERN).size());
        assertEquals(6, manager.getRandomTracks(50, TrackCategory.MODERN).size());
    }

    /**
     * This means that if randomTracks is called on a category that doesn't have any tracks it will
     * return empty list
     */
    @Test
    void testRandomTracksEmpty() throws TrackLoadException {
        manager.load(tracksLocation);
        assertEquals(0, manager.getRandomTracks(50, TrackCategory.BASIC).size());
    }
}
