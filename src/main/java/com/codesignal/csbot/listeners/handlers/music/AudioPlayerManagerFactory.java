package com.codesignal.csbot.listeners.handlers.music;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;


public class AudioPlayerManagerFactory {
    private static DefaultAudioPlayerManager manager;

    public static DefaultAudioPlayerManager getInstance() {
         if (manager == null) {
             manager = new DefaultAudioPlayerManager();
             manager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
             manager.getConfiguration().setOpusEncodingQuality(10);
             RequestConfig requestConfig = RequestConfig.custom()
                     .setConnectionRequestTimeout(2000)
                     .setConnectTimeout(2000)
                     .setSocketTimeout(10000)
                     .build();
             manager.setHttpBuilderConfigurator((HttpClientBuilder builder) -> {
                 builder.setDefaultRequestConfig(requestConfig);
             });
             AudioSourceManagers.registerRemoteSources(manager);
         }
         return manager;
    }
}
