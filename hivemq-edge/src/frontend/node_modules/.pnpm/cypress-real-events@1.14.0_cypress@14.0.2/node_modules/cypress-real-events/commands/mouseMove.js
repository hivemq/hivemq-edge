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
exports.realMouseMove = realMouseMove;
const fireCdpCommand_1 = require("../fireCdpCommand");
const getCypressElementCoordinates_1 = require("../getCypressElementCoordinates");
const getModifiers_1 = require("../getModifiers");
/** @ignore this, update documentation for this function at index.d.ts */
function realMouseMove(subject_1, x_1, y_1) {
    return __awaiter(this, arguments, void 0, function* (subject, x, y, options = {}) {
        var _a;
        const basePosition = (0, getCypressElementCoordinates_1.getCypressElementCoordinates)(subject, (_a = options.position) !== null && _a !== void 0 ? _a : "topLeft", options.scrollBehavior);
        const log = Cypress.log({
            $el: subject,
            name: "realMouseMove",
            consoleProps: () => ({
                "Applied To": subject.get(0),
                "Absolute Element Coordinates": basePosition,
            }),
        });
        const modifiers = (0, getModifiers_1.getModifiers)(options);
        log.snapshot("before");
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchMouseEvent", {
            type: "mouseMoved",
            x: x * basePosition.frameScale + basePosition.x,
            y: y * basePosition.frameScale + basePosition.y,
            modifiers: modifiers,
        });
        log.snapshot("after").end();
        return subject;
    });
}
