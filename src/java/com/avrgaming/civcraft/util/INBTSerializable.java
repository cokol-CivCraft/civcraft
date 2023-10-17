package com.avrgaming.civcraft.util;

import net.minecraft.server.v1_12_R1.NBTTagCompound;

public interface INBTSerializable {
    void saveToNBT(NBTTagCompound nbt);

    void loadFromNBT(NBTTagCompound nbt);
}
