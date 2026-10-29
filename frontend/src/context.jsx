import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, tokenStore } from './api.js';

const USER_KEY = 'bmh_user';
const CITY_KEY = 'bmh_city';
export const CITIES = ['Mumbai', 'Delhi', 'Bengaluru', 'Pune'];

const AuthContext = createContext(null);
const CityContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY));
    } catch {
      return null;
    }
  });

  const logout = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  const authenticate = useCallback(async (path, payload) => {
    const data = await api.post(path, payload);
    const { token, ...profile } = data;
    tokenStore.set(token);
    localStorage.setItem(USER_KEY, JSON.stringify(profile));
    setUser(profile);
    return profile;
  }, []);

  const login = useCallback((email, password) => authenticate('/auth/login', { email, password }), [authenticate]);
  const register = useCallback((name, email, password) => authenticate('/auth/register', { name, email, password }), [authenticate]);

  // The API answers 401 once the token expires: drop the stale session.
  useEffect(() => {
    window.addEventListener('bmh-session-expired', logout);
    return () => window.removeEventListener('bmh-session-expired', logout);
  }, [logout]);

  const value = useMemo(() => ({ user, isAdmin: user?.role === 'ADMIN', login, register, logout }), [user, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function CityProvider({ children }) {
  const [city, setCityState] = useState(() => localStorage.getItem(CITY_KEY) || CITIES[0]);
  const setCity = (c) => {
    localStorage.setItem(CITY_KEY, c);
    setCityState(c);
  };
  return <CityContext.Provider value={{ city, setCity }}>{children}</CityContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
export const useCity = () => useContext(CityContext);
