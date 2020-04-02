/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

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

$(function() {
    $("body").keypress(function (event) {
        switch(event.which) {
            case 110:
                var mapping = nextMapping();
                $('html, body').animate({scrollTop: $(mapping).offset().top - 200}, 100);
                break;
        }
    });

    // highlight
    $("span.mv.token, span.token.upd").click(function(event) {
        if ($(this).hasClass("selected")) {
            $("span.mv.token, span.token.upd").removeClass("selected");
        } else {
            $("span.mv.token, span.token.upd").removeClass("selected");
            var eltId = $(this).attr("id");
            var refEltId = getMappedElement(eltId);
            $("#" + refEltId).addClass("selected");
            $(this).addClass("selected");
            var sel = "#dst";
            if (isSrc(refEltId))
                var sel = "#src";
            $div = $(sel);
            $span = $("#" + refEltId);
        }
        event.stopPropagation();
    });

    $("span.add.token, span.token.del").click(function(event) {
        $("span.mv.token, span.token.upd").removeClass("selected");
        event.stopPropagation();
    });

    // tooltip
    $("span.token").hover(
    	function (event) {
    		$(this).tooltip('show');
    		event.stopPropagation();
    	},
    	function (event) {
    		$(this).tooltip('hide');
    		event.stopPropagation();
    	}
    );
});
