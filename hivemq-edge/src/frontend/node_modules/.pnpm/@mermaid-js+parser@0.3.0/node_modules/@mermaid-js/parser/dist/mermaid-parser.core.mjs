import {
  GitGraphModule,
  createGitGraphServices
} from "./chunks/mermaid-parser.core/chunk-NCMFTTUW.mjs";
import {
  InfoModule,
  createInfoServices
} from "./chunks/mermaid-parser.core/chunk-4YFB5VUC.mjs";
import {
  PacketModule,
  createPacketServices
} from "./chunks/mermaid-parser.core/chunk-EQFLFMNE.mjs";
import {
  PieModule,
  createPieServices
} from "./chunks/mermaid-parser.core/chunk-BI6EQKOQ.mjs";
import {
  ArchitectureModule,
  createArchitectureServices
} from "./chunks/mermaid-parser.core/chunk-FF7BQXOH.mjs";
import {
  AbstractMermaidTokenBuilder,
  AbstractMermaidValueConverter,
  Architecture,
  ArchitectureGeneratedModule,
  Branch,
  Commit,
  CommonTokenBuilder,
  CommonValueConverter,
  GitGraph,
  GitGraphGeneratedModule,
  Info,
  InfoGeneratedModule,
  Merge,
  MermaidGeneratedSharedModule,
  Packet,
  PacketBlock,
  PacketGeneratedModule,
  Pie,
  PieGeneratedModule,
  PieSection,
  Statement,
  __name,
  isArchitecture,
  isBranch,
  isCommit,
  isCommon,
  isGitGraph,
  isInfo,
  isMerge,
  isPacket,
  isPacketBlock,
  isPie,
  isPieSection
} from "./chunks/mermaid-parser.core/chunk-Y27MQZ3U.mjs";

// src/parse.ts
var parsers = {};
var initializers = {
  info: /* @__PURE__ */ __name(async () => {
    const { createInfoServices: createInfoServices2 } = await import("./chunks/mermaid-parser.core/info-46DW6VJ7.mjs");
    const parser = createInfoServices2().Info.parser.LangiumParser;
    parsers.info = parser;
  }, "info"),
  packet: /* @__PURE__ */ __name(async () => {
    const { createPacketServices: createPacketServices2 } = await import("./chunks/mermaid-parser.core/packet-W2GHVCYJ.mjs");
    const parser = createPacketServices2().Packet.parser.LangiumParser;
    parsers.packet = parser;
  }, "packet"),
  pie: /* @__PURE__ */ __name(async () => {
    const { createPieServices: createPieServices2 } = await import("./chunks/mermaid-parser.core/pie-BEWT4RHE.mjs");
    const parser = createPieServices2().Pie.parser.LangiumParser;
    parsers.pie = parser;
  }, "pie"),
  architecture: /* @__PURE__ */ __name(async () => {
    const { createArchitectureServices: createArchitectureServices2 } = await import("./chunks/mermaid-parser.core/architecture-I3QFYML2.mjs");
    const parser = createArchitectureServices2().Architecture.parser.LangiumParser;
    parsers.architecture = parser;
  }, "architecture"),
  gitGraph: /* @__PURE__ */ __name(async () => {
    const { createGitGraphServices: createGitGraphServices2 } = await import("./chunks/mermaid-parser.core/gitGraph-YCYPL57B.mjs");
    const parser = createGitGraphServices2().GitGraph.parser.LangiumParser;
    parsers.gitGraph = parser;
  }, "gitGraph")
};
async function parse(diagramType, text) {
  const initializer = initializers[diagramType];
  if (!initializer) {
    throw new Error(`Unknown diagram type: ${diagramType}`);
  }
  if (!parsers[diagramType]) {
    await initializer();
  }
  const parser = parsers[diagramType];
  const result = parser.parse(text);
  if (result.lexerErrors.length > 0 || result.parserErrors.length > 0) {
    throw new MermaidParseError(result);
  }
  return result.value;
}
__name(parse, "parse");
var MermaidParseError = class extends Error {
  constructor(result) {
    const lexerErrors = result.lexerErrors.map((err) => err.message).join("\n");
    const parserErrors = result.parserErrors.map((err) => err.message).join("\n");
    super(`Parsing failed: ${lexerErrors} ${parserErrors}`);
    this.result = result;
  }
  static {
    __name(this, "MermaidParseError");
  }
};
export {
  AbstractMermaidTokenBuilder,
  AbstractMermaidValueConverter,
  Architecture,
  ArchitectureGeneratedModule,
  ArchitectureModule,
  Branch,
  Commit,
  CommonTokenBuilder,
  CommonValueConverter,
  GitGraph,
  GitGraphGeneratedModule,
  GitGraphModule,
  Info,
  InfoGeneratedModule,
  InfoModule,
  Merge,
  MermaidGeneratedSharedModule,
  MermaidParseError,
  Packet,
  PacketBlock,
  PacketGeneratedModule,
  PacketModule,
  Pie,
  PieGeneratedModule,
  PieModule,
  PieSection,
  Statement,
  createArchitectureServices,
  createGitGraphServices,
  createInfoServices,
  createPacketServices,
  createPieServices,
  isArchitecture,
  isBranch,
  isCommit,
  isCommon,
  isGitGraph,
  isInfo,
  isMerge,
  isPacket,
  isPacketBlock,
  isPie,
  isPieSection,
  parse
};
