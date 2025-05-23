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
exports.realSwipe = realSwipe;
const fireCdpCommand_1 = require("../fireCdpCommand");
const getCypressElementCoordinates_1 = require("../getCypressElementCoordinates");
function forEachSwipePosition(_a, onStep_1) {
    return __awaiter(this, arguments, void 0, function* ({ length, step, startPosition, direction, }, onStep) {
        if (length < step) {
            throw new Error("cy.realSwipe: options.length can not be smaller than options.step");
        }
        const getPositionByDirection = {
            toTop: (step) => ({
                x: startPosition.x,
                y: startPosition.y - step,
            }),
            toBottom: (step) => ({
                x: startPosition.x,
                y: startPosition.y + step,
            }),
            toLeft: (step) => ({
                x: startPosition.x - step,
                y: startPosition.y,
            }),
            toRight: (step) => ({
                x: startPosition.x + step,
                y: startPosition.y,
            }),
        };
        for (let i = 0; i <= length; i += step) {
            yield onStep(getPositionByDirection[direction](i));
        }
    });
}
function realSwipe(subject_1, direction_1) {
    return __awaiter(this, arguments, void 0, function* (subject, direction, options = {}) {
        var _a, _b, _c, _d;
        const position = typeof options.x === "number" || typeof options.y === "number"
            ? { x: (_a = options.x) !== null && _a !== void 0 ? _a : 0, y: (_b = options.y) !== null && _b !== void 0 ? _b : 0 }
            : options.touchPosition;
        const length = (_c = options.length) !== null && _c !== void 0 ? _c : 10;
        const step = (_d = options.step) !== null && _d !== void 0 ? _d : 10;
        const elementCoordinates = (0, getCypressElementCoordinates_1.getCypressElementCoordinates)(subject, position);
        const startPosition = { x: elementCoordinates.x, y: elementCoordinates.y };
        const log = Cypress.log({
            $el: subject,
            name: "realSwipe",
            consoleProps: () => ({
                "Applied To": subject.get(0),
                "Swipe Length": length,
                "Swipe Step": step,
            }),
        });
        log.snapshot("before");
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchTouchEvent", {
            type: "touchStart",
            touchPoints: [startPosition],
        });
        if (options.touchMoveDelay) {
            // cy.wait() can't be used here since we are in a command that returns a promise.
            // Read more: https://on.cypress.io/returning-promise-and-commands-in-another-command
            yield wait(options.touchMoveDelay);
        }
        yield forEachSwipePosition({
            length,
            step,
            direction,
            startPosition,
        }, (position) => (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchTouchEvent", {
            type: "touchMove",
            touchPoints: [position],
        }));
        yield (0, fireCdpCommand_1.fireCdpCommand)("Input.dispatchTouchEvent", {
            type: "touchEnd",
            touchPoints: [],
        });
        log.snapshot("after").end();
        return subject;
    });
}
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
