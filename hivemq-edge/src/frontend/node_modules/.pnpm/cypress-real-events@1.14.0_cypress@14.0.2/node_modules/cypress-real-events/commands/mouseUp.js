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
exports.realMouseUp = realMouseUp;
const fireCdpCommand_1 = require("../fireCdpCommand");
const getCypressElementCoordinates_1 = require("../getCypressElementCoordinates");
const mouseButtonNumbers_1 = require("../mouseButtonNumbers");
const getModifiers_1 = require("../getModifiers");
/** @ignore this, update documentation for this function at index.d.ts */
function realMouseUp(subject_1) {
    return __awaiter(this, arguments, void 0, function* (subject, options = {}) {
        var _a, _b, _c;
        const position = options.x && options.y ? { x: options.x, y: options.y } : options.position;
        const { x, y } = (0, getCypressElementCoordinates_1.getCypressElementCoordinates)(subject, position, options.scrollBehavior);
        const log = Cypress.log({
            $el: subject,
            name: "realMouseUp",
            consoleProps: () => ({
                "Applied To": subject.get(0),
                "Absolute Coordinates": { x, y },
            }),
        });
        const modifiers = (0, getModifiers_1.getModifiers)(options);
        log.snapshot("before");
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchMouseEvent", {
            type: "mouseReleased",
            x,
            y,
            clickCount: 1,
            buttons: mouseButtonNumbers_1.mouseButtonNumbers[(_a = options.button) !== null && _a !== void 0 ? _a : "left"],
            pointerType: (_b = options.pointer) !== null && _b !== void 0 ? _b : "mouse",
            button: (_c = options.button) !== null && _c !== void 0 ? _c : "left",
            modifiers: modifiers,
        });
        log.snapshot("after").end();
        return subject;
    });
}
