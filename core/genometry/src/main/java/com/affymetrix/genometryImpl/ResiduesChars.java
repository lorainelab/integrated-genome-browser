package com.affymetrix.genometry;

import java.util.HashMap;
import java.util.Map;

public enum ResiduesChars {

    A(new char[]{'A', 'a'}, 0),
    T(new char[]{'T', 't'}, 1),
    G(new char[]{'G', 'g'}, 2),
    C(new char[]{'C', 'c'}, 3),
    N(new char[]{'N', 'n', 'D', 'd', '_'}, 4);

    final static Map<Character, Integer> rcMap;
    final static Map<Integer, Character> reverseRcMap;

    static {
        rcMap = new HashMap<>(10);
        reverseRcMap = new HashMap<>(5);
        for (ResiduesChars rc : values()) {
            for (char ch : rc.chars) {
                rcMap.put(ch, rc.value);
            }
            reverseRcMap.put(rc.value, rc.chars[0]);
        }
    }

    public static char getCharFor(int j) {
        return reverseRcMap.get(j);
    }

    char[] chars;
    int value;

    private ResiduesChars(char[] chars, int value) {
        this.chars = chars;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(chars[0]);
    }

    public static int getValue(char ch) {
        Integer val = rcMap.get(ch);
        if (val != null) {
            return val;
        }
        return -1;
    }

}
