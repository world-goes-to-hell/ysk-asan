import { useId, useState } from 'react';

import styles from '../../styles/autocomplete.module.css';

/**
 * 자동완성 입력. getSuggestions(value) → 문자열 배열을 드롭다운으로 보여주고
 * 클릭/방향키(↑↓ Enter Esc)로 선택한다. value/onChange 는 상위가 관리(controlled).
 */
export default function AutocompleteInput({
  value,
  onChange,
  getSuggestions,
  className = '',
  ...inputProps
}) {
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(-1);
  const listId = useId();

  const suggestions = open ? getSuggestions(value).slice(0, 8) : [];

  const close = () => {
    setOpen(false);
    setHighlight(-1);
  };

  const pick = (s) => {
    onChange(s);
    close();
  };

  const onKeyDown = (e) => {
    if (!open || suggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlight((h) => (h + 1) % suggestions.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlight((h) => (h <= 0 ? suggestions.length - 1 : h - 1));
    } else if (e.key === 'Enter' && highlight >= 0) {
      e.preventDefault();
      pick(suggestions[highlight]);
    } else if (e.key === 'Escape') {
      close();
    }
  };

  return (
    <div className={styles.wrap}>
      <input
        {...inputProps}
        className={`form-input ${className}`}
        value={value}
        autoComplete="off"
        role="combobox"
        aria-expanded={open && suggestions.length > 0}
        aria-controls={listId}
        aria-autocomplete="list"
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
          setHighlight(-1);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(close, 120)}
        onKeyDown={onKeyDown}
      />
      {open && suggestions.length > 0 && (
        <ul className={styles.list} id={listId} role="listbox">
          {suggestions.map((s, i) => (
            <li
              key={s}
              role="option"
              aria-selected={i === highlight}
              className={`${styles.item} ${i === highlight ? styles.active : ''}`}
              onMouseDown={(e) => {
                e.preventDefault(); // blur 전에 선택 처리
                pick(s);
              }}
              onMouseEnter={() => setHighlight(i)}
            >
              {s}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
