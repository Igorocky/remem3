function preventDefaultOnEnterAction(event, btnIdToClick) {
    if(event.keyCode === 13){
        event.preventDefault();
        if (btnIdToClick !== null) {
            document.getElementById(btnIdToClick).click();
        }
    }
}