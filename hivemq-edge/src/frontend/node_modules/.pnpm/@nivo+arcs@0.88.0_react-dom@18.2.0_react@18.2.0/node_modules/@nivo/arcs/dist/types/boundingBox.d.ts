/**
 * Computes the bounding box for a circle arc.
 *
 * Assumptions:
 *   - Anywhere the arc intersects an axis will be a max or a min.
 *   - If the arc doesn't intersect an axis, then the center
 *     will be one corner of the bounding rectangle,
 *     and this is the only case when it will be.
 *   - The only other possible extreme points of the sector to consider
 *     are the endpoints of the radii.
 *
 * This script was built within the help of this answer on stackoverflow:
 *   https://stackoverflow.com/questions/1336663/2d-bounding-box-of-a-sector
 */
export declare const computeArcBoundingBox: (centerX: number, centerY: number, radius: number, startAngle: number, endAngle: number, includeCenter?: boolean) => {
    points: [number, number][];
    x: number;
    y: number;
    width: number;
    height: number;
};
//# sourceMappingURL=boundingBox.d.ts.map