import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";
import { Button } from "./Button";

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `px-3 py-2 rounded-md text-sm font-medium ${
    isActive ? "bg-blue-100 text-blue-700" : "text-gray-600 hover:bg-gray-100"
  }`;

export function Layout() {
  const { logout } = useAuth();
  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200">
        <div className="max-w-5xl mx-auto flex items-center justify-between px-4 py-3">
          <span className="font-semibold text-gray-900">ResumeRocket</span>
          <nav className="flex items-center gap-1">
            <NavLink to="/profile" className={navLinkClass}>
              Profile
            </NavLink>
            <NavLink to="/tailor" className={navLinkClass}>
              Tailor a Resume
            </NavLink>
            <NavLink to="/resumes" className={navLinkClass}>
              My Resumes
            </NavLink>
            <Button variant="secondary" onClick={logout} className="ml-2">
              Sign out
            </Button>
          </nav>
        </div>
      </header>
      <main className="max-w-5xl mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
