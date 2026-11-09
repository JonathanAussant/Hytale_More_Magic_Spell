package org.MoreMagicSpell.Builtin.glyphs.StoneWallGlyph;

import org.MoreMagicSpell.Spells.StoneWallSpell;

import com.riprod.hexcode.api.execution.HexExecuter;
import com.riprod.hexcode.core.common.execution.context.HexContext;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;

import com.riprod.hexcode.utils.HexVarUtil;

public class StoneWallGlyph extends GlyphAsset {

    public static final String TYPE = "StoneWall";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        var target = glyph.readSlot(StoneWallSlots.TARGET, hexContext);
        if (target == null) {
            return;
        }
        var rotation = glyph.readSlot(StoneWallSlots.ROTATION, hexContext, target);
        var durationVar = glyph.readSlot(StoneWallSlots.DURATION, hexContext);
        var duration = durationVar.scalar();

        if (duration == null) {
            duration = 8d;
        }

        StoneWallSpell.CastSpell(hexContext.getAccessor(), target.position(hexContext.getAccessor()), rotation.rotation(hexContext.getAccessor()),
                duration.intValue() * 1000);

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
