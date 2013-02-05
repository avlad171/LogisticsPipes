package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.interfaces.ILogisticsModule;
import logisticspipes.logic.BaseLogicCrafting;
import logisticspipes.logic.TemporaryLogic;
import logisticspipes.modules.ModuleApiaristSink;
import logisticspipes.pipes.basic.RoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class PipeItemsApiaristSink extends RoutedPipe {
	
	private ModuleApiaristSink sinkModule;

	public PipeItemsApiaristSink(int itemID) {
		super(new TemporaryLogic(), itemID);
		sinkModule = new ModuleApiaristSink();
		sinkModule.registerHandler(null, null, this, this);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_APIARIST_SINK_TEXTURE;
	}

	@Override
	public ILogisticsModule getLogisticsModule() {
		return sinkModule;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void setTile(TileEntity tile) {
		super.setTile(tile);
		sinkModule.registerPosition(xCoord, yCoord, zCoord, 0);
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

}
