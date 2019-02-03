package astavie.thermallogistics.block;

import astavie.thermallogistics.ThermalLogistics;
import astavie.thermallogistics.tile.TileTerminal;
import astavie.thermallogistics.tile.TileTerminalItem;
import cofh.core.block.BlockCoreTile;
import cofh.core.block.TileNameable;
import cofh.core.util.CoreUtils;
import cofh.core.util.RayTracer;
import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.core.util.helpers.ServerHelper;
import cofh.core.util.helpers.WrenchHelper;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;

public abstract class BlockTerminal extends BlockCoreTile {

	@Override
	@SuppressWarnings("deprecation")
	public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
		return new ItemStack(ThermalLogistics.terminal);
	}

	public static final PropertyEnum<Direction> DIRECTION = PropertyEnum.create("direction", Direction.class, Direction.values());
	public static final PropertyBool ACTIVE = PropertyBool.create("active");

	public BlockTerminal(String name) {
		super(Material.IRON, "logistics");

		this.setTranslationKey("terminal");
		this.name = name;

		this.setCreativeTab(ThermalLogistics.tab);

		this.setHardness(5F);
		this.setResistance(25F);

		this.setDefaultState(this.blockState.getBaseState().withProperty(ACTIVE, false).withProperty(DIRECTION, Direction.NORTH));
	}

	public enum Direction implements IStringSerializable {
		UP_NORTH, UP_SOUTH, UP_WEST, UP_EAST, NORTH, SOUTH, WEST, EAST, DOWN_NORTH, DOWN_SOUTH, DOWN_WEST, DOWN_EAST;

		private final String name;

		Direction() {
			this.name = toString().toLowerCase();
		}

		@Override
		public String getName() {
			return name;
		}

		public static Direction getDirection(BlockPos pos, EntityLivingBase placer) {
			return getDirection(EnumFacing.getDirectionFromEntityLiving(pos, placer), placer.getHorizontalFacing().getOpposite());
		}

		public static Direction getDirection(EnumFacing vertical, EnumFacing horizontal) {
			if (vertical == EnumFacing.UP) {
				switch (horizontal) {
					case NORTH:
						return UP_NORTH;
					case SOUTH:
						return UP_SOUTH;
					case WEST:
						return UP_WEST;
					case EAST:
						return UP_EAST;
				}

			} else if (vertical == EnumFacing.DOWN) {
				switch (horizontal) {
					case NORTH:
						return DOWN_NORTH;
					case SOUTH:
						return DOWN_SOUTH;
					case WEST:
						return DOWN_WEST;
					case EAST:
						return DOWN_EAST;
				}
			} else {
				switch (horizontal) {
					case NORTH:
						return NORTH;
					case SOUTH:
						return SOUTH;
					case WEST:
						return WEST;
					case EAST:
						return EAST;
				}
			}
			return NORTH;
		}

	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		return getDefaultState().withProperty(DIRECTION, Direction.getDirection(pos, placer));
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase living, ItemStack stack) {
		TileTerminal tile = (TileTerminal) world.getTileEntity(pos);
		tile.setCustomName(ItemHelper.getNameFromItemStack(stack));
		if (stack.getTagCompound() != null)
			tile.requester.set(new ItemStack(stack.getTagCompound().getCompoundTag("Requester")));
		super.onBlockPlacedBy(world, pos, state, living, stack);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		RayTraceResult traceResult = RayTracer.retrace(player);

		if (traceResult == null) {
			return false;
		}
		PlayerInteractEvent event = new PlayerInteractEvent.RightClickBlock(player, hand, pos, side, traceResult.hitVec);
		if (MinecraftForge.EVENT_BUS.post(event) || event.getResult() == Event.Result.DENY) {
			return false;
		}
		if (player.isSneaking()) {
			if (WrenchHelper.isHoldingUsableWrench(player, traceResult)) {
				if (ServerHelper.isServerWorld(world) && canDismantle(world, pos, state, player)) {
					dismantleBlock(world, pos, state, player, false);
					WrenchHelper.usedWrench(player, traceResult);
				}
				return true;
			}
		}
		TileNameable tile = (TileNameable) world.getTileEntity(pos);

		if (tile == null || tile.isInvalid())
			return false;
		if (WrenchHelper.isHoldingUsableWrench(player, traceResult)) {
			if (tile.canPlayerAccess(player)) {
				if (ServerHelper.isServerWorld(world))
					tile.onWrench(player, side);
				WrenchHelper.usedWrench(player, traceResult);
			}
			return true;
		}
		if (!tile.canPlayerAccess(player))
			return false;
		if (tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
			ItemStack heldItem = player.getHeldItem(hand);
			if (FluidHelper.isFluidHandler(heldItem)) {
				FluidHelper.interactWithHandler(heldItem, tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null), player, hand);
				return true;
			}
		}
		return !ServerHelper.isServerWorld(world) || tile.openGui(player);
	}

	@Override
	@SuppressWarnings("deprecation")
	public IBlockState getStateFromMeta(int meta)  {
		return this.getDefaultState().withProperty(DIRECTION, Direction.values()[meta]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(DIRECTION).ordinal();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, ACTIVE, DIRECTION);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		CoreUtils.dropItemStackIntoWorldWithVelocity(((TileTerminalItem) world.getTileEntity(pos)).requester.get(), world, pos);
		super.breakBlock(world, pos, state);
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return new ItemStack(ThermalLogistics.terminal);
	}

	@Override
	public ArrayList<ItemStack> dropDelegate(NBTTagCompound nbt, IBlockAccess world, BlockPos pos, int fortune) {
		return new ArrayList<>(Collections.singleton(new ItemStack(ThermalLogistics.terminal)));
	}

	@Override
	public NBTTagCompound getItemStackTag(IBlockAccess world, BlockPos pos) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("Requester", ((TileTerminal) world.getTileEntity(pos)).requester.get().writeToNBT(new NBTTagCompound()));
		return tag;
	}

	@Override
	public ArrayList<ItemStack> dismantleBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player, boolean returnDrops) {
		NBTTagCompound nbt = getItemStackTag(world, pos);
		((TileTerminal) world.getTileEntity(pos)).requester.set(ItemStack.EMPTY);
		return dismantleDelegate(nbt, world, pos, player, returnDrops, false);
	}

	@Override
	public ArrayList<ItemStack> dismantleDelegate(NBTTagCompound nbt, World world, BlockPos pos, EntityPlayer player, boolean returnDrops, boolean simulate) {
		ArrayList<ItemStack> ret = new ArrayList<>();
		if (world.getBlockState(pos).getBlock() != this)
			return ret;

		ItemStack dropBlock = new ItemStack(ThermalLogistics.terminal);
		if (nbt != null && !nbt.isEmpty())
			dropBlock.setTagCompound(nbt);

		ret.add(dropBlock);

		if (!simulate) {
			world.setBlockToAir(pos);
			if (!returnDrops) {
				float f = 0.3F;
				double x2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
				double y2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;
				double z2 = world.rand.nextFloat() * f + (1.0F - f) * 0.5D;

				EntityItem dropEntity = new EntityItem(world, pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2, dropBlock);
				dropEntity.setPickupDelay(10);
				world.spawnEntity(dropEntity);

				if (player != null)
					CoreUtils.dismantleLog(player.getName(), this, 0, pos);
			}
		}
		return ret;
	}

	@Override
	public boolean preInit() {
		ForgeRegistries.BLOCKS.register(setRegistryName(name));
		return true;
	}

	@Override
	public boolean initialize() {
		return true;
	}

}
