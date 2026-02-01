package com.SXD.changkaishen_mod_forge.trade;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class VillagerInteractionHandler {

    @SubscribeEvent
    public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        // 检查是否与村民交互
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }

        Player player = event.getEntity();

        // 检查游戏规则是否启用
        if (!player.level().getGameRules().getBoolean(
                com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE)) {
            return;
        }

        // 获取当前汇率
        int exchangeRate = 1;
        try {
            com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                    com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(player.level());
            exchangeRate = data.getIntegerExchangeRate();
        } catch (Exception e) {
            exchangeRate = 1;
        }

        // 检查汇率是否有效（至少为1）
        if (exchangeRate < 1) {
            exchangeRate = 1;
        }

        // 确保村民有交易列表，如果没有则重新生成
        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            // 重新生成村民的交易列表
            villager.setVillagerData(villager.getVillagerData());
            offers = villager.getOffers();

            if (offers == null || offers.isEmpty()) {
                return;
            }
        }

        // 修改村民的当前交易
        modifyVillagerTrades(villager, offers, exchangeRate);
    }

    private static void modifyVillagerTrades(Villager villager, MerchantOffers offers, int exchangeRate) {
        if (offers == null || offers.isEmpty()) {
            return;
        }

        // 创建新的交易列表
        List<MerchantOffer> modifiedOffers = new ArrayList<>();

        for (MerchantOffer originalOffer : offers) {
            MerchantOffer modifiedOffer = modifyMerchantOffer(originalOffer, exchangeRate);
            if (modifiedOffer != null) {
                modifiedOffers.add(modifiedOffer);
            } else {
                modifiedOffers.add(originalOffer);
            }
        }

        // 清空并重新添加修改后的交易
        offers.clear();
        for (MerchantOffer offer : modifiedOffers) {
            offers.add(offer);
        }
    }

    private static MerchantOffer modifyMerchantOffer(MerchantOffer original, int exchangeRate) {
        if (original == null) {
            return null;
        }

        ItemStack costA = original.getCostA().copy();
        ItemStack costB = original.getCostB().copy();
        ItemStack result = original.getResult().copy();

        boolean modified = false;

        // 修改第一成本（绿宝石 -> 金圆券）
        if (costA.getItem() == Items.EMERALD && costA.getCount() > 0) {
            int originalEmeraldCount = costA.getCount();
            int newGoldYuanCount = originalEmeraldCount * exchangeRate;
            // 确保至少为1
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
        if (!costB.isEmpty() && costB.getItem() == Items.EMERALD && costB.getCount() > 0) {
            int originalEmeraldCount = costB.getCount();
            int newGoldYuanCount = originalEmeraldCount * exchangeRate;
            // 确保至少为1
            if (newGoldYuanCount < 1) {
                newGoldYuanCount = 1;
            }
            costB = new ItemStack(
                    com.SXD.changkaishen_mod_forge.item.ModItems.GOLD_YUAN_CERTIFICATE.get(),
                    newGoldYuanCount
            );
            modified = true;
        }

        // 如果没有修改，返回原交易
        if (!modified) {
            return original;
        }

        // 创建新的交易，保持原交易的其它属性
        MerchantOffer newOffer = new MerchantOffer(
                costA,
                costB,
                result,
                original.getUses(),
                original.getMaxUses(),
                original.getXp(),
                original.getPriceMultiplier(),
                original.getDemand()
        );

        return newOffer;
    }
}