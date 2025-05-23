import { CompleteTheme } from '@nivo/core';
import { ReactElement } from 'react';
type PartialShallow<T> = {
    [P in keyof T]?: T[P] extends object ? object : T[P];
};
type PropertyName = string | number | symbol;
type IterateeShorthand<T> = PropertyName | [PropertyName, any] | PartialShallow<T>;
type ListIterator<T, TResult> = (value: T, index: number, collection: ArrayLike<T>) => TResult;
type ListIterateeCustom<T, TResult> = ListIterator<T, TResult> | IterateeShorthand<T>;
export type RelativeOrAbsolutePosition = number | {
    abs: number;
};
export type AnnotationPositionGetter<Datum> = (datum: Datum) => {
    x: number;
    y: number;
};
export type AnnotationDimensionsGetter<Datum> = (datum: Datum) => {
    size: number;
    width: number;
    height: number;
};
export type NoteComponent<Datum> = (props: {
    datum: Datum;
    x: number;
    y: number;
}) => JSX.Element;
export type NoteSvg<Datum> = string | ReactElement | NoteComponent<Datum>;
export type NoteCanvasRenderer<Datum> = (ctx: CanvasRenderingContext2D, props: {
    datum: Datum;
    x: number;
    y: number;
    theme: CompleteTheme;
}) => void;
export type NoteCanvas<Datum> = string | NoteCanvasRenderer<Datum>;
export type Note<Datum> = NoteSvg<Datum> | NoteCanvas<Datum>;
export interface BaseAnnotationSpec<Datum> {
    x?: number;
    y?: number;
    note: Note<Datum>;
    noteX: RelativeOrAbsolutePosition;
    noteY: RelativeOrAbsolutePosition;
    noteWidth?: number;
    noteTextOffset?: number;
}
export type CircleAnnotationSpec<Datum> = BaseAnnotationSpec<Datum> & {
    type: 'circle';
    size?: number;
    offset?: number;
    height?: never;
    width?: never;
};
export type DotAnnotationSpec<Datum> = BaseAnnotationSpec<Datum> & {
    type: 'dot';
    size: number;
    offset?: number;
    height?: never;
    width?: never;
};
export type RectAnnotationSpec<Datum> = BaseAnnotationSpec<Datum> & {
    type: 'rect';
    width?: number;
    height?: number;
    offset?: number;
    size?: never;
    borderRadius?: number;
};
export type AnnotationSpec<Datum> = CircleAnnotationSpec<Datum> | DotAnnotationSpec<Datum> | RectAnnotationSpec<Datum>;
export type AnnotationType = AnnotationSpec<unknown>['type'];
export type AnnotationMatcher<Datum> = AnnotationSpec<Datum> & {
    match: ListIterateeCustom<Datum, boolean>;
    offset?: number;
};
export type BoundAnnotation<Datum> = Required<AnnotationSpec<Datum>> & {
    x: number;
    y: number;
    datum: Datum;
};
export type AnnotationInstructions = {
    points: [number, number][];
    text: [number, number];
    angle: number;
};
export type ComputedAnnotation<Datum> = BoundAnnotation<Datum> & {
    computed: AnnotationInstructions;
};
export {};
//# sourceMappingURL=types.d.ts.map