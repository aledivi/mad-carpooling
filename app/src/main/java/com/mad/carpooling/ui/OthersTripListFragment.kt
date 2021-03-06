package com.mad.carpooling.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.model.Trip
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ceil


private lateinit var auth: FirebaseAuth

class OthersTripListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private lateinit var tripMap: HashMap<String, Trip>
    private lateinit var optionsMenu: Menu
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var sliderPrice: RangeSlider
    private lateinit var tvSliderPrice: TextView
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnClear: MaterialButton
    private lateinit var etSearchDeparture: EditText
    private lateinit var etSearchArrival: EditText
    private lateinit var etSearchDate: EditText
    private lateinit var etSearchTime: EditText
    private lateinit var chipSearchResults: Chip
    private lateinit var emptyView: TextView
    private var searchIsValid: Boolean = false
    private val sharedViewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /* viewModelFactory = SharedViewModelFactory(TripRepository())
         model = ViewModelProvider(this, viewModelFactory)
             .get(SharedViewModel::class.java)*/

        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        chipSearchResults = view.findViewById(R.id.chip_search_results)
        //swipeContainer = view.findViewById(R.id.swipeContainer)
        //swipeContainer.isEnabled = true
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false //prevent toolbar to expand on scroll
        emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        val tripAdapter = OthersTripAdapter(sharedViewModel)
        rv.adapter = tripAdapter

        initFab(view)

        //initSwipeRefresh(swipeContainer, tripAdapter)

        sharedViewModel.getCurrentUserData().observe(viewLifecycleOwner, Observer { currentUser ->
            // update after login/logout
            sharedViewModel.getOthersTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
                // Update the UI
                emptyView.isVisible = newTripsMap.isEmpty()
                tripAdapter.submitList(newTripsMap.values.toList())
                tripMap = newTripsMap
                initSearch(newTripsMap, tripAdapter)
            })
        })

    }

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    private fun initSwipeRefresh(
        swipeContainer: SwipeRefreshLayout?,
        tripAdapter: OthersTripAdapter
    ) {
        swipeContainer?.setOnRefreshListener {
            tripMap = sharedViewModel.getOthersTrips().value!!
            emptyView.isVisible = tripMap.isEmpty()
            tripAdapter.submitList(tripMap.values.toList())
            initSearch(tripMap, tripAdapter)
            swipeContainer.isRefreshing = false
        }
    }


    private fun initFab(view: View) {
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_triplist)

        auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser == null) {
            fab.hide()
        } else {
            fab.show()
            fab.setOnClickListener {
                val action = OthersTripListFragmentDirections.actionNavOthersTripListToNavTripEdit(
                    "",
                    isNew = true
                )
                val navController = Navigation.findNavController(view)
                navController.navigate(action) //a new one from scratch
            }
            rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0 && fab.visibility == View.VISIBLE) {
                        fab.hide()
                    } else if (dy < 0 && fab.visibility != View.VISIBLE) {
                        fab.show()
                    }
                }
            })
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_others_trip, menu)
        optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                val isExpanded = (appBarLayout.height - appBarLayout.bottom) != 0
                appBarLayout.setExpanded(isExpanded, true)
                if (isExpanded) {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)
                    item.iconTintList = ColorStateList.valueOf(Color.WHITE)
                } else {
                    item.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
                    item.iconTintList = ColorStateList.valueOf(Color.WHITE)
                }
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun initSearch(tripsMap: HashMap<String, Trip>, tripAdapter: OthersTripAdapter) {
        sliderPrice = (activity as MainActivity).findViewById(R.id.range_slider)
        tvSliderPrice = (activity as MainActivity).findViewById(R.id.tv_price_slider)
        btnSearch = (activity as MainActivity).findViewById(R.id.btn_search)
        btnClear = (activity as MainActivity).findViewById(R.id.btn_clear)
        etSearchDeparture = (activity as MainActivity).findViewById(R.id.et_search_departure)
        etSearchArrival = (activity as MainActivity).findViewById(R.id.et_search_arrival)
        etSearchDate = (activity as MainActivity).findViewById(R.id.et_search_date)
        etSearchTime = (activity as MainActivity).findViewById(R.id.et_search_time)

        etSearchDate.setOnClickListener {
            showDatePickerDialog()
        }

        etSearchTime.setOnClickListener {
            showTimePickerDialog()
        }


        sliderPrice.valueFrom = 0f

        val maxPrice = tripsMap.maxByOrNull { it.value.price }?.value?.price ?: 1f
        sliderPrice.valueTo = (5 * (ceil(abs(maxPrice / 5).toDouble()))).toFloat()
        sliderPrice.values = mutableListOf(sliderPrice.valueFrom, sliderPrice.valueTo)
        tvSliderPrice.text =
            "${("%.2f".format(sliderPrice.valueFrom))} - ${("%.2f".format(sliderPrice.valueTo))} ???"
        sliderPrice.addOnChangeListener { slider, value, fromUser ->
            tvSliderPrice.text =
                "${("%.2f".format(slider.values[0]))} - ${("%.2f".format(slider.values[1]))} ???"
            btnSearch.isEnabled =
                slider.values[0] != slider.valueFrom || slider.values[1] != slider.valueTo || searchIsValid
        }
        appBarLayout = (activity as MainActivity).findViewById(R.id.appbar_layout) as AppBarLayout

        findNavController().addOnDestinationChangedListener { _, _, _ ->
            appBarLayout.setExpanded(
                false
            )

        }

        validateSearch()
        btnSearch.setOnClickListener {
            tripAdapter.filterTrips(
                etSearchDeparture.text?.trim().toString(),
                etSearchArrival.text?.trim().toString(),
                etSearchDate.text?.trim().toString(),
                etSearchTime.text?.trim().toString(),
                sliderPrice.values,
                tripsMap.values
            )
            val searchItem = optionsMenu.findItem(R.id.action_search)
            searchItem.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_search)
            searchItem.iconTintList = ColorStateList.valueOf(Color.WHITE)
            appBarLayout.setExpanded(false)
            rv.setPadding(0, (40f * Resources.getSystem().displayMetrics.density).toInt(), 0, 0)

            chipSearchResults.visibility = View.VISIBLE
            chipSearchResults.setOnCloseIconClickListener {
                clearSearch(tripAdapter)
                chipSearchResults.visibility = View.GONE
                rv.setPadding(0, 0, 0, 0)
            }
        }
        btnClear.setOnClickListener {
            clearSearch(tripAdapter)
        }
    }

    private fun clearSearch(tripAdapter: OthersTripAdapter) {
        etSearchDeparture.text?.clear()
        etSearchArrival.text?.clear()
        etSearchDate.text?.clear()
        etSearchTime.text?.clear()
        sliderPrice.values = mutableListOf(sliderPrice.valueFrom, sliderPrice.valueTo)
        tripAdapter.submitList(tripMap.values.toList())
    }

    private fun validateSearch() {

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnSearch.isEnabled =
                    etSearchDeparture.text.trim().isNotEmpty() || etSearchArrival.text.trim()
                        .isNotEmpty() || etSearchDate.text.trim()
                        .isNotEmpty() || etSearchTime.text.trim()
                        .isNotEmpty()
                searchIsValid =
                    etSearchDeparture.text.trim().isNotEmpty() || etSearchArrival.text.trim()
                        .isNotEmpty() || etSearchDate.text.trim()
                        .isNotEmpty() || etSearchTime.text.trim()
                        .isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        }

        etSearchDeparture.addTextChangedListener(textWatcher)
        etSearchArrival.addTextChangedListener(textWatcher)
        etSearchDate.addTextChangedListener(textWatcher)
        etSearchTime.addTextChangedListener(textWatcher)
    }

    class OthersTripAdapter(private val sharedViewModel: SharedViewModel) :
        ListAdapter<Trip, OthersTripAdapter.TripViewHolder>(TaskDiffCallback()) {

        class TaskDiffCallback : DiffUtil.ItemCallback<Trip>() {

            override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem == newItem
            }
        }

        class TripViewHolder(v: View, sharedViewModel: Any?) : RecyclerView.ViewHolder(v) {

            val tripRL: RelativeLayout = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnStar: CheckBox = v.findViewById(R.id.trip_star)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val timestamp = v.findViewById<TextView>(R.id.trip_timestamp)
            private val price = v.findViewById<TextView>(R.id.trip_price)

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                auth = Firebase.auth
                val user = auth.currentUser
                location.text = "${trip.departure} - ${trip.arrival}"
                timestamp.text = (LocalDateTime.ofInstant(
                    trip.timestamp.toDate().toInstant(),
                    ZoneId.systemDefault()
                )).format(
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                )
                price.text = "Price: ${("%.2f".format(trip.price))} ???"
                if (trip.imageCarURL != "") {
                    Glide.with(this.itemView).load(trip.imageCarURL).into(ivCar)
                }
                // clear any listener to avoid recyclerview messing up with checkbox state,
                // treating the checkbox as a brand new one every time
                var auth = Firebase.auth
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    btnStar.visibility = View.GONE
                } else {
                    btnStar.setOnCheckedChangeListener(null)
                    btnStar.isChecked = trip.interestedPeople?.contains(user?.uid) == true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout, sharedViewModel)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            auth = Firebase.auth
            var auth = Firebase.auth
            val currentUser = auth.currentUser
            val user = auth.currentUser
            holder.bind(getItem(position))

            holder.tripRL.setOnClickListener {
                if (currentUser == null) {
                    Toast.makeText(
                        holder.itemView.context,
                        "You must be logged to see details",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val action =
                        OthersTripListFragmentDirections.actionNavOthersTripListToNavTripDetails(
                            getItem(position).id,
                        )
                    Navigation.findNavController(holder.tripRL).navigate(action)
                }
            }


            if (currentUser == null) {
                holder.btnStar.visibility = View.GONE
            } else {
                holder.btnStar.visibility = View.VISIBLE
                holder.btnStar.setOnCheckedChangeListener { it, isChecked ->
                    if (isChecked) {
                        sharedViewModel.addInterest(getItem(position).id,"interestedPeople","favTrips",user?.uid!!)
                    } else {
                        sharedViewModel.removeInterest(getItem(position).id,"interestedPeople","favTrips",user?.uid!!)
                        sharedViewModel.removeAccepted(getItem(position).id, "acceptedPeople", "seats", user.uid, 1)
                    }
                }
            }
        }

        fun filterTrips(
            departure: String,
            arrival: String,
            date: String,
            time: String,
            prices: MutableList<Float>,
            tripList: MutableCollection<Trip>
        ) {
            var formattedDate: String = ""
            var formattedTime: String = ""
            if (date != "") {
                formattedDate = (SimpleDateFormat.getDateInstance(
                    SimpleDateFormat.SHORT,
                    Locale.getDefault()
                )).format(
                    SimpleDateFormat.getDateInstance(
                        SimpleDateFormat.SHORT,
                        Locale.getDefault()
                    ).parse(date)!!
                )
            }
            if (time != "") {
                formattedTime = (SimpleDateFormat.getTimeInstance(
                    SimpleDateFormat.SHORT,
                    Locale.getDefault()
                )).format(
                    SimpleDateFormat.getTimeInstance(
                        SimpleDateFormat.SHORT,
                        Locale.getDefault()
                    ).parse(time)!!
                )
            }
            val resultList = ArrayList<Trip>()
            for (trip in tripList) {
                val tripDate = (SimpleDateFormat.getDateInstance(
                    SimpleDateFormat.SHORT,
                    Locale.getDefault()
                )).format(trip.timestamp.toDate())
                    .toString()
                val tripTime =
                    (SimpleDateFormat.getTimeInstance(
                        SimpleDateFormat.SHORT,
                        Locale.getDefault()
                    )).format(trip.timestamp.toDate())
                        .toString()

                if (trip.departure.lowercase(Locale.ROOT)
                        .contains(departure.lowercase(Locale.ROOT))
                    && trip.arrival.lowercase(Locale.ROOT)
                        .contains(arrival.lowercase(Locale.ROOT))
                    && trip.price >= prices[0] && trip.price <= prices[1]
                    && tripDate.contains(formattedDate)
                    && tripTime.contains(formattedTime) //TODO this seems useless, better use a range slider for time too
                ) {
                    resultList.add(trip)
                }
            }
            submitList(resultList)
        }


    }

    private fun showDatePickerDialog() {
        val dateFragment = DatePickerFragment(etSearchDate)
        dateFragment.show(requireActivity().supportFragmentManager, "datePicker")
    }

    private fun showTimePickerDialog() {
        val timeFragment = TimePickerFragment(etSearchTime)
        timeFragment.show(requireActivity().supportFragmentManager, "timePicker")
    }

}


