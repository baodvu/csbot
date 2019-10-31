package com.codesignal.csbot.listeners.handlers.music;

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

import java.util.List;

public class PlayCommandHandler extends AbstractCommandHandler {
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

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        if (event.getMessage().getContentRaw().split(" ")[0].endsWith("skip")) {
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

        final String trackUrl;
        if (song.startsWith("http")) {
            trackUrl = song;
        } else {
            String songId;
            try {
                songId = querySong(song);
            } catch (Exception e) {
                event.getChannel().sendMessage("Error: " + e.getMessage()).queue();
                return;
            }
            if (songId != null) trackUrl = "https://drive.google.com/uc?id=" + songId;
            else {
                event.getChannel().sendMessage("No songs found").queue();
                return;
            }
        }

        if (!manager.isConnected()) {
            manager.openAudioConnection(voiceChannel);
        }
        playerManager.loadItemOrdered(guild.getId(), trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();
                scheduler.queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage(
                        "Adding to queue " + firstTrack.getInfo().title
                                + " (first track of playlist " + playlist.getName() + ")").queue();
                scheduler.queue(firstTrack);
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

    private String querySong(String title) throws UnirestException {
        JsonNode resp = Unirest.get(
                "https://script.google.com/macros/s/AKfycbwR5eAczqX9ZxTX7Jd0iCRj38ZPXTO3Rf0eSD-mickeVmnyphc/exec")
                .field("title", title).asJson().getBody();
        JSONArray files = resp.getObject().getJSONArray("files");
        return files.isEmpty() ? null : files.getJSONObject(0).getString("id");
    }
}