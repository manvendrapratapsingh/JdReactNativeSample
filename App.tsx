/**
 * JustDial OCR React Native Integration App
 * Integrates with native Android OCR app
 *
 * @format
 */

import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  Alert,
  NativeModules,
  useColorScheme,
} from 'react-native';

const { OCRNative } = NativeModules;

function App() {
  const isDarkMode = useColorScheme() === 'dark';
  const [ocrResult, setOcrResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const backgroundStyle = {
    backgroundColor: isDarkMode ? '#1a1a1a' : '#f5f5f5',
    flex: 1,
  };

  const processCheque = async () => {
    try {
      setLoading(true);
      setOcrResult(null);
      
      console.log('Launching OCR app for cheque processing...');
      const result = await OCRNative.processCheque();
      
      console.log('OCR Result received:', result);
      setOcrResult(result);
    } catch (error) {
      console.error('OCR Processing Error:', error);
      Alert.alert(
        'Error', 
        `Failed to process cheque: ${error.message || 'Unknown error'}`
      );
    } finally {
      setLoading(false);
    }
  };

  const processENach = async () => {
    try {
      setLoading(true);
      setOcrResult(null);
      
      console.log('Launching OCR app for E-NACH processing...');
      const result = await OCRNative.processENach();
      
      console.log('OCR Result received:', result);
      setOcrResult(result);
    } catch (error) {
      console.error('OCR Processing Error:', error);
      Alert.alert(
        'Error', 
        `Failed to process E-NACH: ${error.message || 'Unknown error'}`
      );
    } finally {
      setLoading(false);
    }
  };

  const clearResult = () => {
    setOcrResult(null);
  };

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar 
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView style={styles.scrollView}>
        <View style={styles.container}>
          <Text style={[styles.title, { color: isDarkMode ? '#fff' : '#000' }]}>
            JustDial OCR Integration
          </Text>
          <Text style={[styles.subtitle, { color: isDarkMode ? '#ccc' : '#666' }]}>
            Tap a button below to launch the OCR app and process documents
          </Text>
          
          <View style={styles.buttonContainer}>
            <TouchableOpacity
              style={[styles.button, styles.chequeButton]}
              onPress={processCheque}
              disabled={loading}
            >
              <Text style={styles.buttonText}>
                {loading ? 'Processing...' : 'Process Cheque'}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.button, styles.enachButton]}
              onPress={processENach}
              disabled={loading}
            >
              <Text style={styles.buttonText}>
                {loading ? 'Processing...' : 'Process E-NACH'}
              </Text>
            </TouchableOpacity>

            {ocrResult && (
              <TouchableOpacity
                style={[styles.button, styles.clearButton]}
                onPress={clearResult}
              >
                <Text style={styles.buttonText}>Clear Result</Text>
              </TouchableOpacity>
            )}
          </View>

          {ocrResult && (
            <View style={[styles.resultContainer, { backgroundColor: isDarkMode ? '#2a2a2a' : '#fff' }]}>
              <Text style={[styles.resultTitle, { color: isDarkMode ? '#fff' : '#000' }]}>
                OCR Result:
              </Text>
              <ScrollView style={styles.resultScrollView} nestedScrollEnabled>
                <Text style={[styles.resultText, { color: isDarkMode ? '#ccc' : '#333' }]}>
                  {ocrResult}
                </Text>
              </ScrollView>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  scrollView: {
    flex: 1,
  },
  container: {
    flex: 1,
    padding: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 10,
  },
  subtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 30,
    paddingHorizontal: 20,
  },
  buttonContainer: {
    width: '100%',
    alignItems: 'center',
    gap: 15,
  },
  button: {
    width: '80%',
    paddingVertical: 15,
    paddingHorizontal: 20,
    borderRadius: 8,
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  chequeButton: {
    backgroundColor: '#4CAF50',
  },
  enachButton: {
    backgroundColor: '#2196F3',
  },
  clearButton: {
    backgroundColor: '#FF9800',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  resultContainer: {
    width: '100%',
    marginTop: 30,
    padding: 15,
    borderRadius: 8,
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.22,
    shadowRadius: 2.22,
  },
  resultTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  resultScrollView: {
    maxHeight: 300,
  },
  resultText: {
    fontSize: 14,
    fontFamily: 'monospace',
    lineHeight: 20,
  },
});

export default App;
