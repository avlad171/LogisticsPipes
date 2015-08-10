/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0.1, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/dev/LICENSE.md
 *
 * This file can instead be distributed under the license terms of the MIT license:
 *
 * Copyright (c) 2015  RS485
 *
 * This MIT license was reworded to only match this file. If you use the regular MIT license in your project, replace this copyright notice (this line and any lines below and NOT the copyright line above) with the lines from the original MIT license located here: http://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this file and associated documentation files (the "Source Code"), to deal in the Source Code without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Source Code, and to permit persons to whom the Source Code is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Source Code, which also can be distributed under the MIT.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package network.rs485.logisticspipes.world;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.utils.IPositionRotateble;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import lombok.Data;

@Data
public class DoubleCoordinates implements IPositionRotateble, ICoordinates {

	private double xCoord;
	private double yCoord;
	private double zCoord;

	public DoubleCoordinates() {
		setXCoord(0.0);
		setYCoord(0.0);
		setZCoord(0.0);
	}

	public DoubleCoordinates(double xCoord, double yCoord, double zCoord) {
		setXCoord(xCoord);
		setYCoord(yCoord);
		setZCoord(zCoord);
	}

	public DoubleCoordinates(DoubleCoordinates copy) {
		this(copy.getXCoord(), copy.getYCoord(), copy.getZCoord());
	}

	public DoubleCoordinates(TileEntity tile) {
		this(tile.xCoord, tile.yCoord, tile.zCoord);
	}

	public DoubleCoordinates(CoreUnroutedPipe pipe) {
		this(pipe.getX(), pipe.getY(), pipe.getZ());
	}

	public DoubleCoordinates(IPipeInformationProvider pipe) {
		this(pipe.getX(), pipe.getY(), pipe.getZ());
	}

	public DoubleCoordinates(CoordinatesPacket packet) {
		this(packet.getPosX(), packet.getPosY(), packet.getPosZ());
	}

	public DoubleCoordinates(Entity entity) {
		this(entity.posX, entity.posY, entity.posZ);
	}

	@Override
	public double getXDouble() {
		return getXCoord();
	}

	@Override
	public double getYDouble() {
		return getYCoord();
	}

	@Override
	public double getZDouble() {
		return getZCoord();
	}

	@Override
	public int getXInt() {
		return (int) getXCoord();
	}

	@Override
	public int getYInt() {
		return (int) getYCoord();
	}

	@Override
	public int getZInt() {
		return (int) getZCoord();
	}

	public TileEntity getTileEntity(IBlockAccess world) {
		return world.getTileEntity(getXInt(), getYInt(), getZInt());
	}

	public DoubleCoordinates moveForward(ForgeDirection dir, double steps) {
		switch (dir) {
			case UP:
				yCoord += steps;
				break;
			case DOWN:
				yCoord -= steps;
				break;
			case NORTH:
				zCoord -= steps;
				break;
			case SOUTH:
				zCoord += steps;
				break;
			case EAST:
				xCoord += steps;
				break;
			case WEST:
				xCoord -= steps;
				break;
			default:
		}
		return this;
	}

	public DoubleCoordinates moveForward(ForgeDirection dir) {
		return moveForward(dir, 1);
	}

	public DoubleCoordinates moveBackward(ForgeDirection dir, double steps) {
		return moveForward(dir, -1 * steps);
	}

	public DoubleCoordinates moveBackward(ForgeDirection dir) {
		return moveBackward(dir, 1);
	}

	@Override
	public String toString() {
		return "(" + getXCoord() + ", " + getYCoord() + ", " + getZCoord() + ")";
	}

	public String toIntBasedString() {
		return "(" + getXCoord() + ", " + getYCoord() + ", " + getZCoord() + ")";
	}

	public Block getBlock(IBlockAccess world) {
		return world.getBlock(getXInt(), getYInt(), getZInt());
	}

	public boolean blockExists(World world) {
		return world.blockExists(getXInt(), getYInt(), getZInt());
	}

	public double distanceTo(DoubleCoordinates targetPos) {
		return Math.sqrt(Math.pow(targetPos.getXCoord() - getXCoord(), 2) + Math.pow(targetPos.getYCoord() - getYCoord(), 2) + Math.pow(targetPos.getZCoord() - getZCoord(), 2));
	}

	public DoubleCoordinates center() {
		DoubleCoordinates coords = new DoubleCoordinates();
		coords.setXCoord(getXInt() + 0.5);
		coords.setYCoord(getYInt() + 0.5);
		coords.setYCoord(getZInt() + 0.5);
		return this;
	}

	public void writeToNBT(String prefix, NBTTagCompound nbt) {
		nbt.setDouble(prefix + "xPos", xCoord);
		nbt.setDouble(prefix + "yPos", yCoord);
		nbt.setDouble(prefix + "zPos", zCoord);
	}

	public static DoubleCoordinates readFromNBT(String prefix, NBTTagCompound nbt) {
		if (nbt.hasKey(prefix + "xPos") && nbt.hasKey(prefix + "yPos") && nbt.hasKey(prefix + "zPos")) {
			return new DoubleCoordinates(nbt.getDouble(prefix + "xPos"), nbt.getDouble(prefix + "yPos"), nbt.getDouble(prefix + "zPos"));
		}
		return null;
	}

	public DoubleCoordinates add(DoubleCoordinates toAdd) {
		setXCoord(getXCoord() + toAdd.getXCoord());
		setYCoord(getYCoord() + toAdd.getYCoord());
		setZCoord(getZCoord() + toAdd.getZCoord());
		return this;
	}

	public void setBlockToAir(World world) {
		world.setBlockToAir(getXInt(), getYInt(), getZInt());
	}

	@Override
	public void rotateLeft() {
		double tmp = getZCoord();
		setZCoord(-getXCoord());
		setXCoord(tmp);
	}

	@Override
	public void rotateRight() {
		double tmp = getXCoord();
		setXCoord(-getZCoord());
		setZCoord(tmp);
	}

	@Override
	public void mirrorX() {
		setXCoord(-getXCoord());
	}

	@Override
	public void mirrorZ() {
		setZCoord(-getZCoord());
	}
}