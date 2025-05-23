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
var HoverController_1;
import { KeyChord } from '../../../../base/common/keyCodes.js';
import { Disposable, DisposableStore } from '../../../../base/common/lifecycle.js';
import { EditorAction, registerEditorAction, registerEditorContribution } from '../../../browser/editorExtensions.js';
import { Range } from '../../../common/core/range.js';
import { EditorContextKeys } from '../../../common/editorContextKeys.js';
import { ILanguageService } from '../../../common/languages/language.js';
import { GotoDefinitionAtPositionEditorContribution } from '../../gotoSymbol/browser/link/goToDefinitionAtPosition.js';
import { ContentHoverWidget, ContentHoverController } from './contentHover.js';
import { MarginHoverWidget } from './marginHover.js';
import { IInstantiationService } from '../../../../platform/instantiation/common/instantiation.js';
import { IOpenerService } from '../../../../platform/opener/common/opener.js';
import { editorHoverBorder } from '../../../../platform/theme/common/colorRegistry.js';
import { registerThemingParticipant } from '../../../../platform/theme/common/themeService.js';
import { HoverParticipantRegistry } from './hoverTypes.js';
import { MarkdownHoverParticipant } from './markdownHoverParticipant.js';
import { MarkerHoverParticipant } from './markerHoverParticipant.js';
import { InlineSuggestionHintsContentWidget } from '../../inlineCompletions/browser/inlineCompletionsHintsWidget.js';
import { IKeybindingService } from '../../../../platform/keybinding/common/keybinding.js';
import { RunOnceScheduler } from '../../../../base/common/async.js';
import * as nls from '../../../../nls.js';
import './hover.css';
// sticky hover widget which doesn't disappear on focus out and such
const _sticky = false;
let HoverController = HoverController_1 = class HoverController extends Disposable {
    constructor(_editor, _instantiationService, _openerService, _languageService, _keybindingService) {
        super();
        this._editor = _editor;
        this._instantiationService = _instantiationService;
        this._openerService = _openerService;
        this._languageService = _languageService;
        this._keybindingService = _keybindingService;
        this._listenersStore = new DisposableStore();
        this._hoverState = {
            mouseDown: false,
            contentHoverFocused: false,
            activatedByDecoratorClick: false
        };
        this._reactToEditorMouseMoveRunner = this._register(new RunOnceScheduler(() => this._reactToEditorMouseMove(this._mouseMoveEvent), 0));
        this._hookListeners();
        this._register(this._editor.onDidChangeConfiguration((e) => {
            if (e.hasChanged(60 /* EditorOption.hover */)) {
                this._unhookListeners();
                this._hookListeners();
            }
        }));
    }
    static get(editor) {
        return editor.getContribution(HoverController_1.ID);
    }
    _hookListeners() {
        const hoverOpts = this._editor.getOption(60 /* EditorOption.hover */);
        this._hoverSettings = {
            enabled: hoverOpts.enabled,
            sticky: hoverOpts.sticky,
            hidingDelay: hoverOpts.delay
        };
        if (hoverOpts.enabled) {
            this._listenersStore.add(this._editor.onMouseDown((e) => this._onEditorMouseDown(e)));
            this._listenersStore.add(this._editor.onMouseUp(() => this._onEditorMouseUp()));
            this._listenersStore.add(this._editor.onMouseMove((e) => this._onEditorMouseMove(e)));
            this._listenersStore.add(this._editor.onKeyDown((e) => this._onKeyDown(e)));
        }
        else {
            this._listenersStore.add(this._editor.onMouseMove((e) => this._onEditorMouseMove(e)));
            this._listenersStore.add(this._editor.onKeyDown((e) => this._onKeyDown(e)));
        }
        this._listenersStore.add(this._editor.onMouseLeave((e) => this._onEditorMouseLeave(e)));
        this._listenersStore.add(this._editor.onDidChangeModel(() => {
            this._cancelScheduler();
            this._hideWidgets();
        }));
        this._listenersStore.add(this._editor.onDidChangeModelContent(() => this._cancelScheduler()));
        this._listenersStore.add(this._editor.onDidScrollChange((e) => this._onEditorScrollChanged(e)));
    }
    _unhookListeners() {
        this._listenersStore.clear();
    }
    _cancelScheduler() {
        this._mouseMoveEvent = undefined;
        this._reactToEditorMouseMoveRunner.cancel();
    }
    _onEditorScrollChanged(e) {
        if (e.scrollTopChanged || e.scrollLeftChanged) {
            this._hideWidgets();
        }
    }
    _onEditorMouseDown(mouseEvent) {
        var _a;
        this._hoverState.mouseDown = true;
        const target = mouseEvent.target;
        if (target.type === 9 /* MouseTargetType.CONTENT_WIDGET */ && target.detail === ContentHoverWidget.ID) {
            // mouse down on top of content hover widget
            this._hoverState.contentHoverFocused = true;
            return;
        }
        if (target.type === 12 /* MouseTargetType.OVERLAY_WIDGET */ && target.detail === MarginHoverWidget.ID) {
            // mouse down on top of margin hover widget
            return;
        }
        if (target.type !== 12 /* MouseTargetType.OVERLAY_WIDGET */) {
            this._hoverState.contentHoverFocused = false;
        }
        if ((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.widget.isResizing) {
            return;
        }
        this._hideWidgets();
    }
    _onEditorMouseUp() {
        this._hoverState.mouseDown = false;
    }
    _onEditorMouseLeave(mouseEvent) {
        var _a, _b;
        this._cancelScheduler();
        const targetElement = (mouseEvent.event.browserEvent.relatedTarget);
        if (((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.widget.isResizing) || ((_b = this._contentWidget) === null || _b === void 0 ? void 0 : _b.containsNode(targetElement))) {
            // When the content widget is resizing
            // When the mouse is inside hover widget
            return;
        }
        if (_sticky) {
            return;
        }
        this._hideWidgets();
    }
    _isMouseOverWidget(mouseEvent) {
        var _a, _b, _c, _d, _e;
        const target = mouseEvent.target;
        const sticky = this._hoverSettings.sticky;
        if (sticky
            && target.type === 9 /* MouseTargetType.CONTENT_WIDGET */
            && target.detail === ContentHoverWidget.ID) {
            // mouse moved on top of content hover widget
            return true;
        }
        if (sticky
            && ((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.containsNode((_b = mouseEvent.event.browserEvent.view) === null || _b === void 0 ? void 0 : _b.document.activeElement))
            && !((_d = (_c = mouseEvent.event.browserEvent.view) === null || _c === void 0 ? void 0 : _c.getSelection()) === null || _d === void 0 ? void 0 : _d.isCollapsed)) {
            // selected text within content hover widget
            return true;
        }
        if (!sticky
            && target.type === 9 /* MouseTargetType.CONTENT_WIDGET */
            && target.detail === ContentHoverWidget.ID
            && ((_e = this._contentWidget) === null || _e === void 0 ? void 0 : _e.isColorPickerVisible)) {
            // though the hover is not sticky, the color picker is sticky
            return true;
        }
        if (sticky
            && target.type === 12 /* MouseTargetType.OVERLAY_WIDGET */
            && target.detail === MarginHoverWidget.ID) {
            // mouse moved on top of overlay hover widget
            return true;
        }
        return false;
    }
    _onEditorMouseMove(mouseEvent) {
        var _a, _b, _c, _d;
        this._mouseMoveEvent = mouseEvent;
        if (((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.isFocused) || ((_b = this._contentWidget) === null || _b === void 0 ? void 0 : _b.isResizing)) {
            return;
        }
        if (this._hoverState.mouseDown && this._hoverState.contentHoverFocused) {
            return;
        }
        const sticky = this._hoverSettings.sticky;
        if (sticky && ((_c = this._contentWidget) === null || _c === void 0 ? void 0 : _c.isVisibleFromKeyboard)) {
            // Sticky mode is on and the hover has been shown via keyboard
            // so moving the mouse has no effect
            return;
        }
        const mouseIsOverWidget = this._isMouseOverWidget(mouseEvent);
        // If the mouse is over the widget and the hiding timeout is defined, then cancel it
        if (mouseIsOverWidget) {
            this._reactToEditorMouseMoveRunner.cancel();
            return;
        }
        // If the mouse is not over the widget, and if sticky is on,
        // then give it a grace period before reacting to the mouse event
        const hidingDelay = this._hoverSettings.hidingDelay;
        if (((_d = this._contentWidget) === null || _d === void 0 ? void 0 : _d.isVisible) && sticky && hidingDelay > 0) {
            if (!this._reactToEditorMouseMoveRunner.isScheduled()) {
                this._reactToEditorMouseMoveRunner.schedule(hidingDelay);
            }
            return;
        }
        this._reactToEditorMouseMove(mouseEvent);
    }
    _reactToEditorMouseMove(mouseEvent) {
        var _a, _b, _c, _d;
        if (!mouseEvent) {
            return;
        }
        const target = mouseEvent.target;
        const mouseOnDecorator = (_a = target.element) === null || _a === void 0 ? void 0 : _a.classList.contains('colorpicker-color-decoration');
        const decoratorActivatedOn = this._editor.getOption(147 /* EditorOption.colorDecoratorsActivatedOn */);
        const enabled = this._hoverSettings.enabled;
        const activatedByDecoratorClick = this._hoverState.activatedByDecoratorClick;
        if ((mouseOnDecorator && ((decoratorActivatedOn === 'click' && !activatedByDecoratorClick) ||
            (decoratorActivatedOn === 'hover' && !enabled && !_sticky) ||
            (decoratorActivatedOn === 'clickAndHover' && !enabled && !activatedByDecoratorClick))) || (!mouseOnDecorator && !enabled && !activatedByDecoratorClick)) {
            this._hideWidgets();
            return;
        }
        const contentWidget = this._getOrCreateContentWidget();
        if (contentWidget.showsOrWillShow(mouseEvent)) {
            (_b = this._glyphWidget) === null || _b === void 0 ? void 0 : _b.hide();
            return;
        }
        if (target.type === 2 /* MouseTargetType.GUTTER_GLYPH_MARGIN */ && target.position && target.detail.glyphMarginLane) {
            (_c = this._contentWidget) === null || _c === void 0 ? void 0 : _c.hide();
            const glyphWidget = this._getOrCreateGlyphWidget();
            glyphWidget.startShowingAt(target.position.lineNumber, target.detail.glyphMarginLane);
            return;
        }
        if (target.type === 3 /* MouseTargetType.GUTTER_LINE_NUMBERS */ && target.position) {
            (_d = this._contentWidget) === null || _d === void 0 ? void 0 : _d.hide();
            const glyphWidget = this._getOrCreateGlyphWidget();
            glyphWidget.startShowingAt(target.position.lineNumber, 'lineNo');
            return;
        }
        if (_sticky) {
            return;
        }
        this._hideWidgets();
    }
    _onKeyDown(e) {
        var _a;
        if (!this._editor.hasModel()) {
            return;
        }
        const resolvedKeyboardEvent = this._keybindingService.softDispatch(e, this._editor.getDomNode());
        // If the beginning of a multi-chord keybinding is pressed,
        // or the command aims to focus the hover,
        // set the variable to true, otherwise false
        const mightTriggerFocus = (resolvedKeyboardEvent.kind === 1 /* ResultKind.MoreChordsNeeded */ ||
            (resolvedKeyboardEvent.kind === 2 /* ResultKind.KbFound */
                && resolvedKeyboardEvent.commandId === 'editor.action.showHover'
                && ((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.isVisible)));
        if (e.keyCode === 5 /* KeyCode.Ctrl */
            || e.keyCode === 6 /* KeyCode.Alt */
            || e.keyCode === 57 /* KeyCode.Meta */
            || e.keyCode === 4 /* KeyCode.Shift */
            || mightTriggerFocus) {
            // Do not hide hover when a modifier key is pressed
            return;
        }
        this._hideWidgets();
    }
    _hideWidgets() {
        var _a, _b, _c;
        if (_sticky) {
            return;
        }
        if ((this._hoverState.mouseDown
            && this._hoverState.contentHoverFocused
            && ((_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.isColorPickerVisible))
            || InlineSuggestionHintsContentWidget.dropDownVisible) {
            return;
        }
        this._hoverState.activatedByDecoratorClick = false;
        this._hoverState.contentHoverFocused = false;
        (_b = this._glyphWidget) === null || _b === void 0 ? void 0 : _b.hide();
        (_c = this._contentWidget) === null || _c === void 0 ? void 0 : _c.hide();
    }
    _getOrCreateContentWidget() {
        if (!this._contentWidget) {
            this._contentWidget = this._instantiationService.createInstance(ContentHoverController, this._editor);
        }
        return this._contentWidget;
    }
    _getOrCreateGlyphWidget() {
        if (!this._glyphWidget) {
            this._glyphWidget = new MarginHoverWidget(this._editor, this._languageService, this._openerService);
        }
        return this._glyphWidget;
    }
    showContentHover(range, mode, source, focus, activatedByColorDecoratorClick = false) {
        this._hoverState.activatedByDecoratorClick = activatedByColorDecoratorClick;
        this._getOrCreateContentWidget().startShowingAtRange(range, mode, source, focus);
    }
    focus() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.focus();
    }
    scrollUp() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.scrollUp();
    }
    scrollDown() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.scrollDown();
    }
    scrollLeft() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.scrollLeft();
    }
    scrollRight() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.scrollRight();
    }
    pageUp() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.pageUp();
    }
    pageDown() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.pageDown();
    }
    goToTop() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.goToTop();
    }
    goToBottom() {
        var _a;
        (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.goToBottom();
    }
    get isColorPickerVisible() {
        var _a;
        return (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.isColorPickerVisible;
    }
    get isHoverVisible() {
        var _a;
        return (_a = this._contentWidget) === null || _a === void 0 ? void 0 : _a.isVisible;
    }
    dispose() {
        var _a, _b;
        super.dispose();
        this._unhookListeners();
        this._listenersStore.dispose();
        (_a = this._glyphWidget) === null || _a === void 0 ? void 0 : _a.dispose();
        (_b = this._contentWidget) === null || _b === void 0 ? void 0 : _b.dispose();
    }
};
HoverController.ID = 'editor.contrib.hover';
HoverController = HoverController_1 = __decorate([
    __param(1, IInstantiationService),
    __param(2, IOpenerService),
    __param(3, ILanguageService),
    __param(4, IKeybindingService)
], HoverController);
export { HoverController };
var HoverFocusBehavior;
(function (HoverFocusBehavior) {
    HoverFocusBehavior["NoAutoFocus"] = "noAutoFocus";
    HoverFocusBehavior["FocusIfVisible"] = "focusIfVisible";
    HoverFocusBehavior["AutoFocusImmediately"] = "autoFocusImmediately";
})(HoverFocusBehavior || (HoverFocusBehavior = {}));
class ShowOrFocusHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.showHover',
            label: nls.localize({
                key: 'showOrFocusHover',
                comment: [
                    'Label for action that will trigger the showing/focusing of a hover in the editor.',
                    'If the hover is not visible, it will show the hover.',
                    'This allows for users to show the hover without using the mouse.'
                ]
            }, "Show or Focus Hover"),
            metadata: {
                description: `Show or Focus Hover`,
                args: [{
                        name: 'args',
                        schema: {
                            type: 'object',
                            properties: {
                                'focus': {
                                    description: 'Controls if and when the hover should take focus upon being triggered by this action.',
                                    enum: [HoverFocusBehavior.NoAutoFocus, HoverFocusBehavior.FocusIfVisible, HoverFocusBehavior.AutoFocusImmediately],
                                    enumDescriptions: [
                                        nls.localize('showOrFocusHover.focus.noAutoFocus', 'The hover will not automatically take focus.'),
                                        nls.localize('showOrFocusHover.focus.focusIfVisible', 'The hover will take focus only if it is already visible.'),
                                        nls.localize('showOrFocusHover.focus.autoFocusImmediately', 'The hover will automatically take focus when it appears.'),
                                    ],
                                    default: HoverFocusBehavior.FocusIfVisible,
                                }
                            },
                        }
                    }]
            },
            alias: 'Show or Focus Hover',
            precondition: undefined,
            kbOpts: {
                kbExpr: EditorContextKeys.editorTextFocus,
                primary: KeyChord(2048 /* KeyMod.CtrlCmd */ | 41 /* KeyCode.KeyK */, 2048 /* KeyMod.CtrlCmd */ | 39 /* KeyCode.KeyI */),
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor, args) {
        if (!editor.hasModel()) {
            return;
        }
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        const focusArgument = args === null || args === void 0 ? void 0 : args.focus;
        let focusOption = HoverFocusBehavior.FocusIfVisible;
        if (Object.values(HoverFocusBehavior).includes(focusArgument)) {
            focusOption = focusArgument;
        }
        else if (typeof focusArgument === 'boolean' && focusArgument) {
            focusOption = HoverFocusBehavior.AutoFocusImmediately;
        }
        const showContentHover = (focus) => {
            const position = editor.getPosition();
            const range = new Range(position.lineNumber, position.column, position.lineNumber, position.column);
            controller.showContentHover(range, 1 /* HoverStartMode.Immediate */, 1 /* HoverStartSource.Keyboard */, focus);
        };
        const accessibilitySupportEnabled = editor.getOption(2 /* EditorOption.accessibilitySupport */) === 2 /* AccessibilitySupport.Enabled */;
        if (controller.isHoverVisible) {
            if (focusOption !== HoverFocusBehavior.NoAutoFocus) {
                controller.focus();
            }
            else {
                showContentHover(accessibilitySupportEnabled);
            }
        }
        else {
            showContentHover(accessibilitySupportEnabled || focusOption === HoverFocusBehavior.AutoFocusImmediately);
        }
    }
}
class ShowDefinitionPreviewHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.showDefinitionPreviewHover',
            label: nls.localize({
                key: 'showDefinitionPreviewHover',
                comment: [
                    'Label for action that will trigger the showing of definition preview hover in the editor.',
                    'This allows for users to show the definition preview hover without using the mouse.'
                ]
            }, "Show Definition Preview Hover"),
            alias: 'Show Definition Preview Hover',
            precondition: undefined
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        const position = editor.getPosition();
        if (!position) {
            return;
        }
        const range = new Range(position.lineNumber, position.column, position.lineNumber, position.column);
        const goto = GotoDefinitionAtPositionEditorContribution.get(editor);
        if (!goto) {
            return;
        }
        const promise = goto.startFindDefinitionFromCursor(position);
        promise.then(() => {
            controller.showContentHover(range, 1 /* HoverStartMode.Immediate */, 1 /* HoverStartSource.Keyboard */, true);
        });
    }
}
class ScrollUpHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.scrollUpHover',
            label: nls.localize({
                key: 'scrollUpHover',
                comment: [
                    'Action that allows to scroll up in the hover widget with the up arrow when the hover widget is focused.'
                ]
            }, "Scroll Up Hover"),
            alias: 'Scroll Up Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 16 /* KeyCode.UpArrow */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.scrollUp();
    }
}
class ScrollDownHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.scrollDownHover',
            label: nls.localize({
                key: 'scrollDownHover',
                comment: [
                    'Action that allows to scroll down in the hover widget with the up arrow when the hover widget is focused.'
                ]
            }, "Scroll Down Hover"),
            alias: 'Scroll Down Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 18 /* KeyCode.DownArrow */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.scrollDown();
    }
}
class ScrollLeftHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.scrollLeftHover',
            label: nls.localize({
                key: 'scrollLeftHover',
                comment: [
                    'Action that allows to scroll left in the hover widget with the left arrow when the hover widget is focused.'
                ]
            }, "Scroll Left Hover"),
            alias: 'Scroll Left Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 15 /* KeyCode.LeftArrow */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.scrollLeft();
    }
}
class ScrollRightHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.scrollRightHover',
            label: nls.localize({
                key: 'scrollRightHover',
                comment: [
                    'Action that allows to scroll right in the hover widget with the right arrow when the hover widget is focused.'
                ]
            }, "Scroll Right Hover"),
            alias: 'Scroll Right Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 17 /* KeyCode.RightArrow */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.scrollRight();
    }
}
class PageUpHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.pageUpHover',
            label: nls.localize({
                key: 'pageUpHover',
                comment: [
                    'Action that allows to page up in the hover widget with the page up command when the hover widget is focused.'
                ]
            }, "Page Up Hover"),
            alias: 'Page Up Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 11 /* KeyCode.PageUp */,
                secondary: [512 /* KeyMod.Alt */ | 16 /* KeyCode.UpArrow */],
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.pageUp();
    }
}
class PageDownHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.pageDownHover',
            label: nls.localize({
                key: 'pageDownHover',
                comment: [
                    'Action that allows to page down in the hover widget with the page down command when the hover widget is focused.'
                ]
            }, "Page Down Hover"),
            alias: 'Page Down Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 12 /* KeyCode.PageDown */,
                secondary: [512 /* KeyMod.Alt */ | 18 /* KeyCode.DownArrow */],
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.pageDown();
    }
}
class GoToTopHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.goToTopHover',
            label: nls.localize({
                key: 'goToTopHover',
                comment: [
                    'Action that allows to go to the top of the hover widget with the home command when the hover widget is focused.'
                ]
            }, "Go To Top Hover"),
            alias: 'Go To Bottom Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 14 /* KeyCode.Home */,
                secondary: [2048 /* KeyMod.CtrlCmd */ | 16 /* KeyCode.UpArrow */],
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.goToTop();
    }
}
class GoToBottomHoverAction extends EditorAction {
    constructor() {
        super({
            id: 'editor.action.goToBottomHover',
            label: nls.localize({
                key: 'goToBottomHover',
                comment: [
                    'Action that allows to go to the bottom in the hover widget with the end command when the hover widget is focused.'
                ]
            }, "Go To Bottom Hover"),
            alias: 'Go To Bottom Hover',
            precondition: EditorContextKeys.hoverFocused,
            kbOpts: {
                kbExpr: EditorContextKeys.hoverFocused,
                primary: 13 /* KeyCode.End */,
                secondary: [2048 /* KeyMod.CtrlCmd */ | 18 /* KeyCode.DownArrow */],
                weight: 100 /* KeybindingWeight.EditorContrib */
            }
        });
    }
    run(accessor, editor) {
        const controller = HoverController.get(editor);
        if (!controller) {
            return;
        }
        controller.goToBottom();
    }
}
registerEditorContribution(HoverController.ID, HoverController, 2 /* EditorContributionInstantiation.BeforeFirstInteraction */);
registerEditorAction(ShowOrFocusHoverAction);
registerEditorAction(ShowDefinitionPreviewHoverAction);
registerEditorAction(ScrollUpHoverAction);
registerEditorAction(ScrollDownHoverAction);
registerEditorAction(ScrollLeftHoverAction);
registerEditorAction(ScrollRightHoverAction);
registerEditorAction(PageUpHoverAction);
registerEditorAction(PageDownHoverAction);
registerEditorAction(GoToTopHoverAction);
registerEditorAction(GoToBottomHoverAction);
HoverParticipantRegistry.register(MarkdownHoverParticipant);
HoverParticipantRegistry.register(MarkerHoverParticipant);
// theming
registerThemingParticipant((theme, collector) => {
    const hoverBorder = theme.getColor(editorHoverBorder);
    if (hoverBorder) {
        collector.addRule(`.monaco-editor .monaco-hover .hover-row:not(:first-child):not(:empty) { border-top: 1px solid ${hoverBorder.transparent(0.5)}; }`);
        collector.addRule(`.monaco-editor .monaco-hover hr { border-top: 1px solid ${hoverBorder.transparent(0.5)}; }`);
        collector.addRule(`.monaco-editor .monaco-hover hr { border-bottom: 0px solid ${hoverBorder.transparent(0.5)}; }`);
    }
});
