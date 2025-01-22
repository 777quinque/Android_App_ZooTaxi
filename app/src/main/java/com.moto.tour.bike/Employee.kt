package com.moto.tour.bike

data class Employee(
    val username: String,
    val latitude: Double,
    val longitude: Double,
    var distance: Double = 0.0
)