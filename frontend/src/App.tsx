import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./features/auth/ProtectedRoute";
import { Layout } from "./components/Layout";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import ProfilePage from "./pages/ProfilePage";
import TailoringPage from "./pages/TailoringPage";
import VersionHistoryPage from "./pages/VersionHistoryPage";
import ResumeDetailPage from "./pages/ResumeDetailPage";
import ComparePage from "./pages/ComparePage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/profile" replace />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/tailor" element={<TailoringPage />} />
          <Route path="/resumes" element={<VersionHistoryPage />} />
          <Route path="/resumes/compare" element={<ComparePage />} />
          <Route path="/resumes/:id" element={<ResumeDetailPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/profile" replace />} />
    </Routes>
  );
}
