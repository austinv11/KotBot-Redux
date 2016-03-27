package kotbot.rest

/**
 * Represents filter types on requests
 */
enum class FilterType {
    BEFORE, AFTER
}

/**
 * Represents a filter response. If halt is true, then the response code and message parameters are taken into account
 * to halt the connection.
 */
data class FilterResponse(val halt: Boolean, val responseCode: Int = 200, val message: String? = null) {
    
} 
