// DOM Elements
const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const togglePasswordBtn = document.getElementById('togglePassword');
const loginBtn = document.getElementById('loginBtn');
const emailError = document.getElementById('emailError');
const passwordError = document.getElementById('passwordError');
const successMessage = document.getElementById('successMessage');
const rememberMeCheckbox = document.getElementById('rememberMe');

// Form validation patterns
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const passwordMinLength = 6;

// State management
let isFormValid = false;
let isSubmitting = false;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadSavedCredentials();
    addInputAnimations();
});

// Event listeners setup
function initializeEventListeners() {
    // Form submission
    loginForm.addEventListener('submit', handleFormSubmit);
    
    // Input validation
    emailInput.addEventListener('input', validateEmail);
    emailInput.addEventListener('blur', validateEmail);
    passwordInput.addEventListener('input', validatePassword);
    passwordInput.addEventListener('blur', validatePassword);
    
    // Password toggle
    togglePasswordBtn.addEventListener('click', togglePasswordVisibility);
    
    // Real-time form validation
    [emailInput, passwordInput].forEach(input => {
        input.addEventListener('input', validateForm);
    });
    
    // Keyboard navigation
    document.addEventListener('keydown', handleKeyboardNavigation);
    
    // Focus management
    emailInput.addEventListener('focus', clearError);
    passwordInput.addEventListener('focus', clearError);
}

// Form submission handler
async function handleFormSubmit(event) {
    event.preventDefault();
    
    if (isSubmitting) return;
    
    validateForm();
    
    if (!isFormValid) {
        shakeForm();
        return;
    }
    
    isSubmitting = true;
    showLoadingState();
    
    try {
        const formData = {
            email: emailInput.value.trim(),
            password: passwordInput.value,
            rememberMe: rememberMeCheckbox.checked
        };
        
        // Simulate API call
        const response = await simulateLogin(formData);
        
        if (response.success) {
            handleLoginSuccess(formData);
        } else {
            handleLoginError(response.message);
        }
    } catch (error) {
        handleLoginError('Network error. Please try again.');
        console.error('Login error:', error);
    } finally {
        isSubmitting = false;
        hideLoadingState();
    }
}

// Simulate login API call
function simulateLogin(formData) {
    return new Promise((resolve) => {
        setTimeout(() => {
            // Simulate different responses based on email
            if (formData.email === 'admin@example.com' && formData.password === 'password123') {
                resolve({ success: true, token: 'mock-jwt-token' });
            } else if (formData.email === 'error@example.com') {
                resolve({ success: false, message: 'Server error occurred' });
            } else {
                resolve({ success: false, message: 'Invalid email or password' });
            }
        }, 2000);
    });
}

// Email validation
function validateEmail() {
    const email = emailInput.value.trim();
    
    if (!email) {
        showError(emailError, 'Email is required');
        return false;
    }
    
    if (!emailPattern.test(email)) {
        showError(emailError, 'Please enter a valid email address');
        return false;
    }
    
    hideError(emailError);
    return true;
}

// Password validation
function validatePassword() {
    const password = passwordInput.value;
    
    if (!password) {
        showError(passwordError, 'Password is required');
        return false;
    }
    
    if (password.length < passwordMinLength) {
        showError(passwordError, `Password must be at least ${passwordMinLength} characters`);
        return false;
    }
    
    hideError(passwordError);
    return true;
}

// Form validation
function validateForm() {
    const isEmailValid = validateEmail();
    const isPasswordValid = validatePassword();
    
    isFormValid = isEmailValid && isPasswordValid;
    
    // Update button state
    if (isFormValid) {
        loginBtn.classList.add('valid');
    } else {
        loginBtn.classList.remove('valid');
    }
    
    return isFormValid;
}

// Show error message
function showError(errorElement, message) {
    errorElement.textContent = message;
    errorElement.classList.add('show');
    
    // Add error styling to input
    const inputElement = errorElement.previousElementSibling.querySelector('input');
    if (inputElement) {
        inputElement.style.borderColor = '#ff6b6b';
        inputElement.style.background = 'rgba(255, 107, 107, 0.1)';
    }
}

// Hide error message
function hideError(errorElement) {
    errorElement.classList.remove('show');
    
    // Remove error styling from input
    const inputElement = errorElement.previousElementSibling.querySelector('input');
    if (inputElement) {
        inputElement.style.borderColor = '';
        inputElement.style.background = '';
    }
}

// Clear error on focus
function clearError(event) {
    const input = event.target;
    const errorElement = input.closest('.form-group').querySelector('.error-message');
    if (errorElement) {
        hideError(errorElement);
    }
}

// Toggle password visibility
function togglePasswordVisibility() {
    const isPassword = passwordInput.type === 'password';
    passwordInput.type = isPassword ? 'text' : 'password';
    togglePasswordBtn.textContent = isPassword ? '🙈' : '👁️';
    
    // Add animation
    togglePasswordBtn.style.transform = 'scale(0.8)';
    setTimeout(() => {
        togglePasswordBtn.style.transform = 'scale(1)';
    }, 150);
}

// Show loading state
function showLoadingState() {
    loginBtn.classList.add('loading');
    loginBtn.disabled = true;
}

// Hide loading state
function hideLoadingState() {
    loginBtn.classList.remove('loading');
    loginBtn.disabled = false;
}

// Handle successful login
function handleLoginSuccess(formData) {
    // Save credentials if remember me is checked
    if (formData.rememberMe) {
        localStorage.setItem('rememberedEmail', formData.email);
    } else {
        localStorage.removeItem('rememberedEmail');
    }
    
    // Show success message
    successMessage.classList.add('show');
    
    // Simulate redirect after delay
    setTimeout(() => {
        console.log('Redirecting to dashboard...');
        // window.location.href = '/dashboard';
    }, 2000);
}

// Handle login error
function handleLoginError(message) {
    // Show error in a toast or alert
    showToast(message, 'error');
    
    // Clear password field for security
    passwordInput.value = '';
    passwordInput.focus();
}

// Load saved credentials
function loadSavedCredentials() {
    const rememberedEmail = localStorage.getItem('rememberedEmail');
    if (rememberedEmail) {
        emailInput.value = rememberedEmail;
        rememberMeCheckbox.checked = true;
        passwordInput.focus();
    }
}

// Add input animations
function addInputAnimations() {
    const inputs = document.querySelectorAll('input');
    
    inputs.forEach(input => {
        input.addEventListener('focus', function() {
            this.parentElement.classList.add('focused');
        });
        
        input.addEventListener('blur', function() {
            if (!this.value) {
                this.parentElement.classList.remove('focused');
            }
        });
        
        // Check if input has value on load
        if (input.value) {
            input.parentElement.classList.add('focused');
        }
    });
}

// Shake form animation for errors
function shakeForm() {
    loginForm.style.animation = 'shake 0.5s ease-in-out';
    setTimeout(() => {
        loginForm.style.animation = '';
    }, 500);
}

// Keyboard navigation
function handleKeyboardNavigation(event) {
    if (event.key === 'Enter' && event.target.tagName === 'INPUT') {
        const inputs = Array.from(document.querySelectorAll('input'));
        const currentIndex = inputs.indexOf(event.target);
        
        if (currentIndex < inputs.length - 1) {
            inputs[currentIndex + 1].focus();
        } else {
            loginBtn.click();
        }
    }
}

// Toast notification system
function showToast(message, type = 'info') {
    // Remove existing toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }
    
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${type === 'error' ? '❌' : 'ℹ️'}</span>
        <span class="toast-message">${message}</span>
    `;
    
    // Add toast styles
    Object.assign(toast.style, {
        position: 'fixed',
        top: '20px',
        right: '20px',
        background: type === 'error' ? 'rgba(244, 67, 54, 0.9)' : 'rgba(33, 150, 243, 0.9)',
        color: 'white',
        padding: '15px 20px',
        borderRadius: '8px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        zIndex: '1000',
        backdropFilter: 'blur(10px)',
        transform: 'translateX(100%)',
        transition: 'transform 0.3s ease'
    });
    
    document.body.appendChild(toast);
    
    // Animate in
    setTimeout(() => {
        toast.style.transform = 'translateX(0)';
    }, 100);
    
    // Auto remove after 5 seconds
    setTimeout(() => {
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }, 5000);
}

// Add shake animation to CSS dynamically
const shakeKeyframes = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
        20%, 40%, 60%, 80% { transform: translateX(5px); }
    }
`;

const styleSheet = document.createElement('style');
styleSheet.textContent = shakeKeyframes;
document.head.appendChild(styleSheet);

// Utility functions
const utils = {
    // Debounce function for performance
    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    // Format validation messages
    formatValidationMessage: function(field, rule) {
        const messages = {
            required: `${field} is required`,
            email: 'Please enter a valid email address',
            minLength: `${field} must be at least ${rule.value} characters`,
            pattern: `${field} format is invalid`
        };
        return messages[rule.type] || 'Invalid input';
    },
    
    // Sanitize input
    sanitizeInput: function(input) {
        return input.trim().replace(/[<>]/g, '');
    }
};

// Export for potential module use
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        validateEmail,
        validatePassword,
        validateForm,
        utils
    };
}