package com.SXD.changkaishen_mod_forge.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class ModEvents {

    // 记录每个世界的上次汇率，用于检测汇率变化
    private static final Map<String, Integer> lastExchangeRates = new HashMap<>();

    // 记录游戏规则状态
    private static boolean lastGoldYuanEnabled = true;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                // 检查游戏规则
                boolean currentEnabled = level.getGameRules().getBoolean(
                        com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE);

                if (currentEnabled) {
                    com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                            com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(level);

                    // 更新汇率
                    data.updateExchangeRate(level.getGameTime());

                    // 检查汇率是否变化
                    String levelKey = level.dimension().location().toString();
                    int currentRate = data.getIntegerExchangeRate();
                    Integer lastRate = lastExchangeRates.get(levelKey);

                    // 如果汇率发生变化，我们需要更新村民交易
                    if (lastRate != null && lastRate != currentRate) {
                        // 汇率变化，但不立即清空交易列表，而是在下次交互时更新
                        // 我们只记录汇率变化，VillagerInteractionHandler会处理
                    }

                    lastExchangeRates.put(levelKey, currentRate);

                    // 检查游戏规则状态变化
                    if (lastGoldYuanEnabled != currentEnabled) {
                        // 游戏规则从关闭变为开启，我们需要刷新村民交易
                        refreshVillagerTrades(level, data.getIntegerExchangeRate());
                    }
                } else {
                    // 如果游戏规则关闭，清空当前世界的所有玩家背包中的金圆券
                    clearAllGoldYuanFromPlayers(level);

                    // 检查游戏规则状态变化
                    if (lastGoldYuanEnabled != currentEnabled) {
                        // 游戏规则从开启变为关闭，我们需要恢复村民的原版交易
                        refreshVillagerTrades(level, 1);
                    }
                }

                // 更新游戏规则状态
                lastGoldYuanEnabled = currentEnabled;
            }
        }
    }

    private static void refreshVillagerTrades(ServerLevel level, int exchangeRate) {
        // 遍历所有村民实体，重置他们的交易列表
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof Villager villager) {
                // 重置村民的职业来重新生成交易列表
                // 这是修复村民交易的关键：重置职业会重新生成交易列表
                villager.setVillagerData(villager.getVillagerData());

                // 如果游戏规则启用，我们需要应用汇率修改
                if (level.getGameRules().getBoolean(
                        com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE)) {

                    // 在重置后应用汇率修改
                    applyExchangeRateToVillager(villager, exchangeRate);
                }
            }
        });
    }

    private static void applyExchangeRateToVillager(Villager villager, int exchangeRate) {
        // 这个方法的逻辑与VillagerInteractionHandler中的类似
        // 但由于我们是在服务器端直接调用，需要确保线程安全
        var offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }

        var modifiedOffers = new java.util.ArrayList<net.minecraft.world.item.trading.MerchantOffer>();

        for (var originalOffer : offers) {
            var modifiedOffer = modifyMerchantOffer(originalOffer, exchangeRate);
            if (modifiedOffer != null) {
                modifiedOffers.add(modifiedOffer);
            } else {
                modifiedOffers.add(originalOffer);
            }
        }

        // 清空并重新添加修改后的交易
        offers.clear();
        for (var offer : modifiedOffers) {
            offers.add(offer);
        }
    }

    private static net.minecraft.world.item.trading.MerchantOffer modifyMerchantOffer(
            net.minecraft.world.item.trading.MerchantOffer original, int exchangeRate) {

        if (original == null) {
            return null;
        }

        var costA = original.getCostA().copy();
        var costB = original.getCostB().copy();
        var result = original.getResult().copy();

        boolean modified = false;

        // 修改第一成本（绿宝石 -> 金圆券）
        if (costA.getItem() == net.minecraft.world.item.Items.EMERALD && costA.getCount() > 0) {
            int originalEmeraldCount = costA.getCount();
            int newGoldYuanCount = originalEmeraldCount * exchangeRate;
            if (newGoldYuanCount < 1) {
                newGoldYuanCount = 1;
            }
            costA = new ItemStack(
                    com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get(),
                    newGoldYuanCount
            );
            modified = true;
        }

        // 修改第二成本（绿宝石 -> 金圆券）
        if (!costB.isEmpty() && costB.getItem() == net.minecraft.world.item.Items.EMERALD && costB.getCount() > 0) {
            int originalEmeraldCount = costB.getCount();
            int newGoldYuanCount = originalEmeraldCount * exchangeRate;
            if (newGoldYuanCount < 1) {
                newGoldYuanCount = 1;
            }
            costB = new ItemStack(
                    com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get(),
                    newGoldYuanCount
            );
            modified = true;
        }

        if (!modified) {
            return original;
        }

        return new net.minecraft.world.item.trading.MerchantOffer(
                costA,
                costB,
                result,
                original.getUses(),
                original.getMaxUses(),
                original.getXp(),
                original.getPriceMultiplier(),
                original.getDemand()
        );
    }

    private static void clearAllGoldYuanFromPlayers(ServerLevel level) {
        // 遍历当前世界的所有玩家，清除他们背包中的金圆券
        level.players().forEach(player -> {
            clearGoldYuanFromPlayer(player);
        });
    }

    private static void clearGoldYuanFromPlayer(Player player) {
        // 清除玩家背包中的所有金圆券
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() ==
                    com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get()) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        // 清除副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() ==
                com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        // 清除光标上的物品（在交易界面中）
        ItemStack cursorStack = player.containerMenu.getCarried();
        if (!cursorStack.isEmpty() && cursorStack.getItem() ==
                com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get()) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.SXD.changkaishen_mod_forge.command.ExchangeRateCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 服务器启动时，初始化汇率记录
        for (ServerLevel level : event.getServer().getAllLevels()) {
            try {
                com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                        com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(level);

                String levelKey = level.dimension().location().toString();
                int currentRate = data.getIntegerExchangeRate();
                lastExchangeRates.put(levelKey, currentRate);

                // 初始化游戏规则状态
                lastGoldYuanEnabled = level.getGameRules().getBoolean(
                        com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE);
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        // 玩家登录时，如果游戏规则关闭，清除其背包中的金圆券
        if (!event.getEntity().level().getGameRules().getBoolean(
                com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE)) {

            clearGoldYuanFromPlayer(event.getEntity());
        }
    }
}