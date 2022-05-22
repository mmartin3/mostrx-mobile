package com.mattmakesapps.mostrx

import java.io.Serializable

class SearchParameters: Serializable {
    var zip: String? = null
    var lat: String? = null
    var long: String? = null
    var pharmacy: String? = null

    fun isValid(): Boolean {
        return zip != null && lat != null && long != null
    }

    fun selectedPharmacy(name: String?): Boolean {
        if (pharmacy == null || name == null) {
            return true
        }

        val pharmacyOneWord = pharmacy!!.replace("\\s".toRegex(), "")
        val namesToCheck = mutableListOf(name)
        namesToCheck.add(name.uppercase().replace("PHARMACY", "").trim())
        namesToCheck.add(name.uppercase().replace("PHARMACY", "FOOD").trim())
        namesToCheck.add(name.uppercase().replace("FOOD", "").trim())
        namesToCheck.add(name.uppercase().replace("FOOD", "PHARMACY").trim())
        namesToCheck.add("$name Pharmacy")
        namesToCheck.add("$name Food")
        namesToCheck.add("$name Dept.")
        namesToCheck.add("${name}Pharmacy")
        namesToCheck.add("${name}Food")
        namesToCheck.add(name.replace("/", " "))

        for (otherPharmacy in namesToCheck) {
            if (pharmacy.equals(otherPharmacy, true) ||
                pharmacyOneWord.equals(otherPharmacy, true)) {
                    return true
            }
        }

        return false
    }
}
