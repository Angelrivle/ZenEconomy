package com.cuac_xd.zeneconomy.currency;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public record CurrencyModel(
        String id,
        String name,
        String symbol,
        String prefix,
        double defaultBalance,
        double maxBalance, // -1 means no max
        boolean payable,
        boolean decimal,
        int maxDecimals,
        boolean vault,
        boolean balanceShorthand,
        String format,
        String formatShort,
        String decimalFormatPattern,
        String decimalFormatShortPattern,
        List<String> commands,
        Material iconMaterial,
        int customModelData,
        String iconDisplayName,
        List<String> iconLore
) {

    public String formatAmount(double amount) {
        String numStr = formatWithPattern(amount, decimalFormatPattern);
        String res = format
                .replace("%symbol%", symbol)
                .replace("%amount%", numStr)
                .replace("%currency%", name);
        return (prefix != null && !prefix.isEmpty() ? prefix : "") + res;
    }

    public String formatAmountShort(double amount) {
        String numStr = NumberFormatter.formatAbbreviated(amount, maxDecimals);
        String res = formatShort
                .replace("%symbol%", symbol)
                .replace("%amount%", numStr)
                .replace("%currency%", name);
        return (prefix != null && !prefix.isEmpty() ? prefix : "") + res;
    }

    public Component formatComponent(double amount) {
        return MiniMessage.miniMessage().deserialize(formatAmount(amount));
    }

    public Component formatComponentShort(double amount) {
        return MiniMessage.miniMessage().deserialize(formatAmountShort(amount));
    }

    private String formatWithPattern(double amount, String pattern) {
        try {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator(',');
            symbols.setDecimalSeparator('.');
            DecimalFormat df = new DecimalFormat(pattern != null ? pattern : "#,##0.00", symbols);
            return df.format(amount);
        } catch (Exception e) {
            return String.valueOf(amount);
        }
    }

    public boolean canStore(double value) {
        if (value < 0) return false;
        if (maxBalance > 0 && value > maxBalance) return false;
        return true;
    }

    public double clamp(double value) {
        if (value < 0) return 0;
        if (maxBalance > 0 && value > maxBalance) return maxBalance;
        return value;
    }
}
