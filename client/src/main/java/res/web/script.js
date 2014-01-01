if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.slice(0, str.length) == str;
  };
}

$("body").keypress(
	function (event) {
		if (event.which == 116) {
			$('html, body').animate({scrollTop: 0}, 100);
		} else if (event.which == 98) {
			$("html, body").animate({ scrollTop: $(document).height() }, 100);
		} else if (event.which == 113) {
			window.location = "/quit";
		} else if (event.which == 108) {
			window.location = "/list";
		}
	}	
)

$("#infos").popover()