import { UnbindFn, InferEventType, Binding } from './types';
export declare function bind<TTarget extends EventTarget, TType extends InferEventType<TTarget> | (string & {})>(target: TTarget, { type, listener, options }: Binding<TTarget, TType>): UnbindFn;
