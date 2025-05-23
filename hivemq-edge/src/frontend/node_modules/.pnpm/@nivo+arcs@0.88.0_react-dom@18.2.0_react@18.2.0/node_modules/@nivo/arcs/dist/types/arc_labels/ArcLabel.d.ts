/// <reference types="react" />
import { SpringValue, Interpolation } from '@react-spring/web';
import { DatumWithArcAndColor } from '../types';
export interface ArcLabelProps<Datum extends DatumWithArcAndColor> {
    datum: Datum;
    label: string;
    style: {
        progress: SpringValue<number>;
        transform: Interpolation<string>;
        textColor: string;
    };
}
export declare const ArcLabel: <Datum extends DatumWithArcAndColor>({ label, style, }: ArcLabelProps<Datum>) => JSX.Element;
//# sourceMappingURL=ArcLabel.d.ts.map