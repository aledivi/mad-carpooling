package com.mad.carpooling.ui.trip_list

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import android.view.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.material.slider.RangeSlider
import com.mad.carpooling.MainActivity
import com.mad.carpooling.R
import com.mad.carpooling.data.Trip
import com.mad.carpooling.ui.SharedViewModel

private lateinit var auth: FirebaseAuth

class OthersTripListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var sliderPrice: RangeSlider
    private lateinit var tvSliderPrice: TextView
    private var tripMap: HashMap<String, Trip>? = null
    private val model: SharedViewModel by activityViewModels()

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sliderPrice = (activity as MainActivity).findViewById(R.id.range_slider)
        tvSliderPrice = (activity as MainActivity).findViewById(R.id.tv_price_slider)
        sliderPrice.values = mutableListOf(10f, 50f)
        sliderPrice.addOnChangeListener { slider, value, fromUser ->
            tvSliderPrice.text = "${slider.values[0]} - ${slider.values[1]} €"
        }
        appBarLayout = (activity as MainActivity).findViewById(R.id.appbar_layout) as AppBarLayout
        appBarLayout.setExpanded(false) //TODO disable expansion in other fragments
        model.getCurrentUser().observe(viewLifecycleOwner, Observer { currentUser ->
            // update after login/logout
            model.getOthersTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
                // Update the UI
                updateTripList(newTripsMap, view)
            })
        })

    }


    private fun updateTripList(tripsMap: HashMap<String, Trip>, view: View) {
        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false; //prevent toolbar to expand on scroll

        val tripAdapter = OthersTripAdapter(ArrayList((tripsMap.values)))
        rv.adapter = tripAdapter

        //TODO check on tripList size instead
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        if (tripAdapter.itemCount == 0) //from getItemCount
            emptyView.isVisible = true

        val fab = (activity as MainActivity).findViewById<FloatingActionButton>(R.id.fab)
        fab.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.ic_baseline_add))
        var navController: NavController?
        fab.setOnClickListener {
            val action = OthersTripListFragmentDirections.actionNavOthersTripListToNavTripEdit(
                "",
                isNew = true
            )
            navController = Navigation.findNavController(view)
            navController!!.navigate(action) //a new one from scratch
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


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        // optionsMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                appBarLayout.setExpanded((appBarLayout.height - appBarLayout.bottom) != 0, true)
                if ((appBarLayout.height - appBarLayout.bottom) == 0)
                    initSearch()
                true
            }
            else -> item.onNavDestinationSelected(findNavController()) || super.onOptionsItemSelected(
                item
            )
        }
    }

    private fun initSearch() {

    }

    class OthersTripAdapter(private val tripList: ArrayList<Trip>) :
        RecyclerView.Adapter<OthersTripAdapter.TripViewHolder>() {

        class TripViewHolder(v: View) : RecyclerView.ViewHolder(v) {

            val tripRL: RelativeLayout = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnStar: CheckBox = v.findViewById(R.id.trip_star)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val duration = v.findViewById<TextView>(R.id.trip_duration)
            private val price = v.findViewById<TextView>(R.id.trip_price)

            private var navController: NavController? = null

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                auth = Firebase.auth
                val user = auth.currentUser
                location.text = "${trip.departure} - ${trip.arrival}"
                duration.text = "Duration: ${trip.duration}"
                price.text = "Price: ${("%.2f".format(trip.price))} €"
                if (trip.imageCarURL != "") {
                    Glide.with(this.itemView).load(trip.imageCarURL).into(ivCar)
                }
                btnStar.isChecked = trip.interestedPeople?.contains(user?.uid) == true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            val trip = tripList[position]
            val db = Firebase.firestore
            auth = Firebase.auth
            val user = auth.currentUser
            trip.let { holder.bind(it) }
            holder.tripRL.setOnClickListener {
                val action =
                    OthersTripListFragmentDirections.actionNavOthersTripListToNavTripDetails(
                        trip.id,
                    )
                Navigation.findNavController(holder.tripRL).navigate(action)
            }
            holder.btnStar.visibility = View.VISIBLE
            holder.btnStar.setOnClickListener {
                val action =
                    OthersTripListFragmentDirections.actionNavOthersTripListToNavTripDetails(
                        trip.id,
                    )
            holder.btnStar.setOnCheckedChangeListener { it, isChecked ->
                if (isChecked) {
                    db.collection("trips").document(trip.id).update(
                        "interestedPeople", FieldValue.arrayUnion(user.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(user?.uid!!).update(
                            "favTrips", FieldValue.arrayUnion(trip.id)
                        )
                    }
                } else {
                    db.collection("trips").document(trip.id).update(
                        "interestedPeople", FieldValue.arrayRemove(user.uid)
                    ).addOnSuccessListener {
                        db.collection("users").document(user?.uid!!).update(
                            "favTrips", FieldValue.arrayRemove(trip.id)
                        )
                    }
                }
            }

        }

        override fun getItemCount(): Int {
            return tripList.size
        }

    }
}

