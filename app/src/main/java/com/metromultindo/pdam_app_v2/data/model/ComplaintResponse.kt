package com.metromultindo.pdam_app_v2.data.model

data class ComplaintResponse(
    val error: Boolean,
    val message_code: Int,
    val message_text: String,
    val complaint: ComplaintData?
)

data class ComplaintData(
    val id: Int,
    val number: String,
    val date: String,
    val name: String,
    val phone: String,
    val is_customer: Int,
    val is_customer_desc: String,
    val cust_code: String?,
    val image: String?,
    val latlong: String?,
    val location_status: String?
) {
    // Helper function to parse latitude and longitude from latlong string
    fun getLatitude(): Double? {
        return latlong?.split(",")?.get(0)?.toDoubleOrNull()
    }

    fun getLongitude(): Double? {
        return latlong?.split(",")?.get(1)?.toDoubleOrNull()
    }

    // Helper function to check if location data is available
    fun hasLocationData(): Boolean {
        return !latlong.isNullOrEmpty() && latlong.contains(",")
    }

    // Helper function to format location for display
    fun getFormattedLocation(): String {
        return if (hasLocationData()) {
            val lat = getLatitude()
            val lng = getLongitude()
            if (lat != null && lng != null) {
                String.format("%.6f, %.6f", lat, lng)
            } else {
                "Format lokasi tidak valid"
            }
        } else {
            "Lokasi tidak tersedia"
        }
    }
}