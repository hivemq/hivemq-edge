/// <reference types="react" />
import { BarDatum, BarCanvasProps } from './types';
export type ResponsiveBarCanvasProps<RawDatum extends BarDatum> = Omit<BarCanvasProps<RawDatum>, 'height' | 'width'>;
export declare const ResponsiveBarCanvas: import("react").ForwardRefExoticComponent<ResponsiveBarCanvasProps<BarDatum> & import("react").RefAttributes<HTMLCanvasElement>>;
//# sourceMappingURL=ResponsiveBarCanvas.d.ts.map