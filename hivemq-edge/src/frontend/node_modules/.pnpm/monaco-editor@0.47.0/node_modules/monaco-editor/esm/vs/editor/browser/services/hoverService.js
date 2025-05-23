/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
import { registerSingleton } from '../../../platform/instantiation/common/extensions.js';
import { registerThemingParticipant } from '../../../platform/theme/common/themeService.js';
import { editorHoverBorder } from '../../../platform/theme/common/colorRegistry.js';
import { IHoverService } from '../../../platform/hover/browser/hover.js';
import { IContextMenuService, IContextViewService } from '../../../platform/contextview/browser/contextView.js';
import { IInstantiationService } from '../../../platform/instantiation/common/instantiation.js';
import { HoverWidget } from '../widget/hoverWidget/hoverWidget.js';
import { DisposableStore, toDisposable } from '../../../base/common/lifecycle.js';
import { addDisposableListener, EventType, getActiveElement, isAncestorOfActiveElement, isAncestor, getWindow } from '../../../base/browser/dom.js';
import { IKeybindingService } from '../../../platform/keybinding/common/keybinding.js';
import { StandardKeyboardEvent } from '../../../base/browser/keyboardEvent.js';
import { IAccessibilityService } from '../../../platform/accessibility/common/accessibility.js';
import { ILayoutService } from '../../../platform/layout/browser/layoutService.js';
import { mainWindow } from '../../../base/browser/window.js';
let HoverService = class HoverService {
    constructor(_instantiationService, _contextViewService, contextMenuService, _keybindingService, _layoutService, _accessibilityService) {
        this._instantiationService = _instantiationService;
        this._contextViewService = _contextViewService;
        this._keybindingService = _keybindingService;
        this._layoutService = _layoutService;
        this._accessibilityService = _accessibilityService;
        contextMenuService.onDidShowContextMenu(() => this.hideHover());
    }
    showHover(options, focus, skipLastFocusedUpdate) {
        var _a, _b, _c, _d;
        if (getHoverOptionsIdentity(this._currentHoverOptions) === getHoverOptionsIdentity(options)) {
            return undefined;
        }
        if (this._currentHover && ((_b = (_a = this._currentHoverOptions) === null || _a === void 0 ? void 0 : _a.persistence) === null || _b === void 0 ? void 0 : _b.sticky)) {
            return undefined;
        }
        this._currentHoverOptions = options;
        this._lastHoverOptions = options;
        const trapFocus = options.trapFocus || this._accessibilityService.isScreenReaderOptimized();
        const activeElement = getActiveElement();
        // HACK, remove this check when #189076 is fixed
        if (!skipLastFocusedUpdate) {
            if (trapFocus && activeElement) {
                this._lastFocusedElementBeforeOpen = activeElement;
            }
            else {
                this._lastFocusedElementBeforeOpen = undefined;
            }
        }
        const hoverDisposables = new DisposableStore();
        const hover = this._instantiationService.createInstance(HoverWidget, options);
        if ((_c = options.persistence) === null || _c === void 0 ? void 0 : _c.sticky) {
            hover.isLocked = true;
        }
        hover.onDispose(() => {
            var _a, _b;
            const hoverWasFocused = ((_a = this._currentHover) === null || _a === void 0 ? void 0 : _a.domNode) && isAncestorOfActiveElement(this._currentHover.domNode);
            if (hoverWasFocused) {
                // Required to handle cases such as closing the hover with the escape key
                (_b = this._lastFocusedElementBeforeOpen) === null || _b === void 0 ? void 0 : _b.focus();
            }
            // Only clear the current options if it's the current hover, the current options help
            // reduce flickering when the same hover is shown multiple times
            if (this._currentHoverOptions === options) {
                this._currentHoverOptions = undefined;
            }
            hoverDisposables.dispose();
        });
        // Set the container explicitly to enable aux window support
        if (!options.container) {
            const targetElement = options.target instanceof HTMLElement ? options.target : options.target.targetElements[0];
            options.container = this._layoutService.getContainer(getWindow(targetElement));
        }
        const provider = this._contextViewService;
        provider.showContextView(new HoverContextViewDelegate(hover, focus), options.container);
        hover.onRequestLayout(() => provider.layout());
        if ((_d = options.persistence) === null || _d === void 0 ? void 0 : _d.sticky) {
            hoverDisposables.add(addDisposableListener(getWindow(options.container).document, EventType.MOUSE_DOWN, e => {
                if (!isAncestor(e.target, hover.domNode)) {
                    this.doHideHover();
                }
            }));
        }
        else {
            if ('targetElements' in options.target) {
                for (const element of options.target.targetElements) {
                    hoverDisposables.add(addDisposableListener(element, EventType.CLICK, () => this.hideHover()));
                }
            }
            else {
                hoverDisposables.add(addDisposableListener(options.target, EventType.CLICK, () => this.hideHover()));
            }
            const focusedElement = getActiveElement();
            if (focusedElement) {
                const focusedElementDocument = getWindow(focusedElement).document;
                hoverDisposables.add(addDisposableListener(focusedElement, EventType.KEY_DOWN, e => { var _a; return this._keyDown(e, hover, !!((_a = options.persistence) === null || _a === void 0 ? void 0 : _a.hideOnKeyDown)); }));
                hoverDisposables.add(addDisposableListener(focusedElementDocument, EventType.KEY_DOWN, e => { var _a; return this._keyDown(e, hover, !!((_a = options.persistence) === null || _a === void 0 ? void 0 : _a.hideOnKeyDown)); }));
                hoverDisposables.add(addDisposableListener(focusedElement, EventType.KEY_UP, e => this._keyUp(e, hover)));
                hoverDisposables.add(addDisposableListener(focusedElementDocument, EventType.KEY_UP, e => this._keyUp(e, hover)));
            }
        }
        if ('IntersectionObserver' in mainWindow) {
            const observer = new IntersectionObserver(e => this._intersectionChange(e, hover), { threshold: 0 });
            const firstTargetElement = 'targetElements' in options.target ? options.target.targetElements[0] : options.target;
            observer.observe(firstTargetElement);
            hoverDisposables.add(toDisposable(() => observer.disconnect()));
        }
        this._currentHover = hover;
        return hover;
    }
    hideHover() {
        var _a;
        if (((_a = this._currentHover) === null || _a === void 0 ? void 0 : _a.isLocked) || !this._currentHoverOptions) {
            return;
        }
        this.doHideHover();
    }
    doHideHover() {
        this._currentHover = undefined;
        this._currentHoverOptions = undefined;
        this._contextViewService.hideContextView();
    }
    _intersectionChange(entries, hover) {
        const entry = entries[entries.length - 1];
        if (!entry.isIntersecting) {
            hover.dispose();
        }
    }
    _keyDown(e, hover, hideOnKeyDown) {
        var _a, _b;
        if (e.key === 'Alt') {
            hover.isLocked = true;
            return;
        }
        const event = new StandardKeyboardEvent(e);
        const keybinding = this._keybindingService.resolveKeyboardEvent(event);
        if (keybinding.getSingleModifierDispatchChords().some(value => !!value) || this._keybindingService.softDispatch(event, event.target).kind !== 0 /* ResultKind.NoMatchingKb */) {
            return;
        }
        if (hideOnKeyDown && (!((_a = this._currentHoverOptions) === null || _a === void 0 ? void 0 : _a.trapFocus) || e.key !== 'Tab')) {
            this.hideHover();
            (_b = this._lastFocusedElementBeforeOpen) === null || _b === void 0 ? void 0 : _b.focus();
        }
    }
    _keyUp(e, hover) {
        var _a;
        if (e.key === 'Alt') {
            hover.isLocked = false;
            // Hide if alt is released while the mouse is not over hover/target
            if (!hover.isMouseIn) {
                this.hideHover();
                (_a = this._lastFocusedElementBeforeOpen) === null || _a === void 0 ? void 0 : _a.focus();
            }
        }
    }
};
HoverService = __decorate([
    __param(0, IInstantiationService),
    __param(1, IContextViewService),
    __param(2, IContextMenuService),
    __param(3, IKeybindingService),
    __param(4, ILayoutService),
    __param(5, IAccessibilityService)
], HoverService);
export { HoverService };
function getHoverOptionsIdentity(options) {
    var _a;
    if (options === undefined) {
        return undefined;
    }
    return (_a = options === null || options === void 0 ? void 0 : options.id) !== null && _a !== void 0 ? _a : options;
}
class HoverContextViewDelegate {
    get anchorPosition() {
        return this._hover.anchor;
    }
    constructor(_hover, _focus = false) {
        this._hover = _hover;
        this._focus = _focus;
    }
    render(container) {
        this._hover.render(container);
        if (this._focus) {
            this._hover.focus();
        }
        return this._hover;
    }
    getAnchor() {
        return {
            x: this._hover.x,
            y: this._hover.y
        };
    }
    layout() {
        this._hover.layout();
    }
}
registerSingleton(IHoverService, HoverService, 1 /* InstantiationType.Delayed */);
registerThemingParticipant((theme, collector) => {
    const hoverBorder = theme.getColor(editorHoverBorder);
    if (hoverBorder) {
        collector.addRule(`.monaco-workbench .workbench-hover .hover-row:not(:first-child):not(:empty) { border-top: 1px solid ${hoverBorder.transparent(0.5)}; }`);
        collector.addRule(`.monaco-workbench .workbench-hover hr { border-top: 1px solid ${hoverBorder.transparent(0.5)}; }`);
    }
});
