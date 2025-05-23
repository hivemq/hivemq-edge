"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.realType = realType;
const keyCodeDefinitions_1 = require("../keyCodeDefinitions");
const realPress_1 = require("./realPress");
const utils_1 = require("../utils");
const availableChars = Object.keys(keyCodeDefinitions_1.keyCodeDefinitions);
function assertChar(char) {
    if (!availableChars.includes(char)) {
        throw new Error(`Unrecognized character "${char}".`);
    }
}
/** @ignore this, update documentation for this function at index.d.ts */
function realType(text_1) {
    return __awaiter(this, arguments, void 0, function* (text, options = {}) {
        var _a, _b, _c;
        let log;
        if ((_a = options.log) !== null && _a !== void 0 ? _a : true) {
            log = Cypress.log({
                name: "realType",
                consoleProps: () => ({
                    Text: text,
                }),
            });
        }
        log === null || log === void 0 ? void 0 : log.snapshot("before").end();
        const chars = text
            .split(/({.+?})/)
            .filter(Boolean)
            .reduce((acc, group) => {
            return /({.+?})/.test(group)
                ? [...acc, group]
                : [...acc, ...group.split("")];
        }, []);
        for (const char of chars) {
            assertChar(char);
            yield (0, realPress_1.realPress)(char, {
                pressDelay: (_b = options.pressDelay) !== null && _b !== void 0 ? _b : 15,
                log: false,
            });
            yield (0, utils_1.wait)((_c = options.delay) !== null && _c !== void 0 ? _c : 25);
        }
        log === null || log === void 0 ? void 0 : log.snapshot("after").end();
    });
}
