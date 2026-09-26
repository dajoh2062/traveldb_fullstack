import { useLayoutEffect, useRef } from "react";

// Scroll only the listbox: scrollIntoView would also move the page under the input.
export default function useActiveOption(activeId, isOpen) {
  const listRef = useRef(null);

  useLayoutEffect(() => {
    if (!isOpen || !activeId) return;
    const list = listRef.current;
    const option = document.getElementById(activeId);
    if (!list || !option || !list.contains(option)) return;

    const listBounds = list.getBoundingClientRect();
    const optionBounds = option.getBoundingClientRect();
    const top = listBounds.top + list.clientTop;
    const bottom = top + list.clientHeight;
    if (optionBounds.top < top) list.scrollTop += optionBounds.top - top;
    else if (optionBounds.bottom > bottom) list.scrollTop += optionBounds.bottom - bottom;
  }, [activeId, isOpen]);

  return listRef;
}
