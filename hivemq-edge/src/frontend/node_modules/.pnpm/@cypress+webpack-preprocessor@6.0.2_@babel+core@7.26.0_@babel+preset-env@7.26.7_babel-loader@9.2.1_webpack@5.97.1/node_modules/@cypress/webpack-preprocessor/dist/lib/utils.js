"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
var bluebird_1 = __importDefault(require("bluebird"));
function createDeferred() {
    var resolve;
    var reject;
    var promise = new bluebird_1.default(function (_resolve, _reject) {
        resolve = _resolve;
        reject = _reject;
    });
    return {
        //@ts-ignore
        resolve: resolve,
        reject: reject,
        promise: promise,
    };
}
exports.default = {
    createDeferred: createDeferred,
};
