import { createContext, useContext } from 'react';

// showToast(message, type?, duration?) 함수를 그대로 컨텍스트 값으로 노출한다.
export const ToastContext = createContext(() => {});

export function useToast() {
  return useContext(ToastContext);
}
