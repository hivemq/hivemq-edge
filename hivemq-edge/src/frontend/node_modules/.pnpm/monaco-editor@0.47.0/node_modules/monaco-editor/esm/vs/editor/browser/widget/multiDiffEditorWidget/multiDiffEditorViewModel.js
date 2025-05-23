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
import { Disposable } from '../../../../base/common/lifecycle.js';
import { observableFromEvent, observableValue } from '../../../../base/common/observable.js';
import { mapObservableArrayCached } from '../../../../base/common/observableInternal/utils.js';
import { DiffEditorOptions } from '../diffEditor/diffEditorOptions.js';
import { DiffEditorViewModel } from '../diffEditor/diffEditorViewModel.js';
import { IModelService } from '../../../common/services/model.js';
import { IInstantiationService } from '../../../../platform/instantiation/common/instantiation.js';
export class MultiDiffEditorViewModel extends Disposable {
    get contextKeys() {
        return this.model.contextKeys;
    }
    constructor(model, _instantiationService) {
        super();
        this.model = model;
        this._instantiationService = _instantiationService;
        this._documents = observableFromEvent(this.model.onDidChange, /** @description MultiDiffEditorViewModel.documents */ () => this.model.documents);
        this.items = mapObservableArrayCached(this, this._documents, (d, store) => store.add(this._instantiationService.createInstance(DocumentDiffItemViewModel, d)))
            .recomputeInitiallyAndOnChange(this._store);
        this.activeDiffItem = observableValue(this, undefined);
    }
}
let DocumentDiffItemViewModel = class DocumentDiffItemViewModel extends Disposable {
    get originalUri() { var _a; return (_a = this.entry.value.original) === null || _a === void 0 ? void 0 : _a.uri; }
    get modifiedUri() { var _a; return (_a = this.entry.value.modified) === null || _a === void 0 ? void 0 : _a.uri; }
    constructor(entry, _instantiationService, _modelService) {
        var _a, _b;
        super();
        this.entry = entry;
        this._instantiationService = _instantiationService;
        this._modelService = _modelService;
        this.collapsed = observableValue(this, false);
        this.lastTemplateData = observableValue(this, { contentHeight: 500, selections: undefined, });
        function updateOptions(options) {
            return {
                ...options,
                hideUnchangedRegions: {
                    enabled: true,
                },
            };
        }
        const options = new DiffEditorOptions(updateOptions(this.entry.value.options || {}));
        if (this.entry.value.onOptionsDidChange) {
            this._register(this.entry.value.onOptionsDidChange(() => {
                options.updateOptions(updateOptions(this.entry.value.options || {}));
            }));
        }
        const originalTextModel = (_a = this.entry.value.original) !== null && _a !== void 0 ? _a : this._register(this._modelService.createModel('', null));
        const modifiedTextModel = (_b = this.entry.value.modified) !== null && _b !== void 0 ? _b : this._register(this._modelService.createModel('', null));
        this.diffEditorViewModel = this._register(this._instantiationService.createInstance(DiffEditorViewModel, {
            original: originalTextModel,
            modified: modifiedTextModel,
        }, options));
    }
    getKey() {
        var _a, _b;
        return JSON.stringify([
            (_a = this.originalUri) === null || _a === void 0 ? void 0 : _a.toString(),
            (_b = this.modifiedUri) === null || _b === void 0 ? void 0 : _b.toString()
        ]);
    }
};
DocumentDiffItemViewModel = __decorate([
    __param(1, IInstantiationService),
    __param(2, IModelService)
], DocumentDiffItemViewModel);
export { DocumentDiffItemViewModel };
