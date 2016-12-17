

function setFlash(state) {
    $.ajax({
        url: "/flash/"+state,
    });
}

function app(startStop) {
    //get package name
    var package = $('#package').val();

    $.ajax({
        url: "/app/"+startStop+"/"+package,
    });
}

