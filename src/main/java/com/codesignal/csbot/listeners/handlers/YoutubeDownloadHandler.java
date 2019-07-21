package com.codesignal.csbot.listeners.handlers;

import com.codesignal.csbot.utils.Randomizer;
import com.codesignal.csbot.utils.StreamGobbler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class YoutubeDownloadHandler extends AbstractCommandHandler {
    private static final Logger log = LoggerFactory.getLogger(YoutubeDownloadHandler.class);
    private static List<String> names = List.of("youtube-dl");

    public List<String> getNames() { return names; }
    public String getShortDescription() { return "Download an online video"; }

    public YoutubeDownloadHandler() {
        ArgumentParser parser = this.buildArgParser();
        parser.addArgument("link")
                .help("Link to a page that contains a video you want to download");
    }

    public void onMessageReceived(MessageReceivedEvent event) throws ArgumentParserException {
        Namespace ns = this.parseArgs(event);

        String videoUrl = ns.getString("link");

        if (videoUrl.startsWith("<")) {
            videoUrl = videoUrl.substring(1, videoUrl.length() - 1);
        }

        String tempFolder = System.getProperty("user.home") + "/tmp/" + Randomizer.getAlphaNumericString(8);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("youtube-dl", "--max-filesize", "8000k", "-o", tempFolder + "/%(title)s.%(ext)s", videoUrl);
        File homeDir = new File(System.getProperty("user.home"));
        builder.directory(homeDir);

        try {
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), log::info);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            log.info("Exit code: " + exitCode);
        } catch (IOException | InterruptedException exception) {
            log.error(exception.getMessage());
            event.getChannel().sendMessage("Encountered an error: " + exception.getMessage()).queue();
            FileSystemUtils.deleteRecursively(new File(tempFolder));
            return;
        }

        try (Stream<Path> walk = Files.walk(Paths.get(tempFolder))) {
            List<Path> result = walk.filter(Files::isRegularFile).collect(Collectors.toList());

            if (result.isEmpty()) {
                event.getChannel().sendMessage("No videos smaller than 8MB found").queue();
                FileSystemUtils.deleteRecursively(new File(tempFolder));
            } else {
                result.forEach((Path path) -> event.getChannel()
                        .sendFile(new File(path.toString()), path.getFileName().toString()).queue(
                                message ->
                                    // Delete folder after messasge sent.
                                    FileSystemUtils.deleteRecursively(new File(tempFolder))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.getChannel().sendMessage("Download failed: " + e.getMessage()).queue();
            FileSystemUtils.deleteRecursively(new File(tempFolder));
        }
    }
}
