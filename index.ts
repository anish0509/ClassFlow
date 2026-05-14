import { LogBox } from 'react-native';

// 🛡️ Early Intervention Log Filter: Suppress harmless expo-notification internal terminal logs
const ignorePatterns = [
  'expo-notifications',
  'remote notifications',
  'expo-notifications: Android Push notifications',
  'expo-notifications: Push notifications',
  'expo-notifications` functionality is not fully supported',
  'New Architecture is always enabled in Expo Go',
];

const originalError = console.error;
console.error = (...args: any[]) => {
  const message = String(args[0] || '');
  if (ignorePatterns.some(p => message.includes(p))) return;
  originalError(...args);
};

const originalWarn = console.warn;
console.warn = (...args: any[]) => {
  const message = String(args[0] || '');
  if (ignorePatterns.some(p => message.includes(p))) return;
  originalWarn(...args);
};

LogBox.ignoreLogs(ignorePatterns);

import { registerRootComponent } from 'expo';
import App from './App';

registerRootComponent(App);
