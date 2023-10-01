package com.avrgaming.civcraft.structurevalidation;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;

import java.util.Iterator;
import java.util.Map.Entry;

public class StructureValidationPunisher implements Runnable {

    @Override
    public void run() {
        if (StructureValidator.isDisabled()) {
            return;
        }

        Iterator<Entry<BlockCoord, Structure>> structIter = CivGlobal.getStructureIterator();
        while (structIter.hasNext()) {
            Structure struct = structIter.next().getValue();
            if (struct.getCiv().isAdminCiv()) {
                continue;
            }

            if (struct.validated && struct.isActive()) {
                if (!struct.isValid()) {
                    struct.onInvalidPunish();
                }
            }
        }
    }

}
