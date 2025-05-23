import { once } from '../public-utils/once';

// using `cache` as our `isFirefox()` result will not change in a browser

/**
 * Returns `true` if a `Firefox` browser
 * */
export var isFirefox = once(function isFirefox() {
  if (process.env.NODE_ENV === 'test') {
    return false;
  }
  return navigator.userAgent.includes('Firefox');
});