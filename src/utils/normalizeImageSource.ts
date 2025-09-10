import { ImageSourcePropType } from 'react-native';

type AnySource = any;

const isNumber = (v: unknown): v is number => typeof v === 'number';
const isString = (v: unknown): v is string => typeof v === 'string';
const isObject = (v: unknown): v is Record<string, unknown> => !!v && typeof v === 'object';

const isValidUri = (uri: string): boolean => {
  if (!uri) return false;
  // Allow common RN schemes
  return /^(https?:|file:|content:|data:|asset:|android\.resource:)/.test(uri);
};

const normalizeSingle = (src: AnySource): ImageSourcePropType | undefined => {
  if (src == null) return undefined;
  if (isNumber(src)) return src as unknown as ImageSourcePropType; // require('...')
  if (isString(src)) {
    return isValidUri(src) ? ({ uri: src } as unknown as ImageSourcePropType) : undefined;
  }
  if (isObject(src)) {
    // Typical shape: { uri, headers?, width?, height?, cache? }
    const uri = (src as any).uri;
    if (isString(uri) && isValidUri(uri)) {
      return src as ImageSourcePropType;
    }
    // Some libs pass { default: number }
    if (isNumber((src as any).default)) {
      return (src as any).default as ImageSourcePropType;
    }
    return undefined;
  }
  return undefined;
};

export const normalizeImageSource = (
  source: AnySource,
): ImageSourcePropType | ImageSourcePropType[] | undefined => {
  if (Array.isArray(source)) {
    const cleaned = source
      .map(normalizeSingle)
      .filter(Boolean) as ImageSourcePropType[];
    return cleaned.length ? cleaned : undefined;
  }
  return normalizeSingle(source);
};

export const isValidImageSource = (source: AnySource): boolean => {
  return !!normalizeImageSource(source);
};
