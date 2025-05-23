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
exports.realPress = realPress;
const fireCdpCommand_1 = require("../fireCdpCommand");
const keyCodeDefinitions_1 = require("../keyCodeDefinitions");
const keyToModifierBitMap_1 = require("../keyToModifierBitMap");
const utils_1 = require("../utils");
function getKeyDefinition(key) {
    var _a, _b, _c, _d;
    const keyDefinition = keyCodeDefinitions_1.keyCodeDefinitions[key];
    if (!keyDefinition) {
        throw new Error(`Unsupported key '${key}'.`);
    }
    const keyCode = (_a = keyDefinition.keyCode) !== null && _a !== void 0 ? _a : 0;
    return {
        keyCode: keyCode,
        key: (_b = keyDefinition === null || keyDefinition === void 0 ? void 0 : keyDefinition.key) !== null && _b !== void 0 ? _b : "",
        text: keyDefinition.key.length === 1 ? keyDefinition.key : undefined,
        // @ts-expect-error code exists anyway
        code: (_c = keyDefinition.code) !== null && _c !== void 0 ? _c : "",
        // @ts-expect-error location exists anyway
        location: (_d = keyDefinition.location) !== null && _d !== void 0 ? _d : 0,
        windowsVirtualKeyCode: keyCode,
    };
}
/** @ignore this, update documentation for this function at index.d.ts */
function realPress(keyOrShortcut_1) {
    return __awaiter(this, arguments, void 0, function* (keyOrShortcut, options = {}) {
        var _a, _b, _c;
        let log;
        let modifiers = 0;
        const keys = Array.isArray(keyOrShortcut) ? keyOrShortcut : [keyOrShortcut];
        const keyDefinitions = keys.map(getKeyDefinition);
        if ((_a = options.log) !== null && _a !== void 0 ? _a : true) {
            log = Cypress.log({
                name: "realPress",
                consoleProps: () => ({
                    "System Key Definition": keyDefinitions,
                }),
            });
        }
        log === null || log === void 0 ? void 0 : log.snapshot("before").end();
        for (const key of keyDefinitions) {
            modifiers |= (_b = keyToModifierBitMap_1.keyToModifierBitMap[key.key]) !== null && _b !== void 0 ? _b : 0;
            yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchKeyEvent", Object.assign({ type: key.text ? "keyDown" : "rawKeyDown", modifiers }, key));
            if (key.code === "Enter") {
                yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchKeyEvent", {
                    type: "char",
                    unmodifiedText: "\r",
                    text: "\r",
                });
            }
            yield (0, utils_1.wait)((_c = options.pressDelay) !== null && _c !== void 0 ? _c : 25);
        }
        yield Promise.all(keyDefinitions.map((key) => {
            return (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchKeyEvent", Object.assign({ type: "keyUp", modifiers }, key));
        }));
        log === null || log === void 0 ? void 0 : log.snapshot("after").end();
    });
}
