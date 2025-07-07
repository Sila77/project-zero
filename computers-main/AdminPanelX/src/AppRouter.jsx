import React from 'react';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Spinner } from 'react-bootstrap';

import AdminLayout from './layouts/AdminLayout/AdminLayout';
import LoginPage from './pages/Login/LoginPage';
import Dashboard from './pages/Dashboard/Dashboard';
import ComponentsPage from './pages/Components/ComponentsPage';
import AddComponentPage from './pages/AddComponent/AddComponentPage';
import EditComponentPage from './pages/EditComponent/EditComponentPage';


const PrivateRoute = () => {
  const { user, isAdmin, token } = useAuth();

  // Show a loading spinner while the token is being verified on initial load
  if (!token && !user) {
    // If there's no token at all, just redirect
     return <Navigate to="/login" replace />;
  }
  
  if (token && !user) {
    // If there's a token but the user object isn't populated yet, it's loading.
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spinner animation="border" />
      </div>
    );
  }

  // If the user is loaded but is not an admin, deny access
  if (!isAdmin) {
    return (
      <div style={{ textAlign: 'center', marginTop: '5rem' }}>
        <h1>Access Denied</h1>
        <p>You do not have permission to view this page.</p>
      </div>
    );
  }

  // If all checks pass, render the requested admin page layout
  return <AdminLayout />;
};

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      
      {/* All admin routes are nested under the PrivateRoute */}
      <Route path="/" element={<PrivateRoute />}>
        {/* The PrivateRoute renders AdminLayout, which contains an <Outlet> */}
        {/* These child routes will be rendered inside the AdminLayout's <Outlet> */}
        <Route index element={<Dashboard />} />
        <Route path="components" element={<ComponentsPage />} />
        <Route path="add-component" element={<AddComponentPage />} />
        <Route path="edit-component/:id" element={<EditComponentPage />} />

      </Route>

      {/* Optional: A catch-all route for 404 Not Found */}
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
};

export default AppRouter;