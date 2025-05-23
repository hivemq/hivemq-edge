import { type AllDragTypes, type CleanupFn } from '../internal-types';
export declare function register<TypeKey extends AllDragTypes['type']>(args: {
    typeKey: TypeKey;
    mount: () => CleanupFn;
}): CleanupFn;
