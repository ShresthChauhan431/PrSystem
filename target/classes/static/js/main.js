/**
 * PR Review System – Main JavaScript
 * Handles auto-dismiss alerts, confirm dialogs, and form submission prevention
 */

(function() {
    'use strict';

    // =========================================================================
    // Auto-dismiss flash alerts after 4 seconds
    // =========================================================================
    document.addEventListener('DOMContentLoaded', function() {
        const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
        alerts.forEach(function(alert) {
            // Only auto-dismiss if it has the dismissible class or is a flash message
            if (alert.classList.contains('alert-dismissible') || 
                alert.classList.contains('alert-success') || 
                alert.classList.contains('alert-danger') ||
                alert.classList.contains('alert-warning') ||
                alert.classList.contains('alert-info')) {
                setTimeout(function() {
                    const bsAlert = new bootstrap.Alert(alert);
                    bsAlert.close();
                }, 4000);
            }
        });
    });

    // =========================================================================
    // Confirm dialogs for destructive actions
    // =========================================================================
    document.addEventListener('DOMContentLoaded', function() {
        // Reject PR form
        const rejectForms = document.querySelectorAll('form[action*="/reject"]');
        rejectForms.forEach(function(form) {
            form.addEventListener('submit', function(e) {
                if (!confirm('Are you sure you want to reject this PR?')) {
                    e.preventDefault();
                    return false;
                }
            });
        });

        // Merge PR form
        const mergeForms = document.querySelectorAll('form[action*="/merge"]');
        mergeForms.forEach(function(form) {
            form.addEventListener('submit', function(e) {
                if (!confirm('Are you sure you want to merge this PR?')) {
                    e.preventDefault();
                    return false;
                }
            });
        });

        // Generic destructive action handler for buttons with data-confirm attribute
        const confirmButtons = document.querySelectorAll('[data-confirm]');
        confirmButtons.forEach(function(button) {
            button.addEventListener('click', function(e) {
                const message = button.getAttribute('data-confirm') || 'Are you sure?';
                if (!confirm(message)) {
                    e.preventDefault();
                    return false;
                }
            });
        });
    });

    // =========================================================================
    // Prevent double form submission on PR submit buttons
    // =========================================================================
    document.addEventListener('DOMContentLoaded', function() {
        const forms = document.querySelectorAll('form');
        forms.forEach(function(form) {
            let isSubmitting = false;
            
            form.addEventListener('submit', function(e) {
                if (isSubmitting) {
                    e.preventDefault();
                    return false;
                }

                // Find submit buttons within this form
                const submitButtons = form.querySelectorAll('button[type="submit"], input[type="submit"]');
                
                // Check if this is a PR-related form (create, update, etc.)
                const isPRForm = form.action && (
                    form.action.includes('/prs/new') ||
                    form.action.includes('/prs/') && form.action.includes('/edit') ||
                    form.action.includes('/reviews/submit')
                );

                if (isPRForm || submitButtons.length > 0) {
                    isSubmitting = true;
                    
                    // Disable submit buttons
                    submitButtons.forEach(function(btn) {
                        btn.disabled = true;
                        const originalText = btn.innerHTML || btn.value;
                        btn.setAttribute('data-original-text', originalText);
                        if (btn.tagName === 'BUTTON') {
                            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Submitting...';
                        } else {
                            btn.value = 'Submitting...';
                        }
                    });

                    // Re-enable after 5 seconds as a safety measure (in case of error)
                    setTimeout(function() {
                        isSubmitting = false;
                        submitButtons.forEach(function(btn) {
                            btn.disabled = false;
                            const originalText = btn.getAttribute('data-original-text');
                            if (originalText) {
                                if (btn.tagName === 'BUTTON') {
                                    btn.innerHTML = originalText;
                                } else {
                                    btn.value = originalText;
                                }
                            }
                        });
                    }, 5000);
                }
            });
        });
    });

    // =========================================================================
    // Utility: Show flash message programmatically
    // =========================================================================
    window.showFlashMessage = function(message, type) {
        type = type || 'info';
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-' + type + ' alert-dismissible fade show';
        alertDiv.setAttribute('role', 'alert');
        alertDiv.innerHTML = '<span>' + message + '</span>' +
            '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>';
        
        const container = document.querySelector('.container') || document.body;
        container.insertBefore(alertDiv, container.firstChild);
        
        // Auto-dismiss after 4 seconds
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alertDiv);
            bsAlert.close();
        }, 4000);
    };

})();
