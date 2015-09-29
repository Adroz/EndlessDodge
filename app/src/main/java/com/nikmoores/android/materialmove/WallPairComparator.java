package com.nikmoores.android.materialmove;

import java.util.Comparator;

/**
 * Created by Nik on 29/09/2015.
 */
public enum WallPairComparator implements Comparator<WallPair> {
    TOP_SORT {
        public int compare(WallPair o1, WallPair o2) {
            return Integer.valueOf(o1.getTop()).compareTo(o2.getTop());
        }
    },
    ELEVATION_SORT {
        public int compare(WallPair o1, WallPair o2) {
            return Integer.valueOf(o1.getElevation()).compareTo(o2.getElevation());
        }
    };

    public static Comparator<WallPair> descending(final Comparator<WallPair> other) {
        return new Comparator<WallPair>() {
            public int compare(WallPair o1, WallPair o2) {
                return -1 * other.compare(o1, o2);
            }
        };
    }

    public static Comparator<WallPair> getComparator(final WallPairComparator... multipleOptions) {
        return new Comparator<WallPair>() {
            public int compare(WallPair o1, WallPair o2) {
                for (WallPairComparator option : multipleOptions) {
                    int result = option.compare(o1, o2);
                    if (result != 0) {
                        return result;
                    }
                }
                return 0;
            }
        };
    }
}