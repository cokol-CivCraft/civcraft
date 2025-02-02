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
package com.avrgaming.civcraft.object;

import com.avrgaming.civcraft.exception.InvalidNameException;

import java.util.UUID;


public class NamedObject {
    public static final UUID NULL_UUID = new UUID(0, 0);

    /* Unique Id of named object. */
    private UUID uuid = NULL_UUID;

    /* Display name of the object. */
    private String name;

    public void setName(String newname) throws InvalidNameException {
        validateName(newname);
        this.name = newname;
    }

    public String getName() {
        return this.name;
    }

    private void validateName(String name) throws InvalidNameException {
        if (name == null) {
            throw new InvalidNameException();
        }

        switch (name.toLowerCase()) {
            case "", "null", "none", "town", "group", "civ", "resident" -> throw new InvalidNameException(name);
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
