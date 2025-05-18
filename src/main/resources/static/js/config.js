window.onload = scrollToId();

// TODO document.getElementBtId('id').getAttribute('class');
// then search class element for value that determines last visited segment

function scrollToId() {
    var select = document.getElementsByClassName("custom-page-scroll")[0].id;
    var element;
    if (select == "tv") {
        element = document.getElementById("tv_paths");
    }
    if (select == "movie") {
        element = document.getElementById("movie_paths");
    }
    if (select == "backup") {
        element = document.getElementById("backup_db");
    }
    if (select == "extensions") {
        element = document.getElementById("extensions_list");
    }
    if (select != null && select != "") {
        window.scrollTo({
            top: element.getBoundingClientRect().top + window.pageYOffset - getTopHeight(),
            behavior: 'smooth'
        });
    }
};

function getTopHeight() {
    var stickyHeader = document.getElementById("top");
    var console = document.getElementById("console");
    var topMenuHeight = (stickyHeader) ? stickyHeader.offsetHeight : 0;
    var consoleHeight = (console) ? console.offsetHeight : 0;
    return topMenuHeight + consoleHeight;
}