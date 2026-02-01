package com.SXD.changkaishen_mod_forge.item;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "changkaishen_mod_forge");

    // 堆叠上限设为64（标准堆叠）
    public static final RegistryObject<Item> GOLD_YUAN_CERTIFICATE = ITEMS.register("gold_yuan_certificate",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        // 检查是否为原材料标签页
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            // 只在游戏规则启用时显示金圆券
            // 由于BuildCreativeModeTabContentsEvent在客户端触发，我们可以检查客户端的游戏规则
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

            if (mc.level != null) {
                // 客户端世界存在，检查游戏规则
                boolean enabled = mc.level.getGameRules().getBoolean(
                        com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE);

                if (enabled) {
                    event.accept(GOLD_YUAN_CERTIFICATE.get());
                }
                // 如果游戏规则关闭，不添加金圆券
            } else {
                // 客户端世界不存在（例如在主菜单），默认添加
                // 这样在进入世界前可以在创造模式看到物品
                event.accept(GOLD_YUAN_CERTIFICATE.get());
            }
        }
    }
}