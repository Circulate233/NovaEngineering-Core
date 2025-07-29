package github.kasuminova.novaeng.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class CommandBuilder extends CommandBase {
    public static final CommandBuilder INSTANCE = new CommandBuilder();
    public boolean isTickWork = false;
    public boolean isQuarryWork = false;

    private CommandBuilder(){

    }

    @Override
    public @NotNull String getName() {
        return "builder_log";
    }

    @Override
    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "Usage: /builder_log [tick|quarry] [true|false]";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender, String @NotNull [] args) {
        if (args.length >= 2){
            switch (args[0]){
                case "tick" -> isTickWork = args[1].equals("true");
                case "quarry" -> isQuarryWork = args[1].equals("true");
            }
        }
    }

    public @NotNull List<String> getTabCompletions(@NotNull MinecraftServer server, @NotNull ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args,
                    "tick","quarry"
            );
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args,
                    "true","false"
            );
        }
        return super.getTabCompletions(server, sender, args, targetPos);
    }

}
