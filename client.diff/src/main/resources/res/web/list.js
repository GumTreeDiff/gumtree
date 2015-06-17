if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.slice(0, str.length) == str;
  };
}

$("body").keypress(
	function (event) {
		if (event.which == 113) {
			window.location = "/quit";
		}
	}	
)