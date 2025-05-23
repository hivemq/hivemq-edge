import { UnbindFn, InferEventType, Binding } from './types';

export function bind<
  TTarget extends EventTarget,
  TType extends InferEventType<TTarget> | (string & {}),
>(target: TTarget, { type, listener, options }: Binding<TTarget, TType>): UnbindFn {
  target.addEventListener(type, listener, options);

  return function unbind() {
    target.removeEventListener(type, listener, options);
  };
}
