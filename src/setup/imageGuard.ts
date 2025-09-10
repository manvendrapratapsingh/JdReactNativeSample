import React from 'react';
import { Image, ImageBackground } from 'react-native';
import { isValidImageSource, normalizeImageSource } from '../utils/normalizeImageSource';

// Guard against invalid Image source props that crash RCTImageView
const origCreateElement = React.createElement;

// Types are loose because we want to be resilient in JS runtime
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function patchedCreateElement(type: any, props: any, ...children: any[]) {
  try {
    const isNativeImageTag =
      typeof type === 'string' && (type === 'RCTImageView' || type === 'ImageView' || /Image/i.test(type));
    if ((type === Image || type === ImageBackground || isNativeImageTag) && props) {
      const nextProps = { ...props };

      if ('source' in nextProps) {
        const normalized = normalizeImageSource(nextProps.source);
        if (normalized === undefined) {
          // Drop invalid sources to avoid native crash
          delete nextProps.source;
          if (__DEV__) {
            // eslint-disable-next-line no-console
            console.warn('[image-guard] Dropped invalid Image source for', type);
          }
        } else {
          // Preserve RN-supported shapes: number | object | array
          nextProps.source = normalized as any;
        }
      }

      if ('defaultSource' in nextProps && !isValidImageSource(nextProps.defaultSource)) {
        delete nextProps.defaultSource;
        if (__DEV__) {
          // eslint-disable-next-line no-console
          console.warn('[image-guard] Dropped invalid defaultSource for', type);
        }
      }

      return origCreateElement(type, nextProps, ...children);
    }
  } catch (e) {
    // In case of any unexpected error, fall back to original flow
  }

  return origCreateElement(type, props, ...children);
}

// Install the patch once
if ((React as any).createElement !== patchedCreateElement) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  (React as any).createElement = patchedCreateElement as any;
}
