var __defProp = Object.defineProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });

// src/language/generated/ast.ts
import { AbstractAstReflection } from "langium";
var Statement = "Statement";
var Architecture = "Architecture";
function isArchitecture(item) {
  return reflection.isInstance(item, Architecture);
}
__name(isArchitecture, "isArchitecture");
var Branch = "Branch";
function isBranch(item) {
  return reflection.isInstance(item, Branch);
}
__name(isBranch, "isBranch");
var Checkout = "Checkout";
var CherryPicking = "CherryPicking";
var Commit = "Commit";
function isCommit(item) {
  return reflection.isInstance(item, Commit);
}
__name(isCommit, "isCommit");
var Common = "Common";
function isCommon(item) {
  return reflection.isInstance(item, Common);
}
__name(isCommon, "isCommon");
var GitGraph = "GitGraph";
function isGitGraph(item) {
  return reflection.isInstance(item, GitGraph);
}
__name(isGitGraph, "isGitGraph");
var Info = "Info";
function isInfo(item) {
  return reflection.isInstance(item, Info);
}
__name(isInfo, "isInfo");
var Merge = "Merge";
function isMerge(item) {
  return reflection.isInstance(item, Merge);
}
__name(isMerge, "isMerge");
var Packet = "Packet";
function isPacket(item) {
  return reflection.isInstance(item, Packet);
}
__name(isPacket, "isPacket");
var PacketBlock = "PacketBlock";
function isPacketBlock(item) {
  return reflection.isInstance(item, PacketBlock);
}
__name(isPacketBlock, "isPacketBlock");
var Pie = "Pie";
function isPie(item) {
  return reflection.isInstance(item, Pie);
}
__name(isPie, "isPie");
var PieSection = "PieSection";
function isPieSection(item) {
  return reflection.isInstance(item, PieSection);
}
__name(isPieSection, "isPieSection");
var Direction = "Direction";
var MermaidAstReflection = class extends AbstractAstReflection {
  static {
    __name(this, "MermaidAstReflection");
  }
  getAllTypes() {
    return ["Architecture", "Branch", "Checkout", "CherryPicking", "Commit", "Common", "Direction", "Edge", "GitGraph", "Group", "Info", "Junction", "Merge", "Packet", "PacketBlock", "Pie", "PieSection", "Service", "Statement"];
  }
  computeIsSubtype(subtype, supertype) {
    switch (subtype) {
      case Branch:
      case Checkout:
      case CherryPicking:
      case Commit:
      case Merge: {
        return this.isSubtype(Statement, supertype);
      }
      case Direction: {
        return this.isSubtype(GitGraph, supertype);
      }
      default: {
        return false;
      }
    }
  }
  getReferenceType(refInfo) {
    const referenceId = `${refInfo.container.$type}:${refInfo.property}`;
    switch (referenceId) {
      default: {
        throw new Error(`${referenceId} is not a valid reference id.`);
      }
    }
  }
  getTypeMetaData(type) {
    switch (type) {
      case "Architecture": {
        return {
          name: "Architecture",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "edges", defaultValue: [] },
            { name: "groups", defaultValue: [] },
            { name: "junctions", defaultValue: [] },
            { name: "services", defaultValue: [] },
            { name: "title" }
          ]
        };
      }
      case "Branch": {
        return {
          name: "Branch",
          properties: [
            { name: "name" },
            { name: "order" }
          ]
        };
      }
      case "Checkout": {
        return {
          name: "Checkout",
          properties: [
            { name: "branch" }
          ]
        };
      }
      case "CherryPicking": {
        return {
          name: "CherryPicking",
          properties: [
            { name: "id" },
            { name: "parent" },
            { name: "tags", defaultValue: [] }
          ]
        };
      }
      case "Commit": {
        return {
          name: "Commit",
          properties: [
            { name: "id" },
            { name: "message" },
            { name: "tags", defaultValue: [] },
            { name: "type" }
          ]
        };
      }
      case "Common": {
        return {
          name: "Common",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "title" }
          ]
        };
      }
      case "Edge": {
        return {
          name: "Edge",
          properties: [
            { name: "lhsDir" },
            { name: "lhsGroup", defaultValue: false },
            { name: "lhsId" },
            { name: "lhsInto", defaultValue: false },
            { name: "rhsDir" },
            { name: "rhsGroup", defaultValue: false },
            { name: "rhsId" },
            { name: "rhsInto", defaultValue: false },
            { name: "title" }
          ]
        };
      }
      case "GitGraph": {
        return {
          name: "GitGraph",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "statements", defaultValue: [] },
            { name: "title" }
          ]
        };
      }
      case "Group": {
        return {
          name: "Group",
          properties: [
            { name: "icon" },
            { name: "id" },
            { name: "in" },
            { name: "title" }
          ]
        };
      }
      case "Info": {
        return {
          name: "Info",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "title" }
          ]
        };
      }
      case "Junction": {
        return {
          name: "Junction",
          properties: [
            { name: "id" },
            { name: "in" }
          ]
        };
      }
      case "Merge": {
        return {
          name: "Merge",
          properties: [
            { name: "branch" },
            { name: "id" },
            { name: "tags", defaultValue: [] },
            { name: "type" }
          ]
        };
      }
      case "Packet": {
        return {
          name: "Packet",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "blocks", defaultValue: [] },
            { name: "title" }
          ]
        };
      }
      case "PacketBlock": {
        return {
          name: "PacketBlock",
          properties: [
            { name: "end" },
            { name: "label" },
            { name: "start" }
          ]
        };
      }
      case "Pie": {
        return {
          name: "Pie",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "sections", defaultValue: [] },
            { name: "showData", defaultValue: false },
            { name: "title" }
          ]
        };
      }
      case "PieSection": {
        return {
          name: "PieSection",
          properties: [
            { name: "label" },
            { name: "value" }
          ]
        };
      }
      case "Service": {
        return {
          name: "Service",
          properties: [
            { name: "icon" },
            { name: "iconText" },
            { name: "id" },
            { name: "in" },
            { name: "title" }
          ]
        };
      }
      case "Direction": {
        return {
          name: "Direction",
          properties: [
            { name: "accDescr" },
            { name: "accTitle" },
            { name: "dir" },
            { name: "statements", defaultValue: [] },
            { name: "title" }
          ]
        };
      }
      default: {
        return {
          name: type,
          properties: []
        };
      }
    }
  }
};
var reflection = new MermaidAstReflection();

// src/language/generated/grammar.ts
import { loadGrammarFromJson } from "langium";
var loadedInfoGrammar;
var InfoGrammar = /* @__PURE__ */ __name(() => loadedInfoGrammar ?? (loadedInfoGrammar = loadGrammarFromJson('{"$type":"Grammar","isDeclared":true,"name":"Info","imports":[],"rules":[{"$type":"ParserRule","name":"Info","entry":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[],"cardinality":"*"},{"$type":"Keyword","value":"info"},{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[],"cardinality":"*"},{"$type":"Group","elements":[{"$type":"Keyword","value":"showInfo"},{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[],"cardinality":"*"}],"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[],"cardinality":"?"}]},"definesHiddenTokens":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"TitleAndAccessibilities","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"accDescr","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@4"},"arguments":[]}},{"$type":"Assignment","feature":"accTitle","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[]}}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]}],"cardinality":"+"},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"EOL","fragment":true,"dataType":"string","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[],"cardinality":"+"},{"$type":"EndOfFile"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"NEWLINE","definition":{"$type":"RegexToken","regex":"/\\\\r?\\\\n/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_DESCR","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accDescr(?:[\\\\t ]*:([^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)|\\\\s*{([^}]*)})/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accTitle[\\\\t ]*:(?:[^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*title(?:[\\\\t ][^\\\\n\\\\r]*?(?=%%)|[\\\\t ][^\\\\n\\\\r]*|)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","hidden":true,"name":"WHITESPACE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]+/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"YAML","definition":{"$type":"RegexToken","regex":"/---[\\\\t ]*\\\\r?\\\\n(?:[\\\\S\\\\s]*?\\\\r?\\\\n)?---(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"DIRECTIVE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%{[\\\\S\\\\s]*?}%%(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"SINGLE_LINE_COMMENT","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%[^\\\\n\\\\r]*/"},"fragment":false}],"definesHiddenTokens":false,"hiddenTokens":[],"interfaces":[{"$type":"Interface","name":"Common","attributes":[{"$type":"TypeAttribute","name":"accDescr","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"accTitle","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"title","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}}],"superTypes":[]}],"types":[],"usedGrammars":[]}')), "InfoGrammar");
var loadedPacketGrammar;
var PacketGrammar = /* @__PURE__ */ __name(() => loadedPacketGrammar ?? (loadedPacketGrammar = loadGrammarFromJson(`{"$type":"Grammar","isDeclared":true,"name":"Packet","imports":[],"rules":[{"$type":"ParserRule","name":"Packet","entry":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"},{"$type":"Keyword","value":"packet-beta"},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@4"},"arguments":[]},{"$type":"Assignment","feature":"blocks","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]},"cardinality":"*"}]},{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"+"},{"$type":"Assignment","feature":"blocks","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]},"cardinality":"+"}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"}]}]},"definesHiddenTokens":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"PacketBlock","definition":{"$type":"Group","elements":[{"$type":"Assignment","feature":"start","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]}},{"$type":"Group","elements":[{"$type":"Keyword","value":"-"},{"$type":"Assignment","feature":"end","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]}}],"cardinality":"?"},{"$type":"Keyword","value":":"},{"$type":"Assignment","feature":"label","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[]}},{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"INT","type":{"$type":"ReturnType","name":"number"},"definition":{"$type":"RegexToken","regex":"/0|[1-9][0-9]*/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"STRING","definition":{"$type":"RegexToken","regex":"/\\"[^\\"]*\\"|'[^']*'/"},"fragment":false,"hidden":false},{"$type":"ParserRule","name":"TitleAndAccessibilities","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"accDescr","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@7"},"arguments":[]}},{"$type":"Assignment","feature":"accTitle","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@8"},"arguments":[]}},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@9"},"arguments":[]}}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}],"cardinality":"+"},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"EOL","fragment":true,"dataType":"string","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"+"},{"$type":"EndOfFile"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"NEWLINE","definition":{"$type":"RegexToken","regex":"/\\\\r?\\\\n/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_DESCR","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accDescr(?:[\\\\t ]*:([^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)|\\\\s*{([^}]*)})/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accTitle[\\\\t ]*:(?:[^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*title(?:[\\\\t ][^\\\\n\\\\r]*?(?=%%)|[\\\\t ][^\\\\n\\\\r]*|)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","hidden":true,"name":"WHITESPACE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]+/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"YAML","definition":{"$type":"RegexToken","regex":"/---[\\\\t ]*\\\\r?\\\\n(?:[\\\\S\\\\s]*?\\\\r?\\\\n)?---(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"DIRECTIVE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%{[\\\\S\\\\s]*?}%%(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"SINGLE_LINE_COMMENT","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%[^\\\\n\\\\r]*/"},"fragment":false}],"definesHiddenTokens":false,"hiddenTokens":[],"interfaces":[{"$type":"Interface","name":"Common","attributes":[{"$type":"TypeAttribute","name":"accDescr","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"accTitle","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"title","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}}],"superTypes":[]}],"types":[],"usedGrammars":[]}`)), "PacketGrammar");
var loadedPieGrammar;
var PieGrammar = /* @__PURE__ */ __name(() => loadedPieGrammar ?? (loadedPieGrammar = loadGrammarFromJson('{"$type":"Grammar","isDeclared":true,"name":"Pie","imports":[],"rules":[{"$type":"ParserRule","name":"Pie","entry":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"},{"$type":"Keyword","value":"pie"},{"$type":"Assignment","feature":"showData","operator":"?=","terminal":{"$type":"Keyword","value":"showData"},"cardinality":"?"},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@4"},"arguments":[]},{"$type":"Assignment","feature":"sections","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]},"cardinality":"*"}]},{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"+"},{"$type":"Assignment","feature":"sections","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]},"cardinality":"+"}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"*"}]}]},"definesHiddenTokens":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"PieSection","definition":{"$type":"Group","elements":[{"$type":"Assignment","feature":"label","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]}},{"$type":"Keyword","value":":"},{"$type":"Assignment","feature":"value","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[]}},{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"PIE_SECTION_LABEL","definition":{"$type":"RegexToken","regex":"/\\"[^\\"]+\\"/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"PIE_SECTION_VALUE","type":{"$type":"ReturnType","name":"number"},"definition":{"$type":"RegexToken","regex":"/(0|[1-9][0-9]*)(\\\\.[0-9]+)?/"},"fragment":false,"hidden":false},{"$type":"ParserRule","name":"TitleAndAccessibilities","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"accDescr","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@7"},"arguments":[]}},{"$type":"Assignment","feature":"accTitle","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@8"},"arguments":[]}},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@9"},"arguments":[]}}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}],"cardinality":"+"},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"EOL","fragment":true,"dataType":"string","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[],"cardinality":"+"},{"$type":"EndOfFile"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"NEWLINE","definition":{"$type":"RegexToken","regex":"/\\\\r?\\\\n/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_DESCR","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accDescr(?:[\\\\t ]*:([^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)|\\\\s*{([^}]*)})/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accTitle[\\\\t ]*:(?:[^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*title(?:[\\\\t ][^\\\\n\\\\r]*?(?=%%)|[\\\\t ][^\\\\n\\\\r]*|)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","hidden":true,"name":"WHITESPACE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]+/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"YAML","definition":{"$type":"RegexToken","regex":"/---[\\\\t ]*\\\\r?\\\\n(?:[\\\\S\\\\s]*?\\\\r?\\\\n)?---(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"DIRECTIVE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%{[\\\\S\\\\s]*?}%%(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"SINGLE_LINE_COMMENT","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%[^\\\\n\\\\r]*/"},"fragment":false}],"definesHiddenTokens":false,"hiddenTokens":[],"interfaces":[{"$type":"Interface","name":"Common","attributes":[{"$type":"TypeAttribute","name":"accDescr","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"accTitle","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"title","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}}],"superTypes":[]}],"types":[],"usedGrammars":[]}')), "PieGrammar");
var loadedArchitectureGrammar;
var ArchitectureGrammar = /* @__PURE__ */ __name(() => loadedArchitectureGrammar ?? (loadedArchitectureGrammar = loadGrammarFromJson('{"$type":"Grammar","isDeclared":true,"name":"Architecture","imports":[],"rules":[{"$type":"ParserRule","name":"Architecture","entry":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[],"cardinality":"*"},{"$type":"Keyword","value":"architecture-beta"},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@16"},"arguments":[]}]},{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[],"cardinality":"*"}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[],"cardinality":"*"}]}]},"definesHiddenTokens":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Statement","fragment":true,"definition":{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"groups","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}},{"$type":"Assignment","feature":"services","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@6"},"arguments":[]}},{"$type":"Assignment","feature":"junctions","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@7"},"arguments":[]}},{"$type":"Assignment","feature":"edges","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@8"},"arguments":[]}}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"LeftPort","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Keyword","value":":"},{"$type":"Assignment","feature":"lhsDir","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@9"},"arguments":[]}}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"RightPort","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Assignment","feature":"rhsDir","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@9"},"arguments":[]}},{"$type":"Keyword","value":":"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Arrow","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]},{"$type":"Assignment","feature":"lhsInto","operator":"?=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@15"},"arguments":[]},"cardinality":"?"},{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"--"},{"$type":"Group","elements":[{"$type":"Keyword","value":"-"},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@13"},"arguments":[]}},{"$type":"Keyword","value":"-"}]}]},{"$type":"Assignment","feature":"rhsInto","operator":"?=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@15"},"arguments":[]},"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Group","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"group"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}},{"$type":"Assignment","feature":"icon","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@12"},"arguments":[]},"cardinality":"?"},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@13"},"arguments":[]},"cardinality":"?"},{"$type":"Group","elements":[{"$type":"Keyword","value":"in"},{"$type":"Assignment","feature":"in","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}}],"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Service","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"service"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}},{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"iconText","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@11"},"arguments":[]}},{"$type":"Assignment","feature":"icon","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@12"},"arguments":[]}}],"cardinality":"?"},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@13"},"arguments":[]},"cardinality":"?"},{"$type":"Group","elements":[{"$type":"Keyword","value":"in"},{"$type":"Assignment","feature":"in","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}}],"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Junction","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"junction"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}},{"$type":"Group","elements":[{"$type":"Keyword","value":"in"},{"$type":"Assignment","feature":"in","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}}],"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Edge","definition":{"$type":"Group","elements":[{"$type":"Assignment","feature":"lhsId","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}},{"$type":"Assignment","feature":"lhsGroup","operator":"?=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@14"},"arguments":[]},"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@4"},"arguments":[]},{"$type":"Assignment","feature":"rhsId","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@10"},"arguments":[]}},{"$type":"Assignment","feature":"rhsGroup","operator":"?=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@14"},"arguments":[]},"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"ARROW_DIRECTION","definition":{"$type":"TerminalAlternatives","elements":[{"$type":"TerminalAlternatives","elements":[{"$type":"TerminalAlternatives","elements":[{"$type":"CharacterRange","left":{"$type":"Keyword","value":"L"}},{"$type":"CharacterRange","left":{"$type":"Keyword","value":"R"}}]},{"$type":"CharacterRange","left":{"$type":"Keyword","value":"T"}}]},{"$type":"CharacterRange","left":{"$type":"Keyword","value":"B"}}]},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARCH_ID","definition":{"$type":"RegexToken","regex":"/[\\\\w]+/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARCH_TEXT_ICON","definition":{"$type":"RegexToken","regex":"/\\\\(\\"[^\\"]+\\"\\\\)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARCH_ICON","definition":{"$type":"RegexToken","regex":"/\\\\([\\\\w-:]+\\\\)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARCH_TITLE","definition":{"$type":"RegexToken","regex":"/\\\\[[\\\\w ]+\\\\]/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARROW_GROUP","definition":{"$type":"RegexToken","regex":"/\\\\{group\\\\}/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ARROW_INTO","definition":{"$type":"RegexToken","regex":"/<|>/"},"fragment":false,"hidden":false},{"$type":"ParserRule","name":"TitleAndAccessibilities","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"accDescr","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@19"},"arguments":[]}},{"$type":"Assignment","feature":"accTitle","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@21"},"arguments":[]}}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}],"cardinality":"+"},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"EOL","fragment":true,"dataType":"string","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[],"cardinality":"+"},{"$type":"EndOfFile"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"NEWLINE","definition":{"$type":"RegexToken","regex":"/\\\\r?\\\\n/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_DESCR","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accDescr(?:[\\\\t ]*:([^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)|\\\\s*{([^}]*)})/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accTitle[\\\\t ]*:(?:[^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*title(?:[\\\\t ][^\\\\n\\\\r]*?(?=%%)|[\\\\t ][^\\\\n\\\\r]*|)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","hidden":true,"name":"WHITESPACE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]+/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"YAML","definition":{"$type":"RegexToken","regex":"/---[\\\\t ]*\\\\r?\\\\n(?:[\\\\S\\\\s]*?\\\\r?\\\\n)?---(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"DIRECTIVE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%{[\\\\S\\\\s]*?}%%(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"SINGLE_LINE_COMMENT","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%[^\\\\n\\\\r]*/"},"fragment":false}],"definesHiddenTokens":false,"hiddenTokens":[],"interfaces":[{"$type":"Interface","name":"Common","attributes":[{"$type":"TypeAttribute","name":"accDescr","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"accTitle","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"title","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}}],"superTypes":[]}],"types":[],"usedGrammars":[]}')), "ArchitectureGrammar");
var loadedGitGraphGrammar;
var GitGraphGrammar = /* @__PURE__ */ __name(() => loadedGitGraphGrammar ?? (loadedGitGraphGrammar = loadGrammarFromJson(`{"$type":"Grammar","isDeclared":true,"name":"GitGraph","interfaces":[{"$type":"Interface","name":"Common","attributes":[{"$type":"TypeAttribute","name":"accDescr","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"accTitle","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}},{"$type":"TypeAttribute","name":"title","isOptional":true,"type":{"$type":"SimpleType","primitiveType":"string"}}],"superTypes":[]}],"rules":[{"$type":"ParserRule","name":"TitleAndAccessibilities","fragment":true,"definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Assignment","feature":"accDescr","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@3"},"arguments":[]}},{"$type":"Assignment","feature":"accTitle","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@4"},"arguments":[]}},{"$type":"Assignment","feature":"title","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@5"},"arguments":[]}}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}],"cardinality":"+"},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"EOL","fragment":true,"dataType":"string","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[],"cardinality":"+"},{"$type":"EndOfFile"}]},"definesHiddenTokens":false,"entry":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"NEWLINE","definition":{"$type":"RegexToken","regex":"/\\\\r?\\\\n/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_DESCR","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accDescr(?:[\\\\t ]*:([^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)|\\\\s*{([^}]*)})/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ACC_TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*accTitle[\\\\t ]*:(?:[^\\\\n\\\\r]*?(?=%%)|[^\\\\n\\\\r]*)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"TITLE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*title(?:[\\\\t ][^\\\\n\\\\r]*?(?=%%)|[\\\\t ][^\\\\n\\\\r]*|)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","hidden":true,"name":"WHITESPACE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]+/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"YAML","definition":{"$type":"RegexToken","regex":"/---[\\\\t ]*\\\\r?\\\\n(?:[\\\\S\\\\s]*?\\\\r?\\\\n)?---(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"DIRECTIVE","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%{[\\\\S\\\\s]*?}%%(?:\\\\r?\\\\n|(?!\\\\S))/"},"fragment":false},{"$type":"TerminalRule","hidden":true,"name":"SINGLE_LINE_COMMENT","definition":{"$type":"RegexToken","regex":"/[\\\\t ]*%%[^\\\\n\\\\r]*/"},"fragment":false},{"$type":"ParserRule","name":"GitGraph","entry":true,"definition":{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[],"cardinality":"*"},{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"gitGraph"},{"$type":"Group","elements":[{"$type":"Keyword","value":"gitGraph"},{"$type":"Keyword","value":":"}]},{"$type":"Keyword","value":"gitGraph:"},{"$type":"Group","elements":[{"$type":"Keyword","value":"gitGraph"},{"$type":"RuleCall","rule":{"$ref":"#/rules@12"},"arguments":[]},{"$type":"Keyword","value":":"}]}]},{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[],"cardinality":"*"},{"$type":"Group","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[],"cardinality":"*"},{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@0"},"arguments":[]},{"$type":"Assignment","feature":"statements","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@11"},"arguments":[]}},{"$type":"RuleCall","rule":{"$ref":"#/rules@2"},"arguments":[]}],"cardinality":"*"}]}]},"definesHiddenTokens":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Statement","definition":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@13"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@14"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@15"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@16"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@17"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Direction","definition":{"$type":"Assignment","feature":"dir","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"LR"},{"$type":"Keyword","value":"TB"},{"$type":"Keyword","value":"BT"}]}},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Commit","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"commit"},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"Keyword","value":"id:"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"msg:","cardinality":"?"},{"$type":"Assignment","feature":"message","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"tag:"},{"$type":"Assignment","feature":"tags","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"type:"},{"$type":"Assignment","feature":"type","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"NORMAL"},{"$type":"Keyword","value":"REVERSE"},{"$type":"Keyword","value":"HIGHLIGHT"}]}}]}],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Branch","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"branch"},{"$type":"Assignment","feature":"name","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@19"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}]}},{"$type":"Group","elements":[{"$type":"Keyword","value":"order:"},{"$type":"Assignment","feature":"order","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@18"},"arguments":[]}}],"cardinality":"?"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Merge","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"merge"},{"$type":"Assignment","feature":"branch","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@19"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}]}},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"Keyword","value":"id:"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"tag:"},{"$type":"Assignment","feature":"tags","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"type:"},{"$type":"Assignment","feature":"type","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"NORMAL"},{"$type":"Keyword","value":"REVERSE"},{"$type":"Keyword","value":"HIGHLIGHT"}]}}]}],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"Checkout","definition":{"$type":"Group","elements":[{"$type":"Alternatives","elements":[{"$type":"Keyword","value":"checkout"},{"$type":"Keyword","value":"switch"}]},{"$type":"Assignment","feature":"branch","operator":"=","terminal":{"$type":"Alternatives","elements":[{"$type":"RuleCall","rule":{"$ref":"#/rules@19"},"arguments":[]},{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}]}},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"ParserRule","name":"CherryPicking","definition":{"$type":"Group","elements":[{"$type":"Keyword","value":"cherry-pick"},{"$type":"Alternatives","elements":[{"$type":"Group","elements":[{"$type":"Keyword","value":"id:"},{"$type":"Assignment","feature":"id","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"tag:"},{"$type":"Assignment","feature":"tags","operator":"+=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]},{"$type":"Group","elements":[{"$type":"Keyword","value":"parent:"},{"$type":"Assignment","feature":"parent","operator":"=","terminal":{"$type":"RuleCall","rule":{"$ref":"#/rules@20"},"arguments":[]}}]}],"cardinality":"*"},{"$type":"RuleCall","rule":{"$ref":"#/rules@1"},"arguments":[]}]},"definesHiddenTokens":false,"entry":false,"fragment":false,"hiddenTokens":[],"parameters":[],"wildcard":false},{"$type":"TerminalRule","name":"INT","type":{"$type":"ReturnType","name":"number"},"definition":{"$type":"RegexToken","regex":"/[0-9]+(?=\\\\s)/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"ID","type":{"$type":"ReturnType","name":"string"},"definition":{"$type":"RegexToken","regex":"/\\\\w([-\\\\./\\\\w]*[-\\\\w])?/"},"fragment":false,"hidden":false},{"$type":"TerminalRule","name":"STRING","definition":{"$type":"RegexToken","regex":"/\\"[^\\"]*\\"|'[^']*'/"},"fragment":false,"hidden":false}],"definesHiddenTokens":false,"hiddenTokens":[],"imports":[],"types":[],"usedGrammars":[]}`)), "GitGraphGrammar");

// src/language/generated/module.ts
var InfoLanguageMetaData = {
  languageId: "info",
  fileExtensions: [".mmd", ".mermaid"],
  caseInsensitive: false
};
var PacketLanguageMetaData = {
  languageId: "packet",
  fileExtensions: [".mmd", ".mermaid"],
  caseInsensitive: false
};
var PieLanguageMetaData = {
  languageId: "pie",
  fileExtensions: [".mmd", ".mermaid"],
  caseInsensitive: false
};
var ArchitectureLanguageMetaData = {
  languageId: "architecture",
  fileExtensions: [".mmd", ".mermaid"],
  caseInsensitive: false
};
var GitGraphLanguageMetaData = {
  languageId: "gitGraph",
  fileExtensions: [".mmd", ".mermaid"],
  caseInsensitive: false
};
var MermaidGeneratedSharedModule = {
  AstReflection: /* @__PURE__ */ __name(() => new MermaidAstReflection(), "AstReflection")
};
var InfoGeneratedModule = {
  Grammar: /* @__PURE__ */ __name(() => InfoGrammar(), "Grammar"),
  LanguageMetaData: /* @__PURE__ */ __name(() => InfoLanguageMetaData, "LanguageMetaData"),
  parser: {}
};
var PacketGeneratedModule = {
  Grammar: /* @__PURE__ */ __name(() => PacketGrammar(), "Grammar"),
  LanguageMetaData: /* @__PURE__ */ __name(() => PacketLanguageMetaData, "LanguageMetaData"),
  parser: {}
};
var PieGeneratedModule = {
  Grammar: /* @__PURE__ */ __name(() => PieGrammar(), "Grammar"),
  LanguageMetaData: /* @__PURE__ */ __name(() => PieLanguageMetaData, "LanguageMetaData"),
  parser: {}
};
var ArchitectureGeneratedModule = {
  Grammar: /* @__PURE__ */ __name(() => ArchitectureGrammar(), "Grammar"),
  LanguageMetaData: /* @__PURE__ */ __name(() => ArchitectureLanguageMetaData, "LanguageMetaData"),
  parser: {}
};
var GitGraphGeneratedModule = {
  Grammar: /* @__PURE__ */ __name(() => GitGraphGrammar(), "Grammar"),
  LanguageMetaData: /* @__PURE__ */ __name(() => GitGraphLanguageMetaData, "LanguageMetaData"),
  parser: {}
};

// src/language/common/valueConverter.ts
import { DefaultValueConverter } from "langium";

// src/language/common/matcher.ts
var accessibilityDescrRegex = /accDescr(?:[\t ]*:([^\n\r]*)|\s*{([^}]*)})/;
var accessibilityTitleRegex = /accTitle[\t ]*:([^\n\r]*)/;
var titleRegex = /title([\t ][^\n\r]*|)/;

// src/language/common/valueConverter.ts
var rulesRegexes = {
  ACC_DESCR: accessibilityDescrRegex,
  ACC_TITLE: accessibilityTitleRegex,
  TITLE: titleRegex
};
var AbstractMermaidValueConverter = class extends DefaultValueConverter {
  static {
    __name(this, "AbstractMermaidValueConverter");
  }
  runConverter(rule, input, cstNode) {
    let value = this.runCommonConverter(rule, input, cstNode);
    if (value === void 0) {
      value = this.runCustomConverter(rule, input, cstNode);
    }
    if (value === void 0) {
      return super.runConverter(rule, input, cstNode);
    }
    return value;
  }
  runCommonConverter(rule, input, _cstNode) {
    const regex = rulesRegexes[rule.name];
    if (regex === void 0) {
      return void 0;
    }
    const match = regex.exec(input);
    if (match === null) {
      return void 0;
    }
    if (match[1] !== void 0) {
      return match[1].trim().replace(/[\t ]{2,}/gm, " ");
    }
    if (match[2] !== void 0) {
      return match[2].replace(/^\s*/gm, "").replace(/\s+$/gm, "").replace(/[\t ]{2,}/gm, " ").replace(/[\n\r]{2,}/gm, "\n");
    }
    return void 0;
  }
};
var CommonValueConverter = class extends AbstractMermaidValueConverter {
  static {
    __name(this, "CommonValueConverter");
  }
  runCustomConverter(_rule, _input, _cstNode) {
    return void 0;
  }
};

// src/language/common/tokenBuilder.ts
import { DefaultTokenBuilder } from "langium";
var AbstractMermaidTokenBuilder = class extends DefaultTokenBuilder {
  static {
    __name(this, "AbstractMermaidTokenBuilder");
  }
  constructor(keywords) {
    super();
    this.keywords = new Set(keywords);
  }
  buildKeywordTokens(rules, terminalTokens, options) {
    const tokenTypes = super.buildKeywordTokens(rules, terminalTokens, options);
    tokenTypes.forEach((tokenType) => {
      if (this.keywords.has(tokenType.name) && tokenType.PATTERN !== void 0) {
        tokenType.PATTERN = new RegExp(tokenType.PATTERN.toString() + "(?:(?=%%)|(?!\\S))");
      }
    });
    return tokenTypes;
  }
};
var CommonTokenBuilder = class extends AbstractMermaidTokenBuilder {
  static {
    __name(this, "CommonTokenBuilder");
  }
};

export {
  __name,
  Statement,
  Architecture,
  isArchitecture,
  Branch,
  isBranch,
  Commit,
  isCommit,
  isCommon,
  GitGraph,
  isGitGraph,
  Info,
  isInfo,
  Merge,
  isMerge,
  Packet,
  isPacket,
  PacketBlock,
  isPacketBlock,
  Pie,
  isPie,
  PieSection,
  isPieSection,
  MermaidGeneratedSharedModule,
  InfoGeneratedModule,
  PacketGeneratedModule,
  PieGeneratedModule,
  ArchitectureGeneratedModule,
  GitGraphGeneratedModule,
  AbstractMermaidValueConverter,
  CommonValueConverter,
  AbstractMermaidTokenBuilder,
  CommonTokenBuilder
};
