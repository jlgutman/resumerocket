import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { getStoredToken, setStoredToken } from "../../services/apiClient";
import * as authApi from "./authApi";

interface AuthContextValue {
  token: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getStoredToken());

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      isAuthenticated: token !== null,
      login: async (email, password) => {
        const response = await authApi.login(email, password);
        setStoredToken(response.token);
        setToken(response.token);
      },
      register: async (email, password, fullName) => {
        const response = await authApi.register(email, password, fullName);
        setStoredToken(response.token);
        setToken(response.token);
      },
      logout: () => {
        setStoredToken(null);
        setToken(null);
      },
    }),
    [token],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
