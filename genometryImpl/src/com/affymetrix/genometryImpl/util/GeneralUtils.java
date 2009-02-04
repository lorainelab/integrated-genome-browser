package com.affymetrix.genometryImpl.util;

import java.io.Closeable;

public final class GeneralUtils {

    /**
     * Safely close a Closeable object.  If it doesn't exist, return.
     */
    public static <S extends Closeable> void safeClose(S s) {
        if (s == null) {
            return;
        }
        try {
            s.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
