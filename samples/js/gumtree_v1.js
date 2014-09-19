currentMapping = 0;

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.slice(0, str.length) == str;
  };
}

function getMappedElement(eltId) {
	if (eltId.startsWith("move-src")) {
		return eltId.replace("src","dst");  	 	
  	}
  	else {
  		return eltId.replace("dst","src");
  	}
}

function nextMapping() {
	if (currentMapping == 0) {
		currentMapping = 1;
		return "#mapping-" + currentMapping.toString();
	} else {
		currentMapping++;
		
		if ($("#mapping-" + currentMapping.toString()).length > 0) {
			return "#mapping-" + currentMapping.toString();
		} else {
			currentMapping = 1;
			return "#mapping-" + currentMapping.toString();		
		}		
	}
}

function isSrc(eltId) {
	return eltId.startsWith("move-src");
}

$("body").keypress(
	function (event) {
		console.log(event.which.toString());
		if (event.which == 110) {
			var mapping = nextMapping();
			$('html, body').animate({scrollTop: $(mapping).offset().top - 200}, 100);
		} else if (event.which == 116) {
			$('html, body').animate({scrollTop: 0}, 100);
		} else if (event.which == 98) {
			$("html, body").animate({ scrollTop: $(document).height() }, 100);
		} else if (event.which == 113) {
			window.location = "/quit";
		}
	}	
)

$("span.mv.token, span.token.upd").click(
	function(event) {
		if ($(this).hasClass("selected")) {
			$("span.mv.token, span.token.upd").removeClass("selected");
		} else {
		$("span.mv.token, span.token.upd").removeClass("selected");
		var eltId = $(this).attr("id");
		var refEltId = getMappedElement(eltId);
		$("#" + refEltId).addClass("selected");
		$(this).addClass("selected");
		var sel = "#dst";
		if (isSrc(refEltId)) var sel = "#src";
		
		$div = $(sel);
		$span = $("#" + refEltId);     		
		}
		event.stopPropagation();
	}
)

$("span.token").hover(
	function (event) {
		$(this).tooltip('show');
		event.stopPropagation();
	}, 
	function (event) {
		$(this).tooltip('hide');
		event.stopPropagation();
	}
)

$("span.add.token, span.token.del").click(
	function(event) {
		$("span.mv.token, span.token.upd").removeClass("selected");
		event.stopPropagation();
	}
)
;
