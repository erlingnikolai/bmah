package enu;

import enu.commands.*;
import enu.commands.CommandParser;
import enu.commands.PingCommand;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;


public class Discord implements EventListener {

    private static final enu.commands.CommandParser parser = new CommandParser();
    private static HashMap<String, Command> commands = new HashMap<>();

    private String token;

    public String getToken() {
        return token;
    }

    public Discord(String token) {
        this.token = token;
    }

    public void onEvent(Event event) {
        if (event instanceof ReadyEvent) commandInput();
        if (event instanceof MessageReceivedEvent) act((MessageReceivedEvent) event);
    }

    private void act(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf("[PM] %s: %s\n", event.getAuthor().getName(), event.getMessage().getContentRaw());
        } else if (event.getMessage().getContentRaw().startsWith("currently on server") && event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            //   Main.startScanProcess(realms, event);

        } else if (event.getMessage().getContentRaw().startsWith("!bot ") && !event.getMessage().getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            handleCommand(parser.parse(event.getMessage().getContentDisplay().toLowerCase().replace("!bot ", ""), event));
        }

    }


    private static void commandInput() {
        commands.put("ping".toLowerCase(), new PingCommand());
    }

    private static void handleCommand(CommandParser.CommandContainer cmd) {
        System.out.println(cmd.invoke);
        if (commands.containsKey(cmd.invoke)) {
            boolean safe = commands.get(cmd.invoke).called(cmd.args, cmd.event);
            if (safe) {
                commands.get(cmd.invoke).action(cmd.args, cmd.event);
                commands.get(cmd.invoke).executed(safe, cmd.event);
            } else {
                commands.get(cmd.invoke).executed(safe, cmd.event);
            }
        }
    }


}
