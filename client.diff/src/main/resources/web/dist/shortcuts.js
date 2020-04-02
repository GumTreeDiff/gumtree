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

$(function(){
//    $("#infos").popover(); //TODO really needed?

    $("#legend").popover();

    $("#shortcuts").popover();

    $("body").keypress(function (event) {
        switch (event.which) {
            case 116:
                $('html, body').animate({scrollTop: 0}, 100);
                break;
            case 98:
                $("html, body").animate({ scrollTop: $(document).height() }, 100);
                break;
            case 113:
                window.location = "/quit";
                break;
            case 108:
                window.location = "/list";
                break;
        }
    });
});