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

// src/command/tokens/extract-color-schemes.ts
var extract_color_schemes_exports = {};
__export(extract_color_schemes_exports, {
  extractColorSchemeTypes: () => extractColorSchemeTypes
});
module.exports = __toCommonJS(extract_color_schemes_exports);

// src/utils/is-object.ts
function isObject(value) {
  const type = typeof value;
  return value != null && (type === "object" || type === "function") && !Array.isArray(value);
}

// src/utils/is-color-hue.ts
var colorHueKeys = [
  "50",
  "100",
  "200",
  "300",
  "400",
  "500",
  "600",
  "700",
  "800",
  "900"
];
function isColorHue(value) {
  if (!isObject(value))
    return false;
  const keys = Object.keys(value);
  return colorHueKeys.every((key) => keys.includes(key));
}

// src/command/tokens/extract-color-schemes.ts
function extractColorSchemeTypes(theme) {
  const { colors } = theme;
  if (!isObject(colors)) {
    return [];
  }
  return Object.entries(colors).reduce(
    (acc, [colorName, colorValues]) => {
      if (isColorHue(colorValues)) {
        acc.push(colorName);
      }
      return acc;
    },
    []
  );
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  extractColorSchemeTypes
});
