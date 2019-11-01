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

import java.util.List;

public class PlayCommandHandler extends AbstractCommandHandler {
    private static final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
    private static final List<String> names = List.of("play", "skip");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Tell the bot to say something."; }

    private final AudioPlayerManager playerManager = AudioPlayerManagerFactory.getInstance();
    private AudioPlayer player;
    private TrackScheduler scheduler;

    public PlayCommandHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("song").nargs("*")
                .help("URL or song name");
        player = playerManager.createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
    }

    public void onMessageReceived(MessageReceivedEvent event, BotCommand botCommand) throws ArgumentParserException {
        if (botCommand.getCommandName().equals("skip")) {
            scheduler.nextTrack();
            return;
        }
        Namespace ns = this.parseArgs(event);
        String song = String.join(" ", ns.getList("song"));
        MessageChannel channel = event.getChannel();

        Guild guild = event.getGuild();
        AudioManager manager = guild.getAudioManager();
        manager.setSendingHandler(new DriveAudioSendHandler(player));
        VoiceChannel voiceChannel = guild.getVoiceChannelById(391833971488456704L);

        JSONObject file = null;
        final String trackUrl;
        if (song.startsWith("http")) {
            trackUrl = song;
        } else {
            try {
                file = querySong(song);
            } catch (Exception e) {
                event.getChannel().sendMessage("Error: " + e.getMessage()).queue();
                return;
            }
            if (file != null) {
                trackUrl = String.format(
                        "https://www.googleapis.com/drive/v3/files/%s?alt=media&key=%s",
                        file.getString("id"), GOOGLE_API_KEY);
            }
            else {
                event.getChannel().sendMessage("No songs found").queue();
                return;
            }
        }
        final String songTitle = file == null ? null : file.getString("name");

        if (!manager.isConnected()) {
            manager.openAudioConnection(voiceChannel);
        }
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
                for (AudioTrack track: playlist.getTracks()) {
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
}