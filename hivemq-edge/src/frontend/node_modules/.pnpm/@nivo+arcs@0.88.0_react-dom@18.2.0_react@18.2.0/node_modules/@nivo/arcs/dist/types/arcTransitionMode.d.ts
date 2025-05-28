import { Arc, DatumWithArc } from './types';
export interface ArcTransitionModeConfig {
    enter: (arc: Arc) => Arc;
    update: (arc: Arc) => Arc;
    leave: (arc: Arc) => Arc;
}
export declare const arcTransitionModes: readonly ["startAngle", "middleAngle", "endAngle", "innerRadius", "centerRadius", "outerRadius", "pushIn", "pushOut"];
export type ArcTransitionMode = (typeof arcTransitionModes)[number];
export declare const arcTransitionModeById: Record<ArcTransitionMode, ArcTransitionModeConfig>;
export interface TransitionExtra<Datum extends DatumWithArc, ExtraProps> {
    enter: (datum: Datum) => ExtraProps;
    update: (datum: Datum) => ExtraProps;
    leave: (datum: Datum) => ExtraProps;
}
export declare const useArcTransitionMode: <Datum extends DatumWithArc, ExtraProps>(mode: ArcTransitionMode, extraTransition?: TransitionExtra<Datum, ExtraProps> | undefined) => {
    enter: (datum: Datum) => {
        startAngle: number;
        endAngle: number;
        innerRadius: number;
        outerRadius: number;
        progress: number;
    };
    update: (datum: Datum) => {
        startAngle: number;
        endAngle: number;
        innerRadius: number;
        outerRadius: number;
        progress: number;
    };
    leave: (datum: Datum) => {
        startAngle: number;
        endAngle: number;
        innerRadius: number;
        outerRadius: number;
        progress: number;
    };
};
//# sourceMappingURL=arcTransitionMode.d.ts.map