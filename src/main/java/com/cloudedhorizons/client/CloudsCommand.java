package com.cloudedhorizons.client;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

/** Client-side {@code /clouds} command for tweaking the cloud renderer at runtime. */
public final class CloudsCommand extends CommandBase {

    private static final String USAGE = "/clouds morph [speed]";

    @Override
    public String getCommandName() {
        return "clouds";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || !"morph".equalsIgnoreCase(args[0]) || args.length > 2) {
            throw new WrongUsageException(USAGE);
        }
        if (args.length == 2) {
            // parseDouble throws a NumberInvalidException, which Minecraft reports in chat.
            CloudRenderer.setMorphSpeed((float) parseDouble(sender, args[1]));
            sender.addChatMessage(new ChatComponentText("Cloud morph speed set to " + CloudRenderer.getMorphSpeed()));
        } else {
            sender.addChatMessage(new ChatComponentText("Cloud morph speed: " + CloudRenderer.getMorphSpeed()));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "morph") : null;
    }
}
