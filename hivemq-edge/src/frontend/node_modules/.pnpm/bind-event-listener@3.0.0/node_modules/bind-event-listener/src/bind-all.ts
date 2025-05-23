import { Binding, InferEventType, Listener, UnbindFn } from './types';
import { bind } from './bind';

function toOptions(value?: boolean | AddEventListenerOptions): AddEventListenerOptions | undefined {
  if (typeof value === 'undefined') {
    return undefined;
  }

  // Convert capture boolean format to options to object
  if (typeof value === 'boolean') {
    return {
      capture: value,
    };
  }

  // Already an object, don't need to convert it
  return value;
}

function getBinding(original: Binding, sharedOptions?: boolean | AddEventListenerOptions): Binding {
  // No shared options, just return original binding
  if (sharedOptions == null) {
    return original;
  }

  const binding: Binding = {
    ...original,
    // Converting both options to object format for merging
    options: {
      ...toOptions(sharedOptions),
      // Giving individual binding option precedence
      ...toOptions(original.options),
    },
  };
  return binding;
}

export function bindAll<
  TTarget extends EventTarget,
  TTypes extends ReadonlyArray<InferEventType<TTarget> | (string & {})>,
>(
  target: TTarget,
  bindings: [
    ...{
      // it could be simplified to `[K in keyof TTypes]: Binding<TTarget, TTypes[K]>;`
      // however, TS@<4.7 has problems with recognizing `TTypes[K]` to have `string` constraint
      // and that is required by `Binding`'s second type parameter
      // we use `TTypes[K] & string` instead to workaround this issue and it works fine
      // but using `Binding<TTarget, TTypes[K] & string>` is not an option
      // as we need to keep `TTypes[K]` "naked" at the `type` property, for it to be a valid inference candidate
      // without that we'd lose the autocompletes for the `type` property
      [K in keyof TTypes]: {
        type: TTypes[K];
        listener: Listener<TTarget, TTypes[K] & string>;
        options?: boolean | AddEventListenerOptions;
      };
    }
  ],
  sharedOptions?: boolean | AddEventListenerOptions,
): UnbindFn {
  const unbinds: UnbindFn[] = bindings.map((original) => {
    const binding: Binding = getBinding(original as never, sharedOptions);
    return bind(target, binding);
  });

  return function unbindAll() {
    unbinds.forEach((unbind) => unbind());
  };
}
