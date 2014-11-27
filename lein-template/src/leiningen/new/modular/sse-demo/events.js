// POST messages using AJAX
$("#chat").click(function(ev) {
    console.log("posting message: " + $("#message").val())
    $.ajax({
    type: "POST",
    url: "/events/messages",
    data: "message=" + encodeURIComponent($("#message").val())})
});


// Listen for messages using HTML5 Server Sent Events (SSE)
es = new EventSource("/events/events");
es.addEventListener("message",
                    function(ev) {
                        console.log("message: " + ev.data)
                        $("#messages").append("<li>"+ev.data+"</li>");
                    });
