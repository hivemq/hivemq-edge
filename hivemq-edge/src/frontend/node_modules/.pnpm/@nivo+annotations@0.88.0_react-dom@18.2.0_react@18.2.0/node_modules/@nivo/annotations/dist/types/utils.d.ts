import { AnnotationSpec, CircleAnnotationSpec, DotAnnotationSpec, Note, NoteCanvas, NoteSvg, RectAnnotationSpec } from './types';
export declare const isSvgNote: <Datum>(note: Note<Datum>) => note is NoteSvg<Datum>;
export declare const isCanvasNote: <Datum>(note: Note<Datum>) => note is NoteCanvas<Datum>;
export declare const isCircleAnnotation: <Datum>(annotationSpec: AnnotationSpec<Datum>) => annotationSpec is CircleAnnotationSpec<Datum>;
export declare const isDotAnnotation: <Datum>(annotationSpec: AnnotationSpec<Datum>) => annotationSpec is DotAnnotationSpec<Datum>;
export declare const isRectAnnotation: <Datum>(annotationSpec: AnnotationSpec<Datum>) => annotationSpec is RectAnnotationSpec<Datum>;
//# sourceMappingURL=utils.d.ts.map