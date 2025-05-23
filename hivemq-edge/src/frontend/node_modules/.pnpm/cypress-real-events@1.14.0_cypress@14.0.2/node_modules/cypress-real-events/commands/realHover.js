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
exports.realHover = realHover;
const fireCdpCommand_1 = require("../fireCdpCommand");
const getCypressElementCoordinates_1 = require("../getCypressElementCoordinates");
const getModifiers_1 = require("../getModifiers");
/** @ignore this, update documentation for this function at index.d.ts */
function realHover(subject_1) {
    return __awaiter(this, arguments, void 0, function* (subject, options = {}) {
        var _a;
        const { x, y } = (0, getCypressElementCoordinates_1.getCypressElementCoordinates)(subject, options.position, options.scrollBehavior);
        const log = Cypress.log({
            $el: subject,
            name: "realHover",
            consoleProps: () => ({
                "Applied To": subject.get(0),
                "Absolute Coordinates": { x, y },
            }),
        });
        const modifiers = (0, getModifiers_1.getModifiers)(options);
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchMouseEvent", {
            x,
            y,
            type: "mouseMoved",
            button: "none",
            pointerType: (_a = options.pointer) !== null && _a !== void 0 ? _a : "mouse",
            modifiers: modifiers,
        });
        log.snapshot().end();
        return subject;
    });
}
