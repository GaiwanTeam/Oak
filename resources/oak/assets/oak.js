/*
 * Oak's sprinkles of JavaScript
 *
 * We generally try to have fairly little JavaScript, but there are some basic
 * interactions and quality of life UX things that people have come to expect,
 * so this is our attempt of providing those with a small blob of Vanilla JS.
 *
 * We generally try to follow hypermedia principles, meaning that the DOM is the
 * source of truth, we merely attach some additional functionality based on
 * elements and attributes.
 *
 * This file is loaded as a module, so functions and variables are private,
 * unless exported or assigned to a global like document/window.
 */

// Some shorthand, has the added benefit that code feels a bit more lisp-ish
const $id = (id)=>document.getElementById(id)
const $query = (sel)=>document.querySelector(sel)
const $queryAll = (sel)=>document.querySelectorAll(sel)
const $tmpl = (id)=>$id(id).content.cloneNode(true)
const $on = (el, evt, f)=>el.addEventListener(evt, f)
const $attr = (el, attr)=>el.getAttribute(attr)
const $setAttr = (el, attr, val)=>el.setAttribute(attr, val)

//////////////////////////////////////////////////////////////////////
// Selector: [type=password]

/**
 * Insert the "eye" button to toggle between password and input field
 */
function handlePasswordField(input) {
  const button = $tmpl('password-visibility-toggle').firstChild
  input.after(button)
  $on(button, 'click', visibilityToggle_onClick)
}

/**
 * Use the aria-pressed attribute to handle toggle state
 */
function visibilityToggle_onClick(e) {
  const button = e.currentTarget
  const input = button.previousElementSibling
  const isPressed = $attr(button, 'aria-pressed') === 'true'
  $setAttr(button, 'aria-pressed', !isPressed)
  $setAttr(input, 'type', isPressed ? 'password' : 'text')
  input.focus()
}

//////////////////////////////////////////////////////////////////////
// Selector: [data-confirm]
//
// Set the `data-confirm` attribute to a message that gets shown to the
// user.

function handleConfirmationDialog(el) {
  $on(el, 'click', ()=>confirm($attr(el, "oak-confirm")))
}

//////////////////////////////////////////////////////////////////////
// Selector: [data-do-remove]

function handleRemove(el) {
  $on(el, 'click', ()=>{
    const remove = $attr(el, "oak-remove")
    if ("self" === remove) {
      el.remove()
    } else if ("parent" === remove) {
      el.parentElement.remove()
    } // else selector based, implement if needed
  })
}

//////////////////////////////////////////////////////////////////////
// Backup Codes

function handleDownload(el) {
  $on(el, 'click', ()=>{
    const dataStr  = $attr(el, "oak-download")
    const codes = JSON.parse(dataStr);
    // Alignment: a row for a code
    const codesString = codes.join('\n');
    const filename = $attr(el, "oak-download-filename")
    const blob = new Blob([codesString], { type: 'text/plain' });

    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;

    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  })
}
//////////////////////////////////////////////////////////////////////
// Selector-based dispatch

const SELECTORS = {
  '[type=password]': handlePasswordField,
  '[oak-confirm]': handleConfirmationDialog,
  '[oak-remove]': handleRemove,
  '[oak-download]': handleDownload,
}

function onDOMReady(_event) {
  for (const sel in SELECTORS) {
    $queryAll(sel).forEach(SELECTORS[sel])
  }
}

document.addEventListener('DOMContentLoaded', onDOMReady);
