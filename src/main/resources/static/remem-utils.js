function preventDefaultOnEnterAction(event, preventDefault, ctrl, btnIdToClick) {
    if (event.keyCode === 13) {
        if (preventDefault) {
            event.preventDefault()
        }
        if (btnIdToClick !== null && (!ctrl || ctrl && event.ctrlKey)) {
            document.getElementById(btnIdToClick).click()
        }
    }
}

function selectNextOption(selId) {
    const sel = document.getElementById(selId)
    const selIdx = sel.options.selectedIndex
    if (selIdx === sel.options.length-1) {
        sel.options[0].selected = true
    } else {
        sel.options[selIdx+1].selected = true
    }
}

function toggleExactMatch(event, selId) {
    if (event.keyCode === 192 && event.ctrlKey) {
        event.preventDefault()
        selectNextOption(selId)
    }
}