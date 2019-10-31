package com.codesignal.csbot.listeners.handlers.music;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

public class AudioPlayerManagerFactory {
    private static DefaultAudioPlayerManager manager;

    public static DefaultAudioPlayerManager getInstance() {
         if (manager == null) {
             manager = new DefaultAudioPlayerManager();
             manager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
             manager.getConfiguration().setOpusEncodingQuality(10);
             AudioSourceManagers.registerRemoteSources(manager);
         }
         return manager;
    }
}
