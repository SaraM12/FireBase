package com.example.firebase

data class DayResult(
    val result: Result
) {
    data class Result(
        val from_time: String,
        val from_uv: Double,
        val to_time: String,
        val to_uv: Double
    )
}