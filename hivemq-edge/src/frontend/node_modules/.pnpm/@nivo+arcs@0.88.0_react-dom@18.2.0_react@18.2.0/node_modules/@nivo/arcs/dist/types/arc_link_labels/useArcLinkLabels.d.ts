import { PropertyAccessor } from '@nivo/core';
import { InheritedColorConfig } from '@nivo/colors';
import { DatumWithArcAndColor } from '../types';
import { ArcLinkWithDatum, ArcLinkLabel } from './types';
/**
 * Compute arc link labels, please note that the datum should
 * contain a color in order to be able to compute the link/label text color.
 *
 * Please see `useArcLinks` for a more detailed explanation
 * about the parameters.
 */
export declare const useArcLinkLabels: <Datum extends DatumWithArcAndColor>({ data, skipAngle, offset, diagonalLength, straightLength, textOffset, label, linkColor, textColor, }: {
    data: Datum[];
    skipAngle?: number | undefined;
    offset?: number | undefined;
    diagonalLength: number;
    straightLength: number;
    textOffset: number;
    label: PropertyAccessor<Datum, string>;
    linkColor: InheritedColorConfig<Datum>;
    textColor: InheritedColorConfig<Datum>;
}) => (ArcLinkWithDatum<Datum> & Omit<ArcLinkLabel<Datum>, keyof ArcLinkWithDatum<Datum>>)[];
//# sourceMappingURL=useArcLinkLabels.d.ts.map