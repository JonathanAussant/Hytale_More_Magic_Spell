package org.MoreMagicSpell.Builtin.glyphs.PetrifyGlyph;

import org.MoreMagicSpell.Spells.PetrifySpell;

import com.riprod.hexcode.api.execution.HexExecuter;
import com.riprod.hexcode.core.common.execution.context.HexContext;
import com.riprod.hexcode.core.common.glyphs.component.Glyph;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;
import com.riprod.hexcode.core.common.glyphs.variables.EntityVar;

public class PetrifyGlyph extends GlyphAsset {
    public static final String TYPE = "Petrify";

    @Override
    public void execute(Glyph glyph, HexContext hexContext) {
        var target = glyph.readSlot(PetrifySlots.TARGET, hexContext);
        if (target == null) {
            return;
        }
        var durationVar = glyph.readSlot(PetrifySlots.DURATION, hexContext);
        var duration = durationVar.scalar();
        if (duration == null) {
            duration = 5d;
        }

        if (target instanceof EntityVar entityTarget) {
            PetrifySpell.CastSpell(hexContext.getAccessor(), entityTarget.getRef(hexContext.getAccessor()), duration.intValue() * 1000);
        }

        HexExecuter.continueFromSlot(glyph, Glyph.NEXT_SLOT, hexContext);
    }
}
