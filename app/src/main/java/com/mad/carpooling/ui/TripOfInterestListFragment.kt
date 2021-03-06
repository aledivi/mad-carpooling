package com.mad.carpooling.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mad.carpooling.R
import com.mad.carpooling.model.Trip
import com.mad.carpooling.repository.TripRepository
import com.mad.carpooling.repository.UserRepository
import com.mad.carpooling.viewmodel.SharedViewModel
import com.mad.carpooling.viewmodel.SharedViewModelFactory
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private lateinit var auth: FirebaseAuth

class TripOfInterestListFragment : Fragment(R.layout.fragment_trip_list) {
    private lateinit var rv: RecyclerView
    private var tripMap: HashMap<String, Trip>? = null

    // Use the 'by activityViewModels()' Kotlin property delegate
    // from the fragment-ktx artifact
    private val sharedViewModel: SharedViewModel by activityViewModels {
        SharedViewModelFactory(
            TripRepository(),
            UserRepository()
        )
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val emptyView = view.findViewById<TextView>(R.id.no_trips_available)
        rv = view.findViewById<RecyclerView>(R.id.triplist_rv)
        rv.layoutManager = LinearLayoutManager(context)
        rv.isNestedScrollingEnabled = false //prevent toolbar to expand on scroll
        val tripAdapter = InterestedTripAdapter(sharedViewModel)
        rv.adapter = tripAdapter

        //val swipeContainer = view.findViewById<SwipeRefreshLayout>(R.id.swipeContainer)
        //swipeContainer.isEnabled = false

        sharedViewModel.getInterestedTrips().observe(viewLifecycleOwner, Observer { newTripsMap ->
            // Update the UI
            emptyView.isVisible = newTripsMap.isEmpty()
            tripAdapter.submitList(newTripsMap.values.toList())
            val fab = view.findViewById<FloatingActionButton>(R.id.fab_triplist)
            fab.hide()
        })

    }

    class InterestedTripAdapter(private val sharedViewModel: SharedViewModel) :
        ListAdapter<Trip, InterestedTripAdapter.TripViewHolder>(TaskDiffCallback()) {

        class TaskDiffCallback : DiffUtil.ItemCallback<Trip>() {

            override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean {
                return oldItem == newItem
            }
        }

        class TripViewHolder(v: View, sharedViewModel: SharedViewModel) :
            RecyclerView.ViewHolder(v) {

            val tripRL: RelativeLayout = v.findViewById<RelativeLayout>(R.id.trip_rl)
            val btnStar: CheckBox = v.findViewById<CheckBox>(R.id.trip_star)
            private val ivCar = v.findViewById<ImageView>(R.id.trip_car)
            private val location = v.findViewById<TextView>(R.id.trip_from_to)
            private val timestamp = v.findViewById<TextView>(R.id.trip_timestamp)
            private val price = v.findViewById<TextView>(R.id.trip_price)
            private var navController: NavController? = null

            @SuppressLint("SetTextI18n")
            fun bind(trip: Trip) {
                auth = Firebase.auth
                val user = auth.currentUser
                if (!trip.visibility)
                    tripRL.alpha = 0.5f
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
                btnStar.setOnCheckedChangeListener(null)
                btnStar.isChecked = trip.interestedPeople?.contains(user?.uid) == true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val layout =
                LayoutInflater.from(parent.context).inflate(R.layout.triplist_layout, parent, false)
            return TripViewHolder(layout, sharedViewModel)
        }

        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            val trip = getItem(position)
            val auth = Firebase.auth
            val user = auth.currentUser

            trip.let { holder.bind(it) }
            holder.tripRL.setOnClickListener {
                val action =
                    TripOfInterestListFragmentDirections.actionNavInterestTripsToNavTripDetails(
                        trip.id,
                    )
                Navigation.findNavController(holder.tripRL).navigate(action)
            }
            holder.btnStar.visibility = View.VISIBLE
            holder.btnStar.setOnCheckedChangeListener { it, isChecked ->
                if (!isChecked) {
                    sharedViewModel.removeInterest(
                        getItem(holder.adapterPosition).id,
                        "interestedPeople",
                        "favTrips",
                        user?.uid!!
                    )
                    sharedViewModel.removeAccepted(
                        getItem(position).id,
                        "acceptedPeople",
                        "seats",
                        user.uid,
                        1
                    )
                }
            }
        }
    }
}


