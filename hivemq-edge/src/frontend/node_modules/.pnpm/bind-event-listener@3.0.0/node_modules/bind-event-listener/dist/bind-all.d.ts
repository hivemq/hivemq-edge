import { InferEventType, Listener, UnbindFn } from './types';
export declare function bindAll<TTarget extends EventTarget, TTypes extends ReadonlyArray<InferEventType<TTarget> | (string & {})>>(target: TTarget, bindings: [
    ...{
        [K in keyof TTypes]: {
            type: TTypes[K];
            listener: Listener<TTarget, TTypes[K] & string>;
            options?: boolean | AddEventListenerOptions;
        };
    }
], sharedOptions?: boolean | AddEventListenerOptions): UnbindFn;
