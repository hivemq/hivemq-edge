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

// src/command/tokens/create-theme-typings-interface.ts
var create_theme_typings_interface_exports = {};
__export(create_theme_typings_interface_exports, {
  createThemeTypingsInterface: () => createThemeTypingsInterface
});
module.exports = __toCommonJS(create_theme_typings_interface_exports);

// src/utils/format-with-prettier.ts
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
function extractPropertyPaths(target, maxDepth = 3) {
  if (!isObject(target) && !Array.isArray(target) || !maxDepth) {
    return [];
  }
  return Object.entries(target).reduce((allPropertyPaths, [key, value]) => {
    if (isObject(value)) {
      extractPropertyPaths(value, maxDepth - 1).forEach(
        (childKey) => allPropertyPaths.push(`${key}.${childKey}`)
      );
    } else {
      allPropertyPaths.push(key);
    }
    return allPropertyPaths;
  }, []);
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

// src/command/tokens/extract-property-keys.ts
function extractPropertyKeys(theme, themePropertyName) {
  const themeProperty = theme[themePropertyName];
  if (!isObject(themeProperty)) {
    return [];
  }
  return Object.keys(themeProperty);
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

// src/command/tokens/create-theme-typings-interface.ts
function applyThemeTypingTemplate(typingContent, template) {
  switch (template) {
    case "augmentation":
      return `// regenerate by running
// npx @chakra-ui/cli tokens path/to/your/theme.(js|ts) --template augmentation --out path/to/this/file 
import { BaseThemeTypings } from "@chakra-ui/styled-system";
declare module "@chakra-ui/styled-system" {
  export interface CustomThemeTypings extends BaseThemeTypings {
    ${typingContent}
  }
}
`;
    case "default":
    default:
      return `// regenerate by running
// npx @chakra-ui/cli tokens path/to/your/theme.(js|ts)
import { BaseThemeTypings } from "./shared.types.js"
export interface ThemeTypings extends BaseThemeTypings {
  ${typingContent}
}
`;
  }
}
async function createThemeTypingsInterface(theme, {
  config,
  strictComponentTypes = false,
  format: format2 = true,
  strictTokenTypes = false,
  template = "default"
}) {
  const unions = config.reduce(
    (allUnions, { key, maxScanDepth, filter = () => true, flatMap = (value) => value }) => {
      const target = theme[key];
      allUnions[key] = [];
      if (isObject(target) || Array.isArray(target)) {
        allUnions[key] = extractPropertyPaths(target, maxScanDepth).filter(filter).flatMap(flatMap);
      }
      if (isObject(theme.semanticTokens)) {
        const semanticTokenKeys = extractSemanticTokenKeys(theme, key).filter(filter).flatMap(flatMap);
        allUnions[key].push(...semanticTokenKeys);
      }
      return allUnions;
    },
    {}
  );
  const textStyles = extractPropertyKeys(theme, "textStyles");
  const layerStyles = extractPropertyKeys(theme, "layerStyles");
  const colorSchemes = extractColorSchemeTypes(theme);
  const componentTypes = extractComponentTypes(theme);
  const typingContent = `${printUnionMap(
    { ...unions, textStyles, layerStyles, colorSchemes },
    strictTokenTypes
  )}
  ${printComponentTypes(componentTypes, strictComponentTypes)}`;
  const themeTypings = applyThemeTypingTemplate(typingContent, template);
  return format2 ? formatWithPrettier(themeTypings) : themeTypings;
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  createThemeTypingsInterface
});
