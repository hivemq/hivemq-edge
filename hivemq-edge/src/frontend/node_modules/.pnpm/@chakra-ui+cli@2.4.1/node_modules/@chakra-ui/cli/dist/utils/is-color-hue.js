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

// src/utils/is-color-hue.ts
var is_color_hue_exports = {};
__export(is_color_hue_exports, {
  isColorHue: () => isColorHue
});
module.exports = __toCommonJS(is_color_hue_exports);

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
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  isColorHue
});
