package com.SXD.changkaishen_mod_forge.world.savedata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ExchangeRateData extends SavedData {
    private static final String DATA_NAME = "changkaishen_mod_forge_exchange_rate";

    private double exchangeRate = 1.0;
    private long lastUpdateTime = 0;

    // 贬值参数
    private static final double DEPRECIATION_RATE = 1.0; // 每次贬值增加1个金圆券（100%）
    private int currentDepreciationInterval = 5400; // 初始1分30秒（5400游戏刻：1.5 * 60 * 20 * 3）
    private static final int MIN_INTERVAL = 300; // 最快15秒（300游戏刻）
    private static final int ACCELERATION = 300; // 每次加快300游戏刻（15秒）
    private int depreciationCount = 0; // 贬值次数

    public double getExchangeRate() {
        return exchangeRate;
    }

    // 获取整数汇率（向上取整）
    public int getIntegerExchangeRate() {
        return (int) Math.ceil(exchangeRate);
    }

    public void setExchangeRate(double rate) {
        this.exchangeRate = rate;
        setDirty();
    }

    public void updateExchangeRate(long currentTime) {
        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            return;
        }

        long timeDiff = currentTime - lastUpdateTime;

        // 检查是否到达贬值时间
        while (timeDiff >= currentDepreciationInterval) {
            // 贬值：增加金圆券数量（向上取整为整数）
            exchangeRate += DEPRECIATION_RATE;
            exchangeRate = Math.ceil(exchangeRate); // 确保是整数

            // 增加贬值次数
            depreciationCount++;

            // 加速贬值间隔（每次加快15秒，直到达到最小值）
            if (currentDepreciationInterval > MIN_INTERVAL) {
                currentDepreciationInterval = Math.max(MIN_INTERVAL, currentDepreciationInterval - ACCELERATION);
            }

            // 更新时间
            lastUpdateTime += currentDepreciationInterval;
            timeDiff = currentTime - lastUpdateTime;

            setDirty();
        }
    }

    public int getCurrentDepreciationInterval() {
        return currentDepreciationInterval;
    }

    public int getDepreciationCount() {
        return depreciationCount;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    // 完整重置方法
    public void reset() {
        exchangeRate = 1.0;
        currentDepreciationInterval = 5400; // 重置为1分30秒
        depreciationCount = 0; // 重置贬值次数
        lastUpdateTime = 0; // 重置更新时间
        setDirty();
    }

    public static ExchangeRateData get(Level level) {
        if (level.isClientSide()) {
            throw new RuntimeException("不能在客户端获取数据");
        }

        ServerLevel serverLevel = (ServerLevel) level;
        DimensionDataStorage storage = serverLevel.getDataStorage();

        return storage.computeIfAbsent(
                ExchangeRateData::load,
                ExchangeRateData::new,
                DATA_NAME
        );
    }

    public static ExchangeRateData load(CompoundTag tag) {
        ExchangeRateData data = new ExchangeRateData();
        data.exchangeRate = tag.contains("exchangeRate") ? tag.getDouble("exchangeRate") : 1.0;
        data.lastUpdateTime = tag.contains("lastUpdateTime") ? tag.getLong("lastUpdateTime") : 0;
        data.currentDepreciationInterval = tag.contains("depreciationInterval") ? tag.getInt("depreciationInterval") : 5400;
        data.depreciationCount = tag.contains("depreciationCount") ? tag.getInt("depreciationCount") : 0;
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putDouble("exchangeRate", exchangeRate);
        tag.putLong("lastUpdateTime", lastUpdateTime);
        tag.putInt("depreciationInterval", currentDepreciationInterval);
        tag.putInt("depreciationCount", depreciationCount);
        return tag;
    }
}