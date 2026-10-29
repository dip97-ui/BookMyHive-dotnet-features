import { Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import Footer from './components/Footer.jsx';
import Home from './pages/Home.jsx';
import EventDetail from './pages/EventDetail.jsx';
import SeatSelection from './pages/SeatSelection.jsx';
import MyBookings from './pages/MyBookings.jsx';
import AuthPage from './pages/AuthPage.jsx';
import Admin from './pages/Admin.jsx';
import { useAuth } from './context.jsx';

function RequireAuth({ admin = false, children }) {
  const { user, isAdmin } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (admin && !isAdmin) {
    return (
      <main className="page narrow">
        <h1>Admins only</h1>
        <p className="muted">Your account doesn’t have access to the admin console.</p>
        <Link className="btn" to="/">Back to home</Link>
      </main>
    );
  }
  return children;
}

function NotFound() {
  return (
    <main className="page narrow">
      <h1>This page isn’t in the hive</h1>
      <p className="muted">The link may be old or mistyped.</p>
      <Link className="btn" to="/">Browse movies and plays</Link>
    </main>
  );
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/events/:id" element={<EventDetail />} />
        <Route path="/shows/:id/seats" element={<SeatSelection />} />
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/register" element={<AuthPage mode="register" />} />
        <Route path="/bookings" element={<RequireAuth><MyBookings /></RequireAuth>} />
        <Route path="/admin" element={<RequireAuth admin><Admin /></RequireAuth>} />
        <Route path="*" element={<NotFound />} />
      </Routes>
      <Footer />
    </>
  );
}
