"use strict";
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
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
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

// src/command/tokens/resolve-output-path.ts
var resolve_output_path_exports = {};
__export(resolve_output_path_exports, {
  resolveOutputPath: () => resolveOutputPath,
  themeInterfaceDestination: () => themeInterfaceDestination
});
module.exports = __toCommonJS(resolve_output_path_exports);
var import_fs = __toESM(require("fs"));
var import_path = __toESM(require("path"));
var import_util = require("util");
var exists = (0, import_util.promisify)(import_fs.default.exists);
var themeInterfaceDestination = [
  "node_modules",
  "@chakra-ui",
  "styled-system",
  "dist",
  "theming.types.d.ts"
];
async function resolveThemingDefinitionPath() {
  const baseDir = import_path.default.join("..", "..", "..");
  const cwd = process.cwd();
  const pathsToTry = [
    import_path.default.resolve(baseDir, "..", ...themeInterfaceDestination),
    import_path.default.resolve(baseDir, "..", "..", ...themeInterfaceDestination),
    import_path.default.resolve(cwd, ...themeInterfaceDestination),
    import_path.default.resolve(cwd, "..", ...themeInterfaceDestination),
    import_path.default.resolve(cwd, "..", "..", ...themeInterfaceDestination)
  ];
  const triedPaths = await Promise.all(
    pathsToTry.map(async (possiblePath) => {
      if (await exists(possiblePath)) {
        return possiblePath;
      }
      return "";
    })
  );
  return triedPaths.find(Boolean);
}
async function resolveOutputPath(overridePath) {
  if (overridePath) {
    return import_path.default.resolve(process.cwd(), overridePath);
  }
  const themingDefinitionFilePath = await resolveThemingDefinitionPath();
  if (!themingDefinitionFilePath) {
    throw new Error(
      "Could not find @chakra-ui/styled-system in node_modules. Please provide `--out` parameter."
    );
  }
  return themingDefinitionFilePath;
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  resolveOutputPath,
  themeInterfaceDestination
});
