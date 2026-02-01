package com.SXD.changkaishen_mod_forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ExchangeRateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("changkaishenmod")
                .requires(source -> source.hasPermission(0))
                .then(Commands.literal("goldyuan")
                        .then(Commands.literal("exchangerate")
                                .executes(ExchangeRateCommand::execute))
                        .then(Commands.literal("info")
                                .executes(ExchangeRateCommand::showInfo))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .executes(ExchangeRateCommand::resetRate))));
    }

    private static boolean isChinese(CommandSourceStack source) {
        // 检查玩家语言是否为中文
        if (source.getPlayer() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) source.getPlayer();
            String language = player.getLanguage();
            return language != null && (language.startsWith("zh_") || language.contains("cn"));
        }
        return false;
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 检查游戏规则是否启用
        if (!level.getGameRules().getBoolean(
                com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE)) {

            Component disabledMessage = isChinese(source)
                    ? Component.literal("金圆券系统已禁用！").withStyle(ChatFormatting.RED)
                    : Component.literal("Gold Yuan Certificate system is disabled!").withStyle(ChatFormatting.RED);
            source.sendFailure(disabledMessage);
            return 0;
        }

        try {
            // 获取汇率数据
            com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                    com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(level);
            int rate = data.getIntegerExchangeRate();

            // 根据语言发送消息
            if (isChinese(source)) {
                Component chineseMessage = Component.literal("1绿宝石 = " + rate + "金圆券")
                        .withStyle(ChatFormatting.GREEN);
                source.sendSuccess(() -> chineseMessage, false);
            } else {
                Component englishMessage = Component.literal("1 Emerald = " + rate + " \"Gold Yuan\" Certificate")
                        .withStyle(ChatFormatting.GREEN);
                source.sendSuccess(() -> englishMessage, false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Component errorMessage = isChinese(source)
                    ? Component.literal("获取汇率失败: " + e.getMessage()).withStyle(ChatFormatting.RED)
                    : Component.literal("Failed to get exchange rate: " + e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendFailure(errorMessage);
            return 0;
        }
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        // 检查游戏规则是否启用
        if (!level.getGameRules().getBoolean(
                com.SXD.changkaishen_mod_forge.gamerule.ModGameRules.RULE_ENABLE_GOLD_YUAN_CERTIFICATE)) {

            Component disabledMessage = isChinese(source)
                    ? Component.literal("金圆券系统已禁用！").withStyle(ChatFormatting.RED)
                    : Component.literal("Gold Yuan Certificate system is disabled!").withStyle(ChatFormatting.RED);
            source.sendFailure(disabledMessage);
            return 0;
        }

        try {
            com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                    com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(level);

            int rate = data.getIntegerExchangeRate();
            int intervalTicks = data.getCurrentDepreciationInterval();
            int intervalSeconds = intervalTicks / 20;
            int depreciationCount = data.getDepreciationCount();
            int minutes = intervalSeconds / 60;
            int seconds = intervalSeconds % 60;

            // 根据语言发送消息
            if (isChinese(source)) {
                source.sendSuccess(() ->
                        Component.literal("=== 金圆券系统信息 ===")
                                .withStyle(ChatFormatting.GOLD), false);

                source.sendSuccess(() ->
                        Component.literal("当前汇率: 1绿宝石 = " + rate + "金圆券")
                                .withStyle(ChatFormatting.GREEN), false);

                source.sendSuccess(() ->
                        Component.literal("贬值次数: " + depreciationCount + "次")
                                .withStyle(ChatFormatting.YELLOW), false);

                source.sendSuccess(() ->
                        Component.literal("下次贬值间隔: " +
                                        (minutes > 0 ? minutes + "分" : "") +
                                        seconds + "秒")
                                .withStyle(ChatFormatting.YELLOW), false);
            } else {
                source.sendSuccess(() ->
                        Component.literal("=== Gold Yuan Certificate System Information ===")
                                .withStyle(ChatFormatting.GOLD), false);

                source.sendSuccess(() ->
                        Component.literal("Current exchange rate: 1 Emerald = " + rate + " \"Gold Yuan\" Certificate")
                                .withStyle(ChatFormatting.GREEN), false);

                source.sendSuccess(() ->
                        Component.literal("Depreciation count: " + depreciationCount + " times")
                                .withStyle(ChatFormatting.YELLOW), false);

                source.sendSuccess(() ->
                        Component.literal("Next depreciation interval: " +
                                        (minutes > 0 ? minutes + " minute" + (minutes > 1 ? "s" : "") + " " : "") +
                                        seconds + " second" + (seconds != 1 ? "s" : ""))
                                .withStyle(ChatFormatting.YELLOW), false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Component errorMessage = isChinese(source)
                    ? Component.literal("获取信息失败: " + e.getMessage()).withStyle(ChatFormatting.RED)
                    : Component.literal("Failed to get information: " + e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendFailure(errorMessage);
            return 0;
        }
    }

    private static int resetRate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        try {
            // 获取汇率数据并重置
            com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData data =
                    com.SXD.changkaishen_mod_forge.world.savedata.ExchangeRateData.get(level);

            // 保存重置前的信息（用于反馈）
            int oldRate = data.getIntegerExchangeRate();
            int oldCount = data.getDepreciationCount();

            // 重置汇率、贬值次数和贬值间隔
            data.reset();

            // 根据语言发送消息
            if (isChinese(source)) {
                source.sendSuccess(() ->
                        Component.literal("✓ 金圆券汇率已成功重置！")
                                .withStyle(ChatFormatting.GREEN), false);

                source.sendSuccess(() ->
                        Component.literal("重置前: 1绿宝石 = " + oldRate + "金圆券 (已贬值" + oldCount + "次)")
                                .withStyle(ChatFormatting.YELLOW), false);

                source.sendSuccess(() ->
                        Component.literal("重置后: 1绿宝石 = 1金圆券 (初始状态)")
                                .withStyle(ChatFormatting.GREEN), false);
            } else {
                source.sendSuccess(() ->
                        Component.literal("✓ Gold Yuan Certificate exchange rate has been successfully reset!")
                                .withStyle(ChatFormatting.GREEN), false);

                source.sendSuccess(() ->
                        Component.literal("Before reset: 1 Emerald = " + oldRate + " \"Gold Yuan\" Certificate (depreciated " + oldCount + " times)")
                                .withStyle(ChatFormatting.YELLOW), false);

                source.sendSuccess(() ->
                        Component.literal("After reset: 1 Emerald = 1 \"Gold Yuan\" Certificate (initial state)")
                                .withStyle(ChatFormatting.GREEN), false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Component errorMessage = isChinese(source)
                    ? Component.literal("✗ 重置失败: " + e.getMessage()).withStyle(ChatFormatting.RED)
                    : Component.literal("✗ Reset failed: " + e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendFailure(errorMessage);
            return 0;
        }
    }
}