package com.elmakers.mine.bukkit.batch;

import com.elmakers.mine.bukkit.action.ActionHandler;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.block.UndoList;
import com.elmakers.mine.bukkit.api.spell.Spell;

public class ActionBatch implements com.elmakers.mine.bukkit.api.batch.SpellBatch {
    private final int actionCount;
    private final CastContext context;
    private final ActionHandler handler;
    private boolean finished = false;

    public ActionBatch(CastContext context, ActionHandler handler) {
        this.context = context;
        this.handler = (ActionHandler)handler.clone();
        this.actionCount = handler.getActionCount();
        if (handler.isUndoable()) {
            UndoList undoList = context.getUndoList();
            if (undoList != null) {
                undoList.setBatch(this);
            }
        }
    }

    @Override
    public int process(int maxBlocks) {
        if (finished) return 0;
        context.setWorkAllowed(maxBlocks);
        handler.perform(context);
        if (handler.isFinished()) {
            handler.finish(context);
            context.finish();
        }
        return maxBlocks - context.getWorkAllowed();
    }

    @Override
    public boolean isFinished() {
        return finished || handler.isFinished();
    }

    @Override
    public void finish() {
        if (!finished) {
            handler.cancel(context);
            handler.finish(context);
            context.finish();

            // Shouldn't need this anymore
            UndoList undoList = context.getUndoList();
            if (undoList != null) {
                undoList.setBatch(null);
            }

            finished = true;
        }
    }

    @Override
    public int size() {
        return actionCount;
    }

    @Override
    public int remaining() {
        return Math.max(0, actionCount - context.getActionsPerformed());
    }

    @Override
    public Spell getSpell() {
        return context.getSpell();
    }
}
