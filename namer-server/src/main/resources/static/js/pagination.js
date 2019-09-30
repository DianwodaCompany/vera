$(function() {

    $('body').on('click', '.pagination-panel .prev', function(e) {

        e.preventDefault();

        var params = getQueryParams();
        var page = parseInt($(this).data('page'), 10);

        if($(this).hasClass('disabled')) {
            return;
        }

        params['page'] = page - 1;

        window.location = window.location.pathname + '?' + $.param(params);

    }).on('click', '.pagination-panel .next', function(e) {

        e.preventDefault();

        var params = getQueryParams();
        var page = parseInt($(this).data('page'), 10);

        if($(this).hasClass('disabled')) {
            return;
        }

        params['page'] = page + 1;

        window.location = window.location.pathname + '?' + $.param(params);

    });


    function getQueryParams() {
        var query = window.location.search.substring(1);
        var vars = query.split("&");
        var params = {};
        for (var i=0;i<vars.length;i++) {
            var pair = vars[i].split("=");
            params[pair[0]] = decodeURIComponent(pair[1]);
        }

        return params;
    }
});