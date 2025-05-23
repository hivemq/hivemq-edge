import _slicedToArray from "@babel/runtime/helpers/slicedToArray";
import { isHoneyPotElement } from './is-honey-pot-element';
export function getElementFromPointWithoutHoneypot(client) {
  // eslint-disable-next-line no-restricted-syntax
  var _document$elementsFro = document.elementsFromPoint(client.x, client.y),
    _document$elementsFro2 = _slicedToArray(_document$elementsFro, 2),
    top = _document$elementsFro2[0],
    second = _document$elementsFro2[1];
  if (!top) {
    return null;
  }
  if (isHoneyPotElement(top)) {
    return second !== null && second !== void 0 ? second : null;
  }
  return top;
}