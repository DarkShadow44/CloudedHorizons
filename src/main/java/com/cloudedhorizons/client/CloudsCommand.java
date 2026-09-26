package com.cloudedhorizons.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;

/** Client-side {@code /clouds} command for tweaking the cloud renderer at runtime. */
public final class CloudsCommand extends CommandBase {

    private static final String USAGE = "/clouds <morph [speed] | height [y] | flyspeed [speed]>";

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
        } else if ("flyspeed".equalsIgnoreCase(args[0])) {
            // Dev helper: sets the local player's creative fly speed (vanilla default 0.05).
            PlayerCapabilities capabilities = Minecraft.getMinecraft().thePlayer.capabilities;
            if (args.length == 2) {
                float speed = (float) parseDouble(sender, args[1]);
                capabilities.setFlySpeed(speed);
                boolean saved = setServerFlySpeed(speed);
                reply(sender, "Fly speed set to " + speed + (saved ? "" : " (client only, not saved)"));
            } else {
                reply(sender, "Fly speed: " + capabilities.getFlySpeed());
            }
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    /**
     * In singleplayer, also sets the fly speed on the integrated server's player, which is what the world saves
     * (the player's abilities in level.dat). The server then keeps it across rejoins and gamemode changes.
     * Returns false on a remote server, where only the client copy can be changed.
     */
    private static boolean setServerFlySpeed(float speed) {
        Minecraft mc = Minecraft.getMinecraft();
        IntegratedServer server = mc.getIntegratedServer();
        if (server == null) {
            return false;
        }
        EntityPlayerMP player = server.getConfigurationManager().func_152612_a(mc.thePlayer.getCommandSenderName());
        if (player == null) {
            return false;
        }
        player.capabilities.setFlySpeed(speed);
        return true;
    }

    private static void reply(ICommandSender sender, String message) {
        sender.addChatMessage(new ChatComponentText(message));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "morph", "height", "flyspeed") : null;
    }
}
