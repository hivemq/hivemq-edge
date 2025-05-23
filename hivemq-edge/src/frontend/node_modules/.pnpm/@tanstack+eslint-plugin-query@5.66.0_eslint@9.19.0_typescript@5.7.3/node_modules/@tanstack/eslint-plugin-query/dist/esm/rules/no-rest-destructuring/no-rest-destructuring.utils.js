import { AST_NODE_TYPES } from "@typescript-eslint/utils";
const NoRestDestructuringUtils = {
  isObjectRestDestructuring(node) {
    if (node.type !== AST_NODE_TYPES.ObjectPattern) {
      return false;
    }
    return node.properties.some((p) => p.type === AST_NODE_TYPES.RestElement);
  }
};
export {
  NoRestDestructuringUtils
};
//# sourceMappingURL=no-rest-destructuring.utils.js.map
