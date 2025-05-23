/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 *--------------------------------------------------------------------------------------------*/
import { Lazy } from '../../../common/lazy.js';
const nullHoverDelegateFactory = () => ({
    get delay() { return -1; },
    dispose: () => { },
    showHover: () => { return undefined; },
});
let hoverDelegateFactory = nullHoverDelegateFactory;
const defaultHoverDelegateMouse = new Lazy(() => hoverDelegateFactory('mouse', false));
const defaultHoverDelegateElement = new Lazy(() => hoverDelegateFactory('element', false));
export function setHoverDelegateFactory(hoverDelegateProvider) {
    hoverDelegateFactory = hoverDelegateProvider;
}
export function getDefaultHoverDelegate(placement, enableInstantHover) {
    if (enableInstantHover) {
        // If instant hover is enabled, the consumer is responsible for disposing the hover delegate
        return hoverDelegateFactory(placement, true);
    }
    if (placement === 'element') {
        return defaultHoverDelegateElement.value;
    }
    return defaultHoverDelegateMouse.value;
}
