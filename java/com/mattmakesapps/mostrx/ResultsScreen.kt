package com.mattmakesapps.mostrx

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import javax.xml.bind.DatatypeConverter

class ResultsScreen : Fragment() {
    private lateinit var resultsView: View
    private lateinit var drug: Drug
    private lateinit var params: SearchParameters

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        resultsView = inflater.inflate(R.layout.fragment_results_screen, container, false)
        val couponResults = arguments?.get("coupons") as CouponResults
        drug = arguments?.get("drug") as Drug
        params = arguments?.get("params") as SearchParameters
        val currencyFormatter = NumberFormat.getCurrencyInstance()
        val moreCoupons = getString(R.string.more_coupons, drug.brandName)
        val restartButton = resultsView.findViewById<MaterialButton>(R.id.new_search_button)
        currencyFormatter.maximumFractionDigits = 0
        resultsView.findViewById<TextView>(R.id.result_drug).text = drug.getDisplayName(true)
        resultsView.findViewById<TextView>(R.id.result_form).text = drug.form
        resultsView.findViewById<TextView>(R.id.result_qty).text = drug.quantity
        resultsView.findViewById<TextView>(R.id.result_dose).text = drug.dosage
        resultsView.findViewById<TextView>(R.id.more_coupons_button).text = moreCoupons
        initTipButton()

        resultsView.findViewById<MaterialButton>(R.id.more_coupons_button).setOnClickListener {
            try {
                openURL("https://mostrx.app/rx/${drug.brandSlug}")
            } catch (e: Exception) {
                println(e.toString())
            }
        }

        restartButton.setOnClickListener {
            Navigation.findNavController(resultsView).navigate(R.id.restart)
        }

        populateRetail(arguments?.getFloat("retail"), currencyFormatter)
        populateAverage(arguments?.getDouble("avg"), currencyFormatter)

        populateCoupon(
            "best_match_",
            couponResults.bestMatch,
            "Didn't find any coupons at ${params.pharmacy}."
        )

        populateCoupon(
            "best_price_",
            couponResults.bestPrice,
            "Couldn't find any coupons for ${drug.getDisplayName()}."
        )

        return resultsView
    }

    private fun populateRetail(retailPrice: Float?, formatter: NumberFormat) {
        retailPrice?.also {
            if (retailPrice > 0) {
                resultsView.findViewById<TextView>(R.id.result_retail).text = formatter.format(it)
            } else {
                resultsView.findViewById<LinearLayout>(R.id.retail_layout).visibility = View.GONE
            }
        }
    }

    private fun populateAverage(avg: Double?, formatter: NumberFormat) {
        avg?.also {
            if (avg > 0) {
                resultsView.findViewById<TextView>(R.id.result_discount).text = formatter.format(avg)
            } else {
                resultsView.findViewById<LinearLayout>(R.id.discount_layout).visibility = View.GONE
            }
        }
    }

    private fun populateCoupon(idPrefix: String, coupon: Coupon, emptyText: String) {
        val defType = "id"
        val packageName = resultsView.context.packageName

        if (coupon.isValid()) {
            resources.getIdentifier("${idPrefix}pharmacy",
                defType,
                packageName).also {
                    resultsView.findViewById<TextView>(it).text = coupon.pharmacy
            }

            resources.getIdentifier("${idPrefix}discounter",
                defType,
                packageName).also {
                resultsView.findViewById<TextView>(it).text = coupon.discounter
            }

            resources.getIdentifier("${idPrefix}total",
                defType,
                packageName).also {
                resultsView.findViewById<TextView>(it).text = coupon.formatPrice()
            }

            resources.getIdentifier("${idPrefix}button",
                defType,
                packageName).also {
                val button = resultsView.findViewById<MaterialButton>(it)

                "Go to ${coupon.discounter}".also { buttonText ->
                    button.text = buttonText
                }

                button.setOnClickListener {
                    clickCoupon(coupon)
                }
            }
        } else {
            resources.getIdentifier("${idPrefix}content",
                defType,
                packageName).also {
                resultsView.findViewById<View>(it).visibility = View.GONE
            }

            resources.getIdentifier("${idPrefix}empty",
                defType,
                packageName).also {
                val view = resultsView.findViewById<TextView>(it)
                view.visibility = View.VISIBLE
                view.text = emptyText
            }
        }
    }

    private fun clickCoupon(coupon: Coupon) {
        val encodedURL = DatatypeConverter.printBase64Binary(coupon.url?.toByteArray())

        val url = Uri.Builder()
            .scheme("https")
            .authority("mostrx.app")
            .appendPath("coupon")
            .appendEncodedPath(coupon.pharmacy)
            .appendEncodedPath(drug.brandName)
            .appendEncodedPath(drug.genericName)
            .appendEncodedPath(drug.slug)
            .appendEncodedPath(drug.ndc)
            .appendEncodedPath(drug.encodedDosage)
            .appendEncodedPath(drug.displayQuantity)
            .appendPath(params.zip)
            .appendPath(coupon.price.toString())
            .appendPath(encodedURL)
            .toString()

        openURL(url)
    }

    private fun openURL(url: String?) {
        if (url != null) {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse(url)
            startActivity(openURL)
        }
    }

    private fun initTipButton() {
        resultsView.findViewById<LinearLayout>(R.id.tip_layout).children.forEach {
            it.setOnClickListener {
                openURL("https://ko-fi.com/fungames")
            }
        }
    }
}
