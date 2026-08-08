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

function clickBtn(btnIdToClick) {
    if (btnIdToClick) {
        document.getElementById(btnIdToClick).click()
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

function markSelectedText(textAreaId) {
    const textArea = document.getElementById(textAreaId);
    const start = textArea.selectionStart;
    const end = textArea.selectionEnd;
    if (start == null || end == null) {
        return;
    }
    const value = textArea.value;
    let rangeStart = start;
    let rangeEnd = end;

    if (start === end) {
        const isNotPartOfWord = (ch) => /[\s.,!?:;()]/.test(ch);
        let left = start;
        let right = start;

        while (left > 0 && !isNotPartOfWord(value[left - 1])) {
            left -= 1;
        }
        while (right < value.length && !isNotPartOfWord(value[right])) {
            right += 1;
        }
        if (left === right) {
            return;
        }
        rangeStart = left;
        rangeEnd = right;
    }

    const selectedText = value.slice(rangeStart, rangeEnd);
    const markedText = `[[${selectedText}]]`;
    textArea.value = value.slice(0, rangeStart) + markedText + value.slice(rangeEnd);
    const newCaret = rangeStart + markedText.length;
    textArea.setSelectionRange(newCaret, newCaret);
    textArea.focus();
}