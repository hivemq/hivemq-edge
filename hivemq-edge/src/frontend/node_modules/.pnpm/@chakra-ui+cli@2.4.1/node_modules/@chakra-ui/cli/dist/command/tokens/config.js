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

// src/command/tokens/config.ts
var config_exports = {};
__export(config_exports, {
  themeKeyConfiguration: () => themeKeyConfiguration
});
module.exports = __toCommonJS(config_exports);
var themeKeyConfiguration = [
  { key: "blur" },
  { key: "borders" },
  { key: "borderStyles" },
  { key: "borderWidths" },
  { key: "breakpoints", filter: (value) => Number.isNaN(Number(value)) },
  { key: "colors", maxScanDepth: 3 },
  { key: "fonts" },
  { key: "fontSizes" },
  { key: "fontWeights" },
  { key: "letterSpacings" },
  { key: "lineHeights" },
  { key: "radii" },
  { key: "shadows" },
  { key: "sizes", maxScanDepth: 2 },
  { key: "space", flatMap: (value) => [value, `-${value}`] },
  { key: "transition" },
  { key: "zIndices" }
];
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  themeKeyConfiguration
});
