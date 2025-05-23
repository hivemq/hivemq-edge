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

// src/command/tokens/extract-component-types.ts
var extract_component_types_exports = {};
__export(extract_component_types_exports, {
  extractComponentTypes: () => extractComponentTypes,
  printComponentTypes: () => printComponentTypes
});
module.exports = __toCommonJS(extract_component_types_exports);

// src/utils/is-object.ts
function isObject(value) {
  const type = typeof value;
  return value != null && (type === "object" || type === "function") && !Array.isArray(value);
}

// src/utils/print-union-type.ts
var AutoCompleteStringType = "(string & {})";
var wrapWithQuotes = (value) => `"${value}"`;
function printUnionType(values, strict = false) {
  if (!values.length) {
    return strict ? "never" : AutoCompleteStringType;
  }
  return values.map(wrapWithQuotes).concat(strict ? [] : [AutoCompleteStringType]).join(" | ");
}

// src/command/tokens/extract-property-paths.ts
function printUnionMap(unions, strict = false) {
  return Object.entries(unions).sort(([a], [b]) => a.localeCompare(b)).map(
    ([targetKey, union]) => `${targetKey}: ${printUnionType(union, strict)};`
  ).join("\n");
}

// src/command/tokens/extract-component-types.ts
function extractComponentTypes(theme) {
  const components = theme.components;
  if (!isObject(components)) {
    return {};
  }
  return Object.entries(components).reduce(
    (allDefs, [componentName, definition]) => {
      if (definition) {
        allDefs[componentName] = {
          sizes: Object.keys(definition.sizes ?? {}),
          variants: Object.keys(definition.variants ?? {})
        };
      }
      return allDefs;
    },
    {}
  );
}
function esc(name) {
  return name.match(/^[a-zA-Z0-9\-_]+$/) ? name : `"${name}"`;
}
function printComponentTypes(componentTypes, strict = false) {
  const types = Object.entries(componentTypes).map(
    ([componentName, unions]) => `${esc(componentName)}: {
  ${printUnionMap(unions, strict)}
}`
  ).join(`
`);
  return `components: {
  ${types}  
}
`;
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  extractComponentTypes,
  printComponentTypes
});
