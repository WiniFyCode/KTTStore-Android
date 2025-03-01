import React, { createContext, useState, useContext, useEffect } from 'react';

const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
  // Lấy theme từ localStorage hoặc mặc định là 'tet'
  const [theme, setTheme] = useState(() => {
    const savedTheme = localStorage.getItem('theme');
    // Chỉ chấp nhận các giá trị hợp lệ: 'tet' hoặc 'normal'
    if (savedTheme && ['tet', 'normal'].includes(savedTheme)) {
      return savedTheme;
    }
    // Nếu giá trị không hợp lệ, xóa khỏi localStorage và dùng giá trị mặc định
    localStorage.removeItem('theme');
    return 'normal';
  });

  // Lưu theme vào localStorage khi thay đổi
  useEffect(() => {
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prevTheme => prevTheme === 'tet' ? 'normal' : 'tet');
  };

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme phải được sử dụng trong ThemeProvider');
  }
  return context;
};
