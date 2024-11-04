package org.moparforia.shared.tracks.filesystem;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import org.moparforia.shared.Tools;
import org.moparforia.shared.tracks.Track;
import org.moparforia.shared.tracks.stats.TrackStats;

public class FileSystemTrackStats implements TrackStats {
    private final int totalAttempts;
    private final int strokes;
    private final int bestPar;
    private final double bestParPercentage;
    public final int numberOfBestPar;
    private final String bestPlayer;
    private final LocalDate bestTime;
    private final int[] ratings;
    private Track track;

    public FileSystemTrackStats(
            int totalAttempts,
            int strokes,
            int bestPar,
            double percentageofBestPar,
            int numberOfBestPar,
            String bestPlayer,
            LocalDate bestTime,
            int[] ratings,
            Track track) {
        this.totalAttempts = totalAttempts;
        this.strokes = strokes;
        this.bestPar = bestPar;
        this.bestParPercentage = percentageofBestPar;
        this.numberOfBestPar = numberOfBestPar;
        this.bestPlayer = bestPlayer;
        this.bestTime = bestTime;
        this.ratings = ratings;
        this.track = track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    @Override
    public Track getTrack() {
        return track;
    }

    @Override
    public int getTotalAttempts() {
        return totalAttempts;
    }

    @Override
    public int getTotalStrokes() {
        return strokes;
    }

    @Override
    public int getBestPar() {
        return bestPar;
    }

    @Override
    public double getPercentageOfBestPar() {
        return bestParPercentage;
    }

    @Override
    public String getBestPlayer() {
        return bestPlayer;
    }

    @Override
    public LocalDate getBestParTime() {
        return bestTime;
    }

    @Override
    public int[] getRatings() {
        return ratings;
    }

    @Override
    public double getAverageRating() {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < ratings.length; i++) {
            sum += i * ratings[i];
            count += ratings[i];
        }
        return sum / count;
    }

    @Override
    public int getNumberOfRatings() {
        return Arrays.stream(ratings).sum();
    }

    @Override
    public String serialize(String splitter) {
        StringBuilder output = new StringBuilder(getTrack().serialize(splitter));
        output.append(splitter);
        output.append(Tools.izer(
                splitter,
                "I " + Tools.commaize(getTotalAttempts(), getTotalStrokes(), getBestPar(), numberOfBestPar),
                "R " + ratingsToString()));
        output.append(splitter);
        if (getBestPar() > 0) {
            output.append(Tools.izer(
                    splitter,
                    "B "
                            + Tools.commaize(
                                    getBestPlayer(),
                                    bestTime.atStartOfDay(ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli())));
        }
        return output.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSystemTrackStats)) return false;
        FileSystemTrackStats that = (FileSystemTrackStats) o;
        return getTotalAttempts() == that.getTotalAttempts()
                && strokes == that.strokes
                && getBestPar() == that.getBestPar()
                && Double.compare(that.bestParPercentage, bestParPercentage) == 0
                && numberOfBestPar == that.numberOfBestPar
                && Objects.equals(getBestPlayer(), that.getBestPlayer())
                && Objects.equals(bestTime, that.bestTime)
                && Arrays.equals(getRatings(), that.getRatings())
                && getTrack().equals(that.getTrack());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(
                getTotalAttempts(),
                strokes,
                getBestPar(),
                bestParPercentage,
                numberOfBestPar,
                getBestPlayer(),
                bestTime,
                getTrack());
        result = 31 * result + Arrays.hashCode(getRatings());
        return result;
    }

    // Old compatibility stuff

    private String ratingsToString() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < ratings.length - 1; i++) {
            buffer.append(ratings[i]).append(",");
        }
        return buffer.toString() + ratings[ratings.length - 1];
    }

    public String networkSerialize() {
        if (getBestPar() < 0) {
            return Tools.tabularize(
                    "V 1",
                    "A " + getTrack().getAuthor(),
                    "N " + getTrack().getName(),
                    "T " + getTrack().getMap(),
                    "I " + Tools.commaize(getTotalAttempts(), getTotalStrokes(), getBestPar(), numberOfBestPar),
                    "R " + ratingsToString());
        }
        return Tools.tabularize(
                "V 1",
                "A " + getTrack().getAuthor(),
                "N " + getTrack().getName(),
                "T " + getTrack().getMap(),
                "I " + Tools.commaize(getTotalAttempts(), getTotalStrokes(), getBestPar(), numberOfBestPar),
                "B "
                        + Tools.commaize(
                                getBestPlayer(),
                                bestTime.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()),
                "R " + ratingsToString());
    }
}
