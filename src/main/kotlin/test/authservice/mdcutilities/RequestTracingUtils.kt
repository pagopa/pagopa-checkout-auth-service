package test.authservice.mdcutilities

import reactor.util.context.Context

/**
 * Tracing utility class that contains helper methods to set authentication information, such as RPT
 * ID into reactor context
 */
class RequestTracingUtils {

    /**
     * Tracing keys enumerations that contains both context key and default value, set in case such
     * information are not taken from incoming request
     */
    enum class TracingEntry(val key: String, val defaultValue: String) {
        RPT_IDS("rptIds", "{rpt-id-not-found}"),
        API_ID("apiId", "{api-id-not-found}"),
    }

    /** Request tracing info */
    data class RequestTracingInfo(
        val rptIds: String,
        val requestMethod: String,
        val requestUriPath: String,
    )

    /**
     * Set request information into context taking values from the input RequestTracingInfo
     *
     * @param requestTracingInfo - the request information record from which retrieve values to be
     *   set into context
     * @param reactorContext - the current reactor context
     * @return the updated context
     */
    fun setRequestInfoIntoReactorContext(
        requestTracingInfo: RequestTracingInfo,
        reactorContext: Context,
    ): Context {
        // starting context
        var context = reactorContext

        // add RPT IDS
        context =
            putInReactorContextIfSetToDefault(
                TracingEntry.RPT_IDS,
                requestTracingInfo.rptIds,
                context,
            )

        // add API ID
        context =
            putInReactorContextIfSetToDefault(
                TracingEntry.API_ID,
                "API-ID-${requestTracingInfo.requestMethod}-${requestTracingInfo.requestUriPath}",
                context,
            )

        return context
    }

    /**
     * Put value into context if the actual context value is not present or set to it's default
     * value
     *
     * @param tracingEntry - the context entry to be value
     * @param valueToSet - the value to set
     * @param reactorContext - the current reactor context
     * @return the updated context
     */
    private fun putInReactorContextIfSetToDefault(
        tracingEntry: TracingEntry,
        valueToSet: String,
        reactorContext: Context,
    ): Context {
        // get the current value or the default if not present
        val currentValue = reactorContext.getOrDefault(tracingEntry.key, tracingEntry.defaultValue)

        // update only if the current value is the default one
        return if (currentValue == tracingEntry.defaultValue) {
            reactorContext.put(tracingEntry.key, valueToSet)
        } else {
            // return the unchanged context if the value is already set
            reactorContext
        }
    }
}
