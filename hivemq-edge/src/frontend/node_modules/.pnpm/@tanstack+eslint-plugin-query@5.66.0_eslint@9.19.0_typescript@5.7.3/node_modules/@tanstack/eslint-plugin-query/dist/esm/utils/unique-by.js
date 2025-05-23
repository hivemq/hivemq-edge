function uniqueBy(arr, fn) {
  return arr.filter((x, i, a) => a.findIndex((y) => fn(x) === fn(y)) === i);
}
export {
  uniqueBy
};
//# sourceMappingURL=unique-by.js.map
