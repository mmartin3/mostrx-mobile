package com.mattmakesapps.mostrx

import java.io.Serializable

class CouponResults: Serializable {
    var bestMatch: Coupon = Coupon()
    var bestPrice: Coupon = Coupon()
}
