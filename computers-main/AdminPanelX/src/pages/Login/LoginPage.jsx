import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Card, Form, Button, Alert } from 'react-bootstrap';
import { BsShieldLockFill } from 'react-icons/bs';


import { loginUser } from '../../services/AuthService';

import './LoginPage.css';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  // --- (Step 2) SIMPLIFY THE LOGIN HANDLER ---
  const handleLogin = async (e) => {
    e.preventDefault();
    setError(''); // Clear previous errors

    try {
      // The API call is now neatly handled by the service
      const token = await loginUser(email, password);
      
      // The component's responsibility is to handle the successful result
      login(token); // Update auth context
      navigate('/'); // Redirect the user

    } catch (err) {
      // The component catches the error thrown by the service and updates the UI
      setError(err.message);
    }
  };

  // The JSX for the form remains exactly the same
  return (
    <div className="login-container">
      <Card className="login-card">
        <Card.Body>
          <div className="text-center mb-4">
            <BsShieldLockFill className="login-icon" />
            <h2 className="login-title mt-2">Admin Panel</h2>
          </div>
          
          {error && <Alert variant="danger" className="login-alert">{error}</Alert>}
          
          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-3">
              <Form.Label>Email</Form.Label>
              <Form.Control
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="login-input"
              />
            </Form.Group>
            <Form.Group className="mb-4">
              <Form.Label>Password</Form.Label>
              <Form.Control
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="login-input"
              />
            </Form.Group>
            <Button className="w-100 login-button" type="submit" variant="primary">
              Log In
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}

export default LoginPage;