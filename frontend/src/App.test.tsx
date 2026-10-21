// @vitest-environment jsdom
import { afterEach, beforeEach, describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
  cleanup,
  fireEvent,
  configure,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom/vitest";
import { App } from "./App";
configure({ asyncUtilTimeout: 10000 });
const catalog = {
  campuses: [{ id: 1, name: "Demo Campus" }],
  categories: [{ id: 1, name: "Electronics" }],
  locations: [{ id: 1, name: "Library", campusId: 1 }],
};
beforeEach(() => {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: string, init?: RequestInit) => {
      if (input === "/api/catalog")
        return new Response(JSON.stringify(catalog));
      if (input === "/api/auth/csrf")
        return new Response(
          JSON.stringify({ token: "test", headerName: "X-CSRF-TOKEN" }),
        );
      if (input === "/api/auth/login")
        return new Response(
          JSON.stringify({ message: "Credentials could not be verified" }),
          { status: 401 },
        );
      if (input === "/api/users/me")
        return new Response(JSON.stringify({ message: "Sign in" }), {
          status: 401,
        });
      throw new Error("Unexpected request " + input + " " + init?.method);
    }),
  );
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});
describe("Foundly user interactions", () => {
  it("shows a useful signed-out home with actual categories and no invented reports", async () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );
    await screen.findByRole("heading", { name: /Lost something/ });
    expect(screen.getByText("Electronics")).toBeInTheDocument();
    expect(
      screen.getByText("Your campus noticeboard is one sign-in away."),
    ).toBeInTheDocument();
  });
  it("protects the dashboard and provides login fields", async () => {
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <App />
      </MemoryRouter>,
    );
    expect(
      await screen.findByRole("heading", { name: "Welcome back." }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Email address")).toBeInTheDocument();
  });
  it("shows a server authentication error without navigating away", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/login"]}>
        <App />
      </MemoryRouter>,
    );
    await screen.findByLabelText("Email address");
    await user.type(
      screen.getByLabelText("Email address"),
      "student@example.test",
    );
    await user.type(screen.getByLabelText("Password"), "long-test-password");
    await user.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Credentials could not be verified",
    );
    expect(screen.getByRole("button", { name: "Sign in" })).toBeEnabled();
  });
  it("supports navigation to the safety guide", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );
    await screen.findByRole("heading", { name: /Lost something/ });
    await user.click(screen.getByRole("link", { name: "Our safety guide" }));
    expect(
      await screen.findByRole("heading", {
        name: "Good reunions start with a little care.",
      }),
    ).toBeInTheDocument();
  });
  it("shows an initial loading state", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise(() => {})),
    );
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByRole("status")).toHaveTextContent("Loading");
  });
});

describe("Authenticated dashboard", () => {
  it("can switch between claims and reports without rendering a stale result", async () => {
    const student = {
      id: 1,
      name: "Campus student",
      role: "USER",
      campusId: 1,
      campusName: "Demo Campus",
    };
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string) => {
        if (input === "/api/users/me")
          return new Response(JSON.stringify(student));
        if (input === "/api/catalog")
          return new Response(JSON.stringify(catalog));
        if (input.startsWith("/api/reports?"))
          return new Response(
            JSON.stringify({ content: [], total: 0, page: 0, size: 12 }),
          );
        if (input.startsWith("/api/claims?"))
          return new Response(JSON.stringify([]));
        throw new Error("Unexpected request " + input);
      }),
    );
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/dashboard"]}>
        <App />
      </MemoryRouter>,
    );
    await screen.findByText(/Your reports will appear here/);
    await user.click(screen.getByRole("button", { name: "My claims" }));
    await screen.findByText("No claims here yet.");
    await user.click(screen.getByRole("button", { name: "My reports" }));
    expect(
      await screen.findByText(/Your reports will appear here/),
    ).toBeInTheDocument();
  });
});

describe("Report creation", () => {
  it("submits report fields to the backend and opens the saved item", async () => {
    const student = {
      id: 1,
      name: "Campus student",
      role: "USER",
      campusId: 1,
      campusName: "Demo Campus",
    };
    const saved = {
      id: 91,
      ownerId: 1,
      type: "LOST",
      title: "Green notebook",
      categoryId: 1,
      category: "Electronics",
      colour: "Green",
      description: "A plain green notebook lost after class",
      locationId: 1,
      location: "Library",
      eventDate: "2026-10-01",
      status: "ACTIVE",
      imageUrl: null,
      createdAt: "2026-10-01T00:00:00Z",
    };
    const requests: Record<string, unknown>[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: string, init?: RequestInit) => {
        if (input === "/api/users/me")
          return new Response(JSON.stringify(student));
        if (input === "/api/catalog")
          return new Response(JSON.stringify(catalog));
        if (input === "/api/auth/csrf")
          return new Response(
            JSON.stringify({ token: "test", headerName: "X-CSRF-TOKEN" }),
          );
        if (input === "/api/reports" && init?.method === "POST") {
          requests.push(JSON.parse(String(init.body)));
          return new Response(JSON.stringify(saved));
        }
        if (input === "/api/reports/91")
          return new Response(JSON.stringify(saved));
        if (input === "/api/matches?reportId=91") return new Response("[]");
        throw new Error("Unexpected request " + input);
      }),
    );
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/report/lost"]}>
        <App />
      </MemoryRouter>,
    );
    await user.type(
      await screen.findByLabelText("Item title"),
      "Green notebook",
    );
    await user.selectOptions(screen.getByLabelText("Category"), "1");
    await user.type(screen.getByLabelText("Description"), saved.description);
    await user.selectOptions(screen.getByLabelText("Campus location"), "1");
    fireEvent.change(screen.getByLabelText("Date lost"), {
      target: { value: "2026-10-01" },
    });
    expect(screen.getByLabelText("Date lost")).toHaveValue("2026-10-01");
    await user.click(screen.getByRole("button", { name: "Post report" }));
    await screen.findByRole("heading", { name: "Green notebook" });
    expect(requests).toHaveLength(1);
    expect(requests[0]).toMatchObject({
      type: "LOST",
      title: "Green notebook",
      categoryId: 1,
      locationId: 1,
      eventDate: "2026-10-01",
    });
  });
});
