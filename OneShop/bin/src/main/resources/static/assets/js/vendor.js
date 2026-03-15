$(document).ready(function() {
    // Sidebar toggle functionality
    $("#menu-toggle").on("click", function(e) {
        e.preventDefault();
        $("#wrapper").toggleClass("toggled");
    });
});