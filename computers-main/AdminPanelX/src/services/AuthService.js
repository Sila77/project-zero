const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Attempts to log in a user with the provided credentials.
 * @param {string} email - The user's email.
 * @param {string} password - The user's password.
 * @returns {Promise<string>} - A promise that resolves to the JWT token on success.
 * @throws {Error} - Throws an error if the login fails, which can be caught by the component.
 */
export const loginUser = async (email, password) => {
  const response = await fetch(`${API_BASE_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    // The component's catch block will handle this error.
    throw new Error('Login failed. Please check your credentials.');
  }

  const data = await response.json();
  // The service's job is to return the essential data, in this case, the token.
  return data.token;
};