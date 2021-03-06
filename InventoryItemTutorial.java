/**
 * Creating an Item that stores an Inventory (such as a Backpack)
 * (and also "How to Properly Override Shift-Clicking" - see Step 3)
 */
/*
I'm back with a new tutorial on how to create an Item that can store an Inventory,
such as a backpack. This time I've commented pretty thoroughly within the code itself,
so I'll let it do most of the talking.

I've included everything you'll need to get it working, so there are no prerequisites
to this tutorial, though it's best if you at least know how to set up a mod.

If you know how to make a custom Item or have experience with TileEntity Gui's, then
you probably won't have any trouble with this.

I do highly suggest reading the tutorial on Item NBT, however, as it will greatly aid
in understanding what's to come. Find it here: http://www.minecraftforge.net/wiki/Item_nbt

NOTE: If you want to open your inventory with the keyboard, check out this tutorial on key binding:
http://www.minecraftforum.net/topic/1798625-162sobiohazardouss-forge-keybinding-tutorial/

NOTES on Updating from Forge 804 to 871:
Three things you'll need to change in your GUI files:
1. I18n.func_135053_a() in now I18n.getString()
2. mc.func_110434_K() is now mc.renderEngine OR mc.getTextureManager()
3. renderEngine.func_110577_a() is now renderEngine.bindTexture()

Now on to the tutorial!
*/

/**
 * Step 1: Set up your main mod space, common proxy and client proxy
 */
/*
 * MAIN MOD DECLARATION
 */
package coolalias.inventoryitem;

@Mod(modid = "inventoryitemmod", name = "Inventory Item Tutorial", version = "1.0.0")
@NetworkMod(clientSideRequired=true, serverSideRequired=false)

public final class InventoryItemMain
{
	@Instance("inventoryitemmod")
	public static InventoryItemMain instance = new InventoryItemMain();

	@SidedProxy(clientSide = "coolalias.inventoryitem.ClientProxy", serverSide = "coolalias.inventoryitem.CommonProxy")
	public static CommonProxy proxy;

	/** This is used to keep track of GUIs that we make*/
	private static int modGuiIndex = 0;

	/** This is the starting index for all of our mod's item IDs */
	private static int modItemIndex = 7000;

	/** Set our custom inventory Gui index to the next available Gui index */
	public static final int GUI_ITEM_INV = modGuiIndex++;

	// ITEMS ETC.
	public static final Item itemstore = new ItemStore(modItemIndex++).setUnlocalizedName("item_store").setCreativeTab(CreativeTabs.tabMisc);

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
	}

	@EventHandler
	public void load(FMLInitializationEvent event)
	{
		// no renderers or entities to register, but whatever
		proxy.registerRenderers();
		// register CommonProxy as our GuiHandler
		NetworkRegistry.instance().registerGuiHandler(this, new CommonProxy());
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
	}
}
/*
 * COMMON PROXY CLASS
 */
public class CommonProxy implements IGuiHandler
{
	public void registerRenderers() {}

	@Override
	public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		// Hooray, no 'magic' numbers - we know exactly which Gui this refers to
		if (guiId == InventoryItemMain.GUI_ITEM_INV)
		{
			// Use the player's held item to create the inventory
			return new ContainerItem(player, player.inventory, new InventoryItem(player.getHeldItem()));
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		if (guiId == InventoryItemMain.GUI_ITEM_INV)
		{
			// We have to cast the new container as our custom class
			// and pass in currently held item for the inventory
			return new GuiItemInventory((ContainerItem) new ContainerItem(player, player.inventory, new InventoryItem(player.getHeldItem())));
		}
		return null;
	}
}

/*
 * CLIENT PROXY CLASS
 */
public class ClientProxy extends CommonProxy
{
	@Override
	public void registerRenderers() {}
}

/**
 * Step 2: Create a custom Inventory class
 */
/*
Create your new class and implement IInventory - it will automatically create all
the methods you need, but you need to fill them in with code. We make our constructor
take an ItemStack argument so we can get and / or set the NBT Tag Compound. This is
how we can save our inventory within an "Item" - by using its enclosing ItemStack.
 */
public class InventoryItem implements IInventory
{
	private String name = "Inventory Item";
	
	/** Provides NBT Tag Compound to reference */
	private final ItemStack invItem;

	/** Defining your inventory size this way is handy */
	public static final int INV_SIZE = 8;

	/** Inventory's size must be same as number of slots you add to the Container class */
	private ItemStack[] inventory = new ItemStack[INV_SIZE];

	/**
	 * @param itemstack - the ItemStack to which this inventory belongs
	 */
	public InventoryItem(ItemStack stack)
	{
		invItem = stack;

		// Create a new NBT Tag Compound if one doesn't already exist, or you will crash
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		// note that it's okay to use stack instead of invItem right there
		// both reference the same memory location, so whatever you change using
		// either reference will change in the other

		// Read the inventory contents from NBT
		readFromNBT(stack.getTagCompound());
	}

	@Override
	public int getSizeInventory()
	{
		return inventory.length;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int slot, int amount)
	{
		ItemStack stack = getStackInSlot(slot);
		if(stack != null)
		{
			if(stack.stackSize > amount)
			{
				stack = stack.splitStack(amount);
				// Don't forget this line or your inventory will not be saved!
				onInventoryChanged();
			}
			else
			{
				// this method also calls onInventoryChanged, so we don't need to call it again
				setInventorySlotContents(slot, null);
			}
		}
		return stack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		ItemStack stack = getStackInSlot(slot);
		setInventorySlotContents(slot, null);
		return stack;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack)
	{
		inventory[slot] = itemstack;

		if (stack != null && stack.stackSize > getInventoryStackLimit())
		{
			stack.stackSize = getInventoryStackLimit();
		}

		// Don't forget this line or your inventory will not be saved!
		onInventoryChanged();
	}

	// 1.7.2 renamed to getInventoryName
	@Override
	public String getInvName()
	{
		return name;
	}

	// 1.7.2 renamed to hasCustomInventoryName
	@Override
	public boolean isInvNameLocalized()
	{
		return name.length() > 0;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	/**
	 * This is the method that will handle saving the inventory contents, as it is called (or should be called!)
	 * anytime the inventory changes. Perfect. Much better than using onUpdate in an Item, as this will also
	 * let you change things in your inventory without ever opening a Gui, if you want.
	 */
	 // 1.7.2 renamed to markDirty
	@Override
	public void onInventoryChanged()
	{
		for (int i = 0; i < getSizeInventory(); ++i)
		{
			if (getStackInSlot(i) != null && getStackInSlot(i).stackSize == 0) {
				inventory[i] = null;
			}
		}
		
		// This line here does the work:		
		writeToNBT(invItem.getTagCompound());
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		return true;
	}

	// 1.7.2 renamed to openInventory
	@Override
	public void openChest() {}

	// 1.7.2 renamed to closeInventory
	@Override
	public void closeChest() {}

	/**
	 * This method doesn't seem to do what it claims to do, as
	 * items can still be left-clicked and placed in the inventory
	 * even when this returns false
	 */
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack)
	{
		// Don't want to be able to store the inventory item within itself
		// Bad things will happen, like losing your inventory
		// Actually, this needs a custom Slot to work
		return !(itemstack.getItem() instanceof ItemStore);
	}

	/**
	 * A custom method to read our inventory from an ItemStack's NBT compound
	 */
	public void readFromNBT(NBTTagCompound compound)
	{
		// Gets the custom taglist we wrote to this compound, if any
		// 1.7.2 change to compound.getTagList("ItemInventory", Constants.NBT.TAG_COMPOUND);
		NBTTagList items = compound.getTagList("ItemInventory");

		for (int i = 0; i < items.tagCount(); ++i)
		{
			// 1.7.2 change to items.getCompoundTagAt(i)
			NBTTagCompound item = (NBTTagCompound) items.tagAt(i);
			int slot = item.getInteger("Slot");

			// Just double-checking that the saved slot index is within our inventory array bounds
			if (slot >= 0 && slot < getSizeInventory()) {
				inventory[slot] = ItemStack.loadItemStackFromNBT(item);
			}
		}
	}

	/**
	 * A custom method to write our inventory to an ItemStack's NBT compound
	 */
	public void writeToNBT(NBTTagCompound tagcompound)
	{
		// Create a new NBT Tag List to store itemstacks as NBT Tags
		NBTTagList items = new NBTTagList();

		for (int i = 0; i < getSizeInventory(); ++i)
		{
			// Only write stacks that contain items
			if (getStackInSlot(i) != null)
			{
				// Make a new NBT Tag Compound to write the itemstack and slot index to
				NBTTagCompound item = new NBTTagCompound();
				item.setInteger("Slot", i);
				// Writes the itemstack in slot(i) to the Tag Compound we just made
				getStackInSlot(i).writeToNBT(item);

				// add the tag compound to our tag list
				items.appendTag(nbttagcompound1);
			}
		}
		// Add the TagList to the ItemStack's Tag Compound with the name "ItemInventory"
		tagcompound.setTag("ItemInventory", items);
	}
}
/*
If you want to be able to place your inventory-holding Item within another
instance of itself, you'll need to have a way to distinguish between each
instance of the item so you can check to make sure you're not placing the item
within itself. What you'll need to do is assign a UUID to a String variable
within your Inventory class for every ItemStack that holds an instance of your
Item, like so:
 */
// declaration of variable:
protected String uniqueID;

/** initialize variable within the constructor: */
uniqueID = "";

if (!itemstack.hasTagCompound())
{
	itemstack.setTagCompound(new NBTTagCompound());
	// no tag compound means the itemstack does not yet have a UUID, so assign one:
	uniqueID = UUID.randomUUID().toString();
}

/** When reading from NBT: */
if ("".equals(uniqueID))
{
	// try to read unique ID from NBT
	uniqueID = tagcompound.getString("uniqueID");
	// if it's still "", assign a new one:
	if ("".equals(uniqueID))
	{
		uniqueID = UUID.randomUUID().toString();
	}
}

/** Writing to NBT: */
// just add this line:
tagcompound.setString("uniqueID", this.uniqueID);
/*
Finally, in your Container class, you will need to check if the currently opened
inventory's uniqueID is equal to the itemstack's uniqueID in the method
'transferStackInSlot' as well as check if the itemstack is the currently equipped 
item in the method 'slotClick'. In both cases, you'll need to prevent the itemstack
from being moved or it will cause bad things to happen.
*/

/**
 * Step 3: Create a custom Container for your Inventory
 */
/*
There's a LOT of code in this one, but read through all of the comments carefully
and it should become clear what everything does.

As a bonus, one of my previous tutorials is included within!
"How to Properly Override Shift-Clicking" is here and better than ever!
At least in my opinion.

If you're like me, and you find no end of frustration trying to figure out which
f-ing index you should use for which slots in your container when overriding
transferStackInSlot, or if your following the original tutorial, then read on.
 */
public class ContainerItem extends Container
{
	/** The Item Inventory for this Container, only needed if you want to reference isUseableByPlayer */
	private final InventoryItem inventory;

	/** Using these will make transferStackInSlot easier to understand and implement
	 * INV_START is the index of the first slot in the Player's Inventory, so our
	 * InventoryItem's number of slots (e.g. 5 slots is array indices 0-4, so start at 5)
	 * Notice how we don't have to remember how many slots we made? We can just use
	 * InventoryItem.INV_SIZE and if we ever change it, the Container updates automatically. */
	private static final int INV_START = InventoryItem.INV_SIZE, INV_END = INV_START+26,
			HOTBAR_START = INV_END+1, HOTBAR_END = HOTBAR_START+8;

	// If you're planning to add armor slots, put those first like this:
	// ARMOR_START = InventoryItem.INV_SIZE, ARMOR_END = ARMOR_START+3,
	// INV_START = ARMOR_END+1, and then carry on like above.

	public ContainerItem(EntityPlayer par1Player, InventoryPlayer inventoryPlayer, InventoryItem inventoryItem)
	{
		this.inventory = inventoryItem;

		int i;

		// ITEM INVENTORY - you'll need to adjust the slot locations to match your texture file
		// I have them set vertically in columns of 4 to the right of the player model
		for (i = 0; i < InventoryItem.INV_SIZE; ++i)
		{
			// You can make a custom Slot if you need different behavior,
			// such as only certain item types can be put into this slot
			// We made a custom slot to prevent our inventory-storing item
			// from being stored within itself, but if you want to allow that and
			// you followed my advice at the end of the above step, then you
			// could get away with using the vanilla Slot class
			this.addSlotToContainer(new SlotItemInv(this.inventory, i, 80 + (18 * (int)(i/4)), 8 + (18*(i%4))));
		}

		// If you want, you can add ARMOR SLOTS here as well, but you need to
		// make a public version of SlotArmor. I won't be doing that in this tutorial.
		/*
		for (i = 0; i < 4; ++i)
		{
			// These are the standard positions for survival inventory layout
			this.addSlotToContainer(new SlotArmor(this.player, inventoryPlayer, inventoryPlayer.getSizeInventory() - 1 - i, 8, 8 + i * 18, i));
		}
		*/

		// PLAYER INVENTORY - uses default locations for standard inventory texture file
		for (i = 0; i < 3; ++i)
		{
			for (int j = 0; j < 9; ++j)
			{
				this.addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		// PLAYER ACTION BAR - uses default locations for standard action bar texture file
		for (i = 0; i < 9; ++i)
		{
			this.addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer)
	{
		// be sure to return the inventory's isUseableByPlayer method
		// if you defined special behavior there:
		return inventory.isUseableByPlayer(player);
	}

	/**
	 * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
	 */
	public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2)
	{
		ItemStack itemstack = null;
		Slot slot = (Slot) this.inventorySlots.get(par2);

		if (slot != null && slot.getHasStack())
		{
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			// If item is in our custom Inventory or armor slot
			if (par2 < INV_START)
			{
				// try to place in player inventory / action bar
				if (!this.mergeItemStack(itemstack1, INV_START, HOTBAR_END+1, true))
				{
					return null;
				}

				slot.onSlotChange(itemstack1, itemstack);
			}
			// Item is in inventory / hotbar, try to place in custom inventory or armor slots
			else
			{
				/*
				If your inventory only stores certain instances of Items,
				you can implement shift-clicking to your inventory like this:
				
				// Check that the item is the right type
				if (itemstack1.getItem() instanceof ItemCustom)
				{
					// Try to merge into your custom inventory slots
					// We use 'InventoryItem.INV_SIZE' instead of INV_START just in case
					// you also add armor or other custom slots
					if (!this.mergeItemStack(itemstack1, 0, InventoryItem.INV_SIZE, false))
					{
						return null;
					}
				}
				// If you added armor slots, check them here as well:
				// Item being shift-clicked is armor - try to put in armor slot
				if (itemstack1.getItem() instanceof ItemArmor)
				{
					int type = ((ItemArmor) itemstack1.getItem()).armorType;
					if (!this.mergeItemStack(itemstack1, ARMOR_START + type, ARMOR_START + type + 1, false))
					{
						return null;
					}
				}
				Otherwise, you have basically 2 choices:
				1. shift-clicking between player inventory and custom inventory
				2. shift-clicking between action bar and inventory
				 
				Be sure to choose only ONE of the following implementations!!!
				*/
				/**
				 * Implementation number 1: Shift-click into your custom inventory
				 */
				if (par2 >= INV_START)
        		    	{
            				// place in custom inventory
        				if (!this.mergeItemStack(itemstack1, 0, INV_START, false))
					{
						return null;
                			}
            			}
				
				/**
				 * Implementation number 2: Shift-click items between action bar and inventory
				 */
				// item is in player's inventory, but not in action bar
				if (par2 >= INV_START && par2 < HOTBAR_START)
				{
					// place in action bar
					if (!this.mergeItemStack(itemstack1, HOTBAR_START, HOTBAR_END+1, false))
					{
						return null;
					}
				}
				// item in action bar - place in player inventory
				else if (par2 >= HOTBAR_START && par2 < HOTBAR_END+1)
				{
					if (!this.mergeItemStack(itemstack1, INV_START, INV_END+1, false))
					{
						return null;
					}
				}
			}

			if (itemstack1.stackSize == 0)
			{
				slot.putStack((ItemStack) null);
			}
			else
			{
				slot.onSlotChanged();
			}

			if (itemstack1.stackSize == itemstack.stackSize)
			{
				return null;
			}

			slot.onPickupFromSlot(par1EntityPlayer, itemstack1);
		}

		return itemstack;
	}

	/**
	 * You should override this method to prevent the player from moving the stack that
	 * opened the inventory, otherwise if the player moves it, the inventory will not
	 * be able to save properly
	 */
	@Override
	public ItemStack slotClick(int slot, int button, int flag, EntityPlayer player) {
		// this will prevent the player from interacting with the item that opened the inventory:
		if (slot >= 0 && getSlot(slot) != null && getSlot(slot).getStack() == player.getHeldItem()) {
			return null;
		}
		return super.slotClick(slot, button, flag, player);
	}
}

/*
Special note: If your custom inventory's stack limit is 1 and you allow shift-clicking itemstacks into it,
you will need to override mergeStackInSlot to avoid losing all the items but one in a stack when you shift-click.
*/
/**
 * Vanilla mergeItemStack method doesn't correctly handle inventories whose
 * max stack size is 1 when you shift-click into the inventory.
 * This is a modified method I wrote to handle such cases.
 * Note you only need it if your slot / inventory's max stack size is 1
 */
@Override
protected boolean mergeItemStack(ItemStack par1ItemStack, int par2, int par3, boolean par4)
{
	boolean flag1 = false;
	int k = par2;

	if (par4)
	{
		k = par3 - 1;
	}

	Slot slot;
	ItemStack itemstack1;

	if (par1ItemStack.isStackable())
	{
		while (par1ItemStack.stackSize > 0 && (!par4 && k < par3 || par4 && k >= par2))
		{
			slot = (Slot) this.inventorySlots.get(k);
			itemstack1 = slot.getStack();

			if (itemstack1 != null && itemstack1.itemID == par1ItemStack.itemID && (!par1ItemStack.getHasSubtypes() || par1ItemStack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(par1ItemStack, itemstack1))
			{
				int l = itemstack1.stackSize + par1ItemStack.stackSize;

				if (l <= par1ItemStack.getMaxStackSize() && l <= slot.getSlotStackLimit())
				{
					par1ItemStack.stackSize = 0;
					itemstack1.stackSize = l;
					inventory.onInventoryChanged();
					flag1 = true;
				}
				else if (itemstack1.stackSize < par1ItemStack.getMaxStackSize() && l < slot.getSlotStackLimit())
				{
					par1ItemStack.stackSize -= par1ItemStack.getMaxStackSize() - itemstack1.stackSize;
					itemstack1.stackSize = par1ItemStack.getMaxStackSize();
					inventory.onInventoryChanged();
					flag1 = true;
				}
			}

			if (par4)
			{
				--k;
			}
			else
			{
				++k;
			}
		}
	}

	if (par1ItemStack.stackSize > 0)
	{
		if (par4)
		{
			k = par3 - 1;
		}
		else
		{
			k = par2;
		}

		while (!par4 && k < par3 || par4 && k >= par2)
		{
			slot = (Slot)this.inventorySlots.get(k);
			itemstack1 = slot.getStack();

			if (itemstack1 == null)
			{
				int l = par1ItemStack.stackSize;

				if (l <= slot.getSlotStackLimit())
				{
					slot.putStack(par1ItemStack.copy());
					par1ItemStack.stackSize = 0;
					inventory.onInventoryChanged();
					flag1 = true;
					break;
				}
				else
				{
					this.putStackInSlot(k, new ItemStack(par1ItemStack.getItem(), slot.getSlotStackLimit()));
					par1ItemStack.stackSize -= slot.getSlotStackLimit();
					inventory.onInventoryChanged();
					flag1 = true;
				}
			}

			if (par4)
			{
				--k;
			}
			else
			{
				++k;
			}
		}
	}

	return flag1;
}
/*
Making the custom slot is very simple, if you're going that route:
*/
public class SlotItemInv extends Slot
{
	public SlotItemInv(IInventory inv, int index, int xPos, int yPos)
	{
		super(inv, index, xPos, yPos);
	}

	// This is the only method we need to override so that
	// we can't place our inventory-storing Item within
	// its own inventory (thus making it permanently inaccessible)
	// as well as preventing abuse of storing backpacks within backpacks
	/**
	 * Check if the stack is a valid item for this slot.
	 */
	@Override
	public boolean isItemValid(ItemStack itemstack)
	{
		// Everything returns true except an instance of our Item
		return !(itemstack.getItem() instanceof ItemStore);
	}
}

/**
 * Step 4: Create the GUI for our custom Inventory
 */

/*
There's not much to this, mostly just copy and paste from vanilla classes.
 */
public class GuiItemInventory extends GuiContainer
{
	/** x and y size of the inventory window in pixels. Defined as float, passed as int
	 * These are used for drawing the player model. */
	private float xSize_lo;
	private float ySize_lo;

	/** ResourceLocation takes 2 parameters: ModId, path to texture at the location:
	 * "src/minecraft/assets/modid/"
	 * 
	 * I have provided a sample texture file that works with this tutorial. Download it
	 * from Forge_Tutorials/textures/gui/
	 */
	private static final ResourceLocation iconLocation = new ResourceLocation("inventoryitemmod", "textures/gui/inventoryitem.png");

	/** The inventory to render on screen */
	private final InventoryItem inventory;

	public GuiItemInventory(ContainerItem containerItem)
	{
		super(containerItem);
		this.inventory = containerItem.inventory;
	}

	/**
	 * Draws the screen and all the components in it.
	 */
	public void drawScreen(int par1, int par2, float par3)
	{
		super.drawScreen(par1, par2, par3);
		this.xSize_lo = (float)par1;
		this.ySize_lo = (float)par2;
	}

	/**
	 * Draw the foreground layer for the GuiContainer (everything in front of the items)
	 */
	protected void drawGuiContainerForegroundLayer(int par1, int par2)
	{
		String s = this.inventory.isInvNameLocalized() ? this.inventory.getInvName() : I18n.getString(this.inventory.getInvName());
		this.fontRenderer.drawString(s, this.xSize / 2 - this.fontRenderer.getStringWidth(s) / 2, 0, 4210752);
		this.fontRenderer.drawString(I18n.getString("container.inventory"), 26, this.ySize - 96 + 4, 4210752);
	}

	/**
	 * Draw the background layer for the GuiContainer (everything behind the items)
	 */
	protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3)
	{
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(iconLocation);
		int k = (this.width - this.xSize) / 2;
		int l = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
		int i1;
		drawPlayerModel(k + 51, l + 75, 30, (float)(k + 51) - this.xSize_lo, (float)(l + 75 - 50) - this.ySize_lo, this.mc.thePlayer);
	}

	/**
	 * This renders the player model in standard inventory position
	 */
	public static void drawPlayerModel(int x, int y, int scale, float yaw, float pitch, EntityLivingBase entity) {
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 50.0F);
		GL11.glScalef(-scale, scale, scale);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		float f2 = entity.renderYawOffset;
		float f3 = entity.rotationYaw;
		float f4 = entity.rotationPitch;
		float f5 = entity.prevRotationYawHead;
		float f6 = entity.rotationYawHead;
		GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-((float) Math.atan(pitch / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
		entity.renderYawOffset = (float) Math.atan(yaw / 40.0F) * 20.0F;
		entity.rotationYaw = (float) Math.atan(yaw / 40.0F) * 40.0F;
		entity.rotationPitch = -((float) Math.atan(pitch / 40.0F)) * 20.0F;
		entity.rotationYawHead = entity.rotationYaw;
		entity.prevRotationYawHead = entity.rotationYaw;
		GL11.glTranslatef(0.0F, entity.yOffset, 0.0F);
		RenderManager.instance.playerViewY = 180.0F;
		RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
		entity.renderYawOffset = f2;
		entity.rotationYaw = f3;
		entity.rotationPitch = f4;
		entity.prevRotationYawHead = f5;
		entity.rotationYawHead = f6;
		GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}
}

/**
 * Step 5: Finally, create your custom Item class that will open the Inventory
 */
/*
Nice to end on an easy one. The item only needs to open the inventory gui.
method. I named it ItemStore to more clearly distinguish it from the rest of my
classes. Not very creative, I know, but sufficient for a tutorial.
 */
public class ItemStore extends Item
{
	public ItemStore(int par1)
	{
		super(par1);
		// ItemStacks that store an NBT Tag Compound are limited to stack size of 1
		setMaxStackSize(1);
		// you'll want to set a creative tab as well, so you can get your item
		setCreativeTab(CreativeTabs.tabMisc);
	}

	// Without this method, your inventory will NOT work!!!
	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 1; // return any value greater than zero
	}
   
    	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
	{
		if (!world.isRemote)
		{
			// If player not sneaking, open the inventory gui
			if (!player.isSneaking()) {
				player.openGui(InventoryItemMain.instance, InventoryItemMain.GUI_ITEM_INV, world, (int) player.posX, (int) player.posY, (int) player.posZ);
			}
			
			// Otherwise, stealthily place some diamonds in there for a nice surprise next time you open it up :)
			else {
				new InventoryItem(player.getHeldItem()).setInventorySlotContents(0, new ItemStack(Item.diamond,4));
			}
		}
		
		return itemstack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister iconRegister)
	{
		this.itemIcon = iconRegister.registerIcon("inventoryitemmod:" + this.getUnlocalizedName().substring(5));
	}
}
/*
And that's it!
*/
