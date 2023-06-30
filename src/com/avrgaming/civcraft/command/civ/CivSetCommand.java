/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.command.civ;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.util.DecimalHelper;

public class CivSetCommand extends CommandBase {

	@Override
	public void init() {
		command = "/civ set";
		displayName = CivSettings.localize.localizedString("cmd_civ_set_Name");
		
		commands.put("taxes", CivSettings.localize.localizedString("cmd_civ_set_taxesDesc"));
		commands.put("science", CivSettings.localize.localizedString("cmd_civ_set_scienceDesc"));
		commands.put("color", CivSettings.localize.localizedString("cmd_civ_set_colorDesc"));

	}

	
	private double vaildatePercentage(String arg) throws CivException {
		try { 
			
			arg = arg.replace("%", "");
			
			Integer amount = Integer.valueOf(arg);
			
			if (amount < 0 || amount > 100) {
				throw new CivException (CivSettings.localize.localizedString("cmd_civ_set_invalidPercent")+" 0% & 100%");
			}
			
			return ((double)amount/100);
			
		} catch (NumberFormatException e) {
			throw new CivException(arg+" "+CivSettings.localize.localizedString("cmd_enterNumerError"));
		}
				
	}
	
	public void taxes_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		if (args.length < 2) {
			CivMessage.send(sender, "Current income percentage:"+" "+civ.getIncomeTaxRateString());
			return;
		}
		
		double newPercentage = vaildatePercentage(args[1]);
		
		if (newPercentage > civ.getGovernment().maximum_tax_rate) {
			throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_overmax")+"("+
					DecimalHelper.formatPercentage(civ.getGovernment().maximum_tax_rate)+")");
		}
		
		civ.setIncomeTaxRate(newPercentage);
		
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("cmd_civ_set_taxesSuccess")+" "+args[1]+" "+"%");
	}
	
	public void science_cmd() throws CivException {
	Civilization civ = getSenderCiv();
		
		if (args.length < 2) {
			CivMessage.send(sender, CivSettings.localize.localizedString("cmd_civ_set_currentScience")+" "+civ.getSciencePercentage());
			return;
		}
		
		double newPercentage = vaildatePercentage(args[1]);
		
		civ.setSciencePercentage(newPercentage);		
		civ.save();
		
		CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_scienceSuccess",args[1]));	
	}
	

	public void color_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		if (args.length < 2) {
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_currentColor",Integer.toHexString(civ.getColor())));
			return;
		}
	
		try {		
			
			int color = Integer.parseInt(args[1], 16);
			if (color > Civilization.HEX_COLOR_MAX) {
				throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_colorInvalid"));
			}
			if (color == 0xFF0000) {
				throw new CivException(CivSettings.localize.localizedString("cmd_civ_set_colorIsBorder"));
			}
			
			civ.setColor(color);
			civ.save();
			CivMessage.sendSuccess(sender, CivSettings.localize.localizedString("var_cmd_civ_set_colorSuccess",Integer.toHexString(color)));
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" "+CivSettings.localize.localizedString("cmd_civ_set_colorInvalid"));
		}
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		this.validLeaderAdvisor();
	}

}
