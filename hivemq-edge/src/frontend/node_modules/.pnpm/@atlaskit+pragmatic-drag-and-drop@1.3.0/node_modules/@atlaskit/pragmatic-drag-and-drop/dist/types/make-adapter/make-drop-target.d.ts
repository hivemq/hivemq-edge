import { type AllDragTypes, type DropTargetAllowedDropEffect, type DropTargetAPI } from '../internal-types';
export declare function makeDropTarget<DragType extends AllDragTypes>({ typeKey, defaultDropEffect, }: {
    typeKey: DragType['type'];
    defaultDropEffect: DropTargetAllowedDropEffect;
}): DropTargetAPI<DragType>;
