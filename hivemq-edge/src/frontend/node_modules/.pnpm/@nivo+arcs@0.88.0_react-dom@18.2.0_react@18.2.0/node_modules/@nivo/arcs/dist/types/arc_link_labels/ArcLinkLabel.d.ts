/// <reference types="react" />
import { SpringValue, Interpolation } from '@react-spring/web';
import { DatumWithArcAndColor } from '../types';
export interface ArcLinkLabelProps<Datum extends DatumWithArcAndColor> {
    datum: Datum;
    label: string;
    style: {
        path: Interpolation<string>;
        thickness: number;
        textPosition: Interpolation<string>;
        textAnchor: Interpolation<'start' | 'end'>;
        linkColor: SpringValue<string>;
        opacity: SpringValue<number>;
        textColor: SpringValue<string>;
    };
}
export declare const ArcLinkLabel: <Datum extends DatumWithArcAndColor>({ label, style, }: ArcLinkLabelProps<Datum>) => JSX.Element;
//# sourceMappingURL=ArcLinkLabel.d.ts.map