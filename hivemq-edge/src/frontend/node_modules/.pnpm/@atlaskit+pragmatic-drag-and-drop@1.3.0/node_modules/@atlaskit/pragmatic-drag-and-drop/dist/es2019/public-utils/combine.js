/** Create a new combined function that will call all the provided functions */
export function combine(...fns) {
  return function cleanup() {
    fns.forEach(fn => fn());
  };
}