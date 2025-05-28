/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
import { getActiveElement } from '../../../../base/browser/dom.js';
import { Codicon } from '../../../../base/common/codicons.js';
import { EditorAction2 } from '../../editorExtensions.js';
import { ICodeEditorService } from '../../services/codeEditorService.js';
import { DiffEditorWidget } from './diffEditorWidget.js';
import { EditorContextKeys } from '../../../common/editorContextKeys.js';
import { localize, localize2 } from '../../../../nls.js';
import { Action2, MenuId, MenuRegistry, registerAction2 } from '../../../../platform/actions/common/actions.js';
import { CommandsRegistry } from '../../../../platform/commands/common/commands.js';
import { IConfigurationService } from '../../../../platform/configuration/common/configuration.js';
import { ContextKeyEqualsExpr, ContextKeyExpr } from '../../../../platform/contextkey/common/contextkey.js';
import './registrations.contribution.js';
export class ToggleCollapseUnchangedRegions extends Action2 {
    constructor() {
        super({
            id: 'diffEditor.toggleCollapseUnchangedRegions',
            title: localize2('toggleCollapseUnchangedRegions', 'Toggle Collapse Unchanged Regions'),
            icon: Codicon.map,
            toggled: ContextKeyExpr.has('config.diffEditor.hideUnchangedRegions.enabled'),
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            menu: {
                when: ContextKeyExpr.has('isInDiffEditor'),
                id: MenuId.EditorTitle,
                order: 22,
                group: 'navigation',
            },
        });
    }
    run(accessor, ...args) {
        const configurationService = accessor.get(IConfigurationService);
        const newValue = !configurationService.getValue('diffEditor.hideUnchangedRegions.enabled');
        configurationService.updateValue('diffEditor.hideUnchangedRegions.enabled', newValue);
    }
}
registerAction2(ToggleCollapseUnchangedRegions);
export class ToggleShowMovedCodeBlocks extends Action2 {
    constructor() {
        super({
            id: 'diffEditor.toggleShowMovedCodeBlocks',
            title: localize2('toggleShowMovedCodeBlocks', 'Toggle Show Moved Code Blocks'),
            precondition: ContextKeyExpr.has('isInDiffEditor'),
        });
    }
    run(accessor, ...args) {
        const configurationService = accessor.get(IConfigurationService);
        const newValue = !configurationService.getValue('diffEditor.experimental.showMoves');
        configurationService.updateValue('diffEditor.experimental.showMoves', newValue);
    }
}
registerAction2(ToggleShowMovedCodeBlocks);
export class ToggleUseInlineViewWhenSpaceIsLimited extends Action2 {
    constructor() {
        super({
            id: 'diffEditor.toggleUseInlineViewWhenSpaceIsLimited',
            title: localize2('toggleUseInlineViewWhenSpaceIsLimited', 'Toggle Use Inline View When Space Is Limited'),
            precondition: ContextKeyExpr.has('isInDiffEditor'),
        });
    }
    run(accessor, ...args) {
        const configurationService = accessor.get(IConfigurationService);
        const newValue = !configurationService.getValue('diffEditor.useInlineViewWhenSpaceIsLimited');
        configurationService.updateValue('diffEditor.useInlineViewWhenSpaceIsLimited', newValue);
    }
}
registerAction2(ToggleUseInlineViewWhenSpaceIsLimited);
MenuRegistry.appendMenuItem(MenuId.EditorTitle, {
    command: {
        id: new ToggleUseInlineViewWhenSpaceIsLimited().desc.id,
        title: localize('useInlineViewWhenSpaceIsLimited', "Use Inline View When Space Is Limited"),
        toggled: ContextKeyExpr.has('config.diffEditor.useInlineViewWhenSpaceIsLimited'),
        precondition: ContextKeyExpr.has('isInDiffEditor'),
    },
    order: 11,
    group: '1_diff',
    when: ContextKeyExpr.and(EditorContextKeys.diffEditorRenderSideBySideInlineBreakpointReached, ContextKeyExpr.has('isInDiffEditor')),
});
MenuRegistry.appendMenuItem(MenuId.EditorTitle, {
    command: {
        id: new ToggleShowMovedCodeBlocks().desc.id,
        title: localize('showMoves', "Show Moved Code Blocks"),
        icon: Codicon.move,
        toggled: ContextKeyEqualsExpr.create('config.diffEditor.experimental.showMoves', true),
        precondition: ContextKeyExpr.has('isInDiffEditor'),
    },
    order: 10,
    group: '1_diff',
    when: ContextKeyExpr.has('isInDiffEditor'),
});
const diffEditorCategory = localize2('diffEditor', "Diff Editor");
export class SwitchSide extends EditorAction2 {
    constructor() {
        super({
            id: 'diffEditor.switchSide',
            title: localize2('switchSide', 'Switch Side'),
            icon: Codicon.arrowSwap,
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            f1: true,
            category: diffEditorCategory,
        });
    }
    runEditorCommand(accessor, editor, arg) {
        const diffEditor = findFocusedDiffEditor(accessor);
        if (diffEditor instanceof DiffEditorWidget) {
            if (arg && arg.dryRun) {
                return { destinationSelection: diffEditor.mapToOtherSide().destinationSelection };
            }
            else {
                diffEditor.switchSide();
            }
        }
        return undefined;
    }
}
registerAction2(SwitchSide);
export class ExitCompareMove extends EditorAction2 {
    constructor() {
        super({
            id: 'diffEditor.exitCompareMove',
            title: localize2('exitCompareMove', 'Exit Compare Move'),
            icon: Codicon.close,
            precondition: EditorContextKeys.comparingMovedCode,
            f1: false,
            category: diffEditorCategory,
            keybinding: {
                weight: 10000,
                primary: 9 /* KeyCode.Escape */,
            }
        });
    }
    runEditorCommand(accessor, editor, ...args) {
        const diffEditor = findFocusedDiffEditor(accessor);
        if (diffEditor instanceof DiffEditorWidget) {
            diffEditor.exitCompareMove();
        }
    }
}
registerAction2(ExitCompareMove);
export class CollapseAllUnchangedRegions extends EditorAction2 {
    constructor() {
        super({
            id: 'diffEditor.collapseAllUnchangedRegions',
            title: localize2('collapseAllUnchangedRegions', 'Collapse All Unchanged Regions'),
            icon: Codicon.fold,
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            f1: true,
            category: diffEditorCategory,
        });
    }
    runEditorCommand(accessor, editor, ...args) {
        const diffEditor = findFocusedDiffEditor(accessor);
        if (diffEditor instanceof DiffEditorWidget) {
            diffEditor.collapseAllUnchangedRegions();
        }
    }
}
registerAction2(CollapseAllUnchangedRegions);
export class ShowAllUnchangedRegions extends EditorAction2 {
    constructor() {
        super({
            id: 'diffEditor.showAllUnchangedRegions',
            title: localize2('showAllUnchangedRegions', 'Show All Unchanged Regions'),
            icon: Codicon.unfold,
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            f1: true,
            category: diffEditorCategory,
        });
    }
    runEditorCommand(accessor, editor, ...args) {
        const diffEditor = findFocusedDiffEditor(accessor);
        if (diffEditor instanceof DiffEditorWidget) {
            diffEditor.showAllUnchangedRegions();
        }
    }
}
registerAction2(ShowAllUnchangedRegions);
const accessibleDiffViewerCategory = localize2('accessibleDiffViewer', "Accessible Diff Viewer");
export class AccessibleDiffViewerNext extends Action2 {
    constructor() {
        super({
            id: AccessibleDiffViewerNext.id,
            title: localize2('editor.action.accessibleDiffViewer.next', 'Go to Next Difference'),
            category: accessibleDiffViewerCategory,
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            keybinding: {
                primary: 65 /* KeyCode.F7 */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            },
            f1: true,
        });
    }
    run(accessor) {
        const diffEditor = findFocusedDiffEditor(accessor);
        diffEditor === null || diffEditor === void 0 ? void 0 : diffEditor.accessibleDiffViewerNext();
    }
}
AccessibleDiffViewerNext.id = 'editor.action.accessibleDiffViewer.next';
MenuRegistry.appendMenuItem(MenuId.EditorTitle, {
    command: {
        id: AccessibleDiffViewerNext.id,
        title: localize('Open Accessible Diff Viewer', "Open Accessible Diff Viewer"),
        precondition: ContextKeyExpr.has('isInDiffEditor'),
    },
    order: 10,
    group: '2_diff',
    when: ContextKeyExpr.and(EditorContextKeys.accessibleDiffViewerVisible.negate(), ContextKeyExpr.has('isInDiffEditor')),
});
export class AccessibleDiffViewerPrev extends Action2 {
    constructor() {
        super({
            id: AccessibleDiffViewerPrev.id,
            title: localize2('editor.action.accessibleDiffViewer.prev', 'Go to Previous Difference'),
            category: accessibleDiffViewerCategory,
            precondition: ContextKeyExpr.has('isInDiffEditor'),
            keybinding: {
                primary: 1024 /* KeyMod.Shift */ | 65 /* KeyCode.F7 */,
                weight: 100 /* KeybindingWeight.EditorContrib */
            },
            f1: true,
        });
    }
    run(accessor) {
        const diffEditor = findFocusedDiffEditor(accessor);
        diffEditor === null || diffEditor === void 0 ? void 0 : diffEditor.accessibleDiffViewerPrev();
    }
}
AccessibleDiffViewerPrev.id = 'editor.action.accessibleDiffViewer.prev';
export function findFocusedDiffEditor(accessor) {
    const codeEditorService = accessor.get(ICodeEditorService);
    const diffEditors = codeEditorService.listDiffEditors();
    const activeElement = getActiveElement();
    if (activeElement) {
        for (const d of diffEditors) {
            const container = d.getContainerDomNode();
            if (isElementOrParentOf(container, activeElement)) {
                return d;
            }
        }
    }
    return null;
}
function isElementOrParentOf(elementOrParent, element) {
    let e = element;
    while (e) {
        if (e === elementOrParent) {
            return true;
        }
        e = e.parentElement;
    }
    return false;
}
CommandsRegistry.registerCommandAlias('editor.action.diffReview.next', AccessibleDiffViewerNext.id);
registerAction2(AccessibleDiffViewerNext);
CommandsRegistry.registerCommandAlias('editor.action.diffReview.prev', AccessibleDiffViewerPrev.id);
registerAction2(AccessibleDiffViewerPrev);
