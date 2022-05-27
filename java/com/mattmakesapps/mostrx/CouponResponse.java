package com.mattmakesapps.mostrx;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CouponResponse implements Serializable {
    private double avgDiscount = 0;
    public Map<String, Datum> data;
    
    public CouponResults getBestMatches(SearchParameters params) {
        double sum = 0, count = 0;
        CouponResults results = new CouponResults();

        for (Datum d : data.values()) {
            for (Coupon c : d.coupons) {
                if (params.selectedPharmacy(c.getPharmacy())) {
                    results.getBestMatch().update(c, false);
                }

                results.getBestPrice().update(c, false);

                count++;
                sum += c.getPrice();
            }
        }

        if (count != 0) {
            avgDiscount = sum / count;
        }

        return results;
    }

    public double getAvgDiscount() {
        return avgDiscount;
    }
    
    public float getAvgRetail() {
        return Objects.requireNonNull(data.get("goodRx")).retail;
    }

    public static class Datum implements Serializable {
        public List<Coupon> coupons;
        public float retail = 0.0f;
    }
}
