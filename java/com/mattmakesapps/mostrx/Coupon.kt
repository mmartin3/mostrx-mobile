package com.mattmakesapps.mostrx

import java.io.Serializable
import java.text.NumberFormat

class Coupon: Comparable<Coupon>, Serializable {
    var url: String? = null
    var pharmacy: String? = null
    var price: Double? = null
    var discounter: String? = null

    fun isValid(): Boolean {
        if (price == null || price!! <= 0) {
            return false
        }

        return pharmacy != null
    }

    fun update(c: Coupon, includeMailOrder: Boolean = false) {
        val aPharmacy = c.pharmacy

        if (aPharmacy == null || !includeMailOrder && isMailOrder(aPharmacy)) {
            return
        }

        c.price?.also { aPrice ->
            if (price == null || aPrice < price!!) {
                price = aPrice
                pharmacy = aPharmacy
                discounter = c.discounter
                url = c.url
            }
        }
    }

    fun formatPrice(): String {
        return if (price == null) {
            "$--.--"
        } else {
            NumberFormat.getCurrencyInstance().format(price)
        }
    }

    private fun isMailOrder(pharmacy: String): Boolean {
        return pharmacy.contains("genius", true) ||
                pharmacy.contains("caremark", true) ||
                pharmacy.contains("express", true) ||
                pharmacy.contains("optum", true) ||
                pharmacy.contains("blink", true) ||
                pharmacy.contains("mail", true) ||
                pharmacy.contains("pack", true) ||
                pharmacy.contains("honeybee", true) ||
                pharmacy.contains("healthwarehouse", true) ||
                pharmacy.contains("health ware", true) ||
                pharmacy.contains("solve", true) ||
                pharmacy.contains("capsule", true) ||
                pharmacy.contains("amazon", true) ||
                pharmacy.contains("capsule", true)
    }

    override fun compareTo(other: Coupon): Int {
        return (price ?: Double.MAX_VALUE).compareTo(other.price ?: Double.MAX_VALUE)
    }
}
