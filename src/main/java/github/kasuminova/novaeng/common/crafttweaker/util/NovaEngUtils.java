package github.kasuminova.novaeng.common.crafttweaker.util;

import crafttweaker.annotations.ZenRegister;
import net.minecraftforge.fml.common.FMLCommonHandler;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

@ZenRegister
@ZenClass("novaeng.NovaEngUtils")
public class NovaEngUtils {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.##");
    public static final BigInteger BigLongMax = BigInteger.valueOf(Long.MAX_VALUE);
    public static boolean isClient = FMLCommonHandler.instance().getEffectiveSide().isClient();

    static {
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.HALF_UP);
    }

    @ZenMethod
    public static String formatFloat(float value, int decimalFraction) {
        return formatDouble(value, decimalFraction);
    }

    @ZenMethod
    public static String formatDouble(double value, int decimalFraction) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(decimalFraction);
        return nf.format(value);
    }

    @ZenMethod
    public static String formatDecimal(double value) {
        return DECIMAL_FORMAT.format(value);
    }

    @ZenMethod
    public static String formatNumber(long value) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return formatFloat((float) value / 1_000L, 2) + "K";
        } else if (value < 1_000_000_000L) {
            return formatDouble((double) value / 1_000_000L, 2) + "M";
        } else if (value < 1_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000L, 2) + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000L, 2) + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000_000L, 2) + "P";
        } else {
            return formatDouble((double) value / 1_000_000_000_000_000_000L, 2) + "E";
        }
    }

    @ZenMethod
    public static String formatNumber(String value) {
        var BigValue = new BigInteger(value);
        var big = BigValue.compareTo(BigLongMax) >= 0 ? Long.MAX_VALUE : BigValue.longValue();
        String zf = "";
        if (value.startsWith("-")){
            zf += "-";
        }
        if (big < 1000) {
            return zf + value;
        } else if (big < 1000000) {
            return zf + (big / 1000) + "K";
        } else if (big < 1000000000) {
            return zf + ((big / 1000)/ 1000) + "M";
        } else if (big < 1000000000000L) {
            return zf + ((big / 1000000)/ 1000) + "G";
        } else if (big < 1000000000000000L) {
            return zf + ((big / 1000000000)/ 1000) + "T";
        } else if (big < 1000000000000000000L) {
            return zf + ((big / 1_000_000_000_000L)/ 1000) + "P";
        } else if (big != (Long.MAX_VALUE)){
            return zf + ((big / 1_000_000_000_000_000L)/ 1000) + "E";
        }  else {
            int cfs = value.length() - 1;
            float cft = (1.00f * Integer.parseInt(value.substring(0,3))) / 100;

            return zf + cft + " * 10 ^ " + cfs;
        }
    }

    @ZenMethod
    public static String formatNumber(long value, int decimalFraction) {
        if (value < 1_000L) {
            return String.valueOf(value);
        } else if (value < 1_000_000L) {
            return formatFloat((float) value / 1_000L, decimalFraction) + "K";
        } else if (value < 1_000_000_000L) {
            return formatDouble((double) value / 1_000_000L, decimalFraction) + "M";
        } else if (value < 1_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000L, decimalFraction) + "G";
        } else if (value < 1_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000L, decimalFraction) + "T";
        } else if (value < 1_000_000_000_000_000_000L) {
            return formatDouble((double) value / 1_000_000_000_000_000L, decimalFraction) + "P";
        } else {
            return formatDouble((double) value / 1_000_000_000_000_000_000L, decimalFraction) + "E";
        }
    }

    @ZenMethod
    public static String formatPercent(double num1, double num2) {
        if (num2 == 0) {
            return "0%";
        }
        return formatDouble((num1 / num2) * 100D, 2) + "%";
    }

    @ZenMethod
    public static String formatFLOPS(double value) {
        if (value < 1000.0F) {
            return formatDouble(value, 1) + "T FloPS";
        }
        return formatDouble(value / 1000.0D, 1) + "P FloPS";
    }

}