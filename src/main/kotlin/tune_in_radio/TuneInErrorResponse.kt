package tune_in_radio

data class TuneInErrorResponse(val status: Int, val fault: String, val faultCode: String) {
    override fun toString(): String {
        return "{status:$status, fault:\"$fault\", fault_code:\"$faultCode\"}"
    }
}
