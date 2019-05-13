package enu.commands;


import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
public class PingCommand implements Command {


    private final String HELP = "USAGE:  !refresh";

    @Override
    public boolean called(String[] args, MessageReceivedEvent event) {
        return true;
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.TEXT)) {
            event.getTextChannel().sendMessage("pong " + event.getAuthor().getName()).queue();
        }
    }





    @Override
    public String help() {
        return HELP;
    }

    @Override
    public void executed(boolean success, MessageReceivedEvent event) {
        return;
    }
}
