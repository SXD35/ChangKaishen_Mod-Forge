package com.SXD.changkaishen_mod_forge;

import com.SXD.changkaishen_mod_forge.item.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ChangKaishen_Mod_Forge.MOD_ID)
public class ChangKaishen_Mod_Forge {
    public static final String MOD_ID = "changkaishen_mod_forge";

    public ChangKaishen_Mod_Forge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册物品
        ModItems.register(modEventBus);

        // 注册通用设置
        modEventBus.addListener(this::commonSetup);

        // 注册Forge事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 注册游戏规则
        event.enqueueWork(() -> {
            com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.register();
        });
    }
}