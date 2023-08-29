package gpl;

import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.util.NBTStaticHelper;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
 
public class AttributeUtil {
    public enum Operation {
        ADD_NUMBER(0),
        MULTIPLY_PERCENTAGE(1),
        ADD_PERCENTAGE(2);
        private final int id;

        Operation(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public static Operation fromId(int id) {
            // Linear scan is very fast for small N
            for (Operation op : values()) {
                if (op.getId() == id) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Corrupt operation ID " + id + " detected.");
        }
    }

    // private List<String> lore = new LinkedList<String>();


    public record AttributeType(String minecraftId) {
        private static final ConcurrentMap<String, AttributeType> LOOKUP = Maps.newConcurrentMap();
        public static final AttributeType GENERIC_MAX_HEALTH = new AttributeType("generic.maxHealth").register();
        public static final AttributeType GENERIC_FOLLOW_RANGE = new AttributeType("generic.followRange").register();
        public static final AttributeType GENERIC_ATTACK_DAMAGE = new AttributeType("generic.attackDamage").register();
        public static final AttributeType GENERIC_MOVEMENT_SPEED = new AttributeType("generic.movementSpeed").register();
        public static final AttributeType GENERIC_KNOCKBACK_RESISTANCE = new AttributeType("generic.knockbackResistance").register();

        /**
         * Construct a new attribute type.
         * <p>
         * Remember to {@link #register()} the type.
         *
         * @param minecraftId - the ID of the type.
         */
        public AttributeType {
        }

        /**
         * Retrieve the associated minecraft ID.
         *
         * @return The associated ID.
         */
        @Override
        public String minecraftId() {
            return minecraftId;
        }

        /**
         * Register the type in the central registry.
         *
         * @return The registered type.
         */
        // Constructors should have no side-effects!
        public AttributeType register() {
            AttributeType old = LOOKUP.putIfAbsent(minecraftId, this);
            return Optional.ofNullable(old).orElse(this);
        }

        /**
         * Retrieve the attribute type associated with a given ID.
         *
         * @param minecraftId The ID to search for.
         * @return The attribute type, or NULL if not found.
         */
        public static AttributeType fromId(String minecraftId) {
            return LOOKUP.get(minecraftId);
        }

        /**
         * Retrieve every registered attribute type.
         *
         * @return Every type.
         */
        public static Iterable<AttributeType> values() {
            return LOOKUP.values();
        }
    }
 
    public static class Attribute {
        private final CompoundTag data;
 
        private Attribute(Builder builder) {
            data = new CompoundTag();
            setAmount(builder.amount);
            setOperation(builder.operation);
            setAttributeType(builder.type);
            setName(builder.name);
            setUUID(builder.uuid);
        }

        private Attribute(CompoundTag data) {
            this.data = data;
        }
        
        public double getAmount() {
            return data.getDouble("Amount");
        }
 
        public void setAmount(double amount) {
            data.putDouble("Amount", amount);
        }
 
        public Operation getOperation() {
            return Operation.fromId(data.getInt("Operation"));
        }

        public void setOperation(@NotNull Operation operation) {
            Preconditions.checkNotNull(operation, "operation cannot be NULL.");
            data.putInt("Operation", operation.getId());
        }
 
        public AttributeType getAttributeType() {
            return AttributeType.fromId(data.getString("AttributeName").replace("\"", ""));
        }

        public void setAttributeType(@NotNull AttributeType type) {
            Preconditions.checkNotNull(type, "type cannot be NULL.");
            data.putString("AttributeName", type.minecraftId());
        }
 
        public String getName() {
            return data.getString("Name").replace("\"", "");
        }

        public void setName(@NotNull String name) {
            data.putString("Name", name);
        }
 
        public UUID getUUID() {
            return new UUID(data.getLong("UUIDMost"), data.getLong("UUIDLeast"));
        }

        public void setUUID(@NotNull UUID id) {
            data.putLong("UUIDLeast", id.getLeastSignificantBits());
            data.putLong("UUIDMost", id.getMostSignificantBits());
        }
 
        /**
         * Construct a new attribute builder with a random UUID and default operation of adding numbers.
         * @return The attribute builder.
         */
        public static Builder newBuilder() {
            return new Builder().uuid(UUID.randomUUID()).operation(Operation.ADD_NUMBER);
        }
        
        // Makes it easier to construct an attribute
        public static class Builder {
            private double amount;
            private Operation operation = Operation.ADD_NUMBER;
            private AttributeType type;
            private String name;
            private UUID uuid;
 
            private Builder() {
                // Don't make this accessible
            }
            
            public Builder amount(double amount) {
                this.amount = amount;
                return this;
            }
            public Builder operation(Operation operation) {
                this.operation = operation;
                return this;
            }
            public Builder type(AttributeType type) {
                this.type = type;
                return this;
            }
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            public Builder uuid(UUID uuid) {
                this.uuid = uuid;
                return this;
            }
            public Attribute build() {
                return new Attribute(this);
            }
        }
    }
    
    // This may be modified
    public net.minecraft.world.item.ItemStack nmsStack;

    private CompoundTag parent;
    private ListTag attributes;
    
    public AttributeUtil(ItemStack stack) {
        // Create a CraftItemStack (under the hood)
        this.nmsStack = CraftItemStack.asNMSCopy(stack);
        
        if (this.nmsStack == null) {
        	return;
        }
        
       // if (nmsStack == null) {
        //	CivLog.error("Couldn't make NMS copyyy of:"+stack);
        	//this.nmsStack = CraftItemStack.asNMSCopy(ItemManager.createItemStack(CivData.WOOL, 1));
        //	if (this.nmsStack == null) {
        	//	return;
        	//}
      //  }
        
        // Load NBT
        if (nmsStack.getTag() == null) {
            parent = new CompoundTag();
            nmsStack.setTag(parent);
        } else {
            parent = nmsStack.getTag();
        }
        
        // Load attribute list
        if (parent.contains("AttributeModifiers")) {
            attributes = parent.getList("AttributeModifiers", NBTStaticHelper.TAG_COMPOUND);
        } else {
        	/* No attributes on this item detected. */
            attributes = new ListTag();
            parent.put("AttributeModifiers", attributes);
        }
    }
    
    /**
     * Retrieve the modified item stack.
     * @return The modified item stack.
     */
    public ItemStack getStack() {
    	if (nmsStack == null) {
            return new ItemStack(Material.WHITE_WOOL);
        }

        if (nmsStack.getTag() != null) {
            if (attributes.isEmpty()) {
    			parent.remove("AttributeModifiers");
    		}
    	}
    	
        return CraftItemStack.asCraftMirror(nmsStack);
    }
    
    /**
     * Retrieve the number of attributes.
     * @return Number of attributes.
     */
    public int size() {
        return attributes.size();
    }
    
    /**
     * Add a new attribute to the list.
     * @param attribute - the new attribute.
     */
    public void add(Attribute attribute) {
        attributes.add(attribute.data);
    }
    
    /**
     * Remove the first instance of the given attribute.
     * <p>
     * The attribute will be removed using its UUID.
     * @param attribute - the attribute to remove.
     * @return TRUE if the attribute was removed, FALSE otherwise.
     */
    public boolean remove(Attribute attribute) {
        UUID uuid = attribute.getUUID();
        
        for (Iterator<Attribute> it = values().iterator(); it.hasNext(); ) {
            if (Objects.equal(it.next().getUUID(), uuid)) {
                it.remove();
                return true;
            }
        }
        return false;
    }
    
    public void removeAll() {
        attributes = new ListTag();
    	 if (parent != null) {
             parent.put("AttributeModifiers", attributes);
    	 }
    }
    
    
    public void clear() {
        parent.put("AttributeModifiers", attributes = new ListTag());
    }

 
    // We can't make Attributes itself iterable without splitting it up into separate classes
    public Iterable<Attribute> values() {
        final List<Tag> list = getList();

        return () -> {
            // Generics disgust me sometimes
            return Iterators.transform(
                    list.iterator(), data -> new Attribute((CompoundTag) data));
        };
    }
 
    @SuppressWarnings("unchecked")
    private List<Tag> getList() {
        try {
            Field listField = ListTag.class.getDeclaredField("list");
            listField.setAccessible(true);
            return (List<Tag>) listField.get(attributes);
            
        } catch (Exception e) {
            throw new RuntimeException("Unable to access reflection.", e);
        }
    }
    
    public void addLore(String str) {
    	if (nmsStack == null) {
    		return;
    	}
    	
    	if (nmsStack.getTag() == null) {
            nmsStack.setTag(new CompoundTag());
    	}
    	//this.lore.add(str);
        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");

        ListTag loreList = displayCompound.getList("Lore", NBTStaticHelper.TAG_STRING);

        loreList.add(StringTag.valueOf(str));
        displayCompound.put("Lore", loreList);
        nmsStack.getTag().put("display", displayCompound);
    }
    
    public String[] getLore() {
    	if (nmsStack == null) {
    		return null;
    	}
    	
    	if (nmsStack.getTag() == null) {
    		return null;
    	}

        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	
    	if (displayCompound == null) {
    		return null;
    	}

        ListTag loreList = displayCompound.getList("Lore", NBTStaticHelper.TAG_STRING);
    	if (loreList == null) {
    		return null;
    	}

        if (loreList.isEmpty()) {
    		return null;
    	}
    	
    	String[] lore = new String[loreList.size()];
    	for (int i = 0; i < loreList.size(); i++) {
            lore[i] = loreList.getString(i).replace("\"", "");
        }
    	
    	return lore;
    }
    
    public void setLore(String string) {
    	String[] strings = new String[1];
    	strings[0] = string;
    	setLore(strings);
    }
    
    public void setLore(String[] strings) {
    	//this.lore.add(str);
        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    
    	if (displayCompound == null) {
            displayCompound = new CompoundTag();
    	}

        ListTag loreList = new ListTag();
    	
    	for (String str : strings) {
            loreList.add(StringTag.valueOf(str));
    	}

        displayCompound.put("Lore", loreList);
        nmsStack.getTag().put("display", displayCompound);
    }
    
    public void addEnhancement(String enhancementName, String key, String value) {
    	if (enhancementName.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}

        CompoundTag compound = nmsStack.getTag().getCompound("item_enhancements");
    	
    	if (compound == null) {
            compound = new CompoundTag();
    	}

        CompoundTag enhCompound = compound.getCompound(enhancementName);
    	if (enhCompound == null) {
            enhCompound = new CompoundTag();
    	}
    	
    	if (key != null) {
    		_setEnhancementData(enhCompound, key, value);
    	}
        enhCompound.putString("name", enhancementName);

        compound.put(enhancementName, enhCompound);
        nmsStack.getTag().put("item_enhancements", compound);
    }
    
//	not used yet...


    private void _setEnhancementData(CompoundTag enhCompound, String key, String value) {
    	if (key.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}

        enhCompound.put(key, StringTag.valueOf(value));
    }
    

	public void setEnhancementData(String enhancementName, String key, String value) {
		addEnhancement(enhancementName, key, value);
	
	}
    
	public String getEnhancementData(String enhName, String key) {
		if (!hasEnhancement(enhName)) {
			return null;
		}

        CompoundTag compound = nmsStack.getTag().getCompound("item_enhancements");
        CompoundTag enhCompound = compound.getCompound(enhName);

        if (!enhCompound.contains(key)) {
			return null;
		}
				
		return enhCompound.getString(key);
	}
	
	public LinkedList<LoreEnhancement> getEnhancements() {
        LinkedList<LoreEnhancement> returnList = new LinkedList<>();

        if (!hasEnhancements()) {
            return returnList;
        }

        CompoundTag compound = nmsStack.getTag().getCompound("item_enhancements");

        for (String keyObj : compound.getAllKeys()) {
            if (keyObj == null) {
                continue;
            }

            Object obj = compound.get(keyObj);

            if (obj instanceof CompoundTag enhCompound) {
                String name = enhCompound.getString("name").replace("\"", "");

                LoreEnhancement enh = LoreEnhancement.enhancements.get(name);
                if (enh != null) {
                    returnList.add(enh);
                }
            }
    	}
    	
    	return returnList;
	}
	
    public boolean hasEnhancement(String enhName) {
        CompoundTag compound = nmsStack.getTag().getCompound("item_enhancements");
    	if (compound == null) {
    		return false;
    	}

        return compound.contains(enhName);
	}
    
	public boolean hasEnhancements() {
		if (nmsStack == null) {
			return false;
		}
		
		if (nmsStack.getTag() == null) {
			return false;
		}

        return nmsStack.getTag().contains("item_enhancements");
	}
    
    public void setCivCraftProperty(String key, String value) {
    	
    	if (nmsStack == null) {
    		return;
    	}
    	
    	if (nmsStack.getTag() == null) {
            nmsStack.setTag(new CompoundTag());
    	}

        CompoundTag civcraftCompound = nmsStack.getTag().getCompound("civcraft");

        civcraftCompound.putString(key, value);
        nmsStack.getTag().put("civcraft", civcraftCompound);
    }
    
    public String getCivCraftProperty(String key) {
    	if (nmsStack == null) {
    		return null;
    	}
        CompoundTag civcraftCompound = nmsStack.getTag().getCompound("civcraft");
    	
    	if (civcraftCompound == null) {
    		return null;
    	}

        StringTag strTag = (StringTag) civcraftCompound.get(key);
    	if (strTag == null) {
    		return null;
    	}
    	
    	return strTag.toString().replace("\"", "");
    }

	public void removeCivCraftProperty(String string) {
		if (nmsStack == null) {
    		return;
    	}

        CompoundTag civcraftCompound = nmsStack.getTag().getCompound("civcraft");
    	if (civcraftCompound == null) {
    		return;
    	}
    	
    	civcraftCompound.remove(string);
    	
    	if (civcraftCompound.isEmpty()) {
			removeCivCraftCompound();
    	}
	}
	
	public void setName(String name) {
		if (nmsStack == null) {
    		return;
    	}
    	
    	if (nmsStack.getTag() == null) {
            nmsStack.setTag(new CompoundTag());
    	}

        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
            displayCompound = new CompoundTag();
    	}

        displayCompound.putString("Name", ChatColor.RESET + name);
        nmsStack.getTag().put("display", displayCompound);
	}
	
	public String getName() {
        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
            displayCompound = new CompoundTag();
    	}

        String name = displayCompound.getString("Name");
		name = name.replace("\"", "");
		return name;
	}


	public void setColor(Long long1) {
        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
            displayCompound = new CompoundTag();
    	}

        displayCompound.putInt("color", long1.intValue());
        nmsStack.getTag().put("display", displayCompound);
	}
	
	public void setSkullOwner(String string) {
		if (nmsStack == null) {
			return;
		}

        CompoundTag skullCompound = nmsStack.getTag().getCompound("SkullOwner");
		if (skullCompound == null) {
            skullCompound = new CompoundTag();
		}

        skullCompound.putString("Name", string);
        nmsStack.getTag().put("SkullOwner", skullCompound);
	}
	
	public void setHideFlag(int flags) {
		if (nmsStack == null) {
			return;
		}

        nmsStack.getTag().putInt("HideFlags", flags);
	}
	
	public int getColor() {
        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	if (displayCompound == null) {
    		return 0;
    	}
    	
    	return displayCompound.getInt("color");
	}
	
	public boolean hasColor() {
		if (nmsStack == null) {
			return false;
		}
		
		if (nmsStack.getTag() == null) {
			return false;
		}

        CompoundTag displayCompound = nmsStack.getTag().getCompound("display");
    	if (displayCompound == null) {
    		return false;
    	}

        return displayCompound.contains("color");
	}
	
	
	public void setLore(LinkedList<String> lore) {
		String[] strs = new String[lore.size()];
		
		for (int i = 0; i < lore.size(); i++) {
			strs[i] = lore.get(i);
		}
		
		setLore(strs);
	}

	public void removeCivCraftCompound() {
		if (nmsStack == null) {
    		return;
    	}

        CompoundTag civcraftCompound = nmsStack.getTag().getCompound("civcraft");
    	if (civcraftCompound == null) {
    		return;
    	}
    	
    	nmsStack.getTag().remove("civcraft");		
	}

	public boolean hasLegacyEnhancements() {
		if (nmsStack == null) {
			return false;
		}
		
		if (nmsStack.getTag() == null) {
			return false;
		}

        return nmsStack.getTag().contains("civ_enhancements");
	}

	public void addLore(String[] lore) {
		for (String str : lore) {
			addLore(str);
		}
	}


}
