package com.mattmakesapps.mostrx

import java.io.Serializable

data class Drug(val brandName: String,
                val genericName: String,
                val selectedBrand: DrugResponse.Brand,
                val selectedForm: DrugResponse.Form,
                val selectedStrength: DrugResponse.Strength,
                val selectedQuantity: DrugResponse.Quantity): Serializable {
    val brandSlug = brandName.nameToSlug()
    val genericSlug = genericName.nameToSlug()
    val branding = if (selectedBrand.isGeneric) Branding.GENERIC else Branding.BRAND
    val slug = if (branding == Branding.BRAND) brandSlug else genericSlug
    val form = selectedForm.toString()
    val encodedForm = form.encodeSlashes()
    val dosage = selectedStrength.toString()
    val encodedDosage = dosage.encodeSlashes()
    val quantity = selectedQuantity.amt.toString()
    val displayQuantity = if (form == "Bottle") "1 Bottle" else "$quantity ${encodedForm}s"
    val ndc = selectedQuantity.ndc.toString()

    fun getDisplayName(parenthetical: Boolean = false): String {
        return when {
            branding == Branding.BRAND -> {
                brandName
            }
            parenthetical && brandName != genericName -> {
                "$genericName (Generic for $brandName)"
            }
            else -> {
                genericName
            }
        }
    }

    private fun String.encodeSlashes(): String {
        return replace("/", "%2F")
    }

    private fun String.nameToSlug(): String {
        return lowercase().replace(' ', '-')
    }
}
