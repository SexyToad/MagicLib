package com.elmakers.mine.bukkit.block;

public class BlockTileEntity extends BlockExtraData {
    protected Object data;

    public BlockTileEntity(Object data) {
        this.data = data;
    }

    public BlockExtraData clone() {
        return new BlockTileEntity(data);
    }
}
