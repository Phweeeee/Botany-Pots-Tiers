package com.ultramega.botanypotstiers.block.inv;

import com.ultramega.botanypotstiers.TieredBotanyPotHelper;
import com.ultramega.botanypotstiers.block.TieredBlockEntityBotanyPot;
import com.ultramega.botanypotstiers.data.recipes.crop.Crop;
import com.ultramega.botanypotstiers.data.recipes.soil.Soil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class TieredBotanyPotContainer extends SimpleContainer implements WorldlyContainer {
    public static final int SOIL_SLOT = 0;
    public static final int CROP_SLOT = 1;
    public static final int[] STORAGE_SLOT = IntStream.range(2, 14).toArray();
    public static final int[] EMPTY_SLOTS = new int[0];

    private final TieredBlockEntityBotanyPot potEntity;

    @Nullable
    private Soil soil = null;

    @Nullable
    private Crop crop = null;

    private int requiredGrowthTime = -1;

    public TieredBotanyPotContainer(TieredBlockEntityBotanyPot potEntity) {
        super(14);
        this.potEntity = potEntity;
    }

    public ItemStack getSoilStack() {
        return this.getItem(SOIL_SLOT);
    }

    public ItemStack getCropStack() {
        return this.getItem(CROP_SLOT);
    }

    public int getRequiredGrowthTime() {
        return this.requiredGrowthTime;
    }

    public TieredBlockEntityBotanyPot getPotEntity() {
        return this.potEntity;
    }

    public void update() {
        final Level level = this.potEntity.getLevel();
        final BlockPos pos = this.potEntity.getBlockPos();

        final boolean revalidateSoil = !this.getSoilStack().isEmpty() && this.soil == null && TieredBotanyPotHelper.findSoil(level, pos, potEntity, this.getSoilStack()) != null;
        final boolean revalidateCrop = !this.getCropStack().isEmpty() && this.crop == null && TieredBotanyPotHelper.findCrop(level, pos, potEntity, this.getCropStack()) != null;

        if (revalidateSoil || revalidateCrop) {

            this.setChanged();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();

        final Level level = this.potEntity.getLevel();
        final BlockPos pos = this.potEntity.getBlockPos();

        this.soil = TieredBotanyPotHelper.findSoil(level, pos, potEntity, this.getSoilStack());
        this.crop = TieredBotanyPotHelper.findCrop(level, pos, potEntity, this.getCropStack());
        if(potEntity.getTier() != null)
            this.requiredGrowthTime = TieredBotanyPotHelper.getRequiredGrowthTicks(potEntity.getLevel(), potEntity.getBlockPos(), potEntity, this.crop, this.soil) / potEntity.getTier().getSpeed();

        final int potLight = this.getPotEntity().getLightLevel();

        if (this.getPotEntity().getLevel() != null && this.getPotEntity().getBlockState().getValue(BlockStateProperties.LEVEL) != potLight) {
            this.getPotEntity().getLevel().setBlock(this.potEntity.getBlockPos(), this.getPotEntity().getBlockState().setValue(BlockStateProperties.LEVEL, potLight), 3);
        }

        this.getPotEntity().markDirty();
    }

    @Nullable
    public Crop getCrop() {
        return this.crop;
    }

    @Nullable
    public Soil getSoil() {
        return this.soil;
    }

    @Override
    public boolean canPlaceItem(int slotId, ItemStack toPlace) {
        final Level level = this.potEntity.getLevel();
        final BlockPos pos = this.potEntity.getBlockPos();

        // Only allow soils in the soil slot.
        if (slotId == SOIL_SLOT && this.getItem(slotId).isEmpty()) {
            return TieredBotanyPotHelper.findSoil(level, pos, potEntity, toPlace) != null;
        }

        // Only allow crop seeds in the crop slot.
        if (slotId == CROP_SLOT && this.getItem(slotId).isEmpty()) {
            return TieredBotanyPotHelper.findCrop(level, pos, potEntity, toPlace) != null;
        }

        // Other slots are not accessible with automation.
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? STORAGE_SLOT : EMPTY_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slotId, ItemStack toInsert, Direction side) {
        // Insertion via automation is not allowed.
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slotId, ItemStack toExtract, Direction side) {
        // Only allow storage slots to be extracted from below.
        return side == Direction.DOWN && this.potEntity.isHopper() && slotId != SOIL_SLOT && slotId != CROP_SLOT;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.potEntity == null || this.potEntity.isRemoved()) {
            return false;
        }

        final BlockPos pos = this.potEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f) <= 24d;
    }
}