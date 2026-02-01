package com.SXD.changkaishen_mod_forge.gamerule;

import net.minecraft.world.level.GameRules;

public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RULE_ENABLE_GOLD_YUAN_CERTIFICATE;

    public static void register() {
        RULE_ENABLE_GOLD_YUAN_CERTIFICATE = GameRules.register(
                "enableGoldYuanCertificate",
                GameRules.Category.MISC,
                GameRules.BooleanValue.create(true)
        );
    }
}