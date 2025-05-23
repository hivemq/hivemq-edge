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

// src/utils/format-with-prettier.ts
var format_with_prettier_exports = {};
__export(format_with_prettier_exports, {
  formatWithPrettier: () => formatWithPrettier
});
module.exports = __toCommonJS(format_with_prettier_exports);
var import_prettier = require("prettier");
async function formatWithPrettier(content) {
  const prettierConfig = await (0, import_prettier.resolveConfig)(process.cwd());
  try {
    return (0, import_prettier.format)(String(content), {
      ...prettierConfig,
      parser: "typescript"
    });
  } catch {
    return String(content);
  }
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  formatWithPrettier
});
