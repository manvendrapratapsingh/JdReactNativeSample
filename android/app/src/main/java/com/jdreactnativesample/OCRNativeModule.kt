package com.jdreactnativesample

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import java.lang.ref.WeakReference

@ReactModule(name = OCRNativeModule.NAME)
class OCRNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "OCRNative"
        private const val TAG = "OCRNativeModule"
        private const val DOCUMENT_TYPE_CHEQUE = "cheque"
        private const val DOCUMENT_TYPE_ENACH = "enach"

        // Keep a weak reference so MainActivity can forward onNewIntent callbacks
        private var instanceRef: WeakReference<OCRNativeModule>? = null

        fun onNewIntentFromActivity(intent: Intent?) {
            // No longer needed for internal processing, but keeping for compatibility
        }
    }

    private var currentPromise: Promise? = null
    private var currentDocumentType: String? = null
    private var activityListener: ActivityEventListener? = null

    override fun getName(): String = NAME

    init {
        setupActivityListener()
        instanceRef = WeakReference(this)
    }

    @ReactMethod
    fun launchOCRApp(documentType: String, promise: Promise) {
        // This method now launches camera capture internally
        launchCameraForOCR(documentType, promise)
    }

    @ReactMethod
    fun launchCameraForOCR(documentType: String, promise: Promise) {
        try {
            Log.d(TAG, "=== Starting JustdialOCR Internal Camera ===")
            Log.d(TAG, "Document type: $documentType")
            
            val currentActivity = reactApplicationContext.currentActivity
            if (currentActivity == null) {
                Log.e(TAG, "Current activity is null")
                promise.reject("NO_ACTIVITY", "Current activity is null")
                return
            }

            // Store promise and document type for later use
            currentPromise = promise
            currentDocumentType = documentType
            
            // Launch JustdialOCR's MainActivityCamera internally
            val ocrCameraIntent = Intent(currentActivity, com.justdial.ocr.MainActivityCamera::class.java)
            ocrCameraIntent.putExtra("document_type", documentType)
            ocrCameraIntent.putExtra("internal_mode", true) // Flag to indicate internal usage
            
            Log.d(TAG, "Launching JustdialOCR camera activity...")
            (currentActivity as Activity).startActivityForResult(ocrCameraIntent, 1001)
            Log.d(TAG, "JustdialOCR camera launched successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception in launchCameraForOCR", e)
            currentPromise = null
            currentDocumentType = null
            promise.reject("LAUNCH_ERROR", "Failed to launch OCR camera: ${e.message}", e)
        }
    }

    private fun setupActivityListener() {
        activityListener = object : BaseActivityEventListener() {
            override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
                Log.d(TAG, "onActivityResult called: requestCode=$requestCode, resultCode=$resultCode")
                if (requestCode == 1001) {
                    handleCameraResult(resultCode, data)
                } else {
                    Log.d(TAG, "Ignoring result - wrong request code")
                }
            }
        }
        reactApplicationContext.addActivityEventListener(activityListener!!)
        Log.d(TAG, "Activity listener registered for camera results")
    }

    @ReactMethod
    fun processDocument(documentType: String, options: ReadableMap, promise: Promise) {
        // Direct launch of JustdialOCR camera - no separate image processing needed
        launchCameraForOCR(documentType, promise)
    }

    // Legacy methods removed - no longer needed for internal processing

    @ReactMethod
    fun processCheque(promise: Promise) {
        launchCameraForOCR(DOCUMENT_TYPE_CHEQUE, promise)
    }

    @ReactMethod
    fun processENach(promise: Promise) {
        launchCameraForOCR(DOCUMENT_TYPE_ENACH, promise)
    }

    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "handleCameraResult: resultCode=$resultCode")
        
        val promise = currentPromise
        val documentType = currentDocumentType
        
        // Clear stored values
        currentPromise = null
        currentDocumentType = null

        if (promise == null) {
            Log.e(TAG, "No promise found to resolve!")
            return
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                // JustdialOCR returned successful result
                val jsonResult = data?.getStringExtra("result_json")
                Log.d(TAG, "JustdialOCR successful result: $jsonResult")
                
                if (jsonResult != null) {
                    Log.d(TAG, "Resolving promise with OCR result")
                    promise.resolve(jsonResult)
                } else {
                    Log.e(TAG, "No JSON result received from JustdialOCR")
                    promise.reject("NO_RESULT", "No OCR result received")
                }
            }
            Activity.RESULT_CANCELED -> {
                // JustdialOCR was cancelled or had error
                val errorMessage = data?.getStringExtra("result_error") ?: "OCR operation was cancelled"
                Log.d(TAG, "JustdialOCR cancelled or error: $errorMessage")
                promise.reject("CANCELLED", errorMessage)
            }
            else -> {
                Log.e(TAG, "Unknown result code from JustdialOCR: $resultCode")
                promise.reject("UNKNOWN_RESULT", "Unknown result code: $resultCode")
            }
        }
    }

    // Direct processing methods removed - now using JustdialOCR's complete flow

    // Activity result handling moved to handleCameraResult method above

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    // For backward compatibility with activities that don't use ActivityResultLauncher
    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN event emitters
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN event emitters
    }
}
