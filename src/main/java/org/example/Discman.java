package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Discman {

    private static final Map<String, Command> commands = new HashMap<>();

    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel().block()
                .createMessage("Pong!").block());
    }

    public static void main(String[] args) {
        String path = "C:\\Users\\gabri\\Documents\\Hackathons\\discman\\tracks\\";
        final File folder = new File(path);
        FileFinder.listFilesFromFolder(folder);

        // Creates AudioPlayer instances and translates URLs to AudioTrack instances
        final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

        /*
         This is an optimization strategy that Discord4J can utilize.
         It is not important to understand
        */
        playerManager.getConfiguration()
                .setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        // Allow playerManager to parse local sources like .mp3 files
        AudioSourceManagers.registerLocalSource(playerManager);

        // Allow playerManager to parse local sources like .mp3 files
        AudioSourceManagers.registerLocalSource(playerManager);

        // Create an AudioPlayer so Discord4J can receive audio data
        final AudioPlayer player = playerManager.createPlayer();

        // We will be creating LavaPlayerAudioProvider in the next step
        AudioProvider provider = new LavaPlayerAudioProvider(player);

        commands.put("help", event -> {
            String message = """
                    Commands:
                    \t •**!help**: show commands
                    \t •**!join**: bot enters voice channel
                    \t •**!leave**: bot leaves voice channel
                    \t •**!play**: <song name>: bot plays song""";
            event.getMessage()
                    .getChannel().block()
                    .createMessage(message).block();
        });

        commands.put("join", event -> {
            final Member member = event.getMember().orElse(null);
            event.getMessage()
                    .getChannel().block()
                    .createMessage("Discman entrou no canal").block();
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null)
                        channel.join(spec -> spec.setProvider(provider)).block();
                }
            }
        });

        commands.put("leave", event -> {
            final Member member = event.getMember().orElse(null);
            if (member != null) {
                final VoiceState voiceState = member.getVoiceState().block();
                if (voiceState != null) {
                    final VoiceChannel channel = voiceState.getChannel().block();
                    if (channel != null) {
                        final VoiceConnection connection = channel.getVoiceConnection().block();
                        connection.disconnect().block();
                    }
                }
            }
        });

        final TrackScheduler scheduler = new TrackScheduler(player);
        commands.put("play", event -> {
            final String content = event.getMessage().getContent();
            final List<String> command = Arrays.asList(content.split(" "));
            List<String> files = FileFinder.finder(command.get(1).toLowerCase().replaceAll(" ", ""));
            if (files.size() == 1)
                playerManager.loadItem(path + files.get(0), scheduler);
            else if (files.size() > 1){
                StringBuilder message = new StringBuilder("Há múltiplas músicas com esse nome. Especifique melhor.\n");
                for (int i = 0; i < files.size() && i < 15; i++) {
                    message.append("\t • ").append(files.get(i)).append("\n");
                }
                event.getMessage()
                        .getChannel().block()
                        .createMessage(message.toString()).block();
            }
            else {
                String message1 = "O QUE VOCÊ FEZZZZ??? ESTÁ BOTANDO FUNK??? MÚSICAS ATUAIS????";
                String message2 = "Você vai sofrer e eu não vou ter pena. É melhor você cobrir seus ouvidos...\nyou know the rules and so do i...";
                event.getMessage()
                        .getChannel().block()
                        .createMessage(message1).block();
                Thread.sleep(1500);
                event.getMessage()
                        .getChannel().block()
                        .createMessage(message2).block();
                playerManager.loadItem(path + "RickAstley-NeverGonnaGiveYouUp.mp3", scheduler);
            }
        });

        final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build()
                .login()
                .block();

        client.getEventDispatcher().on(MessageCreateEvent.class)
                // subscribe is like block, in that it will *request* for action
                // to be done, but instead of blocking the thread, waiting for it
                // to finish, it will just execute the results asynchronously.
                .subscribe(event -> {
                    // 3.1 Message.getContent() is a String
                    final String content = event.getMessage().getContent();

                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        // We will be using ! as our "prefix" to any command in the system.
                        if (content.startsWith('!' + entry.getKey())) {
                            try {
                                entry.getValue().execute(event);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                });

        client.onDisconnect().block();
    }
}