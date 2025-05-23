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
exports.realTouch = realTouch;
const fireCdpCommand_1 = require("../fireCdpCommand");
const getCypressElementCoordinates_1 = require("../getCypressElementCoordinates");
function realTouch(subject_1) {
    return __awaiter(this, arguments, void 0, function* (subject, options = {}) {
        const position = typeof options.x === "number" || typeof options.y === "number"
            ? { x: options.x || 0, y: options.y || 0 }
            : options.position;
        const elementCoordinates = (0, getCypressElementCoordinates_1.getCypressElementCoordinates)(subject, position);
        const elementPoint = { x: elementCoordinates.x, y: elementCoordinates.y };
        const radiusX = options.radiusX || options.radius || 1;
        const radiusY = options.radiusY || options.radius || 1;
        const log = Cypress.log({
            $el: subject,
            name: "realTouch",
            consoleProps: () => ({
                "Applied To": subject.get(0),
                "Absolute Coordinates": [elementPoint],
                "Touched Area (Radius)": {
                    x: radiusX,
                    y: radiusY,
                },
            }),
        });
        log.snapshot("before");
        const touchPoint = Object.assign(Object.assign({}, elementPoint), { radiusX,
            radiusY });
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchTouchEvent", {
            type: "touchStart",
            touchPoints: [touchPoint],
        });
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchTouchEvent", {
            type: "touchEnd",
            touchPoints: [touchPoint],
        });
        log.snapshot("after").end();
        return subject;
    });
}
