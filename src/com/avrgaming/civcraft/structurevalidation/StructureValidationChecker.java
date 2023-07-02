package com.avrgaming.civcraft.structurevalidation;

import java.util.Iterator;
import java.util.Map.Entry;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.war.War;

public class StructureValidationChecker implements Runnable {

	@Override
	public void run() {
		Iterator<Entry<BlockCoord, Structure>> structIter = CivGlobal.getStructureIterator();
		while (structIter.hasNext()) {
			Structure struct = structIter.next().getValue();
			
			if (War.isWarTime()) {
				/* Don't do any work once it's war time. */
                break;
            }

            if (!struct.isActive()) {
                continue;
            }

            if (struct.isIgnoreFloating()) {
                continue;
            }

            CivLog.warning("Doing a structure validate... " + struct.getDisplayName());
            struct.validate(null);

            synchronized (this) {
                try {
                    this.wait(12500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
	}

}
