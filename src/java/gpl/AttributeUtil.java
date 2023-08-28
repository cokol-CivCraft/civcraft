package gpl;

import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.util.NBTStaticHelper;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import net.minecraft.nbt.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
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
        private final NBTTagCompound data;
 
        private Attribute(Builder builder) {
            data = new NBTTagCompound();
            setAmount(builder.amount);
            setOperation(builder.operation);
            setAttributeType(builder.type);
            setName(builder.name);
            setUUID(builder.uuid);
        }
        
        private Attribute(NBTTagCompound data) {
            this.data = data;
        }
        
        public double getAmount() {
            return data.k("Amount");
        }
 
        public void setAmount(double amount) {
            data.a("Amount", amount);
        }
 
        public Operation getOperation() {
            return Operation.fromId(data.getInt("Operation"));
        }

        public void setOperation(@NotNull Operation operation) {
            Preconditions.checkNotNull(operation, "operation cannot be NULL.");
            data.a("Operation", operation.getId());
        }
 
        public AttributeType getAttributeType() {
            return AttributeType.fromId(data.getString("AttributeName").replace("\"", ""));
        }
 
        public void setAttributeType(@Nonnull AttributeType type) {
            Preconditions.checkNotNull(type, "type cannot be NULL.");
            data.a("AttributeName", type.minecraftId());
        }
 
        public String getName() {
            return data.getString("Name").replace("\"", "");
        }
 
        public void setName(@Nonnull String name) {
            data.a("Name", name);
        }
 
        public UUID getUUID() {
            return new UUID(data.getLong("UUIDMost"), data.getLong("UUIDLeast"));
        }
 
        public void setUUID(@Nonnull UUID id) {
            data.a("UUIDLeast", id.getLeastSignificantBits());
            data.a("UUIDMost", id.getMostSignificantBits());
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
    
    private NBTTagCompound parent;
    private NBTTagList attributes;
    
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
        if (nmsStack.v() == null) {
        	parent = new NBTTagCompound();
            nmsStack.c(parent);
        } else {
            parent = nmsStack.v();
        }
        
        // Load attribute list
        if (parent.hasKey("AttributeModifiers")) {
            attributes = parent.getList("AttributeModifiers", NBTStaticHelper.TAG_COMPOUND);
        } else {
        	/* No attributes on this item detected. */
            attributes = new NBTTagList();
            parent.a("AttributeModifiers", attributes);
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

        if (nmsStack.v() != null) {
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
    	 attributes = new NBTTagList();
    	 if (parent != null) {
             parent.a("AttributeModifiers", attributes);
    	 }
    }
    
    
    public void clear() {
        parent.a("AttributeModifiers", attributes = new NBTTagList());
    }
    
    /**
     * Retrieve the attribute at a given index.
     * @param index - the index to look up.
     * @return The attribute at that index.
     */
    public Attribute get(int index) {
        return new Attribute(attributes.get(index));
    }
 
    // We can't make Attributes itself iterable without splitting it up into separate classes
    public Iterable<Attribute> values() {
        final List<NBTBase> list = getList();

        return () -> {
            // Generics disgust me sometimes
            return Iterators.transform(
                    list.iterator(), data -> new Attribute((NBTTagCompound) data));
        };
    }
 
    @SuppressWarnings("unchecked")
    private <T> List<T> getList() {
        try {
            Field listField = NBTTagList.class.getDeclaredField("list");
            listField.setAccessible(true);
            return (List<T>) listField.get(attributes);
            
        } catch (Exception e) {
            throw new RuntimeException("Unable to access reflection.", e);
        }
    }
    
    public void addLore(String str) {
    	if (nmsStack == null) {
    		return;
    	}
    	
    	if (nmsStack.getTag() == null) {
    		nmsStack.setTag(new NBTTagCompound());
    	}
    	//this.lore.add(str);
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    
    	if (displayCompound == null) {
    		displayCompound = new NBTTagCompound();
    	}
    	
    	NBTTagList loreList = displayCompound.getList("Lore", NBTStaticHelper.TAG_STRING);
    	if (loreList == null) {
    		loreList = new NBTTagList();
    	}

        loreList.add(NBTTagString.a(str));
        displayCompound.a("Lore", loreList);
    	nmsStack.getTag().set("display", displayCompound);    	 
    }
    
    public String[] getLore() {
    	if (nmsStack == null) {
    		return null;
    	}
    	
    	if (nmsStack.getTag() == null) {
    		return null;
    	}
    	
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    	
    	if (displayCompound == null) {
    		return null;
    	}

    	NBTTagList loreList = displayCompound.getList("Lore", NBTStaticHelper.TAG_STRING);
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
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    
    	if (displayCompound == null) {
    		displayCompound = new NBTTagCompound();
    	}
    	
    	NBTTagList loreList = new NBTTagList();
    	
    	for (String str : strings) {
        	loreList.add(new NBTTagString(str));
    	}
    	
    	displayCompound.set("Lore", loreList);
    	nmsStack.getTag().set("display", displayCompound);    	 
    }
    
    public void addEnhancement(String enhancementName, String key, String value) {
    	if (enhancementName.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}
    	
    	NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");
    	
    	if (compound == null) {
    		compound = new NBTTagCompound();
    	}
    	
    	NBTTagCompound enhCompound = compound.getCompound(enhancementName);
    	if (enhCompound == null) {
    		enhCompound = new NBTTagCompound();
    	}
    	
    	if (key != null) {
    		_setEnhancementData(enhCompound, key, value);
    	}
        enhCompound.a("name", NBTTagString.a(enhancementName));

        compound.a(enhancementName, enhCompound);
    	nmsStack.getTag().set("item_enhancements", compound);
    }
    
//	not used yet...


    private void _setEnhancementData(NBTTagCompound enhCompound, String key, String value) {
    	if (key.equalsIgnoreCase("name")) {
    		throw new IllegalArgumentException();
    	}

        enhCompound.a(key, NBTTagString.a(value));
    }
    

	public void setEnhancementData(String enhancementName, String key, String value) {
		addEnhancement(enhancementName, key, value);
	
	}
    
	public String getEnhancementData(String enhName, String key) {
		if (!hasEnhancement(enhName)) {
			return null;
		}
    	
		NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");
		NBTTagCompound enhCompound = compound.getCompound(enhName);
		
		if (!enhCompound.hasKey(key)) {
			return null;
		}
				
		return enhCompound.getString(key);
	}
	
	public LinkedList<LoreEnhancement> getEnhancements() {
        LinkedList<LoreEnhancement> returnList = new LinkedList<>();

        if (!hasEnhancements()) {
            return returnList;
        }

        NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");

        for (String keyObj : compound.c()) {
            if (keyObj == null) {
                continue;
            }

            Object obj = compound.get(keyObj);

            if (obj instanceof NBTTagCompound enhCompound) {
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
    	NBTTagCompound compound = nmsStack.getTag().getCompound("item_enhancements");
    	if (compound == null) {
    		return false;
    	}
    	
    	return compound.hasKey(enhName);
	}
    
	public boolean hasEnhancements() {
		if (nmsStack == null) {
			return false;
		}
		
		if (nmsStack.getTag() == null) {
			return false;
		}
		
		return nmsStack.getTag().hasKey("item_enhancements");
	}
    
    public void setCivCraftProperty(String key, String value) {
    	
    	if (nmsStack == null) {
    		return;
    	}
    	
    	if (nmsStack.getTag() == null) {
    		nmsStack.setTag(new NBTTagCompound());
    	}
    	
    	NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
    	
    	if (civcraftCompound == null) {
    		civcraftCompound = new NBTTagCompound();
    	}

        civcraftCompound.a(key, NBTTagString.a(value));
    	nmsStack.getTag().set("civcraft", civcraftCompound);
    }
    
    public String getCivCraftProperty(String key) {
    	if (nmsStack == null) {
    		return null;
    	}
    	NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
    	
    	if (civcraftCompound == null) {
    		return null;
    	}
    	
    	NBTTagString strTag = (NBTTagString) civcraftCompound.get(key);
    	if (strTag == null) {
    		return null;
    	}
    	
    	return strTag.toString().replace("\"", "");
    }

	public void removeCivCraftProperty(String string) {
		if (nmsStack == null) {
    		return;
    	}
    
		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
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
    		nmsStack.setTag(new NBTTagCompound());
    	}
		
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
    		displayCompound = new NBTTagCompound();
    	}

        displayCompound.a("Name", NBTTagString.a(ChatColor.RESET + name));
    	nmsStack.getTag().set("display", displayCompound);    	 
	}
	
	public String getName() {
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
    		displayCompound = new NBTTagCompound();
    	}

        String name = displayCompound.getString("Name");
		name = name.replace("\"", "");
		return name;
	}


	public void setColor(Long long1) {
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    	
		if (displayCompound == null) {
    		displayCompound = new NBTTagCompound();
    	}
				
		displayCompound.set("color", new NBTTagInt(long1.intValue()));
    	nmsStack.getTag().set("display", displayCompound); 
	}
	
	public void setSkullOwner(String string) {
		if (nmsStack == null) {
			return;
		}
		
		NBTTagCompound skullCompound = nmsStack.getTag().getCompound("SkullOwner");
		if (skullCompound == null) {
			skullCompound = new NBTTagCompound();
		}
		
		skullCompound.set("Name", new NBTTagString(string));
		nmsStack.getTag().set("SkullOwner", skullCompound);		
	}
	
	public void setHideFlag(int flags) {
		if (nmsStack == null) {
			return;
		}
		
		nmsStack.getTag().setInt("HideFlags", flags);
	}
	
	public int getColor() {
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
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
		
    	NBTTagCompound displayCompound = nmsStack.getTag().getCompound("display");
    	if (displayCompound == null) {
    		return false;
    	}
    	
    	return displayCompound.hasKey("color");
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
    
		NBTTagCompound civcraftCompound = nmsStack.getTag().getCompound("civcraft");
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
		
        return nmsStack.getTag().hasKey("civ_enhancements");
	}

	public void addLore(String[] lore) {
		for (String str : lore) {
			addLore(str);
		}
	}


}
