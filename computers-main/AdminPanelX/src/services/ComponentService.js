import { showConfirmation, handlePromise } from './NotificationService';

const API_BASE_URL = 'http://localhost:8080/api/components';

/**
 * Fetches all components from the backend.
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<Array>} - A promise that resolves to the array of components.
 */
export const fetchAllComponents = async (token) => {
    const response = await fetch(API_BASE_URL, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error('Failed to fetch components.');
    return response.json();
};

/**
 * Deletes a component after user confirmation.
 * @param {object} component - The component object to delete.
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<boolean>} - A promise that resolves to `true` if deletion was successful, `false` otherwise.
 */
export const deleteComponent = async (component, token) => {
    const isConfirmed = await showConfirmation(
        'Are you sure?',
        `You are about to delete "${component.name}". This cannot be undone.`
    );

    if (!isConfirmed) return false;

    const promise = fetch(`${API_BASE_URL}/${component.id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    }).then(response => {
        if (!response.ok) throw new Error('Deletion failed.');
        return true;
    });

    handlePromise(promise, {
        loading: 'Deleting component...',
        success: `"${component.name}" deleted successfully.`,
        error: 'Could not delete component.'
    });

    return promise;
};

/**
 * Updates the stock of a component.
 * @param {string} componentId - The ID of the component to update.
 * @param {number} quantityChange - The amount to change the stock by (can be negative).
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<object>} - A promise that resolves to the updated component data.
 */
export const updateComponentStock = async (componentId, quantityChange, token) => {
    const promise = fetch(`${API_BASE_URL}/stock/${componentId}`, {
        method: 'PATCH',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ quantity: quantityChange })
    }).then(async response => {
        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Failed to update stock');
        }
        return response.json();
    });

    handlePromise(promise, {
        loading: 'Updating stock...',
        success: 'Stock updated!',
        error: (err) => err.message, // Use the specific error message from the backend
    });

    return promise;
};

/**
 * Creates a new component with an optional image.
 * THIS VERSION IS MODIFIED: It no longer uses handlePromise.
 * It now re-throws the error so the calling component can catch it and decide what to do.
 * @param {object} componentData - The JSON data for the new component.
 * @param {File | null} imageFile - The image file to upload, or null.
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<object>} - A promise that resolves to the newly created component data.
 */
export const createComponent = async (componentData, imageFile, token) => {
    const formData = new FormData();
    const componentBlob = new Blob([JSON.stringify(componentData)], {
        type: 'application/json'
    });
    formData.append('request', componentBlob);

    if (imageFile) {
        formData.append('image', imageFile);
    }
    
    // We will now handle the promise manually in the component.
    const response = await fetch(`${API_BASE_URL}/`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        },
        body: formData,
    });
    
    if (!response.ok) {
        // Try to get a specific error message from the backend
        const errorData = await response.json().catch(() => ({ message: 'Failed to create component. An unknown error occurred.' }));
        throw new Error(errorData.message || `HTTP error! Status: ${response.status}`);
    }

    // If successful, just return the JSON data
    return response.json();
};

/**
 * Fetches a single component's details by its ID.
 * This is required for the Edit Page to load the initial data.
 * @param {string} id - The ID of the component.
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<object>} - A promise that resolves to the component data object.
 */
export const getComponentById = async (id, token) => {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to fetch component details.' }));
        throw new Error(errorData.message || 'Failed to fetch component details.');
    }
    return response.json();
};

/**
 * Updates an existing component.
 * This function is specifically designed to work with the provided Spring Boot backend.
 * @param {string} id - The ID of the component to update.
 * @param {object} componentData - The component data object. Note: Do not include 'type' or 'quantity' here.
 * @param {File | null} imageFile - The new image file, or null if not changing.
 * @param {boolean} removeImage - Flag to indicate if the existing image should be removed.
 * @param {string} token - The JWT token for authorization.
 * @returns {Promise<object>} - A promise that resolves to the updated component data.
 */
export const updateComponent = async (id, componentData, imageFile, removeImage, token) => {
    const formData = new FormData();
    
    // The backend's @RequestPart("request") expects a JSON blob.
    formData.append('request', new Blob([JSON.stringify(componentData)], {
        type: "application/json"
    }));

    // Append the new image file if one was selected.
    if (imageFile) {
        formData.append('image', imageFile);
    }
    
    // The backend's @RequestParam("removeImage") is sent as a query parameter.
    const url = `${API_BASE_URL}/${id}?removeImage=${removeImage}`;

    const response = await fetch(url, {
        method: 'PUT',
        headers: {
            'Authorization': `Bearer ${token}`,
            // 'Content-Type' is set automatically by the browser for FormData. Do not set it manually.
        },
        body: formData,
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'Failed to update component. The server returned an error.' }));
        throw new Error(errorData.message || 'Failed to update component.');
    }

    return response.json();
};