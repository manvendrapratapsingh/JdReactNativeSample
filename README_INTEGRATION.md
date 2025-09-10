# JustDial OCR React Native Integration

This React Native app demonstrates integration with the JustDial OCR Android app for processing bank cheques and e-NACH documents.

## Current Status: ✅ Partially Working

### What's Working:
- ✅ **App Launching**: React Native app successfully launches the JustdialOCR app
- ✅ **Intent Resolution**: Intent filter and package detection working
- ✅ **Camera Opening**: OCR app opens camera for document scanning
- ✅ **OCR Processing**: Image processing with Firebase AI completes successfully

### What's Not Working:
- ❌ **Result Return**: OCR results are not returning back to React Native app
- ❌ **onActivityResult**: Activity result callback not properly receiving data
- ❌ **JSON Display**: Processed JSON not displaying in React Native UI

## Architecture

- **JustdialOCR Project**: Native Android app with `OCRResultActivity` that processes documents and returns JSON results
- **JdReactNativeSample**: React Native app with native module that launches the OCR app and receives results

## Setup Instructions

### 1. JustdialOCR Project (Native Android App)
The following files were added/modified:

- **OCRResultActivity.kt**: New Activity that handles external app requests, processes OCR, and returns JSON results
- **AndroidManifest.xml**: Added intent filter for `com.justdial.ocr.PROCESS_DOCUMENT` action

### 2. React Native App Setup

```bash
cd JdReactNativeSample
npm install
```

The following files were created:
- **OCRNativeModule.kt**: Kotlin native module that launches the OCR app using `startActivityForResult`
- **OCRNativePackage.kt**: React Native package registration
- **App.tsx**: React component with UI to trigger OCR processing and display results

### 3. Running the Apps

1. **Build and install the JustdialOCR app first:**
   ```bash
   cd JustdialOCR
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Build and run the React Native app:**
   ```bash
   cd JdReactNativeSample
   npx react-native run-android
   ```

## How It Works (Current Implementation)

1. User taps "Process Cheque" or "Process E-NACH" in React Native app
2. Native module launches JustdialOCR app with specific intent action ✅
3. OCRResultActivity opens document scanner, processes the image using Firebase AI ✅
4. **[ISSUE]** JSON result should be returned to React Native app via `setResult` ❌
5. **[ISSUE]** React Native app should display the JSON result on screen ❌

## Integration Details

### Intent Communication
- **Action**: `com.justdial.ocr.PROCESS_DOCUMENT`
- **Package**: `com.justdial.ocr`
- **Extra**: `document_type` ("cheque" or "enach")

### Result Format (Expected)
Returns JSON string with OCR data including:
- Bank name, account number, IFSC code
- Account holder name
- Document quality metrics
- Fraud indicators (for cheques)

## Current Issues & Debugging

### Issue: onActivityResult not receiving data
- **Status**: Under investigation
- **Symptoms**: OCR app completes processing but React Native app shows "Operation was cancelled"
- **Debug logs**: Added comprehensive logging to track result flow
- **Possible causes**: 
  - Activity result listener not properly registered
  - Intent extras not matching expected keys
  - Activity lifecycle issues

### Debugging Commands
```bash
# Monitor logs for debugging
adb logcat -s OCRNativeModule:D ReactNativeJS:E

# Check installed packages
adb shell pm list packages | grep -E "(justdial|jdreact)"
```

## Error Handling
- App not installed: Fixed by bypassing Android package visibility restrictions
- Processing failures: Error message with details
- Cancelled operations: "Operation was cancelled" (current issue)

## Dependencies
- React Native 0.81.1
- Native Android with Kotlin
- Firebase AI for OCR processing

## Next Steps
1. Fix onActivityResult data reception
2. Ensure proper intent extra key matching
3. Test full end-to-end integration
4. Document successful JSON result display

---
**Last Updated**: September 9, 2025  
**Status**: App launches successfully, fixing result return mechanism