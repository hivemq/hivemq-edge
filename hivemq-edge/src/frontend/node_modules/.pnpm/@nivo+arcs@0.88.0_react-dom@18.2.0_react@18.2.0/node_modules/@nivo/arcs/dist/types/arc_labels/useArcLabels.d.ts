import { PropertyAccessor } from '@nivo/core';
import { InheritedColorConfig } from '@nivo/colors';
import { DatumWithArcAndColor } from '../types';
import { ArcCenter } from '../centers';
export interface ArcLabel<Datum extends DatumWithArcAndColor> extends ArcCenter<Datum> {
    label: string;
    textColor: string;
}
/**
 * Compute arc labels, please note that the datum should
 * contain a color in order to be able to compute the label text color.
 *
 * Please see `useArcCenters` for a more detailed explanation
 * about the parameters.
 */
export declare const useArcLabels: <Datum extends DatumWithArcAndColor>({ data, offset, skipAngle, label, textColor, }: {
    data: Datum[];
    offset?: number | undefined;
    skipAngle?: number | undefined;
    label: PropertyAccessor<Datum, string>;
    textColor: InheritedColorConfig<Datum>;
}) => (ArcCenter<Datum> & Omit<ArcLabel<Datum>, keyof ArcCenter<Datum>>)[];
//# sourceMappingURL=useArcLabels.d.ts.map