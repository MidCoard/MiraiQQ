package top.focess.qq.core.commands;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import top.focess.qq.api.command.*;

import java.util.List;

public class ExecCommand extends Command {

    public ExecCommand() {
        super("exec");
    }

    @Override
    public void init() {
        this.addExecutor((sender,dataCollection,ioHandler)->{
            String command = dataCollection.get();
            List<String> args = CommandLine.splitCommand(command);
            if (args.size() == 0) {
                ioHandler.outputLang("exec-command-empty-command");
                return CommandResult.REFUSE;
            }
            if (args.get(0).equalsIgnoreCase(this.getName()) || this.getAliases().stream().anyMatch(i -> i.equalsIgnoreCase(args.get(0)))) {
                ioHandler.outputLang("exec-command-error-command");
                return CommandResult.REFUSE;
            }
            CommandLine.exec(sender, command, ioHandler);
            return CommandResult.ALLOW;
        }, CommandArgument.ofString());
    }

    @NotNull
    @Override
    public List<String> usage(CommandSender sender) {
        return Lists.newArrayList("Use: exec <command>");
    }
}
