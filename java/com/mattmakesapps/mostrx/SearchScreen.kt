package com.mattmakesapps.mostrx

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.cielyang.android.clearableedittext.ClearableEditText
import com.google.gson.Gson
import java.lang.IllegalStateException
import java.util.*
import kotlin.math.min

class SearchScreen: Fragment() {
    private lateinit var searchView: View
    private lateinit var choices: DrugResponse
    private lateinit var pharmacySpinner: Spinner
    private lateinit var pharmacies: List<String>
    private lateinit var selection: SearchResponse.SearchResult
    private lateinit var formAdapter: ArrayAdapter<String>
    private lateinit var strengthAdapter: ArrayAdapter<String>
    private lateinit var quantityAdapter: ArrayAdapter<String>
    private val searchParameters: SearchParameters = SearchParameters()
    private val selectableForms: ArrayList<String> = arrayListOf()
    private val selectableStrengths: ArrayList<String> = arrayListOf()
    private val selectableQuantities: ArrayList<String> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        searchView = inflater.inflate(R.layout.fragment_search_screen, container, false)
        val mainActivity = activity as MainActivity?
        val brandSpinner = searchView.findViewById<Spinner>(R.id.brand)
        val formSpinner = searchView.findViewById<Spinner>(R.id.form)
        val quantitySpinner = searchView.findViewById<Spinner>(R.id.quantity)
        val strengthSpinner = searchView.findViewById<Spinner>(R.id.strength)
        pharmacySpinner = searchView.findViewById(R.id.pharmacy)
        loadPreferences()

        choices = arguments?.get("drugData") as DrugResponse
        selection = arguments?.get("selection") as SearchResponse.SearchResult

        val selectableBrands = choices.variations.keys.toList()
        val defaultForm = choices.defaultBrand.defaultForm
        brandSpinner.adapter = ArrayAdapter(searchView.context, R.layout.spinner_list, selectableBrands)
        brandSpinner.setSelection(choices.defaultBrandIndex)

        searchView.findViewById<TextView>(R.id.zip).addTextChangedListener(getTextWatcher())

        searchView.findViewById<Button>(R.id.submit).setOnClickListener {
            submit()
        }

        formAdapter = ArrayAdapter(searchView.context, R.layout.spinner_list, selectableForms)
        strengthAdapter = ArrayAdapter(searchView.context, R.layout.spinner_list, selectableStrengths)
        quantityAdapter = ArrayAdapter(searchView.context, R.layout.spinner_list, selectableQuantities)
        formSpinner.adapter = formAdapter
        strengthSpinner.adapter = strengthAdapter
        quantitySpinner.adapter = quantityAdapter
        updateSpinners()
        formSpinner.setSelection(choices.defaultBrand.defaultFormIndex)
        strengthSpinner.setSelection(defaultForm.defaultStrengthIndex)
        quantitySpinner.setSelection(defaultForm.defaultStrength.defaultQuantityIndex)

        searchView.findViewById<ClearableEditText>(R.id.zip).setOnFocusChangeListener { _, hasFocus ->
            mainActivity?.setEnteringText(hasFocus)
        }

        searchView.findViewById<EditText>(R.id.custom_quantity).also {
            it.setOnFocusChangeListener { _, hasFocus ->
                mainActivity?.setEnteringText(hasFocus)
            }

            it.addTextChangedListener(getQuantityWatcher())
        }

        quantitySpinner.onItemSelectedListener = QuantitySelectedListener(this)

        val listener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateSpinners()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        brandSpinner.onItemSelectedListener = listener
        formSpinner.onItemSelectedListener = listener
        strengthSpinner.onItemSelectedListener = listener

        return searchView
    }

    override fun onStart() {
        super.onStart()
        searchView.findViewById<EditText>(R.id.searchbox)?.clearFocus()
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity?)?.hideKeyboard(searchView.windowToken)
    }

    private fun updateSpinners() {
        val brandSpinner = searchView.findViewById<Spinner>(R.id.brand)
        val formSpinner = searchView.findViewById<Spinner>(R.id.form)
        val strengthSpinner = searchView.findViewById<Spinner>(R.id.strength)
        val selectedBrand = brandSpinner.selectedItem?.toString()
        var selectedForm = formSpinner.selectedItem?.toString()
        var selectedStrength = strengthSpinner.selectedItem?.toString()

        choices.variations[selectedBrand]?.also { brand ->
            brand.forms?.also { forms ->
                if (!forms.containsKey(selectedForm)) {
                    selectedForm = brand.defaultForm.toString()
                }

                forms[selectedForm]?.also { form ->
                    form.strengths?.also { strengths ->
                        if (!strengths.containsKey(selectedStrength)) {
                            selectedStrength = form.defaultStrength.toString()
                        }

                        strengths[selectedStrength]?.quantities?.also { quantities ->
                            selectableForms.clear()
                            selectableStrengths.clear()
                            selectableQuantities.clear()
                            selectableForms.addAll(forms.keys)
                            selectableStrengths.addAll(strengths.keys)
                            selectableQuantities.addAll(quantities.keys)
                            selectableQuantities.add(getString(R.string.custom))
                            formAdapter.notifyDataSetChanged()
                            strengthAdapter.notifyDataSetChanged()
                            quantityAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun submit() {
        if (!searchParameters.isValid()) {
            return
        }

        searchView.findViewById<Button>(R.id.submit).isEnabled = false
        val selectedPharmacy = pharmacySpinner.selectedItem as String?

        if (!selectedPharmacy.isNullOrBlank() &&
            selectedPharmacy != getString(R.string.any_pharmacy)
        ) {
            searchParameters.pharmacy = selectedPharmacy
        } else {
            searchParameters.pharmacy = null
        }

        getDrug()?.also { drug ->
            startSearching(drug)
        }
    }

    private fun startSearching(drug: Drug) {
        val bundle = bundleOf(
            "drug" to drug,
            "params" to searchParameters
        )

        (activity as MainActivity?)?.hideKeyboard(searchView.windowToken)
        savePreferences()
        searchView.findViewById<Button>(R.id.submit).isEnabled = true

        Navigation
            .findNavController(searchView)
            .navigate(R.id.action_search_options_to_loading_screen, bundle)
    }

    private fun quantityChanged(s: CharSequence? = null, isCustom: Boolean = true) {
        searchView.findViewById<Button>(R.id.submit)?.isEnabled = !isCustom ||
                (s ?: searchView.findViewById<EditText>(R.id.custom_quantity).text).isNotBlank()

        if (isCustom) {
            searchView.findViewById<EditText>(R.id.custom_quantity).requestFocus()
        }
    }

    private fun locationChanged(s: CharSequence?) {
        val isValidZip = s?.length == 5
        searchView.findViewById<Button>(R.id.submit)?.isEnabled = isValidZip

        if (!isValidZip) {
            return
        }

        searchView.context?.also {
            (activity as MainActivity?)?.addToRequestQueue(object : StringRequest(
                Method.GET,
                "https://www.needymeds.org/dp_pharma.taf?zip=$s",
                { response ->
                    val arr = Gson().fromJson(response, Array<Pharmacy>::class.java)
                    val pharmacySpinner = searchView.findViewById<Spinner>(R.id.pharmacy)

                    pharmacies = arr.map {
                        Regex("[^A-Za-z ]").replace(it.pharmacyName, "").trim()
                    }.distinct()

                    arr.first().also {
                        searchParameters.lat = it.latitude
                        searchParameters.long = it.longitude
                    }

                    if (searchView.context != null) {
                        try {
                            val adapter = ArrayAdapter(
                                searchView.context,
                                R.layout.spinner_list,
                                listOf(getString(R.string.any_pharmacy)) + pharmacies.sorted()
                            )

                            adapter.setDropDownViewResource(R.layout.spinner)
                            pharmacySpinner?.adapter = adapter
                        } catch (e: Exception) {

                        }
                    }
                }, null) {
            }
            )
        }
    }

    private fun getTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    if (s?.length == 5) {
                        searchParameters.zip = s.toString()
                    }

                    locationChanged(s)
                } catch (e: IllegalStateException) {
                }
            }
        }
    }

    private fun getQuantityWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    quantityChanged(s)
                } catch (e: IllegalStateException) {
                }
            }
        }
    }

    private fun getDrug(): Drug? {
        val brand = searchView.findViewById<Spinner>(R.id.brand)?.selectedItem
        val form = searchView.findViewById<Spinner>(R.id.form)?.selectedItem
        val strength = searchView.findViewById<Spinner>(R.id.strength)?.selectedItem
        var quantity = searchView.findViewById<Spinner>(R.id.quantity)?.selectedItem as String

        val isCustomQuantity = quantity == getString(R.string.custom)

        if (isCustomQuantity) {
            quantity = searchView.findViewById<EditText>(R.id.custom_quantity).text.toString()
        }

        choices.variations[brand]?.also { b ->
            b.forms?.get(form)?.also { f ->
                f.strengths?.get(strength)?.also { s ->
                    s.quantities?.get(quantity)?.also { q ->
                        return Drug(choices.brandName, choices.genericName, b, f, s, q)
                    }
                }
            }
        }

        return null
    }

    private fun savePreferences() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val gson = Gson()
        val recentDrugsKey = getString(R.string.recent_drugs_key)

        val recentDrugsStr = sharedPref.getString(
            recentDrugsKey, null
        ) ?: getString(R.string.default_drugs_json)

        var recentDrugs = gson.fromJson(recentDrugsStr, Array<String>::class.java).toList()
        val newValue = selection.name ?: return

        recentDrugs.filter {
            it != newValue
        }.also { list ->
            val maxSize = min(list.size, 9)
            recentDrugs = listOf(newValue) + list.subList(0, maxSize)
        }

        with (sharedPref.edit()) {
            val list = pharmacies.filter {
                it != getString(R.string.any_pharmacy)
            }

            putString(recentDrugsKey, gson.toJson(recentDrugs))
            putString(getString(R.string.default_zip_key), searchParameters.zip)
            putString(getString(R.string.default_lat_key), searchParameters.lat)
            putString(getString(R.string.default_long_key), searchParameters.long)
            putString(getString(R.string.default_pharmacy_key), searchParameters.pharmacy)
            putStringSet(getString(R.string.default_pharmacies_key), list.toSet())
            apply()
        }
    }

    private fun loadPreferences() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return

        sharedPref.getString(getString(R.string.default_zip_key), null)?.also {
            searchParameters.zip = it
            searchView.findViewById<EditText>(R.id.zip).setText(it)
            searchView.findViewById<Button>(R.id.submit)?.isEnabled = true
        }

        sharedPref.getString(getString(R.string.default_lat_key), null)?.also {
            searchParameters.lat = it
        }

        sharedPref.getString(getString(R.string.default_long_key), null)?.also {
            searchParameters.long = it
        }

        sharedPref.getStringSet(getString(R.string.default_pharmacies_key), setOf(" "))?.also { set ->
            pharmacies = set.toList()

            val adapter = ArrayAdapter(
                searchView.context,
                R.layout.spinner_list,
                listOf(getString(R.string.any_pharmacy)) + pharmacies
            )

            adapter.setDropDownViewResource(R.layout.spinner)
            pharmacySpinner.adapter = adapter

            sharedPref.getString(getString(R.string.default_pharmacy_key), null)?.also {
                pharmacySpinner.setSelection(set.indexOf(it) + 1)
            }
        }
    }

    class QuantitySelectedListener(private val searchScreen: SearchScreen):
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            parent?.adapter?.getItem(position)?.also {
                val quantity = it as String?
                searchScreen.updateSpinners()

                searchScreen.searchView.findViewById<EditText>(R.id.custom_quantity).visibility =
                    customVisibility(quantity)
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }

        private fun customVisibility(quantity: String?): Int {
            return if (isCustomQuantity(quantity)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        private fun isCustomQuantity(quantity: String?): Boolean {
            return quantity == searchScreen.getString(R.string.custom)
        }
    }
}
