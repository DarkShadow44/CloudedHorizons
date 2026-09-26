package com.cloudedhorizons.client;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;

/** Client-side {@code /clouds} command for tweaking the cloud renderer at runtime. */
public final class CloudsCommand extends CommandBase {

    private static final String USAGE = "/clouds <morph [speed] | height [y]>";

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
        if (args.length == 0 || args.length > 2) {
            throw new WrongUsageException(USAGE);
        }
        // parseDouble throws a NumberInvalidException, which Minecraft reports in chat.
        if ("morph".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                CloudRenderer.setMorphSpeed(parseDouble(sender, args[1]));
                reply(sender, "Cloud morph speed set to " + CloudRenderer.getMorphSpeed());
            } else {
                reply(sender, "Cloud morph speed: " + CloudRenderer.getMorphSpeed());
            }
        } else if ("height".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                CloudRenderer.setBaseY(parseDouble(sender, args[1]));
                reply(sender, "Cloud height set to " + CloudRenderer.getBaseY());
            } else {
                reply(sender, "Cloud height: " + CloudRenderer.getBaseY());
            }
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    private static void reply(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "morph", "height") : null;
    }
}
