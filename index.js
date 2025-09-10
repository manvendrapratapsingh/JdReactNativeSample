/**
 * @format
 */

import { AppRegistry } from 'react-native';
// Patch RN.Image and RN.ImageBackground first
import './src/setup/patchReactNativeImage';
// Also install a host-level guard to catch any stray native usage
import './src/setup/imageGuard';
import App from './App';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);
