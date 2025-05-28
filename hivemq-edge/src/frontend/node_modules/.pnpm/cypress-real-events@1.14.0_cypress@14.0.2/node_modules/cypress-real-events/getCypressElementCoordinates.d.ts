export type Position = "topLeft" | "top" | "topRight" | "left" | "center" | "right" | "bottomLeft" | "bottom" | "bottomRight" | {
    x: number;
    y: number;
};
type ScrollBehaviorPosition = "center" | "top" | "bottom" | "nearest";
export type ScrollBehaviorOptions = ScrollBehaviorPosition | false;
/**
 * Cypress Automation debugee is the whole tab.
 * This function returns the element coordinates relative to the whole tab root that can be used in CDP request.
 * @param jqueryEl the element to introspect
 * @param position the position of the event interaction on the element
 * @param scrollBehavior custom scroll behavior options
 */
export declare function getCypressElementCoordinates(jqueryEl: JQuery, position: Position | undefined, scrollBehavior?: ScrollBehaviorOptions): {
    x: number;
    y: number;
    frameScale: number;
};
export {};
