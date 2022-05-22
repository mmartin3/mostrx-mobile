package com.mattmakesapps.mostrx

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.volley.toolbox.StringRequest
import com.cielyang.android.clearableedittext.ClearableEditText
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.util.*

class HomeScreen: Fragment() {
    private lateinit var homeView: View
    private lateinit var searchbox: ClearableEditText
    private lateinit var searchResults: ListView
    private var searchJob: Job? = null
    private var results: List<SearchResponse.SearchResult>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        homeView = inflater.inflate(R.layout.fragment_home_screen, container, false)
        searchbox = homeView.findViewById(R.id.searchbox)
        searchResults = homeView.findViewById(R.id.search_results)
        searchbox.addTextChangedListener(getTextWatcher())

        homeView.findViewById<ListView>(R.id.search_background).adapter = SearchResultAdapter(
            homeView.context,
            R.layout.search_result,
            List(10) { " " }
        )

        homeView.findViewById<ImageView>(R.id.home_pill).startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.rotate)
        )

        searchbox.setOnFocusChangeListener { _, hasFocus ->
            toggleSearchbar(hasFocus)

            if (hasFocus) {
                clearResults()
            } else {
                resetResults()
            }

            (activity as MainActivity?)?.setEnteringText(hasFocus)
        }

        searchResults.setOnItemClickListener { parent, _, position, _ ->
            selectResult(parent, position)
        }

        homeView.findViewById<Button>(R.id.cancel).setOnClickListener {
            searchbox.text?.clear()
            searchbox.clearFocus()
            (activity as MainActivity?)?.hideKeyboard(homeView.windowToken)
            resetResults()
        }

        return homeView
    }

    override fun onResume() {
        super.onResume()
        resetResults()
    }

    private fun toggleSearchbar(hasFocus: Boolean) {
        val logo = homeView.findViewById<ImageView>(R.id.logo)
        val cancelButton = homeView.findViewById<MaterialButton>(R.id.cancel)

        if (hasFocus) {
            logo.visibility = View.GONE
            cancelButton.visibility = View.VISIBLE
        } else {
            logo.visibility = View.VISIBLE
            cancelButton.visibility = View.GONE
        }
    }

    private fun drugRequest(selected: SearchResponse.SearchResult): StringRequest {
        val loadingOverlay = homeView.findViewById<View>(R.id.home_overlay)
        val mainActivity = activity as MainActivity?
        mainActivity?.hideKeyboard(homeView.windowToken)
        loadingOverlay.visibility = View.VISIBLE

        return object: StringRequest(
            Method.GET,
            "https://mostrx.app/api/v1/drug/variations/${selected.slug}",
            { response ->
                val variations = Gson().fromJson(response, DrugResponse::class.java)

                val bundle = bundleOf("drugData" to variations, "selection" to selected)
                homeView.findViewById<View>(R.id.home_overlay).visibility = View.GONE
                homeView.findViewById<TextView>(R.id.discontinued_message).visibility = View.GONE

                try {
                    Navigation
                        .findNavController(homeView)
                        .navigate(R.id.action_home_screen_to_search_options, bundle)
                } catch (e: Exception) {

                }
            }, { error ->
                loadingOverlay.visibility = View.GONE

                mainActivity?.volleyFailed(error, retry = {
                    mainActivity.addToRequestQueue(drugRequest(selected))
                })
            }) {
        }
    }

    private fun clearResults() {
        results = null

        searchResults.adapter = SearchResultAdapter(
            homeView.context,
            R.layout.search_result,
            listOf(" ")
        )
    }

    private fun resetResults() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        val key = getString(R.string.recent_drugs_key)

        val recentDrugsStr = sharedPref?.getString(
            key, null
        ) ?: getString(R.string.default_drugs_json)

        val recentDrugs = Gson().fromJson(recentDrugsStr, Array<String>::class.java)
        results = null

        searchResults.adapter = SearchResultAdapter(
            homeView.context,
            R.layout.search_result,
            recentDrugs.toList()
        )
    }

    private fun selectResult(parent: AdapterView<*>, position: Int) {
        val mainActivity = activity as MainActivity?
        val selected = parent.getItemAtPosition(position) as String

        if (homeView.context == null) {
            return
        }

        if (results == null) {
            val result = SearchResponse.SearchResult()
            result.name = selected
            result.slug = selected.lowercase().replace(' ', '-')
            mainActivity?.addToRequestQueue(drugRequest(result))
        }

        results?.forEach { result ->
            if (result.name.equals(selected)) {
                mainActivity?.addToRequestQueue(drugRequest(result))

                return@forEach
            }
        }

        searchbox.clearFocus()
    }

    private fun throttledSearch(s: CharSequence) {
        val term = Regex("[^A-Za-z0-9 ]").replace(s, "")

        searchJob = CoroutineScope(Dispatchers.Main).launch {
            delay(200)

            (activity as MainActivity?)?.addToRequestQueue(object: StringRequest(
                Method.GET,
                "https://mostrx.app/api/v1/drug/search/$term",
                { response ->
                    if (s.contentEquals(searchbox.text)) {
                        results = Gson().fromJson(response, SearchResponse::class.java).results

                        searchResults.adapter = SearchResultAdapter(
                            homeView.context,
                            R.layout.search_result,
                            results!!.map { it.name }
                        )
                    }
                }, {
                    //println(it)
                }) {
            })
        }
    }

    private fun getTextWatcher(): TextWatcher {
        return object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()

                if (s.isNullOrBlank()) {
                    resetResults()
                } else {
                    throttledSearch(s)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    class SearchResultAdapter<T>(context: Context,
                                 resource: Int,
                                 objects: List<T>): ArrayAdapter<T>(context, resource, objects) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            super.getView(position, convertView, parent).also {
                val rowColor = ResourcesCompat.getColor(
                    context.resources,
                    getRowColor(position),
                    null
                )

                it.setBackgroundColor(rowColor)

                return it
            }
        }

        private fun getRowColor(position: Int): Int {
            return if (position % 2 == 1) {
                R.color.pill_white
            } else {
                R.color.pill_gray
            }
        }
    }
}
