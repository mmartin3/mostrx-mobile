package com.mattmakesapps.mostrx;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class CouponResponse implements Serializable {
    public Map<String, Datum> data;

    public CouponResults getBestMatches(SearchParameters params) {
        CouponResults results = new CouponResults();

        for (Datum d : data.values()) {
            for (Coupon c : d.coupons) {
                if (params.selectedPharmacy(c.getPharmacy())) {
                    results.getBestMatch().update(c, false);
                }

                results.getBestPrice().update(c, false);
            }
        }

        return results;
    }

    public double getAvgDiscount() {
        int count = 0;
        double sum = 0;

        for (Datum d : data.values()) {
            for (Coupon c : d.coupons) {
                count++;
                sum += c.getPrice();
            }
        }

        if (sum == 0) {
            return 0;
        } else {
            return sum / count;
        }
    }

    public static class Datum implements Serializable {
        public List<Coupon> coupons;
        public float retail = 0.0f;
    }
}
