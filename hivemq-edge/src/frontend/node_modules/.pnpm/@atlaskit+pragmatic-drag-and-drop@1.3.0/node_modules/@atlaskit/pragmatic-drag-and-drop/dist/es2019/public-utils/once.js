/** Provide a function that you only ever want to be called a single time */
export function once(fn) {
  let cache = null;
  return function wrapped(...args) {
    if (!cache) {
      const result = fn.apply(this, args);
      cache = {
        result: result
      };
    }
    return cache.result;
  };
}