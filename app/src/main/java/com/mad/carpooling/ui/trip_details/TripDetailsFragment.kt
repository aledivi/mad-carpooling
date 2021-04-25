package com.mad.carpooling.ui.trip_details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mad.carpooling.R
import com.mad.carpooling.TripUtil


class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private lateinit var tripDetailsViewModel: TripDetailsViewModel
    private lateinit var trip: TripUtil.Trip

    private lateinit var tvDepartureLocation: TextView
    private lateinit var tvDepartureDate: TextView
    private lateinit var tvDepartureTime: TextView
    private lateinit var tvArrivalLocation: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvSeats: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var ibtnChattiness: ImageButton
    private lateinit var ibtnSmoking: ImageButton
    private lateinit var ibtnPets: ImageButton
    private lateinit var ibtnMusic: ImageButton
    private var chattiness = false
    private var smoking = false
    private var pets = false
    private var music = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        tvDepartureLocation = view.findViewById(R.id.tv_tripDetails_departureLocation)
        tvDepartureDate = view.findViewById(R.id.tv_tripDetails_departureDate)
        tvDepartureTime = view.findViewById(R.id.tv_tripDetails_departureTime)
        tvArrivalLocation = view.findViewById(R.id.tv_tripDetails_arrivalLocation)
        tvDuration = view.findViewById(R.id.tv_tripDetails_duration)
        tvSeats = view.findViewById(R.id.tv_tripDetails_seats)
        tvPrice = view.findViewById(R.id.tv_tripDetails_price)
        tvDescription = view.findViewById(R.id.tv_tripDetails_description_text)
        ibtnChattiness = view.findViewById(R.id.btn_tripDetails_chattiness)
        ibtnSmoking = view.findViewById(R.id.btn_tripDetails_smoking)
        ibtnPets = view.findViewById(R.id.btn_tripDetails_pets)
        ibtnMusic = view.findViewById(R.id.btn_tripDetails_music)

        initTripDetails()

        val btnProfile = view.findViewById<Button>(R.id.btn_tripDetails_showProfile)
        btnProfile.setOnClickListener {
            // TODO: pass correct information about profile
            findNavController().navigate(R.id.action_nav_trip_details_to_nav_show_profile)
        }
    }

    private fun initTripDetails() {
        val args: TripDetailsFragmentArgs by navArgs()
        val tripId = args.id
        val bundle = args.stops
        val stops =
            bundle?.getSerializable("stops") as HashMap<Int, String>

        trip = TripUtil.Trip(
            args.id,
            "args.nickname",
            args.departure,
            args.arrival,
            args.duration,
            args.depDate,
            args.depTime,
            args.seats,
            args.price,
            args.chattiness,
            args.smoking,
            args.pets,
            args.music,
            args.description,
            stops
        )

        tvDepartureLocation.text = trip.departure
        tvArrivalLocation.text = trip.arrival
        tvDepartureDate.text = trip.depDate
        tvDepartureTime.text = trip.depTime
        tvDuration.text = trip.duration
        tvSeats.text = trip.seats.toString()
        tvPrice.text = trip.price.toString()
        tvDescription.text = trip.description
        chattiness = changeStatePreference(trip.chattiness, ibtnChattiness)
        smoking = changeStatePreference(trip.smoking, ibtnSmoking)
        pets = changeStatePreference(trip.pets, ibtnPets)
        music = changeStatePreference(trip.music, ibtnMusic)
    }

    private fun editTrip() {

        // TODO: enable button only if matching profile
        val bundle = Bundle()
        bundle.putSerializable("stops", trip.stops)
        val action = TripDetailsFragmentDirections.actionNavTripDetailsToNavTripEdit(
            trip.id,
            trip.departure,
            trip.arrival,
            trip.duration,
            trip.price,
            trip.seats,
            trip.depDate,
            trip.depTime,
            trip.chattiness,
            trip.smoking,
            trip.pets,
            trip.music,
            trip.description,
            bundle,
            false
        )

        findNavController().navigate(action)
    }

    private fun changeStatePreference(state: Boolean, ibtn: ImageButton) : Boolean{
        val typedValue = TypedValue()
        val theme = requireContext().theme
        var color = 0

        if(state){
            theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
            color = typedValue.data
        }
        else{
            theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
            color = typedValue.data //2298478592.toInt()
        }
        ibtn.isSelected = state
        ibtn.setColorFilter(color)
        return state
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.edit_trip -> {
                editTrip()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trip_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
}