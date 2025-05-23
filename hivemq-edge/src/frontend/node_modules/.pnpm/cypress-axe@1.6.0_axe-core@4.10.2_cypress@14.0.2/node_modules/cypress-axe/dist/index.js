"use strict";
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.configureAxe = exports.injectAxe = void 0;
exports.injectAxe = function (injectOptions) {
    var fileName = (injectOptions === null || injectOptions === void 0 ? void 0 : injectOptions.axeCorePath) ||
        (typeof (require === null || require === void 0 ? void 0 : require.resolve) === 'function'
            ? require.resolve('axe-core/axe.min.js')
            : 'node_modules/axe-core/axe.min.js');
    cy.readFile(fileName).then(function (source) {
        return cy.window({ log: false }).then(function (window) {
            window.eval(source);
        });
    });
};
exports.configureAxe = function (configurationOptions) {
    if (configurationOptions === void 0) { configurationOptions = {}; }
    cy.window({ log: false }).then(function (win) {
        return win.axe.configure(configurationOptions);
    });
};
function isEmptyObjectorNull(value) {
    if (value == null) {
        return true;
    }
    return Object.entries(value).length === 0 && value.constructor === Object;
}
function summarizeResults(includedImpacts, violations) {
    return includedImpacts &&
        Array.isArray(includedImpacts) &&
        Boolean(includedImpacts.length)
        ? violations.filter(function (v) { return v.impact && includedImpacts.includes(v.impact); })
        : violations;
}
var checkA11y = function (context, options, violationCallback, skipFailures) {
    if (skipFailures === void 0) { skipFailures = false; }
    cy.window({ log: false })
        .then(function (win) {
        if (isEmptyObjectorNull(context)) {
            context = undefined;
        }
        if (isEmptyObjectorNull(options)) {
            options = undefined;
        }
        if (isEmptyObjectorNull(violationCallback)) {
            violationCallback = undefined;
        }
        var _a = options || {}, includedImpacts = _a.includedImpacts, interval = _a.interval, retries = _a.retries, axeOptions = __rest(_a, ["includedImpacts", "interval", "retries"]);
        var remainingRetries = retries || 0;
        function runAxeCheck() {
            return win.axe
                .run(context || win.document, axeOptions)
                .then(function (_a) {
                var violations = _a.violations;
                var results = summarizeResults(includedImpacts, violations);
                if (results.length > 0 && remainingRetries > 0) {
                    remainingRetries--;
                    return new Promise(function (resolve) {
                        setTimeout(resolve, interval || 1000);
                    }).then(runAxeCheck);
                }
                else {
                    return results;
                }
            });
        }
        return runAxeCheck();
    })
        .then(function (violations) {
        if (violations.length) {
            if (violationCallback) {
                violationCallback(violations);
            }
            violations.forEach(function (v) {
                var selectors = v.nodes
                    .reduce(function (acc, node) { return acc.concat(node.target); }, [])
                    .join(', ');
                Cypress.log({
                    $el: Cypress.$(selectors),
                    name: 'a11y error!',
                    consoleProps: function () { return v; },
                    message: v.id + " on " + v.nodes.length + " Node" + (v.nodes.length === 1 ? '' : 's'),
                });
            });
        }
        return cy.wrap(violations, { log: false });
    })
        .then(function (violations) {
        if (!skipFailures) {
            assert.equal(violations.length, 0, violations.length + " accessibility violation" + (violations.length === 1 ? '' : 's') + " " + (violations.length === 1 ? 'was' : 'were') + " detected");
        }
        else if (violations.length) {
            Cypress.log({
                name: 'a11y violation summary',
                message: violations.length + " accessibility violation" + (violations.length === 1 ? '' : 's') + " " + (violations.length === 1 ? 'was' : 'were') + " detected",
            });
        }
    });
};
Cypress.Commands.add('injectAxe', exports.injectAxe);
Cypress.Commands.add('configureAxe', exports.configureAxe);
Cypress.Commands.add('checkA11y', checkA11y);
