import { textMediaType } from '../../util/media-types/text-media-type';
export function containsText({
  source
}) {
  return source.types.includes(textMediaType);
}

/* Get the plain text that a user is dragging */
export function getText({
  source
}) {
  return source.getStringData(textMediaType);
}