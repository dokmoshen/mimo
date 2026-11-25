package app.mimo.notes.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {

    private const val BIOMETRIC = BiometricManager.Authenticators.BIOMETRIC_WEAK
    private const val DEVICE = BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canUseBiometric(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun canUseDeviceCredential(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(DEVICE) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun canAuthenticate(context: Context): Boolean =
        canUseBiometric(context) || canUseDeviceCredential(context)

    /**
     * Shows the system prompt. When [pinFallbackLabel] is set and biometrics
     * are enrolled, the prompt gets a negative button that opens the app PIN.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit,
        pinFallbackLabel: String? = null,
        onPinFallback: (() -> Unit)? = null,
    ) {
        val biometricAvailable = canUseBiometric(activity)
        val allowed = when {
            biometricAvailable && pinFallbackLabel != null -> BIOMETRIC
            biometricAvailable -> BIOMETRIC or DEVICE
            else -> DEVICE
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && onPinFallback != null) {
                        onPinFallback()
                    } else {
                        onError(errorCode)
                    }
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(allowed)
            .setConfirmationRequired(false)
            .apply {
                if (biometricAvailable && pinFallbackLabel != null) {
                    setNegativeButtonText(pinFallbackLabel)
                }
            }
            .build()
        prompt.authenticate(info)
    }
}
