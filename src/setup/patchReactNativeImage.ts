import React from 'react';
import * as RN from 'react-native';
import { normalizeImageSource, isValidImageSource } from '../utils/normalizeImageSource';

function withImageGuard<P extends { source?: any; defaultSource?: any }>(
  Base: React.ComponentType<P>,
) {
  const Guarded = React.forwardRef<any, P>((props, ref) => {
    const next: any = { ...props };

    if ('source' in next) {
      const normalized = normalizeImageSource(next.source);
      if (normalized === undefined) {
        delete next.source;
        if (__DEV__) console.warn('[image-guard] Dropped invalid Image source (RN patch)');
      } else {
        // Preserve RN-supported shapes: number | object | array
        next.source = normalized as any;
      }
    }

    if ('defaultSource' in next && !isValidImageSource(next.defaultSource)) {
      delete next.defaultSource;
      if (__DEV__) console.warn('[image-guard] Dropped invalid defaultSource (RN patch)');
    }

    return React.createElement(Base as any, { ...next, ref });
  });

  Guarded.displayName = `Guarded(${Base.displayName || Base.name || 'Image'})`;
  return Guarded;
}

// Patch only once
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const anyRN: any = RN as any;
if (!anyRN.__IMAGE_GUARD_INSTALLED__) {
  const OriginalImage = RN.Image;
  const OriginalImageBackground = RN.ImageBackground;

  // Replace exports on the RN module object
  // This affects all imports that read RN.Image after this file runs
  (anyRN as any).Image = withImageGuard(OriginalImage as any);
  (anyRN as any).ImageBackground = withImageGuard(OriginalImageBackground as any);
  anyRN.__IMAGE_GUARD_INSTALLED__ = true;
}
