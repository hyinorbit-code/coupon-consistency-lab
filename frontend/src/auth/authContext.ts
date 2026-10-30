import { createContext } from 'react'

export type AuthState = {
  token: string | null
  signIn: (token: string) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
