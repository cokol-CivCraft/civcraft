package com.avrgaming.civcraft.command.town;

import com.avrgaming.civcraft.command.CommandProblemException;
import com.avrgaming.civcraft.command.DispatcherCommand;
import com.avrgaming.civcraft.command.Sender;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static com.avrgaming.civcraft.command.CivCom.literal;

public class Town2Command extends DispatcherCommand {
    static final CommandDispatcher<Sender> dispatcher = new CommandDispatcher<>();
    static final LiteralArgumentBuilder<Sender> root = literal("town2");

    static {
        root.then(literal("list").executes(Town2Command::list_cmd));

        root.then(literal("claim").requires(source -> source.getResident().hasTown()).executes(Town2Command::claim_cmd));
        root.then(literal("unclaim").requires(source -> source.getResident().hasTown()).executes(Town2Command::unclaim_cmd));
        root.then(literal("members").requires(source -> source.getResident().hasTown()).executes(Town2Command::members_cmd));
        dispatcher.register(root);
    }

    private static int unclaim_cmd(CommandContext<Sender> context) {
        Town town = context.getSource().getTown();
        Player player = context.getSource().getPlayer();
        Resident resident = context.getSource().getResident();
        TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
        if (tc == null) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_plotNotOwned"));
        }

        if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }

        if (town.getTownChunks().size() <= 1) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_unclaimError"));
        }

        if (!tc.getCanUnclaim()) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_unclaim_errorTownHall"));
        }

        if (tc.getTown() != resident.getTown()) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_unclaimNotInTown"));
        }

        if (tc.perms.getOwner() != null && tc.perms.getOwner() != resident) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_unclaimOtherRes"));
        }

        try {
            TownChunk.unclaim(tc);
        } catch (CivException e) {
            context.getSource().sendMessage(ChatColor.RED + "Unexpected problem");
            e.printStackTrace();
        }

        CivMessage.sendSuccess(context.getSource(), CivSettings.localize.localizedString("var_cmd_town_unclaimSuccess", tc.getCenterString()));
        return 1;
    }

    public static int claim_cmd(CommandContext<Sender> context) {
        Player player = context.getSource().getPlayer();
        Town town = context.getSource().getTown();

        if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
            throw new CommandProblemException(CivSettings.localize.localizedString("cmd_town_claimNoPerm"));
        }

        try {
            TownChunk.claim(town, player);
        } catch (CivException e) {
            context.getSource().sendMessage(ChatColor.RED + e.getMessage());
            return 0;
        }
        return 1;
    }

    @Override
    public CommandDispatcher<Sender> getDispatcher() {
        return dispatcher;
    }

    public static int members_cmd(CommandContext<Sender> context) {
        Town town = context.getSource().getTown();

        CivMessage.sendHeading(context.getSource(), CivSettings.localize.localizedString("var_town_membersHeading", town.getName()));
        StringBuilder out = new StringBuilder();
        for (Resident res : town.getResidents()) {
            out.append(res.getName()).append(", ");
        }
        CivMessage.send(context.getSource(), out.toString());
        return 1;
    }

    public static int list_cmd(CommandContext<Sender> context) {
        StringBuilder out = new StringBuilder();

        CivMessage.sendHeading(context.getSource(), CivSettings.localize.localizedString("cmd_town_listHeading"));
        for (Town town : CivGlobal.getTowns()) {
            out.append(town.getName()).append("(").append(town.getCiv().getName()).append(")").append(", ");
        }

        CivMessage.send(context.getSource(), out.toString());
        return 1;
    }
}
