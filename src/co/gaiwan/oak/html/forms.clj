(ns co.gaiwan.oak.html.forms
  "Form components"
  (:require
   [clojure.string :as str]
   [co.gaiwan.oak.html.graphics :as g]
   [co.gaiwan.oak.html.tokens :refer :all]
   [co.gaiwan.oak.lib.ring-csp :as csp]
   [lambdaisland.ornament :as o]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn form
  "Drop in replacement for :form, but adds a anti-forgery-token field"
  [props & children]
  (into
   [:form props
    [:input {:type "hidden"
             :id "__anti-forgery-token"
             :name "__anti-forgery-token"
             :value
             (-> *anti-forgery-token*
                 (str/replace "&" "&amp;")
                 (str/replace "\"" "&quot;")
                 (str/replace "<" "&lt;"))}]]
   children))

(o/defstyled input-group :div
  {:margin-bottom --size-4
   :position "relative"}
  ["&:has([type=hidden])"
   {:display       "none"}]
  [:label
   {:display       "block"
    :margin-bottom --size-2
    :font-weight   600}]
  [:input
   {:width "100%"}
   [:&:focus
    {:outline :none
     :border-color --oak-green-5}]
   ["&[aria-invalid='true']" {:box-shadow (str "0 0 0 3px " --bg-panel ", 0 0 0 5px " --status-error)}]]
  [:.error {:color   --status-error
            :margin-top "0.5rem"
            :font-weight 600}]
  [".input-wrap:not(:has([aria-invalid='true'])) + .error" {:display "none"}]
  [g/circle-bang {:display       :inline-block
                  :width         "1em"
                  :height        "1em"
                  :margin-right  "0.3em"
                  :margin-bottom "-0.1em"}]
  [:.input-wrap {:position "relative"}]
  [:.eye {:position "absolute"
          :border "none"
          :background-color "transparent"
          :right "0.5rem"
          :top "50%"
          :transform "translateY(-50%)"}
   ["&[aria-pressed='false']" [g/eye {:display "block"}]]
   ["&[aria-pressed='true']" [g/eye-closed {:display "block"}]]
   [:svg {:display "none"
          :height "2em"
          :width "2em"}]]
  ([props]
   [:<>
    [:label {:for (:id props)} (:label props)]
    [:div.input-wrap
     [:input (cond-> (assoc (dissoc props :error) :aria-describedby (str (:id props) "-error"))
               (:error props)
               (assoc :aria-invalid true))]
     (when (= "password" (:type props))
       [:<>
        [:button.eye {:type "button" :aria-pressed "false"}
         [g/eye]
         [g/eye-closed]]])]
    [:div.error {:id (str (:id props) "-error")} [g/circle-bang] (:error props)]
    (when (= "password" (:type props))
      [:script {:nonce (str csp/*csp-nonce*)}
       "(function(fg) {
          function toggleAriaPressed(event, input) {
              const button = event.currentTarget;
              const isPressed = button.getAttribute('aria-pressed') === 'true';
              button.setAttribute('aria-pressed', !isPressed);
              input.setAttribute('type', isPressed ? 'password' : 'text')
              input.focus()
          }

          fg.querySelector('button').addEventListener('click', (e)=>toggleAriaPressed(e, fg.querySelector('input')));
        })(document.currentScript.parentElement)"])]))

(defn password-validate-script []
  [:script {:nonce (str csp/*csp-nonce*)}
   "
(function(fg) {
    const currentPassword = fg.querySelector('#current-password');
    const newPassword = fg.querySelector('#new-password');
    const confirmPassword = fg.querySelector('#confirm-new-password');
    const submitBtn = fg.querySelector('input[type=\"submit\"]');

    const errorDivMap = new Map();

    // init error area
    function initErrorDiv(input) {
        const existingErrorDiv = input.parentElement.parentElement.querySelector('.error');
        if (existingErrorDiv) {
            let errorMessage = existingErrorDiv.querySelector('.error-message');
            if (!errorMessage) {
                errorMessage = document.createElement('span');
                errorMessage.className = 'error-message';
                existingErrorDiv.appendChild(errorMessage);
            }
            errorDivMap.set(input, { div: existingErrorDiv, message: errorMessage });
        }
    }

    // init error div for each field
    initErrorDiv(currentPassword);
    initErrorDiv(newPassword);
    initErrorDiv(confirmPassword);

    // check password strength
    function validatePasswordStrength(password) {
        const errors = [];
        if (password.length < 8) {
            errors.push('Password must be at least 8 characters');
        }
        if (!/[A-Z]/.test(password)) {
            errors.push('Password must contain at least one uppercase letter');
        }
        if (!/[a-z]/.test(password)) {
            errors.push('Password must contain at least one lowercase letter');
        }
        if (!/[0-9]/.test(password)) {
            errors.push('Password must contain at least one number');
        }
        return errors;
    }

    // show error message
    function showError(input, message) {
        const errorData = errorDivMap.get(input);
        if (!errorData) return;

        errorData.message.textContent = message;
        errorData.div.style.display = 'flex';
        errorData.div.style.alignItems = 'center';
        errorData.div.style.gap = '0.5rem';
        input.classList.add('error-input');
        input.setAttribute('aria-invalid', 'true');
    }

    // clear error message
    function clearError(input) {
        const errorData = errorDivMap.get(input);
        if (!errorData) return;

        errorData.message.textContent = '';
        errorData.div.style.display = 'none';
        input.classList.remove('error-input');
        input.removeAttribute('aria-invalid');
    }

    // check if password match
    function checkPasswordsMatch() {
        const newPass = newPassword.value;
        const confirmPass = confirmPassword.value;

        if (confirmPass === '') {
            clearError(confirmPassword);
            return false;
        }

        if (newPass !== confirmPass) {
            showError(confirmPassword, 'Passwords do not match');
            return false;
        } else {
            clearError(confirmPassword);
            return true;
        }
    }

    // check new password strength
    function checkNewPasswordStrength() {
        const password = newPassword.value;

        if (password === '') {
            clearError(newPassword);
            return false;
        }

        const errors = validatePasswordStrength(password);

        if (errors.length > 0) {
            showError(newPassword, errors[0]);
            return false;
        } else {
            clearError(newPassword);
            return true;
        }
    }

    // New Password check when input
    newPassword.addEventListener('input', function() {
        checkNewPasswordStrength();
        if (confirmPassword.value) {
            checkPasswordsMatch();
        }
    });

    // New Password check when losing the focus
    newPassword.addEventListener('blur', function() {
        if (this.value) {
            checkNewPasswordStrength();
        }
    });

    // Confirm Password check when input
    confirmPassword.addEventListener('input', function() {
        checkPasswordsMatch();
    });

    // Confirm Password check when losing the focus
    confirmPassword.addEventListener('blur', function() {
        if (this.value) {
            checkPasswordsMatch();
        }
    });

    // check before submit
    fg.addEventListener('submit', function(e) {
        let isValid = true;

        // check current password
        if (currentPassword.value.trim() === '') {
            showError(currentPassword, 'Current password is required');
            isValid = false;
        } else {
            clearError(currentPassword);
        }

        // check new password
        if (newPassword.value.trim() === '') {
            showError(newPassword, 'New password is required');
            isValid = false;
        } else if (!checkNewPasswordStrength()) {
            isValid = false;
        }

        // check confirmed password
        if (confirmPassword.value.trim() === '') {
            showError(confirmPassword, 'Please confirm your new password');
            isValid = false;
        } else if (!checkPasswordsMatch()) {
            isValid = false;
        }

        // force the password needs to be updated
        if (isValid && newPassword.value === currentPassword.value) {
            showError(newPassword, 'New password must be different from current password');
            isValid = false;
        }

        if (!isValid) {
            e.preventDefault();
            // focus on first error field
            const firstError = fg.querySelector('[aria-invalid=" true "]');
            if (firstError) {
                firstError.focus();
            }
        } else {
            // show submitting state
            submitBtn.disabled = true;
            submitBtn.value = 'Updating...';
        }
    });

    // add CSS styles
    const style = document.createElement('style');
    style.textContent = `
        .error-message {
            color: #ef476f;
            font-size: 0.875rem;
            font-weight: 500;
            line-height: 1.4;
        }
        .error-input {
            border-color: #ef476f !important;
            box-shadow: 0 0 0 1px #ef476f !important;
        }
        input[type=\" submit \"]:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
    `;
    document.head.appendChild(style);
})(document.currentScript.parentElement);
   "])

(o/defstyled submit-disable :input
  {:width            "100%"})

(o/defstyled submit-delete :input.cautious-action.severe
  {:width            "100%"})

(o/defstyled submit :input.call-to-action
  {:width            "100%"})
