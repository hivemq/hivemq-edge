export function addAttribute(element, {
  attribute,
  value
}) {
  element.setAttribute(attribute, value);
  return () => element.removeAttribute(attribute);
}