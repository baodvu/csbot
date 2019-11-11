package com.codesignal.csbot.listeners.handlers.music;

import com.codesignal.csbot.listeners.BotCommand;
import com.codesignal.csbot.listeners.handlers.AbstractCommandHandler;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class PlayCommandHandler extends AbstractCommandHandler {
    private static final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
    private static final Map<String, String> RADIO_LINKS = Map.of(
            "npr", "https://npr-ice.streamguys1.com/live.mp3"
    );
    private static final List<String> names = List.of("play", "skip", "stop", "pause", "seek", "playrandom");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Tell the bot to say something."; }

    private final AudioPlayerManager playerManager = AudioPlayerManagerFactory.getInstance();
    private final AudioPlayer player;
    private final TrackScheduler scheduler;

    public PlayCommandHandler() {
        player = playerManager.createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) {
        String query = botCommand.getCommandParams();
        if (query.matches("^<.*://.*>$")) {
            query = query.substring(1, query.length() - 1);
        }
        if (RADIO_LINKS.containsKey(query)) {
            query = RADIO_LINKS.get(query);
        }
        MessageChannel channel = event.getChannel();

        Guild guild = event.getGuild();
        AudioManager manager = guild.getAudioManager();
        manager.setSendingHandler(new DriveAudioSendHandler(player));
        VoiceChannel voiceChannel = guild.getVoiceChannelById(391833971488456704L);

        if (botCommand.getCommandName().equals("stop")) {
            scheduler.stopAllTracks();
            manager.closeAudioConnection();
            return;
        }
        if (botCommand.getCommandName().equals("pause")) {
            player.setPaused(!player.isPaused());
            if (player.getPlayingTrack() != null) {
                channel.sendMessage(String.format("Track is %s at %s",
                        player.isPaused() ? "paused" : "resumed",
                        formatDuration(player.getPlayingTrack().getPosition()))).queue();
            }
            return;
        }
        if (botCommand.getCommandName().equals("seek")) {
            AudioTrack track = player.getPlayingTrack();
            if (track == null) {
                channel.sendMessage("No playing track found").queue();
                return;
            }
            if (!track.isSeekable()) {
                channel.sendMessage("Track is not seekable").queue();
                return;
            }
            if (query.startsWith("-")) {
                long delta = parseDuration(query.substring(1));
                track.setPosition(Math.max(0, track.getPosition() - delta));
            } else if (query.startsWith("+")) {
                long delta = parseDuration(query.substring(1));
                track.setPosition(track.getPosition() + delta);
            } else {
                long position = parseDuration(query);
                track.setPosition(position);
            }
            channel.sendMessage("Track is at " + formatDuration(track.getPosition())).queue();
            return;
        }
        if (botCommand.getCommandName().equals("skip")) {
            if (!scheduler.nextTrack()) {
                manager.closeAudioConnection();
            }
            return;
        }

        if (!manager.isConnected()) {
            manager.openAudioConnection(voiceChannel);
        }


        List<JSONObject> files = new ArrayList<>();
        final List<String> trackUrls = new ArrayList<>();

        if (query.startsWith("http")) {
            trackUrls.add(query);
        } else {
            try {
                if (botCommand.getCommandName().equals("playrandom")) {
                    int splitIdx = query.indexOf(" ");
                    if (splitIdx == -1) {
                        channel.sendMessage("Syntax: playrandom <number of songs> <folder search terms>").queue();
                        return;
                    }
                    files.addAll(queryRandomSongs(
                            query.substring(splitIdx + 1),
                            Math.min(10, Math.max(0, Integer.parseInt(query.substring(0, splitIdx))))));
                } else {
                    files.add(querySong(query));
                }
            } catch (Exception e) {
                event.getChannel().sendMessage("Error: " + e.getMessage()).queue();
                return;
            }
            for (JSONObject file: files) {
                trackUrls.add(String.format(
                        "https://www.googleapis.com/drive/v3/files/%s?alt=media&key=%s",
                        file.getString("id"), GOOGLE_API_KEY));
            }
            if (files.isEmpty()) {
                event.getChannel().sendMessage("No songs found").queue();
                return;
            }
        }

        int i = 0;
        for (String trackUrl: trackUrls) {
            final String songTitle = i < files.size() ? files.get(i++).getString("name") : null;

            playerManager.loadItemOrdered(guild.getId(), trackUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    String title = songTitle != null ? songTitle : track.getInfo().title;
                    channel.sendMessage("Adding to queue " + title).queue();
                    scheduler.queue(track, () -> {
                        channel.sendMessage("Playing " + title).queue();
                        return null;
                    });
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    channel.sendMessage("Found " + playlist.getTracks().size() + " tracks").queue();
                    for (AudioTrack track : playlist.getTracks()) {
                        scheduler.queue(track, () -> {
                            channel.sendMessage("Playing " + track.getInfo().title).queue();
                            return null;
                        });
                    }
                }

                @Override
                public void noMatches() {
                    channel.sendMessage("Nothing found by " + trackUrl).queue();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    channel.sendMessage("Could not play: " + exception.getMessage()).queue();
                }
            });
        }
    }

    private JSONObject querySong(String title) throws UnirestException {
        JsonNode resp = Unirest
                .get(
                "https://script.google.com/macros/s/AKfycbwR5eAczqX9ZxTX7Jd0iCRj38ZPXTO3Rf0eSD-mickeVmnyphc/exec")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, " +
                        "like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                .queryString("title", title)
                .asJson().getBody();
        JSONArray files = resp.getObject().getJSONArray("files");
        return files.isEmpty() ? null : files.getJSONObject(0);
    }

    private List<JSONObject> queryRandomSongs(String folderTitle, int count) throws UnirestException {
        JsonNode resp = Unirest
                .get(
                "https://script.google.com/macros/s/AKfycbz5UIZG80cPz0naFrYJNMqIeRAKgs7dfJ5hlfOI2o-qENuO6nc/exec")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, " +
                        "like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                .queryString("title", folderTitle)
                .asJson().getBody();
        JSONArray files = resp.getObject().getJSONArray("files");
        List<JSONObject> available = new ArrayList<>();
        for (int i = 0; i < files.length(); ++i ) {
            available.add(files.getJSONObject(i));
        }
        Collections.shuffle(available);
        return available.subList(0, count);
    }

    private String formatDuration(long milliseconds) {
        return String.format("%02d:%02d", milliseconds / 1000 / 60, milliseconds / 1000 % 60);
    }

    private long parseDuration(String rawString) {
        long milliseconds = 0;
        String[] parts = rawString.split(":");
        if (parts.length > 0) {
            milliseconds += Integer.parseInt(parts[parts.length - 1]) * 1000;
        }
        if (parts.length > 1) {
            milliseconds += Integer.parseInt(parts[parts.length - 2]) * 60 * 1000;
        }
        if (parts.length > 2) {
            milliseconds += Integer.parseInt(parts[parts.length - 3]) * 60 * 60 * 1000;
        }
        return milliseconds;
    }
}