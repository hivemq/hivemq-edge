import { DatumWithArc } from '../types';
import { ArcLinkWithDatum } from './types';
/**
 * Compute links for an array of data containing arcs.
 *
 * This is typically used to create labels for arcs,
 * and it's used for the `useArcLinkLabels` hook.
 */
export declare const useArcLinks: <Datum extends DatumWithArc, ExtraProps extends Record<string, any> = Record<string, any>>({ data, skipAngle, offset, diagonalLength, straightLength, computeExtraProps, }: {
    data: Datum[];
    skipAngle?: number | undefined;
    offset?: number | undefined;
    diagonalLength: number;
    straightLength: number;
    computeExtraProps?: ((datum: ArcLinkWithDatum<Datum>) => ExtraProps) | undefined;
}) => (ArcLinkWithDatum<Datum> & ExtraProps)[];
//# sourceMappingURL=useArcLinks.d.ts.map