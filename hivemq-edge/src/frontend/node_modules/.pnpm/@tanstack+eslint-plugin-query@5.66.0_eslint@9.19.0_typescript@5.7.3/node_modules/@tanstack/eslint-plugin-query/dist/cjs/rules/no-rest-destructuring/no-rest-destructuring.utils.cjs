"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const utils = require("@typescript-eslint/utils");
const NoRestDestructuringUtils = {
  isObjectRestDestructuring(node) {
    if (node.type !== utils.AST_NODE_TYPES.ObjectPattern) {
      return false;
    }
    return node.properties.some((p) => p.type === utils.AST_NODE_TYPES.RestElement);
  }
};
exports.NoRestDestructuringUtils = NoRestDestructuringUtils;
//# sourceMappingURL=no-rest-destructuring.utils.cjs.map
