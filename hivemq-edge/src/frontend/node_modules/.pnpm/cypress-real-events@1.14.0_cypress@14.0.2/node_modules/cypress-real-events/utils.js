"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.wait = void 0;
/**
 * Returns a Promise that resolves after `ms` milliseconds.
 * @param ms wait time in milliseconds.
 */
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
exports.wait = wait;
