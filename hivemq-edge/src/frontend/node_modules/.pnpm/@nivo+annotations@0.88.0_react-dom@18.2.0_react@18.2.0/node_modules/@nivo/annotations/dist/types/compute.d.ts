import { AnnotationPositionGetter, AnnotationDimensionsGetter, BoundAnnotation, AnnotationMatcher, AnnotationInstructions } from './types';
export declare const bindAnnotations: <Datum = {
    x: number;
    y: number;
}>({ data, annotations, getPosition, getDimensions, }: {
    data: readonly Datum[];
    annotations: readonly AnnotationMatcher<Datum>[];
    getPosition: AnnotationPositionGetter<Datum>;
    getDimensions: AnnotationDimensionsGetter<Datum>;
}) => BoundAnnotation<Datum>[];
export declare const getLinkAngle: (sourceX: number, sourceY: number, targetX: number, targetY: number) => number;
export declare const computeAnnotation: <Datum>(annotation: BoundAnnotation<Datum>) => AnnotationInstructions;
//# sourceMappingURL=compute.d.ts.map