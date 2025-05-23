"use strict";
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// src/command/tokens/extract-semantic-token-keys.ts
var extract_semantic_token_keys_exports = {};
__export(extract_semantic_token_keys_exports, {
  extractSemanticTokenKeys: () => extractSemanticTokenKeys
});
module.exports = __toCommonJS(extract_semantic_token_keys_exports);

// src/utils/is-object.ts
function isObject(value) {
  const type = typeof value;
  return value != null && (type === "object" || type === "function") && !Array.isArray(value);
}

// src/command/tokens/extract-semantic-token-keys.ts
var hasSemanticTokens = (theme) => isObject(theme.semanticTokens);
function extractSemanticTokenKeys(theme, themePropertyName) {
  if (!hasSemanticTokens(theme)) {
    return [];
  }
  const themeProperty = theme["semanticTokens"][themePropertyName];
  if (!isObject(themeProperty)) {
    return [];
  }
  return Object.keys(flattenSemanticTokens(themeProperty));
}
function flattenSemanticTokens(target) {
  if (!isObject(target) && !Array.isArray(target)) {
    return target;
  }
  return Object.entries(target).reduce((result, [key, value]) => {
    if (isObject(value) && !("default" in value) || Array.isArray(value)) {
      Object.entries(flattenSemanticTokens(value)).forEach(
        ([childKey, childValue]) => {
          result[`${key}.${childKey}`] = childValue;
        }
      );
    } else {
      result[key] = value;
    }
    return result;
  }, {});
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  extractSemanticTokenKeys
});
