import { createContext, useContext } from "react";
import type { User, Catalog } from "./api";
export const Context = createContext<{
  user: User | null;
  catalog: Catalog;
  reload: () => Promise<void>;
}>({
  user: null,
  catalog: { campuses: [], categories: [], locations: [] },
  reload: async () => {},
});
export function useApp() {
  return useContext(Context);
}
