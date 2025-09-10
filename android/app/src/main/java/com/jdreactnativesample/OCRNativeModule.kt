package com.jdreactnativesample

import android.app.Activity
import android.content.Intent
import com.facebook.react.bridge.*
import java.lang.ref.WeakReference
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = OCRNativeModule.NAME)
class OCRNativeModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "OCRNative"
        private const val JUSTDIAL_OCR_PACKAGE = "com.justdial.ocr"
        private const val OCR_ACTION = "com.justdial.ocr.PROCESS_DOCUMENT"
        private const val DOCUMENT_TYPE_CHEQUE = "cheque"
        private const val DOCUMENT_TYPE_ENACH = "enach"

        // Keep a weak reference so MainActivity can forward onNewIntent callbacks
        private var instanceRef: WeakReference<OCRNativeModule>? = null

        fun onNewIntentFromActivity(intent: Intent?) {
            instanceRef?.get()?.handleIncomingIntent(intent)
        }
    }

    private var currentPromise: Promise? = null

    override fun getName(): String = NAME

    private var activityListener: ActivityEventListener? = null

    init {
        setupActivityListener()
        instanceRef = WeakReference(this)
    }

    private fun setupActivityListener() {
        activityListener = object : BaseActivityEventListener() {
            override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
                android.util.Log.d("OCRNativeModule", "onActivityResult called: requestCode=$requestCode, resultCode=$resultCode, data=$data")
                if (requestCode == 1001) {
                    android.util.Log.d("OCRNativeModule", "Processing OCR result")
                    handleActivityResult(resultCode, data)
                } else {
                    android.util.Log.d("OCRNativeModule", "Ignoring result - wrong request code")
                }
            }
        }
        reactApplicationContext.addActivityEventListener(activityListener!!)
        android.util.Log.d("OCRNativeModule", "Activity listener registered")
    }

    @ReactMethod
    fun launchOCRApp(documentType: String, promise: Promise) {
        try {
            android.util.Log.d("OCRNativeModule", "=== Starting launchOCRApp ===")
            android.util.Log.d("OCRNativeModule", "Document type: $documentType")
            android.util.Log.d("OCRNativeModule", "Target package: $JUSTDIAL_OCR_PACKAGE")

            val currentActivity = reactApplicationContext.currentActivity
            if (currentActivity == null) {
                android.util.Log.e("OCRNativeModule", "Current activity is null")
                promise.reject("NO_ACTIVITY", "Current activity is null")
                return
            }

            // Store the promise to resolve it later
            currentPromise = promise

            // Primary attempt: Launch OCRResultActivity directly using the documented action
            try {
                val actionIntent = Intent().apply {
                    action = OCR_ACTION
                    setPackage(JUSTDIAL_OCR_PACKAGE)
                    putExtra("document_type", documentType)
                }
                android.util.Log.d("OCRNativeModule", "Launching via action: $actionIntent")
                (currentActivity as Activity).startActivityForResult(actionIntent, 1001)
                android.util.Log.d("OCRNativeModule", "OCR app launched via action")
                return
            } catch (e: Exception) {
                android.util.Log.e("OCRNativeModule", "Failed to launch via action", e)
            }

            // Final fallback: explicit component if they expose it
            try {
                val explicit = Intent().apply {
                    setClassName(JUSTDIAL_OCR_PACKAGE, "com.justdial.ocr.OCRResultActivity")
                    putExtra("document_type", documentType)
                }
                android.util.Log.d("OCRNativeModule", "Launching explicit component: $explicit")
                (currentActivity as Activity).startActivityForResult(explicit, 1001)
                android.util.Log.d("OCRNativeModule", "OCR app launched via explicit component")
                return
            } catch (e2: Exception) {
                android.util.Log.e("OCRNativeModule", "Failed explicit component launch", e2)
                currentPromise = null
                promise.reject("LAUNCH_FAILED", "Failed to launch OCR app: ${e2.message}")
                return
            }
        
        } catch (e: Exception) {
            android.util.Log.e("OCRNativeModule", "Exception in launchOCRApp", e)
            promise.reject("LAUNCH_ERROR", "Failed to launch OCR app: ${e.message}", e)
        }
    }

    @ReactMethod
    fun processDocument(documentType: String, options: ReadableMap, promise: Promise) {
        try {
            val currentActivity = reactApplicationContext.currentActivity
            if (currentActivity == null) {
                promise.reject("NO_ACTIVITY", "Current activity is null")
                return
            }

            currentPromise = promise

            val intent = Intent().apply {
                action = OCR_ACTION
                setPackage(JUSTDIAL_OCR_PACKAGE)
                putExtra("document_type", documentType)
                addCategory(Intent.CATEGORY_DEFAULT)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Pass generic extras map directly to the target app (type-preserving)
            if (options.hasKey("extras") && options.getType("extras") == ReadableType.Map) {
                val extras = options.getMap("extras")
                if (extras != null) {
                    putExtrasFromReadableMap(intent, extras)
                }
            }

            // If caller provides an imageUri, attach it properly and grant permission
            if (options.hasKey("imageUri") && options.getType("imageUri") == ReadableType.String) {
                val uriString = options.getString("imageUri")
                if (!uriString.isNullOrEmpty()) {
                    val uri = android.net.Uri.parse(uriString)
                    intent.setDataAndType(uri, "image/*")
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    try {
                        val clip = android.content.ClipData.newUri(reactApplicationContext.contentResolver, "image", uri)
                        intent.clipData = clip
                    } catch (_: Exception) {}
                }
            }

            // Optional override for action and package if needed
            if (options.hasKey("action") && options.getType("action") == ReadableType.String) {
                intent.action = options.getString("action")
            }
            if (options.hasKey("package") && options.getType("package") == ReadableType.String) {
                intent.setPackage(options.getString("package"))
            }

            try {
                (currentActivity as Activity).startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                currentPromise = null
                promise.reject("LAUNCH_FAILED", "Failed to launch OCR app: ${e.message}")
                return
            }
        } catch (e: Exception) {
            currentPromise = null
            promise.reject("LAUNCH_ERROR", "Failed to launch OCR app: ${e.message}", e)
        }
    }

    private fun putExtrasFromReadableMap(intent: Intent, map: ReadableMap) {
        val iterator = map.keySetIterator()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (map.getType(key)) {
                ReadableType.Boolean -> intent.putExtra(key, map.getBoolean(key))
                ReadableType.Number -> {
                    val num = map.getDouble(key)
                    if (num % 1.0 == 0.0) intent.putExtra(key, num.toLong()) else intent.putExtra(key, num)
                }
                ReadableType.String -> intent.putExtra(key, map.getString(key))
                ReadableType.Map -> {
                    // Flatten nested maps into Bundle
                    val child = map.getMap(key)
                    if (child != null) intent.putExtra(key, bundleFromReadableMap(child))
                }
                ReadableType.Array -> {
                    val arr = map.getArray(key)
                    if (arr != null) intent.putExtra(key, arrayFromReadableArray(arr))
                }
                ReadableType.Null -> intent.putExtra(key, null as String?)
            }
        }
    }

    private fun bundleFromReadableMap(map: ReadableMap): android.os.Bundle {
        val bundle = android.os.Bundle()
        val iterator = map.keySetIterator()
        while (iterator.hasNextKey()) {
            val key = iterator.nextKey()
            when (map.getType(key)) {
                ReadableType.Boolean -> bundle.putBoolean(key, map.getBoolean(key))
                ReadableType.Number -> {
                    val num = map.getDouble(key)
                    if (num % 1.0 == 0.0) bundle.putLong(key, num.toLong()) else bundle.putDouble(key, num)
                }
                ReadableType.String -> bundle.putString(key, map.getString(key))
                ReadableType.Map -> bundle.putBundle(key, bundleFromReadableMap(map.getMap(key)!!))
                ReadableType.Array -> bundle.putParcelableArrayList(key, arrayFromReadableArray(map.getArray(key)!!))
                ReadableType.Null -> bundle.putString(key, null)
            }
        }
        return bundle
    }

    private fun arrayFromReadableArray(array: ReadableArray): java.util.ArrayList<android.os.Parcelable> {
        val list = java.util.ArrayList<android.os.Parcelable>()
        for (i in 0 until array.size()) {
            when (array.getType(i)) {
                ReadableType.Boolean -> list.add(android.os.Bundle().apply { putBoolean("value", array.getBoolean(i)) })
                ReadableType.Number -> {
                    val num = array.getDouble(i)
                    list.add(android.os.Bundle().apply { if (num % 1.0 == 0.0) putLong("value", num.toLong()) else putDouble("value", num) })
                }
                ReadableType.String -> list.add(android.os.Bundle().apply { putString("value", array.getString(i)) })
                ReadableType.Map -> list.add(bundleFromReadableMap(array.getMap(i)!!))
                ReadableType.Array -> list.add(android.os.Bundle().apply { putParcelableArrayList("value", arrayFromReadableArray(array.getArray(i)!!)) })
                ReadableType.Null -> list.add(android.os.Bundle().apply { putString("value", null) })
            }
        }
        return list
    }

    @ReactMethod
    fun processCheque(promise: Promise) {
        launchOCRApp(DOCUMENT_TYPE_CHEQUE, promise)
    }

    @ReactMethod
    fun processENach(promise: Promise) {
        launchOCRApp(DOCUMENT_TYPE_ENACH, promise)
    }

    private fun handleActivityResult(resultCode: Int, data: Intent?) {
        android.util.Log.d("OCRNativeModule", "handleActivityResult: resultCode=$resultCode")
        
        val promise = currentPromise
        currentPromise = null

        if (promise == null) {
            android.util.Log.e("OCRNativeModule", "No promise found to resolve!")
            return
        }

        when (resultCode) {
            Activity.RESULT_OK -> {
                val jsonResult = data?.getStringExtra("result_json")
                android.util.Log.d("OCRNativeModule", "RESULT_OK: jsonResult=$jsonResult")
                
                if (jsonResult != null) {
                    android.util.Log.d("OCRNativeModule", "Resolving promise with JSON result")
                    promise.resolve(jsonResult)
                } else {
                    android.util.Log.e("OCRNativeModule", "JSON result is null")
                    // Check all extras in the intent
                    data?.extras?.let { extras ->
                        for (key in extras.keySet()) {
                            android.util.Log.d("OCRNativeModule", "Intent extra: $key = ${extras.get(key)}")
                        }
                    }
                    promise.reject("NO_RESULT", "No JSON result received")
                }
            }
            Activity.RESULT_CANCELED -> {
                val errorMessage = data?.getStringExtra("result_error") ?: "Operation was cancelled"
                android.util.Log.d("OCRNativeModule", "RESULT_CANCELED: errorMessage=$errorMessage")
                promise.reject("CANCELLED", errorMessage)
            }
            else -> {
                android.util.Log.e("OCRNativeModule", "Unknown result code: $resultCode")
                promise.reject("UNKNOWN_RESULT", "Unknown result code: $resultCode")
            }
        }
    }

    // Handle results that arrive via onNewIntent when the OCR app calls back into our activity
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        try {
            val extras = intent.extras
            if (extras == null) return

            val jsonResult = intent.getStringExtra("result_json")
            val errorMessage = intent.getStringExtra("result_error")

            if (jsonResult != null) {
                android.util.Log.d("OCRNativeModule", "onNewIntent: received result_json")
                currentPromise?.let {
                    currentPromise = null
                    it.resolve(jsonResult)
                    return
                }
                // Fallback: emit event if no pending promise
                sendEvent("OCRResult", Arguments.createMap().apply { putString("result_json", jsonResult) })
            } else if (errorMessage != null) {
                android.util.Log.d("OCRNativeModule", "onNewIntent: received result_error")
                currentPromise?.let {
                    currentPromise = null
                    it.reject("CANCELLED", errorMessage)
                    return
                }
                sendEvent("OCRResultError", Arguments.createMap().apply { putString("error", errorMessage) })
            }
        } catch (e: Exception) {
            android.util.Log.e("OCRNativeModule", "Error handling incoming intent", e)
        }
    }

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
