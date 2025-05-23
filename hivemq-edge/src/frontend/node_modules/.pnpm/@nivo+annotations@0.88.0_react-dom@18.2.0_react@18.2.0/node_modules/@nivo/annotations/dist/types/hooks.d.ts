import { AnnotationDimensionsGetter, AnnotationMatcher, AnnotationPositionGetter, BoundAnnotation } from './types';
/**
 * Bind annotations to a dataset.
 */
export declare const useAnnotations: <Datum>({ data, annotations, getPosition, getDimensions, }: {
    data: readonly Datum[];
    annotations: readonly AnnotationMatcher<Datum>[];
    getPosition: AnnotationPositionGetter<Datum>;
    getDimensions: AnnotationDimensionsGetter<Datum>;
}) => BoundAnnotation<Datum>[];
export declare const useComputedAnnotations: <Datum>({ annotations, }: {
    annotations: readonly BoundAnnotation<Datum>[];
}) => ({
    computed: import("./types").AnnotationInstructions;
    x: number;
    y: number;
    note: import("./types").Note<Datum>;
    noteX: import("./types").RelativeOrAbsolutePosition;
    noteY: import("./types").RelativeOrAbsolutePosition;
    noteWidth: number;
    noteTextOffset: number;
    type: "circle";
    size: number;
    offset: number;
    height: never;
    width: never;
    datum: Datum;
} | {
    computed: import("./types").AnnotationInstructions;
    x: number;
    y: number;
    note: import("./types").Note<Datum>;
    noteX: import("./types").RelativeOrAbsolutePosition;
    noteY: import("./types").RelativeOrAbsolutePosition;
    noteWidth: number;
    noteTextOffset: number;
    type: "dot";
    size: number;
    offset: number;
    height: never;
    width: never;
    datum: Datum;
} | {
    computed: import("./types").AnnotationInstructions;
    x: number;
    y: number;
    note: import("./types").Note<Datum>;
    noteX: import("./types").RelativeOrAbsolutePosition;
    noteY: import("./types").RelativeOrAbsolutePosition;
    noteWidth: number;
    noteTextOffset: number;
    type: "rect";
    width: number;
    height: number;
    offset: number;
    size: never;
    borderRadius: number;
    datum: Datum;
})[];
export declare const useComputedAnnotation: <Datum>(annotation: BoundAnnotation<Datum>) => import("./types").AnnotationInstructions;
//# sourceMappingURL=hooks.d.ts.map