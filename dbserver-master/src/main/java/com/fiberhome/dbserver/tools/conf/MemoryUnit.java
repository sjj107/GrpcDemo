package com.fiberhome.dbserver.tools.conf;

/**
 * <p>
 * 内存单位
 * </p>
 * 
 * @author P.F.XING
 */
public enum MemoryUnit
{
    BYTES(0), KILOBYTES(10), MEGABYTES(20), GIGABYTES(30), TERABYTES(40);

    private final int index;

    private MemoryUnit(int index)
    {
        this.index = index;
    }

    public long convert(long duration, MemoryUnit unit)
    {
        return doConvert(unit.index - this.index, duration);
    }

    public long toBytes(long amount)
    {
        return doConvert(this.index - BYTES.index, amount);
    }

    public int toBytes(int amount)
    {
        return doConvert(this.index - BYTES.index, amount);
    }

    private static long doConvert(int delta, long amount)
    {
        if (amount >= 0L)
        {
            if (delta == 0)
            {
                return amount;
            }
            if (delta < 0)
            {
                return amount >>> -delta;
            }
            if (delta >= Long.numberOfLeadingZeros(amount))
            {
                return 9223372036854775807L;
            }
            return amount << delta;
        }

        throw new IllegalArgumentException();
    }

    private static int doConvert(int delta, int amount)
    {
        if (amount >= 0)
        {
            if (delta == 0)
            {
                return amount;
            }
            if (delta < 0)
            {
                return amount >>> -delta;
            }
            if (delta >= Integer.numberOfLeadingZeros(amount))
            {
                return 2147483647;
            }
            return amount << delta;
        }

        throw new IllegalArgumentException();
    }
}
