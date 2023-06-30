package com.avrgaming.civcraft.command;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.TradeRequest;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.util.CivColor;

public class TradeCommand extends CommandBase {

	public static int TRADE_TIMEOUT = 30000;
	
	@Override
	public void init() {
		command = "/trade";
		displayName = CivSettings.localize.localizedString("cmd_trade_Name");
		sendUnknownToDefault = true;
	}

	@Override
	public void doDefaultAction() throws CivException {
		Resident resident = getNamedResident(0);
		Resident trader = getResident();
		
		if (resident.isInsideArena() || trader.isInsideArena()) {
			throw new CivException(CivSettings.localize.localizedString("cmd_trade_ArenaError"));
		}
		
		double max_trade_distance;
		try {
			max_trade_distance = CivSettings.getDouble(CivSettings.civConfig, "global.max_trade_distance");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		Player traderPlayer = CivGlobal.getPlayer(trader);
		Player residentPlayer = CivGlobal.getPlayer(resident);
		
		if (trader == resident) {
			throw new CivException(CivSettings.localize.localizedString("cmd_trade_YourselfError"));
		}
		
		if (traderPlayer.getLocation().distance(residentPlayer.getLocation()) > max_trade_distance) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_trade_tooFarError",resident.getName()));
		}
		
		if (TradeInventoryListener.tradeInventories.containsKey(TradeInventoryListener.getTradeInventoryKey(resident))) {
			throw new CivException(CivSettings.localize.localizedString("var_cmd_trade_alreadyTradingError",resident.getName()));
		}
		
		TradeRequest tradeRequest = new TradeRequest();
		tradeRequest.resident = resident;
		tradeRequest.trader = trader;
		
		CivGlobal.questionPlayer(traderPlayer, residentPlayer, 
				CivSettings.localize.localizedString("cmd_trade_popTheQuestion")+" "+traderPlayer.getName()+"?",
				TRADE_TIMEOUT, tradeRequest);
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_trade_requestSent"));
	}

	@Override
	public void showHelp() {
		CivMessage.send(sender, CivColor.LightPurple+command+" "+CivColor.Yellow+CivSettings.localize.localizedString("cmd_trade_resName")+" "+
				CivColor.LightGray+CivSettings.localize.localizedString("cmd_trade_cmdDesc"));
	}

	@Override
	public void permissionCheck() throws CivException {		
	}

}
