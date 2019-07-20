package com.codesignal.csbot.watchers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


class UTCWatcher {
    void run(JDA discordClient) {
        // A discord channel to display UTC clock.
        long CLOCK_CHANNEL = 602184894684201000L;
        VoiceChannel channel = discordClient.getVoiceChannelById(CLOCK_CHANNEL);
        if (channel == null) return;

        String timeString = Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        channel.getManager().setName(timeString + " (UTC)").queue();
    }
}
