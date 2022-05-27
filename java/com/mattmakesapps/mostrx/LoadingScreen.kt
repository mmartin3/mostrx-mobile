package com.mattmakesapps.mostrx

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class LoadingScreen: Fragment() {
    private lateinit var loadingView: View
    private lateinit var drug: Drug
    private lateinit var params: SearchParameters

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        loadingView = inflater.inflate(R.layout.fragment_loading_screen, container, false)
        drug = (arguments?.get("drug") as Drug?)!!
        params = arguments?.get("params") as SearchParameters
        loadResults()

        return loadingView
    }

    private fun loadResults() {
        "${getString(R.string.finding_prefix)} ${drug.brandName}".also {
            loadingView.findViewById<TextView>(R.id.label).text = it
        }

        loadingView.findViewById<ImageView>(R.id.pill).startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.rotate)
        )

        (activity as MainActivity?)?.addToRequestQueue(couponRequest())
    }

    private fun couponRequest(): StringRequest {
        val req = object : StringRequest(
            Method.GET,
            requestURL(drug, params),
            { response ->
                processResults(Gson().fromJson(response, CouponResponse::class.java), params)
            }, { error ->
                val mainActivity = activity as MainActivity?

                mainActivity?.volleyFailed(error, retry = {
                    mainActivity.addToRequestQueue(couponRequest())
                })
            })
        { }

        req.retryPolicy = object : RetryPolicy {
            override fun getCurrentTimeout(): Int {
                return 50000
            }

            override fun getCurrentRetryCount(): Int {
                return 50000
            }

            @Throws(VolleyError::class)
            override fun retry(error: VolleyError) {
            }
        }

        return req
    }

    private fun requestURL(drug: Drug, params: SearchParameters): String {
        return Uri.Builder()
            .scheme("https")
            .authority("mostrx.app")
            .appendPath("api")
            .appendPath("v1")
            .appendPath("coupons")
            .appendEncodedPath(drug.brandName)
            .appendPath(drug.ndc)
            .appendEncodedPath(drug.genericSlug)
            .appendEncodedPath(drug.brandSlug)
            .appendPath(drug.branding.toString())
            .appendEncodedPath(drug.encodedForm)
            .appendEncodedPath(drug.encodedDosage)
            .appendEncodedPath(drug.quantity)
            .appendPath(params.zip)
            .appendPath(params.lat)
            .appendPath(params.long)
            .appendPath("1")
            .appendPath(Random.nextLong(1000000, 9999999).toString())
            .toString()
    }

    private fun processResults(coupons: CouponResponse, params: SearchParameters) {
        val bundle = bundleOf(
            "coupons" to coupons.getBestMatches(params),
            "avg" to coupons.avgDiscount,
            "retail" to coupons.avgRetail,
            "drug" to drug,
            "params" to params
        )

        view?.also {
            CoroutineScope(Dispatchers.Main).launch {
                Navigation
                    .findNavController(it)
                    .navigate(R.id.action_loading_screen_to_results_screen, bundle)
            }
        }
    }
}
