package com.fiberhome.dbserver.tools.util;

public enum MathUtils
{
    ;

    /**
     * mod method
     *
     * @param v int
     * @param m int
     * @return  the (positive) remainder of the division of v by mod.
     */
    public static int mod(int v, int m)
    {
        int r = v % m;
        if (r < 0)
        {
            r += m;
        }
        return r;
    }

}