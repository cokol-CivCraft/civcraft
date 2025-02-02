/*
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
package com.avrgaming.civcraft.components;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class NonMemberFeeComponent extends Component {

    private final Buildable buildable;
    private double feeRate = 0.05;

    public NonMemberFeeComponent(Buildable buildable) {
        this.buildable = buildable;
    }


    private String getKey() {
        return buildable.getDisplayName() + ":" + buildable.getUUID() + ":" + "fee";
    }

    @Override
    public void onLoad() {
        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getKey());

        if (entries.isEmpty()) {
            buildable.sessionAdd(getKey(), String.valueOf(feeRate));
            return;
        }

        feeRate = Double.parseDouble(entries.get(0).value);

    }

    @Override
    public void onSave() {

        ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getKey());

        if (entries.isEmpty()) {
            buildable.sessionAdd(getKey(), String.valueOf(feeRate));
            return;
        }
        CivGlobal.getSessionDB().update(entries.get(0).request_id, getKey(), String.valueOf(feeRate));
    }


    public double getFeeRate() {
        return feeRate;
    }


    public void setFeeRate(double feeRate) {
        this.feeRate = feeRate;
        onSave();
    }


    public Buildable getBuildable() {
        return buildable;
    }

    public String getFeeString() {
        DecimalFormat df = new DecimalFormat();
        return df.format(this.getFeeRate() * 100) + "%";
    }

}
