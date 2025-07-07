const API_BASE_URL = 'http://localhost:8080/api/lookups';

/**
 * Fetches all lookup data needed for the forms (sockets, form factors, etc.).
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<object>} - A promise that resolves to the lookup data object.
 */
export const fetchAllLookups = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch form lookup data.');
    return response.json();
};