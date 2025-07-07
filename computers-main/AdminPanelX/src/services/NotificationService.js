import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';

// Initialize SweetAlert with React content capabilities
const MySwal = withReactContent(Swal);

// A reusable configuration for our dark-themed modals
const swalDarkConfig = {
    background: 'var(--secondary-bg)',
    color: 'var(--text-primary)',
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
};

// --- EXPORTED FUNCTIONS ---

/**
 * Displays a confirmation dialog before performing a dangerous action.
 * @param {string} title - The title of the dialog (e.g., 'Are you sure?').
 * @param {string} text - The descriptive text for the dialog.
 * @returns {Promise<boolean>} - A promise that resolves to `true` if confirmed, `false` otherwise.
 */
export const showConfirmation = async (title, text) => {
    const result = await MySwal.fire({
        title,
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Yes, proceed!',
        ...swalDarkConfig
    });
    return result.isConfirmed;
};


/**
 * A wrapper for a standard success toast.
 * @param {string} message - The message to display.
 */
export const notifySuccess = (message) => {
    toast.success(message);
};

/**
 * A wrapper for a standard error toast.
 * @param {string} message - The message to display.
 */
export const notifyError = (message) => {
    toast.error(message);
};

/**
 * A wrapper for handling async operations with loading/success/error toasts.
 * @param {Promise} promise - The async function to execute.
 * @param {object} messages - The messages for each state.
 * @param {string} messages.loading - The message to show while loading.
 * @param {string} messages.success - The message to show on success.
 * @param {string} messages.error - The message to show on error.
 */
export const handlePromise = (promise, messages) => {
    toast.promise(promise, messages);
};
