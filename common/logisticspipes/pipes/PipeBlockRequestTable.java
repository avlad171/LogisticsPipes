package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cpw.mods.fml.common.network.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.blocks.crafting.AutoCraftingInventory;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IRequestWatcher;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.OrderWatchRemovePacket;
import logisticspipes.network.packets.orderer.OrdererWatchPacket;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.LinkedLogisticsOrderList;
import logisticspipes.routing.LogisticsOrder;
import logisticspipes.routing.RoutedEntityItem;
import logisticspipes.security.SecuritySettings;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.CraftingUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.SimpleStackInventory;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.Icon;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.network.Player;

public class PipeBlockRequestTable extends PipeItemsRequestLogistics implements ISimpleInventoryEventHandler, IRequestWatcher, IGuiOpenControler {

	public SimpleStackInventory inv = new SimpleStackInventory(27, "Crafting Resources", 64);
	public ItemIdentifierInventory matrix = new ItemIdentifierInventory(9, "Crafting Matrix", 1);
	public ItemIdentifierInventory resultInv = new ItemIdentifierInventory(1, "Crafting Result", 1);
	public SimpleStackInventory toSortInv = new SimpleStackInventory(1, "Sorting Slot", 64);
	private IRecipe cache;
	private EntityPlayer fake;
	private int delay = 0;
	private int tick = 0;
	
	private PlayerCollectionList localGuiWatcher = new PlayerCollectionList();
	public Map<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>> watchedRequests = new HashMap<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>>();
	private int localLastUsedWatcherId = 0;

	public PipeBlockRequestTable(int itemID) {
		super(itemID);
		matrix.addListener(this);
	}

	@Override
	public boolean handleClick(EntityPlayer entityplayer, SecuritySettings settings) {
		//allow using upgrade manager
		if(SimpleServiceLocator.buildCraftProxy.isUpgradeManagerEquipped(entityplayer) && !(entityplayer.isSneaking())) {
			return false;
		}
		if(MainProxy.isServer(getWorld())) {
			if(settings == null || settings.openGui) {
				openGui(entityplayer);
			} else {
				entityplayer.sendChatToPlayer(ChatMessageComponent.createFromText("Permission denied"));
			}
		}
		return true;
	}

	@Override
	public void ignoreDisableUpdateEntity() {
		super.ignoreDisableUpdateEntity();
		if(tick++ == 5) {
			this.getWorld().markBlockForRenderUpdate(this.getX(), this.getY(), this.getZ());
		}
		if(MainProxy.isClient(getWorld())) return;
		//XXX TODO Cuurently DEV ONLY
		if(!LogisticsPipes.DEBUG) return;
		if(tick % 5 == 0 && !localGuiWatcher.isEmpty()) {
			checkForExpired();
			if(getUpgradeManager().hasCraftingMonitoringUpgrade()) {
				for(Entry<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>> entry:watchedRequests.entrySet()) {
					MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(entry.getValue().getValue2()).setStack(entry.getValue().getValue1()).setInteger(entry.getKey()).setTilePos(this.container), localGuiWatcher);
				}
			}
		} else if(tick % 20 == 0) {
			checkForExpired();
		}
	}

	private void checkForExpired() {
		Iterator<Entry<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>>> iter = watchedRequests.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>> entry = iter.next();
			if(isDone(entry.getValue().getValue2())) {
				MainProxy.sendToPlayerList(PacketHandler.getPacket(OrderWatchRemovePacket.class).setInteger(entry.getKey()).setTilePos(this.container), localGuiWatcher);
				iter.remove();
			}
		}
	}

	private boolean isDone(LinkedLogisticsOrderList orders) {
		boolean isDone = true;
		for(LogisticsOrder order:orders) {
			if(!order.isFinished()) isDone = false;
		}
		for(LinkedLogisticsOrderList orderList:orders.getSubOrders()) {
			if(!isDone(orderList)) isDone = false;
		}
		return isDone;
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		ItemStack stack = toSortInv.getStackInSlot(0);
		if(stack != null) {
			if(delay > 0) {
				delay--;
				return;
			}
			IRoutedItem itemToSend = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(this.container, stack);
			SimpleServiceLocator.logisticsManager.assignDestinationFor(itemToSend, this.getRouter().getSimpleID(), false);
			if(itemToSend.getDestinationUUID() != null) {
				ForgeDirection dir = this.getRouteLayer().getOrientationForItem(itemToSend, null);
				super.queueRoutedItem(itemToSend, dir.getOpposite());
				MainProxy.sendSpawnParticlePacket(Particles.OrangeParticle, getX(), getY(), getZ(), this.getWorld(), 4);
				toSortInv.clearInventorySlotContents(0);
			} else {
				delay = 100;
			}
		} else {
			delay = 0;
		}
	}

	@Override
	public void openGui(EntityPlayer entityplayer) {
		entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_Request_Table_ID, this.getWorld(), this.getX() , this.getY(), this.getZ());
	}
	
	@Override
	public TextureType getCenterTexture() {
		return Textures.empty;
	}
	
	@Override
	public TextureType getRoutedTexture(ForgeDirection connection) {
		return Textures.empty_1;
	}

	@Override
	public TextureType getNonRoutedTexture(ForgeDirection connection) {
		return Textures.empty_2;
	}

	public Icon getTextureFor(int l) {
		ForgeDirection dir = ForgeDirection.getOrientation(l);
		switch(dir) {
			case UP:
				return Textures.LOGISTICS_REQUEST_TABLE[0];
			case DOWN:
				return Textures.LOGISTICS_REQUEST_TABLE[1];
			default:
				if(this.container.getRenderState().pipeConnectionMatrix.isConnected(dir)) {
					if (this.container.getRenderState().textureMatrix.getTextureIndex(dir) == 1) {
						return Textures.LOGISTICS_REQUEST_TABLE[2];
					} else {
						return Textures.LOGISTICS_REQUEST_TABLE[3];
					}
				} else {
					return Textures.LOGISTICS_REQUEST_TABLE[4];
				}
		}
	}

	@Override
	public void onAllowedRemoval() {
		if(MainProxy.isServer(this.getWorld())) {
			inv.dropContents(getWorld(), getX(), getY(), getZ());
		}
	}
	
	public void cacheRecipe() {
		cache = null;
		resultInv.clearInventorySlotContents(0);
		AutoCraftingInventory craftInv = new AutoCraftingInventory("");//TODO
		for(int i=0; i<9;i++) {
			craftInv.setInventorySlotContents(i, matrix.getStackInSlot(i));
		}
		for(IRecipe r : CraftingUtil.getRecipeList()) {
			if(r.matches(craftInv, getWorld())) {
				cache = r;
				resultInv.setInventorySlotContents(0, r.getCraftingResult(craftInv));
				break;
			}
		}
	}

	public ItemStack getOutput() {
		if(cache == null) {
			cacheRecipe();
			if(cache == null) return null;
		}
		int[] toUse = new int[9];
		int[] used = new int[inv.getSizeInventory()];
outer:
		for(int i=0;i<9;i++) {
			ItemStack item = matrix.getStackInSlot(i);
			if(item == null) {
				toUse[i] = -1;
				continue;
			}
			ItemIdentifier ident = ItemIdentifier.get(item);
			for(int j=0;j<inv.getSizeInventory();j++) {
				item = inv.getStackInSlot(j);
				if(item == null) continue;
				if(ident.equalsForCrafting(ItemIdentifier.get(item))) {
					if(item.stackSize > used[j]) {
						used[j]++;
						toUse[i] = j;
						continue outer;
					}
				}
			}
			//Not enough material
			return null;
		}
		AutoCraftingInventory crafter = new AutoCraftingInventory("");//TODO
		for(int i=0;i<9;i++) {
			int j = toUse[i];
			if(j != -1) crafter.setInventorySlotContents(i, inv.getStackInSlot(j));
		}
		if(!cache.matches(crafter, getWorld())) return null; //Fix MystCraft
		ItemStack result = cache.getCraftingResult(crafter);
		if(result == null) return null;
		if(!ItemIdentifier.get(resultInv.getStackInSlot(0)).equalsWithoutNBT(ItemIdentifier.get(result))) return null;
		crafter = new AutoCraftingInventory("");//TODO
		for(int i=0;i<9;i++) {
			int j = toUse[i];
			if(j != -1) crafter.setInventorySlotContents(i, inv.decrStackSize(j, 1));
		}
		result = cache.getCraftingResult(crafter);
		if(fake == null) {
			fake = MainProxy.getFakePlayer(this.container);
		}
		result = result.copy();
		SlotCrafting craftingSlot = new SlotCrafting(fake, crafter, resultInv, 0, 0, 0);
		craftingSlot.onPickupFromSlot(fake, result);
		for(int i=0;i<9;i++) {
			ItemStack left = crafter.getStackInSlot(i);
			crafter.setInventorySlotContents(i, null);
			if(left != null) {
				left.stackSize = inv.addCompressed(left, false);
				if(left.stackSize > 0) {
					ItemIdentifierInventory.dropItems(getWorld(), left, getX(), getY(), getZ());
				}
			}
		}
		for(int i=0;i<fake.inventory.getSizeInventory();i++) {
			ItemStack left = fake.inventory.getStackInSlot(i);
			fake.inventory.setInventorySlotContents(i, null);
			if(left != null) {
				left.stackSize = inv.addCompressed(left, false);
				if(left.stackSize > 0) {
					ItemIdentifierInventory.dropItems(getWorld(), left, getX(), getY(), getZ());
				}
			}
		}
		return result;
	}

	public ItemStack getResultForClick() {
		ItemStack result = getOutput();
		if(result == null)
			return null;
		result.stackSize = inv.addCompressed(result, false);
		if(result.stackSize > 0)
			return result;
		return null;
	}

	@Override
	public void InventoryChanged(IInventory inventory) {
		if(inventory == matrix) {
			cacheRecipe();
		}
	}

	public void handleNEIRecipePacket(ItemStack[] content) {
		for(int i=0;i<9;i++) {
			matrix.setInventorySlotContents(i, content[i]);
		}
		cacheRecipe();
	}

	@Override
	public void readFromNBT(NBTTagCompound par1nbtTagCompound) {
		super.readFromNBT(par1nbtTagCompound);
		inv.readFromNBT(par1nbtTagCompound, "inv");
		matrix.readFromNBT(par1nbtTagCompound, "matrix");
		toSortInv.readFromNBT(par1nbtTagCompound, "toSortInv");
		//TODO NPEs on world load
		//cacheRecipe();
	}

	@Override
	public void writeToNBT(NBTTagCompound par1nbtTagCompound) {
		super.writeToNBT(par1nbtTagCompound);
		inv.writeToNBT(par1nbtTagCompound, "inv");
		matrix.writeToNBT(par1nbtTagCompound, "matrix");
		toSortInv.writeToNBT(par1nbtTagCompound, "toSortInv");
	}

	@Override
	public boolean sharesInventoryWith(CoreRoutedPipe other){
		return false;
	}

	@Override
	public TransportLayer getTransportLayer() {
		if (_transportLayer == null) {
			_transportLayer = new TransportLayer() {
				@Override
				public void handleItem(IRoutedItem item) {
					PipeBlockRequestTable.this.notifyOfItemArival((RoutedEntityItem)item);
					if(item.getItemStack() != null) {
						ItemStack stack = item.getItemStack();
						stack.stackSize = inv.addCompressed(stack, false);
						item.setItemStack(stack);
					}
				}
				@Override
				public ForgeDirection itemArrived(IRoutedItem item, ForgeDirection denyed) {
					return null;
				}
				@Override
				public boolean stillWantItem(IRoutedItem item) {
					return false;
				}
			};
		}
		return _transportLayer;
	}

	@Override
	public void handleOrderList(ItemIdentifierStack stack, LinkedLogisticsOrderList orders) {
		if(!getUpgradeManager().hasCraftingMonitoringUpgrade()) return;
		watchedRequests.put(++localLastUsedWatcherId, new Pair<ItemIdentifierStack, LinkedLogisticsOrderList>(stack, orders));
		MainProxy.sendToPlayerList(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(orders).setStack(stack).setInteger(localLastUsedWatcherId).setTilePos(this.container), localGuiWatcher);
	}

	@Override
	public void guiOpenedByPlayer(EntityPlayer player) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrderWatchRemovePacket.class).setInteger(-1).setTilePos(this.container), (Player)player);
		localGuiWatcher.add(player);
		for(Entry<Integer, Pair<ItemIdentifierStack, LinkedLogisticsOrderList>> entry:watchedRequests.entrySet()) {
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OrdererWatchPacket.class).setOrders(entry.getValue().getValue2()).setStack(entry.getValue().getValue1()).setInteger(entry.getKey()).setTilePos(this.container), (Player) player);
		}
	}

	@Override
	public void guiClosedByPlayer(EntityPlayer player) {
		localGuiWatcher.remove(player);
	}

	@Override
	public void handleClientSideListInfo(int id, ItemIdentifierStack stack, LinkedLogisticsOrderList orders) {
		if(MainProxy.isClient(getWorld())) {
			watchedRequests.put(id, new Pair<ItemIdentifierStack, LinkedLogisticsOrderList>(stack, orders));
		}
	}

	@Override
	public void handleClientSideRemove(int id) {
		if(MainProxy.isClient(getWorld())) {
			if(id == -1) {
				watchedRequests.clear();
			} else {
				watchedRequests.remove(id);
			}
		}
	}
}
