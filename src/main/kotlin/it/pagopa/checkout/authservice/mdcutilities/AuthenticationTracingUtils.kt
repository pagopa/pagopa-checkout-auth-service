package it.pagopa.checkout.authservice.mdcutilities

import reactor.util.context.Context

/**
 * Tracing utility class that contains helper methods to set authentication information, such as
 * client IP and RPT ID into reactor context
 */
class AuthenticationTracingUtils {

    /**
     * Tracing keys enumerations that contains both context key and default value, set in case such
     * information are not taken from incoming request
     */
    enum class TracingEntry(val key: String, val defaultValue: String) {
        RPT_ID("rptId", "{rptId-not-found}"),
        CLIENT_IP("clientIp", "{client-ip-not-found}"),
        API_ID("apiId", "{api-id-not-found}"),
    }

    /** Authentication information record */
    // in Kotlin the data class provides equals(), toString() etc.
    data class AuthenticationInfo(
        val rptId: String?,
        val clientIp: String,
        val requestMethod: String,
        val requestUriPath: String,
    )

    /**
     * Set authentication information into context taking information from the input
     * AuthenticationInfo
     *
     * @param authenticationInfo - the authentication information record from which retrieve
     *   information to be set into context
     * @param reactorContext - the current reactor context
     * @return the updated context
     */
    fun setAuthInfoIntoReactorContext(
        authenticationInfo: AuthenticationInfo,
        reactorContext: Context,
    ): Context {
        // starting context
        var context = reactorContext

        // add source IP to context
        context =
            putInReactorContextIfSetToDefault(
                TracingEntry.CLIENT_IP,
                authenticationInfo.clientIp,
                context,
            )

        // add RPT ID if it's not null
        authenticationInfo.rptId?.let {
            context = putInReactorContextIfSetToDefault(TracingEntry.RPT_ID, it, context)
        }

        // add API ID
        context =
            putInReactorContextIfSetToDefault(
                TracingEntry.API_ID,
                "API-ID-${authenticationInfo.requestMethod}-${authenticationInfo.requestUriPath}",
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
