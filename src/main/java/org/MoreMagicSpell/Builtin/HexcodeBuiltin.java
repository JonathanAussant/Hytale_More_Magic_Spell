package org.MoreMagicSpell.Builtin;

import org.MoreMagicSpell.Builtin.glyphs.PetrifyGlyph.PetrifyGlyph;
import org.MoreMagicSpell.Builtin.glyphs.StoneWallGlyph.StoneWallGlyph;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.common.glyphs.registry.GlyphAsset;

public class HexcodeBuiltin {
    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static void Setup() {
        GlyphAsset.register(PetrifyGlyph.class, PetrifyGlyph.TYPE, PetrifyGlyph::new);
        GlyphAsset.register(StoneWallGlyph.class, StoneWallGlyph.TYPE, StoneWallGlyph::new);
        LOGGER.atInfo().log("Registered 2 Hexcode Builtin Glyphs");
    }
}
