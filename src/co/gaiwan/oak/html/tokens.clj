(ns co.gaiwan.oak.html.tokens
  (:require
   [garden.stylesheet :as gs]
   [lambdaisland.ornament :as o]))

(o/defprop --font-system-ui "system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans,sans-serif")

(o/defprop --white "#fff")
(o/defprop --gray-0 "#f8f9fa")
(o/defprop --gray-1 "#f1f3f5")
(o/defprop --gray-2 "#e9ecef")
(o/defprop --gray-3 "#dee2e6")
(o/defprop --gray-4 "#ced4da")
(o/defprop --gray-5 "#adb5bd")
(o/defprop --gray-6 "#868e96")
(o/defprop --gray-7 "#495057")
(o/defprop --gray-8 "#343a40")
(o/defprop --gray-9 "#212529")
(o/defprop --gray-10 "#16191d")
(o/defprop --gray-11 "#0d0f12")
(o/defprop --gray-12 "#030507")
(o/defprop --black "#000")

(o/defprop --oak-green-0 "#f4fde2")
(o/defprop --oak-green-1 "#E6F2D7")
(o/defprop --oak-green-2 "#D8E7CC")
(o/defprop --oak-green-3 "#CADCC1")
(o/defprop --oak-green-4 "#BCD1B6")
(o/defprop --oak-green-5 "#AEC6AB")
(o/defprop --oak-green-6 "#A0BBA0")
(o/defprop --oak-green-7 "#92B095")
(o/defprop --oak-green-8 "#84A58A")
(o/defprop --oak-green-9 "#769A7F")
(o/defprop --oak-green-10 "#688F74")
(o/defprop --oak-green-11 "#5A8469")
(o/defprop --oak-green-12 "#4e765c")

(o/defprop --blue-0 "#e7f5ff")
(o/defprop --blue-1 "#d0ebff")
(o/defprop --blue-2 "#a5d8ff")
(o/defprop --blue-3 "#74c0fc")
(o/defprop --blue-4 "#4dabf7")
(o/defprop --blue-5 "#339af0")
(o/defprop --blue-6 "#228be6")
(o/defprop --blue-7 "#1c7ed6")
(o/defprop --blue-8 "#1971c2")
(o/defprop --blue-9 "#1864ab")
(o/defprop --blue-10 "#145591")
(o/defprop --blue-11 "#114678")
(o/defprop --blue-12 "#0d375e")

(o/defprop --yellow-0 "#fff9db")
(o/defprop --yellow-1 "#fff3bf")
(o/defprop --yellow-2 "#ffec99")
(o/defprop --yellow-3 "#ffe066")
(o/defprop --yellow-4 "#ffd43b")
(o/defprop --yellow-5 "#fcc419")
(o/defprop --yellow-6 "#fab005")
(o/defprop --yellow-7 "#f59f00")
(o/defprop --yellow-8 "#f08c00")
(o/defprop --yellow-9 "#e67700")
(o/defprop --yellow-10 "#b35c00")
(o/defprop --yellow-11 "#804200")
(o/defprop --yellow-12 "#663500")

(o/defprop --red-0 "#fff5f5")
(o/defprop --red-1 "#ffe3e3")
(o/defprop --red-2 "#ffc9c9")
(o/defprop --red-3 "#ffa8a8")
(o/defprop --red-4 "#ff8787")
(o/defprop --red-5 "#ff6b6b")
(o/defprop --red-6 "#fa5252")
(o/defprop --red-7 "#f03e3e")
(o/defprop --red-8 "#e03131")
(o/defprop --red-9 "#c92a2a")

;; Sizes

(o/defprop --size-000 "-.5rem")
(o/defprop --size-00 "-.25rem")
(o/defprop --size-1 ".25rem")
(o/defprop --size-2 ".5rem")
(o/defprop --size-3 "1rem")
(o/defprop --size-4 "1.25rem")
(o/defprop --size-5 "1.5rem")
(o/defprop --size-6 "1.75rem")
(o/defprop --size-7 "2rem")
(o/defprop --size-8 "3rem")
(o/defprop --size-9 "4rem")
(o/defprop --size-10 "5rem")
(o/defprop --size-11 "7.5rem")
(o/defprop --size-12 "10rem")
(o/defprop --size-13 "15rem")
(o/defprop --size-14 "20rem")
(o/defprop --size-15 "30rem")

(o/defprop --radius-1 "2px")
(o/defprop --radius-2 "5px")
(o/defprop --radius-3 "1rem")
(o/defprop --radius-4 "2rem")
(o/defprop --radius-5 "4rem")
(o/defprop --radius-6 "8rem")

(o/defprop --surface-bg "The main background of the page/app." --gray-2)
(o/defprop --panel-bg "Background for layered elements (e.g., cards, panels)." --white)
(o/defprop --interactive-bg "Background for hover/active states on backgrounds." --gray-2)
(o/defprop --border-subtle "Faint lines, dividers, or subtle separators." --gray-4)

(o/defprop --surface-text "Default text color for the main surface background." --gray-12)
(o/defprop --panel-text "Text color used in layered panels; slightly less dominant than surface-text." --gray-8)
(o/defprop --text-subtle "Secondary text, captions, or placeholder text." --gray-6)
(o/defprop --text-inverted "Text color used on dark or accent backgrounds (e.g., buttons)." --gray-0)

(o/defprop --action-primary "Main button/link color." --blue-6)
(o/defprop --status-success "Background/icon for positive feedback." --oak-green-6)
(o/defprop --status-error "Background/icon for negative feedback or errors." --red-6)
(o/defprop --status-warning "Background/icon for caution or warnings." --yellow-7)
(o/defprop --status-info "Background/icon for informational messages." --blue-6)

(o/defrules dark-mode-tokens
  (gs/at-media
   {:prefers-color-scheme 'dark}
   [":where(html)"
    {;; Interface & Backgrounds (Inverted Grays)
     --surface-bg     --gray-10
     --panel-bg       --gray-12
     --interactive-bg --gray-11
     --border-subtle  --gray-7

     ;; Text & Icons (Inverted Grays)
     --surface-text  --gray-1
     --panel-text    --gray-4
     --text-subtle   --gray-6
     --text-inverted --gray-12

     ;; Status & Action (Lighter Hues to Pop on Dark BG)
     --action-primary --blue-4
     --status-success --oak-green-4
     --status-error   --red-4
     --status-warning --yellow-4
     --status-info    --blue-4}]))
