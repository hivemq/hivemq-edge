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

// src/command/tokens/extract-property-keys.ts
var extract_property_keys_exports = {};
__export(extract_property_keys_exports, {
  extractPropertyKeys: () => extractPropertyKeys
});
module.exports = __toCommonJS(extract_property_keys_exports);

// src/utils/is-object.ts
function isObject(value) {
  const type = typeof value;
  return value != null && (type === "object" || type === "function") && !Array.isArray(value);
}

// src/command/tokens/extract-property-keys.ts
function extractPropertyKeys(theme, themePropertyName) {
  const themeProperty = theme[themePropertyName];
  if (!isObject(themeProperty)) {
    return [];
  }
  return Object.keys(themeProperty);
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  extractPropertyKeys
});
