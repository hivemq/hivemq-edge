"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
function uniqueBy(arr, fn) {
  return arr.filter((x, i, a) => a.findIndex((y) => fn(x) === fn(y)) === i);
}
exports.uniqueBy = uniqueBy;
//# sourceMappingURL=unique-by.cjs.map
